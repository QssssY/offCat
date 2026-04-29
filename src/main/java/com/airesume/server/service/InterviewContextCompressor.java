package com.airesume.server.service;

import com.airesume.server.config.AiTokenLimitConfig;
import com.airesume.server.util.TokenEstimator;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

/**
 * 面试上下文压缩服务
 *
 * 所属模块：AI Token 优化模块 - 面试场景
 * 职责：对模拟面试的历史对话进行压缩和摘要，降低长对话场景下的 token 消耗
 * 用途：当面试轮次增多时，避免将完整历史对话全量传入 AI，改用摘要+最近轮次的方式
 *
 * 【压缩策略】
 * 1. 分层压缩：超过阈值轮次后，将早期对话压缩为摘要，保留最近 N 轮完整对话
 * 2. 评价专用压缩：生成评价报告时，使用更激进的压缩策略（摘要+最近 6 轮）
 * 3. 主题提取：从对话中提取涉及的技术关键词，便于快速了解面试范围
 *
 * 【配置依赖】
 * 通过 AiTokenLimitConfig 读取压缩阈值：
 * - historySummaryThreshold：触发压缩的最小轮次（默认 6 轮）
 * - recentMessagesToKeep：保留最近完整对话的轮数（默认 3 轮）
 * - interviewRoundMax：单轮对话最大 token 数
 * - interviewEvaluationMax：评价报告最大 token 数
 *
 * @author AI Resume Team
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class InterviewContextCompressor {

    private final AiTokenLimitConfig tokenLimitConfig;

    /**
     * 压缩面试历史对话
     *
     * @param history     完整历史对话列表
     * @param currentRound 当前对话轮次（用户消息数）
     * @return 压缩后的对话列表（摘要 + 最近完整对话）
     *
     * 【压缩逻辑】
     * 1. 若当前轮次 < threshold（默认 6），不压缩，返回原文
     * 2. 若总 token 未超限，不压缩，返回原文
     * 3. 否则将前 (history.size() - keepRecent) 轮压缩为摘要
     * 4. 摘要格式：[历史对话摘要 - 前 X 轮] + 每轮简要内容
     * 5. 保留最近 keepRecent 轮（默认 3 轮）完整对话
     */
    public List<InterviewAiService.ChatMessageItem> compressHistory(
            List<InterviewAiService.ChatMessageItem> history,
            int currentRound) {
        if (history == null || history.isEmpty()) {
            return history;
        }

        int threshold = tokenLimitConfig.getHistorySummaryThreshold();
        int keepRecent = tokenLimitConfig.getRecentMessagesToKeep();

        // 基于总消息数判断是否达到阈值（而不是user消息数）
        if (history.size() < threshold) {
            return history;
        }

        int totalTokens = estimateHistoryTokens(history);
        int maxTokens = tokenLimitConfig.getInterviewRoundMax();

        // token 未超限且总消息数未达阈值，不压缩
        if (totalTokens <= maxTokens && history.size() < threshold) {
            return history;
        }

        int summaryCount = history.size() - keepRecent;
        if (summaryCount <= 0) {
            return history;
        }

        // 提取最近 keepRecent 轮完整对话
        List<InterviewAiService.ChatMessageItem> recentMessages = history.subList(
                history.size() - keepRecent, history.size());

        // 生成早期对话摘要
        StringBuilder summaryBuilder = new StringBuilder();
        summaryBuilder.append("[历史对话摘要 - 前").append(summaryCount).append("轮]\n");

        for (int i = 0; i < summaryCount; i++) {
            InterviewAiService.ChatMessageItem item = history.get(i);
            if ("user".equals(item.role())) {
                // 候选人回答摘要，限制 80 字
                String brief = summarizeMessage(item.content(), 80);
                summaryBuilder.append("- 候选人：").append(brief).append("\n");
            } else if ("assistant".equals(item.role())) {
                // 面试官提问摘要，限制 60 字
                String brief = summarizeMessage(item.content(), 60);
                summaryBuilder.append("  面试官：").append(brief).append("\n");
            }
        }

        String summaryContent = summaryBuilder.toString();
        int summaryTokens = TokenEstimator.estimateTokens(summaryContent);
        int recentTokens = estimateHistoryTokens(recentMessages);

        log.info("面试历史对话已压缩：原始{}轮/{}token，压缩为摘要+最近{}轮/{}token",
                history.size(), totalTokens, keepRecent, summaryTokens + recentTokens);

        // 组装压缩后的对话：system 摘要 + 最近完整对话
        List<InterviewAiService.ChatMessageItem> compressed = new ArrayList<>();
        compressed.add(new InterviewAiService.ChatMessageItem("system", summaryContent));
        compressed.addAll(recentMessages);

        return compressed;
    }

    /**
     * 生成对话整体摘要
     *
     * @param history 完整历史对话列表
     * @return 对话摘要文本（含轮次统计、主题提取）
     *
     * 【用途】
     * 用于面试评价报告生成时，提供对话的整体概览，
     * 替代全量对话历史，大幅降低 token 消耗。
     */
    public String generateConversationSummary(List<InterviewAiService.ChatMessageItem> history) {
        if (history == null || history.isEmpty()) {
            return "";
        }

        StringBuilder summary = new StringBuilder();
        summary.append("[面试摘要]\n");

        int userCount = 0;
        int assistantCount = 0;
        List<String> keyTopics = new ArrayList<>();

        // 统计对话轮次和提取主题
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

        // 添加涉及的技术主题（最多 5 个）
        if (!keyTopics.isEmpty()) {
            summary.append("- 涉及主题：")
                    .append(String.join("、", keyTopics.subList(0, Math.min(5, keyTopics.size()))))
                    .append("\n");
        }

        return summary.toString();
    }

    /**
     * 为评价报告压缩历史对话
     *
     * @param history         完整历史对话列表
     * @param existingSummary 已有的阶段性摘要（可为 null）
     * @return 压缩后的对话列表
     *
     * 【压缩逻辑】
     * 1. 若总 token 未超限，返回原文
     * 2. 若已有摘要，将摘要作为 system 消息前置
     * 3. 保留最近 6 轮完整对话（比常规压缩多保留几轮，确保评价有足够依据）
     * 4. 返回：system 摘要 + 最近 6 轮
     */
    public List<InterviewAiService.ChatMessageItem> compressForEvaluation(
            List<InterviewAiService.ChatMessageItem> history,
            String existingSummary) {
        if (history == null || history.isEmpty()) {
            return history;
        }

        int totalTokens = estimateHistoryTokens(history);
        int maxTokens = tokenLimitConfig.getInterviewEvaluationMax();

        // token 未超限，不压缩
        if (totalTokens <= maxTokens) {
            return history;
        }

        // 评价报告保留最近 6 轮（比常规对话多，确保评价有充分依据）
        int keepRecent = Math.min(6, history.size());
        List<InterviewAiService.ChatMessageItem> recentMessages = history.subList(
                history.size() - keepRecent, history.size());

        List<InterviewAiService.ChatMessageItem> compressed = new ArrayList<>();

        // 若已有阶段性摘要，前置到 system 消息
        if (existingSummary != null && !existingSummary.isBlank()) {
            compressed.add(new InterviewAiService.ChatMessageItem("system",
                    "[前期对话摘要]\n" + existingSummary));
        }

        compressed.addAll(recentMessages);

        int compressedTokens = estimateHistoryTokens(compressed);
        log.info("面试评价历史已压缩：原始{}轮/{}token，压缩后{}轮/{}token",
                history.size(), totalTokens, compressed.size(), compressedTokens);

        return compressed;
    }

    /**
     * 估算历史对话的 token 数量
     *
     * @param history 历史对话列表
     * @return 预估总 token 数
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
     * 对单条消息内容进行摘要
     *
     * @param content   原始消息内容
     * @param maxLength 最大保留字符数
     * @return 摘要后的文本（超限则截断并追加...）
     */
    private String summarizeMessage(String content, int maxLength) {
        if (content == null || content.isBlank()) {
            return "";
        }
        // 规范化空白字符
        String trimmed = content.trim().replaceAll("\\s+", " ");
        if (trimmed.length() <= maxLength) {
            return trimmed;
        }
        return trimmed.substring(0, maxLength) + "...";
    }

    /**
     * 从文本中提取技术主题关键词
     *
     * @param content 文本内容
     * @param topics  主题列表（会追加到该列表中）
     *
     * 【提取规则】
     * 匹配预定义的技术关键词列表，用于了解面试涉及的技术范围。
     * 目前覆盖：Java 生态、数据库、中间件、云原生、AI 等领域。
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
