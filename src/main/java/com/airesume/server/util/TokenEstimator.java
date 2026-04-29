package com.airesume.server.util;
import lombok.experimental.UtilityClass;

/**
 * Token 估算工具类
 *
 * 所属模块：AI Token 优化模块 - 基础设施层
 * 职责：基于字符类型对输入文本进行快速的 Token 数量估算
 * 用途：在发送 AI 请求前预估输入 token 数，用于触发压缩/截断策略
 *
 * 【估算原理】
 * 不同字符类型的 token 消耗系数不同：
 * - 中文字符：约 1.5 token/字（CJK 统一表意文字）
 * - 英文字母：约 0.25 token/字
 * - 数字：约 0.25 token/字
 * - 代码符号：约 0.35 token/字（括号、分号等）
 * - 其他字符：按英文字母系数计算
 *
 * 【注意】
 * 此为估算值，与实际 tokenizer 可能存在 ±20% 误差，仅用于触发阈值判断，
 * 不用于精确计费。设置阈值时应留有余量（如 CONTEXT_WINDOW_RATIO = 0.8）。
 *
 * @author AI Resume Team
 */
@UtilityClass
public class TokenEstimator {

    /** 中文字符 token 系数（CJK 文字在大模型中通常占 1~2 个 token） */
    private static final double CHINESE_TOKENS_PER_CHAR = 1.5;
    /** 英文字母 token 系数 */
    private static final double ENGLISH_TOKENS_PER_CHAR = 0.25;
    /** 代码符号 token 系数 */
    private static final double CODE_TOKENS_PER_CHAR = 0.35;
    /** 数字 token 系数 */
    private static final double NUMBER_TOKENS_PER_CHAR = 0.25;

    /** 默认最大上下文 token 数（适用于 DeepSeek-V3 等 8K 上下文模型） */
    public static final int DEFAULT_MAX_TOKENS = 8192;
    /** 上下文窗口安全使用率阈值（超过此比例触发截断，留 20% 余量给输出） */
    public static final double CONTEXT_WINDOW_RATIO = 0.8;

    /**
     * 估算文本的 token 数量
     *
     * @param text 待估算的文本（可为 null 或空字符串）
     * @return 预估 token 数，向上取整
     */
    public int estimateTokens(String text) {
        if (text == null || text.isEmpty()) {
            return 0;
        }

        int chineseCount = 0;
        int englishCount = 0;
        int numberCount = 0;
        int codeCount = 0;
        int otherCount = 0;

        // 逐字符分类统计，按字符类型应用不同系数
        for (char c : text.toCharArray()) {
            if (isChinese(c)) {
                chineseCount++;
            } else if (isEnglishLetter(c)) {
                englishCount++;
            } else if (isDigit(c)) {
                numberCount++;
            } else if (isCodeChar(c)) {
                codeCount++;
            } else {
                otherCount++;
            }
        }

        double tokens = chineseCount * CHINESE_TOKENS_PER_CHAR
                + englishCount * ENGLISH_TOKENS_PER_CHAR
                + numberCount * NUMBER_TOKENS_PER_CHAR
                + codeCount * CODE_TOKENS_PER_CHAR
                + otherCount * ENGLISH_TOKENS_PER_CHAR;

        return (int) Math.ceil(tokens);
    }

    /**
     * 估算截断后文本的 token 数量（用于验证截断效果）
     *
     * @param text     原始文本
     * @param maxTokens 最大允许 token 数
     * @return 截断后文本的预估 token 数
     */
    public int estimateTokensSafeTruncate(String text, int maxTokens) {
        if (text == null || text.isEmpty()) {
            return 0;
        }

        int estimated = estimateTokens(text);
        if (estimated <= maxTokens) {
            return estimated;
        }

        // 按中文字符系数反推目标字符数，再乘 0.7 保守系数确保不超阈值
        int targetChars = (int) (maxTokens / CHINESE_TOKENS_PER_CHAR * 0.7);
        return estimateTokens(text.substring(0, Math.min(text.length(), targetChars)));
    }

    /**
     * 安全截断文本至指定 token 数以内
     *
     * @param text      原始文本
     * @param maxTokens 最大允许 token 数
     * @return 截断后的文本（若原文未超限则返回原文）
     *
     * 【截断策略】
     * 按当前文本的 token 密度比例计算目标字符数，再乘 0.8 保守系数，
     * 确保截断后实际 token 数不超过 maxTokens。
     */
    public String safeTruncate(String text, int maxTokens) {
        if (text == null || text.isEmpty()) {
            return text;
        }

        int estimated = estimateTokens(text);
        if (estimated <= maxTokens) {
            return text;
        }

        // 按比例计算目标字符数，留 20% 安全余量
        int targetChars = (int) (text.length() * ((double) maxTokens / estimated) * 0.8);
        return text.substring(0, Math.max(0, targetChars));
    }

    /**
     * 判断文本是否需要截断
     *
     * @param text      待检查文本
     * @param maxTokens 最大允许 token 数
     * @return true 表示需要截断
     */
    public boolean needsTruncation(String text, int maxTokens) {
        return estimateTokens(text) > maxTokens;
    }

    /**
     * 计算 token 使用率
     *
     * @param usedTokens       已使用 token 数
     * @param maxContextTokens 最大上下文 token 数
     * @return 使用率（0.0 ~ 1.0）
     */
    public double calculateUsageRatio(int usedTokens, int maxContextTokens) {
        if (maxContextTokens <= 0) {
            return 0.0;
        }
        return (double) usedTokens / maxContextTokens;
    }

    /**
     * 获取安全的最大 token 数（按上下文窗口的 80% 计算）
     *
     * @param maxContextTokens 模型最大上下文 token 数
     * @return 建议的安全最大输入 token 数
     */
    public int getSafeMaxTokens(int maxContextTokens) {
        return (int) (maxContextTokens * CONTEXT_WINDOW_RATIO);
    }

    /** 判断字符是否为中文字符（CJK 统一表意文字区） */
    private boolean isChinese(char c) {
        return Character.UnicodeBlock.of(c) == Character.UnicodeBlock.CJK_UNIFIED_IDEOGRAPHS
                || Character.UnicodeBlock.of(c) == Character.UnicodeBlock.CJK_COMPATIBILITY_IDEOGRAPHS
                || Character.UnicodeBlock.of(c) == Character.UnicodeBlock.CJK_RADICALS_SUPPLEMENT
                || Character.UnicodeBlock.of(c) == Character.UnicodeBlock.CJK_SYMBOLS_AND_PUNCTUATION;
    }

    /** 判断字符是否为英文字母 */
    private boolean isEnglishLetter(char c) {
        return (c >= 'a' && c <= 'z') || (c >= 'A' && c <= 'Z');
    }

    /** 判断字符是否为数字 */
    private boolean isDigit(char c) {
        return c >= '0' && c <= '9';
    }

    /** 判断字符是否为代码常用符号（括号、引号、运算符等） */
    private boolean isCodeChar(char c) {
        return c == '{' || c == '}' || c == '[' || c == ']' || c == '(' || c == ')'
                || c == ';' || c == ',' || c == '.' || c == ':' || c == '"'
                || c == '\'' || c == '<' || c == '>' || c == '/' || c == '\\'
                || c == '=' || c == '+' || c == '-' || c == '*' || c == '&'
                || c == '|' || c == '^' || c == '%' || c == '$' || c == '#'
                || c == '@' || c == '!' || c == '?' || c == '_' || c == '`';
    }
}
