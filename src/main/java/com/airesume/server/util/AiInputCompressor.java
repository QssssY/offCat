package com.airesume.server.util;

import lombok.experimental.UtilityClass;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.regex.Pattern;

/**
 * AI 输入压缩工具类
 *
 * 所属模块：AI Token 优化模块 - 基础设施层
 * 职责：对发送给 AI 大模型的输入文本进行压缩和结构化处理
 * 用途：减少冗余内容、去除重复信息、提取关键数据，降低 token 消耗
 *
 * 【压缩策略】
 * 1. 空白规范化：去除多余空行、连续空格、首尾空白
 * 2. 内容去重：去除完全重复的行
 * 3. 简历结构化压缩：按章节保留关键信息，压缩描述性文字
 * 4. 项目描述压缩：保留含技术栈和量化数据的句子
 * 5. JD 关键词提取：提取含能力关键词的句子
 *
 * 【使用场景】
 * - 简历诊断前压缩简历文本
 * - 面试润色前压缩 JD 描述
 * - 评价报告前压缩对话历史
 *
 * @author AI Resume Team
 */
@UtilityClass
public class AiInputCompressor {

    /** 匹配 3 个及以上连续换行符，替换为 2 个换行（保留段落分隔） */
    private static final Pattern MULTIPLE_BLANK_LINES = Pattern.compile("\n{3,}");
    /** 匹配 2 个及以上连续空格或制表符，替换为单个空格 */
    private static final Pattern MULTIPLE_SPACES = Pattern.compile("[ \t]{2,}");
    /** 匹配末尾多余换行符 */
    private static final Pattern TRAILING_BLANK_LINES = Pattern.compile("\n+$");
    /** 匹配开头多余换行符 */
    private static final Pattern LEADING_BLANK_LINES = Pattern.compile("^\n+");

    /** 简历关键章节关键词，用于识别简历结构 */
    private static final Set<String> RESUME_KEY_SECTIONS = Set.of(
            "个人信息", "基本信息", "姓名", "联系方式", "电话", "邮箱", "邮箱地址",
            "教育背景", "学历", "毕业院校", "专业", "工作经历", "工作经验", "工作年限",
            "项目经历", "项目经验", "项目描述", "项目名称", "技术栈", "职责", "成果",
            "技能", "专业技能", "技术技能", "自我评价", "个人总结", "求职意向"
    );

    /** 中文姓名匹配模式（2~4 个中文字符） */
    private static final Pattern CHINESE_NAME_PATTERN = Pattern.compile("[\u4e00-\u9fa5]{2,4}");
    /** 手机号匹配模式（中国大陆 11 位手机号） */
    private static final Pattern PHONE_PATTERN = Pattern.compile("1[3-9]\\d{9}");
    /** 邮箱匹配模式 */
    private static final Pattern EMAIL_PATTERN = Pattern.compile("[a-zA-Z0-9._%+-]+@[a-zA-Z0-9.-]+\\.[a-zA-Z]{2,}");

    /**
     * 规范化空白字符
     *
     * @param text 原始文本
     * @return 规范化后的文本（去除多余空行、连续空格、首尾空白）
     */
    public String normalizeWhitespace(String text) {
        if (text == null || text.isEmpty()) {
            return text;
        }

        String result = text;
        // 将 3 个以上连续换行替换为 2 个换行，保留段落结构
        result = MULTIPLE_BLANK_LINES.matcher(result).replaceAll("\n\n");
        // 将 2 个以上连续空格/制表符替换为单个空格
        result = MULTIPLE_SPACES.matcher(result).replaceAll(" ");
        // 去除开头多余换行
        result = LEADING_BLANK_LINES.matcher(result).replaceAll("");
        // 去除末尾多余换行
        result = TRAILING_BLANK_LINES.matcher(result).replaceAll("\n");
        result = result.trim();

        return result;
    }

    /**
     * 去除重复内容
     *
     * @param text 原始文本
     * @return 去重后的文本（完全相同的行只保留第一次出现）
     *
     * 【注意】
     * 仅去除完全相同的行（trim 后比较），保留空行作为段落分隔。
     * 适用于简历中常见的重复章节标题或复制粘贴导致的重复内容。
     */
    public String deduplicateContent(String text) {
        if (text == null || text.isEmpty()) {
            return text;
        }

        String[] lines = text.split("\n");
        Set<String> seenLines = new HashSet<>();
        List<String> deduplicated = new ArrayList<>();
        String lastLine = "";

        for (String line : lines) {
            String normalized = line.trim();
            if (normalized.isEmpty()) {
                // 空行作为段落分隔，但避免连续多个空行
                if (!lastLine.isEmpty() && !deduplicated.isEmpty()) {
                    deduplicated.add("");
                    lastLine = "";
                }
                continue;
            }

            // 只保留首次出现的非空行
            if (!seenLines.contains(normalized)) {
                seenLines.add(normalized);
                deduplicated.add(line);
                lastLine = normalized;
            }
        }

        // 去除末尾可能遗留的空行
        while (!deduplicated.isEmpty() && deduplicated.get(deduplicated.size() - 1).trim().isEmpty()) {
            deduplicated.remove(deduplicated.size() - 1);
        }

        return String.join("\n", deduplicated);
    }

    /**
     * 压缩简历文本
     *
     * @param rawResume 原始简历文本
     * @param maxTokens 最大允许 token 数
     * @return 压缩后的简历文本
     *
     * 【压缩策略】
     * 1. 先进行空白规范化和去重
     * 2. 识别简历章节，保留关键章节标题
     * 3. 对自我评价等主观描述进行截断（超过 100 字截断）
     * 4. 若仍超 token 限制，进行安全截断
     */
    public String compressResume(String rawResume, int maxTokens) {
        if (rawResume == null || rawResume.isEmpty()) {
            return rawResume;
        }

        // 第一步：规范化空白和去重
        String normalized = normalizeWhitespace(rawResume);
        normalized = deduplicateContent(normalized);

        StringBuilder compressed = new StringBuilder();
        String[] lines = normalized.split("\n");

        Set<String> extractedInfo = new HashSet<>();

        for (String line : lines) {
            String trimmed = line.trim();
            if (trimmed.isEmpty()) {
                compressed.append("\n");
                continue;
            }

            String lower = trimmed.toLowerCase();

            // 根据章节类型决定压缩策略
            if (containsAny(lower, "个人信息", "基本信息")) {
                // 提取姓名、电话、邮箱等关键信息
                String extracted = extractBasicInfo(trimmed);
                if (!extracted.isEmpty() && extractedInfo.add(extracted)) {
                    compressed.append(trimmed).append("\n");
                }
            } else if (containsAny(lower, "教育背景", "学历", "毕业院校")) {
                compressed.append(trimmed).append("\n");
            } else if (containsAny(lower, "工作经历", "工作经验")) {
                compressed.append(trimmed).append("\n");
            } else if (containsAny(lower, "项目经历", "项目经验")) {
                compressed.append(trimmed).append("\n");
            } else if (containsAny(lower, "技能", "专业技能", "技术技能")) {
                compressed.append(trimmed).append("\n");
            } else if (containsAny(lower, "自我评价", "个人总结")) {
                // 自我评价通常较空泛，超过 100 字进行截断
                if (trimmed.length() > 100) {
                    compressed.append(trimmed.substring(0, 100)).append("...\n");
                } else {
                    compressed.append(trimmed).append("\n");
                }
            } else {
                compressed.append(trimmed).append("\n");
            }
        }

        String result = compressed.toString();
        // 若仍超 token 限制，进行安全截断
        if (TokenEstimator.needsTruncation(result, maxTokens)) {
            result = TokenEstimator.safeTruncate(result, maxTokens);
        }

        return result;
    }

    /**
     * 压缩项目描述
     *
     * @param projectDesc 原始项目描述文本
     * @param maxTokens   最大允许 token 数
     * @return 压缩后的项目描述
     *
     * 【压缩策略】
     * 1. 保留前 2 个句子（通常是项目背景和技术栈）
     * 2. 后续只保留含技术关键词或量化数据的句子
     * 3. 若 token 接近上限则提前终止
     */
    public String compressProjectDescription(String projectDesc, int maxTokens) {
        if (projectDesc == null || projectDesc.isEmpty()) {
            return projectDesc;
        }

        String normalized = normalizeWhitespace(projectDesc);

        // 按句子分割（支持中文和英文句号/分号）
        String[] sentences = normalized.split("[。；.；]");
        StringBuilder compressed = new StringBuilder();
        int addedCount = 0;

        for (String sentence : sentences) {
            String trimmed = sentence.trim();
            if (trimmed.isEmpty()) {
                continue;
            }

            // 前 2 个句子必保留，后续只保留含技术栈或量化数据的句子
            if (addedCount < 2 || containsTechOrMetric(trimmed)) {
                if (compressed.length() > 0) {
                    compressed.append("；");
                }
                compressed.append(trimmed);
                addedCount++;

                // 若 token 接近上限（80%），提前终止
                if (TokenEstimator.estimateTokens(compressed.toString()) > maxTokens * 0.8) {
                    break;
                }
            }
        }

        String result = compressed.toString();
        if (result.isEmpty()) {
            // 兜底：若过滤后为空，返回原文的前半部分
            result = projectDesc.substring(0, Math.min(projectDesc.length(), maxTokens / 2));
        }

        return result;
    }

    /**
     * 从文本中提取基本信息（姓名、电话、邮箱）
     *
     * @param text 原始文本
     * @return 提取的基本信息字符串
     */
    public String extractBasicInfo(String text) {
        StringBuilder info = new StringBuilder();

        var nameMatcher = CHINESE_NAME_PATTERN.matcher(text);
        if (nameMatcher.find()) {
            info.append("姓名:").append(nameMatcher.group()).append(" ");
        }

        var phoneMatcher = PHONE_PATTERN.matcher(text);
        if (phoneMatcher.find()) {
            info.append("电话:").append(phoneMatcher.group()).append(" ");
        }

        var emailMatcher = EMAIL_PATTERN.matcher(text);
        if (emailMatcher.find()) {
            info.append("邮箱:").append(emailMatcher.group()).append(" ");
        }

        return info.toString();
    }

    /**
     * 从文本中提取关键词
     *
     * @param text      原始文本（如 JD 描述）
     * @param maxTokens 最大允许 token 数
     * @return 提取的关键词句子列表
     *
     * 【提取规则】
     * 优先提取含能力关键词的句子（熟练、精通、熟悉、掌握、负责、主导、独立）。
     * 适用于从 JD 中提取核心要求，减少无意义描述。
     */
    public String extractKeywords(String text, int maxTokens) {
        if (text == null || text.isEmpty()) {
            return text;
        }

        Set<String> keywords = new HashSet<>();

        String[] lines = text.split("\n");
        for (String line : lines) {
            String trimmed = line.trim();
            if (trimmed.isEmpty()) continue;

            // 优先提取含能力关键词的句子
            if (containsAny(trimmed.toLowerCase(), "熟练", "精通", "熟悉", "掌握", "负责", "主导", "独立")) {
                if (TokenEstimator.estimateTokens(String.join(" ", keywords)) < maxTokens * 0.7) {
                    keywords.add(trimmed.length() > 50 ? trimmed.substring(0, 50) : trimmed);
                }
            }

            // 若接近上限则提前终止
            if (TokenEstimator.estimateTokens(String.join("\n", keywords)) > maxTokens * 0.8) {
                break;
            }
        }

        // 兜底：若未提取到关键词，返回原文截断
        if (keywords.isEmpty()) {
            return TokenEstimator.safeTruncate(text, maxTokens);
        }

        String result = String.join("\n", keywords);
        return TokenEstimator.safeTruncate(result, maxTokens);
    }

    /**
     * 将原始文本转换为结构化格式
     *
     * @param rawText 原始文本
     * @param type    内容类型（简历/项目/JD/通用）
     * @return 结构化压缩后的文本
     *
     * 【类型说明】
     * - RESUME：调用 compressResume，最大 4000 token
     * - PROJECT：调用 compressProjectDescription，最大 1500 token
     * - JD：调用 extractKeywords，最大 2000 token
     * - GENERAL：仅规范化空白并截断，最大 3000 token
     */
    public String toStructuredFormat(String rawText, ContentType type) {
        if (rawText == null || rawText.isEmpty()) {
            return rawText;
        }

        switch (type) {
            case RESUME -> {
                return compressResume(rawText, 4000);
            }
            case PROJECT -> {
                return compressProjectDescription(rawText, 1500);
            }
            case JD -> {
                return extractKeywords(rawText, 2000);
            }
            default -> {
                String normalized = normalizeWhitespace(rawText);
                return TokenEstimator.safeTruncate(normalized, 3000);
            }
        }
    }

    /**
     * 安全截断文本至指定 token 数
     *
     * @param text      原始文本
     * @param maxTokens 最大允许 token 数
     * @return 截断后的文本
     */
    public String safeTruncate(String text, int maxTokens) {
        return TokenEstimator.safeTruncate(text, maxTokens);
    }

    /** 判断文本是否包含任意一个关键词 */
    private boolean containsAny(String text, String... keywords) {
        for (String keyword : keywords) {
            if (text.contains(keyword)) {
                return true;
            }
        }
        return false;
    }

    /**
     * 判断文本是否包含技术关键词或量化数据
     *
     * @param text 待检查文本
     * @return true 表示包含技术栈或量化指标
     */
    private boolean containsTechOrMetric(String text) {
        // 常见技术关键词
        boolean hasTech = containsAny(text.toLowerCase(),
                "spring", "java", "mysql", "redis", "kafka", "docker", "kubernetes",
                "微服务", "分布式", "缓存", "数据库", "框架", "架构", "高并发", "分布式");

        // 量化数据模式：数字+%、数字+单位、中文数字+时间单位
        boolean hasMetric = Pattern.compile("\\d+%?|\\d+[万亿千万百万个]+|[一二三四五六七八九十]+[年月日]").matcher(text).find();

        return hasTech || hasMetric;
    }

    /**
     * 内容类型枚举
     *
     * RESUME：简历文本
     * PROJECT：项目描述
     * JD：岗位描述
     * GENERAL：通用文本
     */
    public enum ContentType {
        RESUME,
        PROJECT,
        JD,
        GENERAL
    }
}
