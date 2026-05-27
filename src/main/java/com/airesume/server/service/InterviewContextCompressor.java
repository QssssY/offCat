package com.airesume.server.service;

import com.airesume.server.config.AiTokenLimitConfig;
import com.airesume.server.util.TokenEstimator;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 面试上下文压缩服务
 *
 * 所属模块：AI Token 优化模块 - 面试场景
 * 职责：对模拟面试的历史对话进行压缩和摘要，降低长对话场景下的 token 消耗
 * 用途：当面试轮次增多时，避免将完整历史对话全量传入 AI，改用摘要+最近轮次的方式
 *
 * 【压缩策略】
 * 1. AI 摘要压缩（优先）：调用 AI 将早期对话压缩为连贯摘要，保留技术要点和关键结论
 * 2. 截断兜底（降级）：AI 不可用时，回退到硬截断模式（用户 80 字、面试官 60 字）
 * 3. 评价专用压缩：生成评价报告时，用 AI 生成覆盖全部轮次的深度摘要
 *
 * 【配置依赖】
 * 通过 AiTokenLimitConfig 读取：
 * - historySummaryThreshold：触发压缩的最小消息数（默认 6）
 * - recentMessagesToKeep：常规压缩保留最近消息数（默认 3）
 * - evaluationRecentMessagesToKeep：评价报告保留最近消息数（默认 10）
 * - aiSummaryEnabled：是否启用 AI 摘要（默认 true）
 * - aiSummaryTimeoutMs：AI 摘要超时时间（默认 30000ms）
 *
 * @author AI Resume Team
 */
@Service
@Slf4j
public class InterviewContextCompressor {

    private final AiTokenLimitConfig tokenLimitConfig;
    private final AiChatClient aiChatClient;
    private final Clock clock;

    /** 摘要缓存：sessionId → 上次生成的摘要信息，避免每轮都重新调用 AI */
    private final ConcurrentHashMap<String, CachedSummary> summaryCache = new ConcurrentHashMap<>();

    /** 摘要缓存最长保留时间，防止用户中断面试后缓存长期占用内存。 */
    private static final Duration SUMMARY_CACHE_TTL = Duration.ofHours(2);

    /** 获取重新摘要间隔（从配置读取，默认 6 条消息 = 3 轮对话） */
    private int getResummarizeThreshold() {
        return tokenLimitConfig.getResummarizeInterval();
    }

    @Autowired
    public InterviewContextCompressor(AiTokenLimitConfig tokenLimitConfig, AiChatClient aiChatClient) {
        this(tokenLimitConfig, aiChatClient, Clock.systemDefaultZone());
    }

    InterviewContextCompressor(AiTokenLimitConfig tokenLimitConfig, AiChatClient aiChatClient, Clock clock) {
        this.tokenLimitConfig = tokenLimitConfig;
        this.aiChatClient = aiChatClient;
        this.clock = clock;
    }

    /** 摘要缓存条目 */
    private record CachedSummary(String summary, int messageCount, Instant updatedAt) {}

    // ==================== AI 摘要 Prompt 常量 ====================

    /** 面试对话压缩摘要指令 — 保留技术要点、优劣表现、关键结论 */
    private static final String COMPRESS_HISTORY_SYSTEM_PROMPT =
            "你是一个面试对话摘要助手。请将以下面试对话压缩为一段连贯的中文摘要。\n\n" +
            "要求：\n" +
            "1. 保留所有关键技术问答要点（具体的技术概念、候选人的回答内容、面试官的评价判断）\n" +
            "2. 保留候选人回答的优劣表现（哪里答得好，哪里有明显不足）\n" +
            "3. 保留对话中的关键结论和判断（如'候选人对XX掌握扎实'、'候选人对XX概念理解模糊'）\n" +
            "4. 使用简洁的叙述体，按轮次顺序描述\n" +
            "5. 不要省略重要的技术细节，宁可稍长也不要丢失信息\n" +
            "6. 输出纯文本摘要，不要加标题或格式标记\n" +
            "7. 摘要总长度控制在 500 字以内";

    /** 评价报告深度摘要指令 — 覆盖全部轮次，保留评分依据 */
    private static final String COMPRESS_EVALUATION_SYSTEM_PROMPT =
            "你是一个面试对话深度摘要助手。请将以下完整面试对话压缩为一份全面的评估参考摘要。\n\n" +
            "要求：\n" +
            "1. 覆盖面试的所有轮次，不可遗漏任何一轮的关键信息\n" +
            "2. 对每一轮，记录：面试官的问题要点、候选人的回答核心内容、回答质量判断\n" +
            "3. 明确记录候选人的技术强项和薄弱环节（附具体问题和回答作为证据）\n" +
            "4. 记录候选人暴露的任何关键问题（逻辑矛盾、知识盲区、表述模糊等）\n" +
            "5. 整体面试表现趋势（是否越答越差、是否稳定、是否有明显卡壳点）\n" +
            "6. 此摘要将用于生成面试评价报告，必须保留足够细节支持评分和点评\n" +
            "7. 输出纯文本，按轮次顺序组织\n" +
            "8. 摘要总长度控制在 800 字以内";

    /** 发送给 AI 做摘要的最大消息数，防止长对话超出 token 限制 */
    private static final int MAX_MESSAGES_FOR_AI_SUMMARY = 30;

    /** AI 摘要结果的最大字符数，防止摘要本身比原文还长 */
    private static final int MAX_SUMMARY_LENGTH = 1500;

    // ==================== 公开方法 ====================

    /**
     * 压缩面试历史对话
     *
     * @param history      完整历史对话列表
     * @param currentRound 当前对话轮次（用户消息数）
     * @return 压缩后的对话列表（摘要 + 最近完整对话）
     *
     * 【压缩逻辑】
     * 1. 若消息数 < threshold 或 token 未超限，不压缩
     * 2. 优先使用 AI 摘要（若 aiSummaryEnabled=true）
     * 3. AI 不可用时降级到截断模式
     * 4. 组装：system 摘要消息 + 最近 N 轮完整对话
     */
    public List<InterviewAiService.ChatMessageItem> compressHistory(
            List<InterviewAiService.ChatMessageItem> history,
            int currentRound,
            String sessionId) {
        if (history == null || history.isEmpty()) {
            return history;
        }

        int threshold = tokenLimitConfig.getHistorySummaryThreshold();
        int keepRecent = tokenLimitConfig.getRecentMessagesToKeep();

        // 未达到压缩阈值，不压缩
        if (history.size() < threshold) {
            return history;
        }

        int totalTokens = estimateHistoryTokens(history);
        int maxTokens = tokenLimitConfig.getInterviewRoundMax();

        // token 未超限，不压缩
        if (totalTokens <= maxTokens) {
            return history;
        }

        int summaryCount = history.size() - keepRecent;
        if (summaryCount <= 0) {
            return history;
        }

        // 早期消息太少，不值得调 AI 摘要，直接保留全部
        int minMessagesForSummary = 3;
        if (summaryCount < minMessagesForSummary) {
            log.debug("早期消息仅{}条（<{}），跳过压缩", summaryCount, minMessagesForSummary);
            return history;
        }

        // 提取需要压缩的早期对话和保留的最近对话
        List<InterviewAiService.ChatMessageItem> earlyMessages = history.subList(0, summaryCount);
        List<InterviewAiService.ChatMessageItem> recentMessages = history.subList(
                history.size() - keepRecent, history.size());

        // 检查是否可以复用缓存的摘要
        String summaryContent = null;
        CachedSummary cached = sessionId != null ? summaryCache.get(sessionId) : null;
        if (cached != null && cached.summary() != null) {
            int newMessagesSinceCache = summaryCount - cached.messageCount();
            int resummarizeThreshold = getResummarizeThreshold();
            if (newMessagesSinceCache < resummarizeThreshold) {
                // 新消息不足，复用缓存摘要
                log.debug("复用缓存摘要：缓存覆盖{}条，当前{}条，新增{}条（未达阈值{}）",
                        cached.messageCount(), summaryCount, newMessagesSinceCache, resummarizeThreshold);
                summaryContent = cached.summary();
            }
        }

        // 缓存未命中或需要重新摘要
        if (summaryContent == null) {
            List<InterviewAiService.ChatMessageItem> messagesForAi = earlyMessages.size() > MAX_MESSAGES_FOR_AI_SUMMARY
                    ? earlyMessages.subList(earlyMessages.size() - MAX_MESSAGES_FOR_AI_SUMMARY, earlyMessages.size())
                    : earlyMessages;
            String aiResult = aiSummarize(messagesForAi, COMPRESS_HISTORY_SYSTEM_PROMPT);
            if (aiResult != null) {
                summaryContent = "[历史对话摘要 - AI 生成]\n" + aiResult;
                // 更新缓存
                if (sessionId != null) {
                    summaryCache.put(sessionId, new CachedSummary(summaryContent, summaryCount, Instant.now(clock)));
                }
            } else {
                summaryContent = buildTruncatedSummary(earlyMessages);
            }
        }

        int summaryTokens = TokenEstimator.estimateTokens(summaryContent);
        int recentTokens = estimateHistoryTokens(recentMessages);
        int compressedTotal = summaryTokens + recentTokens;
        log.info("面试历史对话已压缩：原始{}轮/{}token，压缩为摘要+最近{}轮/{}token",
                history.size(), totalTokens, keepRecent, compressedTotal);

        // 【防退化】压缩后反而更长时，返回原始历史
        if (compressedTotal >= totalTokens) {
            log.warn("压缩后 token 反而增加（{}->{}），跳过压缩，使用原始历史", totalTokens, compressedTotal);
            return history;
        }

        // 组装压缩后的对话：system 摘要 + 最近完整对话
        List<InterviewAiService.ChatMessageItem> compressed = new ArrayList<>();
        compressed.add(new InterviewAiService.ChatMessageItem("system", summaryContent));
        compressed.addAll(recentMessages);

        return compressed;
    }

    /**
     * 清除指定会话的摘要缓存（面试结束时调用，防止内存泄漏）
     *
     * @param sessionId 会话 ID
     */
    public void evictCache(String sessionId) {
        if (sessionId != null) {
            summaryCache.remove(sessionId);
            log.debug("已清除会话 {} 的摘要缓存", sessionId);
        }
    }

    /**
     * 定时清理过期摘要缓存，兜底处理用户直接离开页面导致 evictCache 未触发的场景。
     */
    @Scheduled(fixedDelayString = "${app.interview.summary-cache-cleanup-interval-ms:600000}")
    public void cleanupExpiredSummaryCacheScheduled() {
        cleanupExpiredSummaryCache();
    }

    /**
     * 清理超过 TTL 的摘要缓存，返回清理数量便于单元测试和运行日志观测。
     */
    int cleanupExpiredSummaryCache() {
        Instant expireBefore = Instant.now(clock).minus(SUMMARY_CACHE_TTL);
        int beforeSize = summaryCache.size();
        summaryCache.entrySet().removeIf(entry -> entry.getValue().updatedAt().isBefore(expireBefore));
        int cleaned = beforeSize - summaryCache.size();
        if (cleaned > 0) {
            log.debug("已清理过期面试摘要缓存 {} 条，剩余 {} 条", cleaned, summaryCache.size());
        }
        return cleaned;
    }

    /**
     * 为评价报告压缩历史对话
     *
     * @param history         完整历史对话列表
     * @param existingSummary 已有的阶段性摘要（可为 null）
     * @return 压缩后的对话列表
     *
     * 【压缩逻辑】
     * 1. 若消息数不足（<=6），不压缩
     * 2. 优先使用 AI 生成覆盖全部轮次的深度摘要（评价报告需要全局视角，不受 token 门槛限制）
     * 3. AI 不可用时降级到主题提取摘要
     * 4. 保留最近 evaluationRecentMessagesToKeep 条完整对话（默认 10 条）
     * 5. 返回：system 摘要 + 最近 N 条完整对话
     */
    public List<InterviewAiService.ChatMessageItem> compressForEvaluation(
            List<InterviewAiService.ChatMessageItem> history,
            String existingSummary) {
        if (history == null || history.isEmpty()) {
            return history;
        }

        // 消息数太少，无需压缩
        int threshold = tokenLimitConfig.getHistorySummaryThreshold();
        if (history.size() <= threshold) {
            return history;
        }

        // 保留最近 N 条消息（默认 10 条，比常规压缩多，确保评价有充分依据）
        int keepRecent = Math.min(tokenLimitConfig.getEvaluationRecentMessagesToKeep(), history.size());
        List<InterviewAiService.ChatMessageItem> recentMessages = history.subList(
                history.size() - keepRecent, history.size());

        List<InterviewAiService.ChatMessageItem> compressed = new ArrayList<>();

        // 生成评价摘要
        String summaryText = null;
        if (existingSummary != null && !existingSummary.isBlank()) {
            // 已有阶段性摘要，直接使用
            summaryText = existingSummary;
        } else {
            // 使用 AI 生成深度摘要（评价报告需要全局视角，不设 token 门槛）
            List<InterviewAiService.ChatMessageItem> messagesForAi = history.size() > MAX_MESSAGES_FOR_AI_SUMMARY
                    ? history.subList(history.size() - MAX_MESSAGES_FOR_AI_SUMMARY, history.size())
                    : history;
            summaryText = aiSummarize(messagesForAi, COMPRESS_EVALUATION_SYSTEM_PROMPT);
            if (summaryText == null) {
                // 降级：主题提取摘要
                summaryText = generateConversationSummary(history);
            }
        }

        if (summaryText != null && !summaryText.isBlank()) {
            compressed.add(new InterviewAiService.ChatMessageItem("system",
                    "[评估参考摘要]\n" + summaryText));
        }

        compressed.addAll(recentMessages);

        int totalTokens = estimateHistoryTokens(history);
        int compressedTokens = estimateHistoryTokens(compressed);
        log.info("面试评价历史已压缩：原始{}轮/{}token，压缩后{}轮/{}token",
                history.size(), totalTokens, compressed.size(), compressedTokens);

        // 【防退化】压缩后反而更长时，返回原始历史
        if (compressedTokens >= totalTokens) {
            log.warn("压缩后 token 反而增加（{}->{}），跳过压缩，使用原始历史", totalTokens, compressedTokens);
            return history;
        }

        return compressed;
    }

    /**
     * 生成对话整体摘要（降级兜底用，不含 AI 调用）
     *
     * @param history 完整历史对话列表
     * @return 对话摘要文本（含轮次统计、主题提取）
     */
    private String generateConversationSummary(List<InterviewAiService.ChatMessageItem> history) {
        if (history == null || history.isEmpty()) {
            return "";
        }

        StringBuilder summary = new StringBuilder();
        summary.append("[面试摘要]\n");

        int userCount = 0;
        int assistantCount = 0;
        List<String> keyTopics = new ArrayList<>();

        for (InterviewAiService.ChatMessageItem item : history) {
            if ("user".equals(item.role())) {
                userCount++;
                extractTopics(item.content(), keyTopics);
            } else if ("assistant".equals(item.role())) {
                assistantCount++;
            }
        }

        summary.append("- 对话轮次：").append(userCount).append("轮\n");
        summary.append("- 面试官提问：").append(assistantCount).append("次\n");

        if (!keyTopics.isEmpty()) {
            summary.append("- 涉及主题：")
                    .append(String.join("、", keyTopics.subList(0, Math.min(5, keyTopics.size()))))
                    .append("\n");
        }

        return summary.toString();
    }

    // ==================== AI 摘要核心逻辑 ====================

    /**
     * 调用 AI 对对话列表进行摘要
     *
     * @param messages     需要摘要的消息列表
     * @param systemPrompt 摘要指令（COMPRESS_HISTORY_SYSTEM_PROMPT 或 COMPRESS_EVALUATION_SYSTEM_PROMPT）
     * @return AI 生成的摘要文本；若 AI 不可用或调用失败，返回 null
     */
    private String aiSummarize(List<InterviewAiService.ChatMessageItem> messages, String systemPrompt) {
        if (!tokenLimitConfig.isAiSummaryEnabled()) {
            log.debug("AI 摘要已禁用，跳过");
            return null;
        }

        try {
            String formatted = formatMessagesForSummary(messages);
            // 拼接结果过长时截断，防止超出模型上下文窗口
            int maxFormattedChars = 12000;
            if (formatted.length() > maxFormattedChars) {
                log.warn("摘要输入过长({}字)，截断到{}字", formatted.length(), maxFormattedChars);
                formatted = formatted.substring(formatted.length() - maxFormattedChars);
            }
            int timeoutMs = tokenLimitConfig.getAiSummaryTimeoutMs();
            log.info("调用 AI 摘要：消息数={}, 输入长度={}字, 超时={}ms", messages.size(), formatted.length(), timeoutMs);

            String result = aiChatClient.chat(systemPrompt, formatted, timeoutMs);

            if (result != null && !result.isBlank() && result.length() > 20) {
                // 防止 AI 返回过长摘要，截断到上限
                if (result.length() > MAX_SUMMARY_LENGTH) {
                    log.warn("AI 摘要过长({}字)，截断到{}字", result.length(), MAX_SUMMARY_LENGTH);
                    result = result.substring(0, MAX_SUMMARY_LENGTH) + "...";
                }
                log.info("AI 摘要成功，长度={}字", result.length());
                return result;
            }

            log.warn("AI 摘要返回结果过短或为空，降级到截断模式");
            return null;
        } catch (Exception e) {
            log.warn("AI 摘要调用失败，降级到截断模式: {}", e.getMessage());
            return null;
        }
    }

    /**
     * 将消息列表格式化为摘要用的文本格式
     *
     * @param messages 消息列表
     * @return 格式化文本，如 "第1轮 面试官：xxx\n候选人：xxx\n\n第2轮 ..."
     */
    private String formatMessagesForSummary(List<InterviewAiService.ChatMessageItem> messages) {
        StringBuilder sb = new StringBuilder();
        int round = 0;

        for (int i = 0; i < messages.size(); i++) {
            InterviewAiService.ChatMessageItem item = messages.get(i);
            if ("assistant".equals(item.role())) {
                round++;
                sb.append("第").append(round).append("轮 面试官：").append(item.content()).append("\n");
            } else if ("user".equals(item.role())) {
                sb.append("候选人：").append(item.content()).append("\n\n");
            } else if ("system".equals(item.role())) {
                // 跳过 system 消息，不纳入摘要
            }
        }

        return sb.toString();
    }

    // ==================== 截断降级逻辑 ====================

    /**
     * 构建截断式摘要（降级兜底，当 AI 不可用时使用）
     *
     * @param messages 需要压缩的消息列表
     * @return 截断后的摘要文本
     */
    private String buildTruncatedSummary(List<InterviewAiService.ChatMessageItem> messages) {
        StringBuilder summaryBuilder = new StringBuilder();
        summaryBuilder.append("[历史对话摘要 - 前").append(messages.size()).append("轮]\n");

        for (InterviewAiService.ChatMessageItem item : messages) {
            if ("user".equals(item.role())) {
                String brief = summarizeMessage(item.content(), 80);
                summaryBuilder.append("- 候选人：").append(brief).append("\n");
            } else if ("assistant".equals(item.role())) {
                String brief = summarizeMessage(item.content(), 60);
                summaryBuilder.append("  面试官：").append(brief).append("\n");
            }
        }

        return summaryBuilder.toString();
    }

    /**
     * 对单条消息内容进行截断（降级兜底用）
     *
     * @param content   原始消息内容
     * @param maxLength 最大保留字符数
     * @return 截断后的文本（超限则截断并追加...）
     */
    private String summarizeMessage(String content, int maxLength) {
        if (content == null || content.isBlank()) {
            return "";
        }
        String trimmed = content.trim().replaceAll("\\s+", " ");
        if (trimmed.length() <= maxLength) {
            return trimmed;
        }
        return trimmed.substring(0, maxLength) + "...";
    }

    // ==================== 工具方法 ====================

    /**
     * 估算历史对话的 token 数量
     */
    private int estimateHistoryTokens(List<InterviewAiService.ChatMessageItem> history) {
        if (history == null || history.isEmpty()) {
            return 0;
        }
        int total = 0;
        for (InterviewAiService.ChatMessageItem item : history) {
            total += TokenEstimator.estimateTokens(item.role());
            total += TokenEstimator.estimateTokens(item.content());
        }
        return total;
    }

    /**
     * 从文本中提取技术主题关键词（降级兜底用）
     */
    private void extractTopics(String content, List<String> topics) {
        if (content == null || content.isBlank()) {
            return;
        }

        String[] techKeywords = {
                "Java", "Spring", "MySQL", "Redis", "Kafka", "Docker", "K8s", "Kubernetes",
                "微服务", "分布式", "高并发", "缓存", "数据库", "架构", "算法",
                "前端", "后端", "全栈", "DevOps", "AI", "机器学习"
        };

        for (String keyword : techKeywords) {
            if (content.toLowerCase().contains(keyword.toLowerCase()) && !topics.contains(keyword)) {
                topics.add(keyword);
            }
        }
    }
}
