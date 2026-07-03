package com.airesume.server.util;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;

import static org.junit.jupiter.api.Assertions.*;

/**
 * TokenEstimator 工具类测试
 *
 * 测试覆盖：
 * - 中文字符 token 估算
 * - 英文字符 token 估算
 * - 数字 token 估算
 * - 代码符号 token 估算
 * - 混合文本 token 估算
 * - 边界条件（null、空字符串、超长文本）
 * - 截断功能
 * - 使用率计算
 */
@DisplayName("TokenEstimator 工具类测试")
class TokenEstimatorTest {

    @Nested
    @DisplayName("estimateTokens 方法测试")
    class EstimateTokensTests {

        @ParameterizedTest
        @NullAndEmptySource
        @DisplayName("null 和空字符串应该返回 0")
        void shouldReturnZeroForNullOrEmpty(String input) {
            assertEquals(0, TokenEstimator.estimateTokens(input));
        }

        @Test
        @DisplayName("纯中文文本应该按 1.5 token/字 估算")
        void shouldEstimateChineseTextCorrectly() {
            // 10 个中文字符 * 1.5 = 15 tokens
            String chineseText = "这是一段中文文本测试";
            assertEquals(15, TokenEstimator.estimateTokens(chineseText));
        }

        @Test
        @DisplayName("纯英文文本应该按 0.25 token/字 估算")
        void shouldEstimateEnglishTextCorrectly() {
            // 10 个英文字符 * 0.25 = 2.5, 向上取整 = 3
            String englishText = "HelloWorld";
            assertEquals(3, TokenEstimator.estimateTokens(englishText));
        }

        @Test
        @DisplayName("纯数字文本应该按 0.25 token/字 估算")
        void shouldEstimateNumbersCorrectly() {
            // 10 个数字 * 0.25 = 2.5, 向上取整 = 3
            String numberText = "1234567890";
            assertEquals(3, TokenEstimator.estimateTokens(numberText));
        }

        @Test
        @DisplayName("代码符号应该按 0.35 token/字 估算")
        void shouldEstimateCodeCharsCorrectly() {
            // 10 个代码符号 * 0.35 = 3.5, 向上取整 = 4
            String codeText = "{}[]();,.";
            assertEquals(4, TokenEstimator.estimateTokens(codeText));
        }

        @Test
        @DisplayName("混合文本应该正确估算")
        void shouldEstimateMixedTextCorrectly() {
            // "Hello你好123{}"
            // 英文: 5 * 0.25 = 1.25
            // 中文: 2 * 1.5 = 3.0
            // 数字: 3 * 0.25 = 0.75
            // 代码: 2 * 0.35 = 0.7
            // 总计: 5.7, 向上取整 = 6
            String mixedText = "Hello你好123{}";
            assertEquals(6, TokenEstimator.estimateTokens(mixedText));
        }

        @Test
        @DisplayName("单个中文字符应该返回 2")
        void shouldEstimateSingleChineseChar() {
            assertEquals(2, TokenEstimator.estimateTokens("你"));
        }

        @Test
        @DisplayName("单个英文字母应该返回 1")
        void shouldEstimateSingleEnglishChar() {
            assertEquals(1, TokenEstimator.estimateTokens("A"));
        }

        @Test
        @DisplayName("空格应该按其他字符处理")
        void shouldEstimateSpacesAsOther() {
            // 空格按英文字母系数 0.25 计算
            String spaces = "   ";
            assertEquals(1, TokenEstimator.estimateTokens(spaces));
        }
    }

    @Nested
    @DisplayName("needsTruncation 方法测试")
    class NeedsTruncationTests {

        @Test
        @DisplayName("文本 token 数小于阈值时应该返回 false")
        void shouldReturnFalseWhenUnderLimit() {
            String shortText = "短文本";
            assertFalse(TokenEstimator.needsTruncation(shortText, 100));
        }

        @Test
        @DisplayName("文本 token 数大于阈值时应该返回 true")
        void shouldReturnTrueWhenOverLimit() {
            // 100 个中文字符 = 150 tokens
            String longText = "你".repeat(100);
            assertTrue(TokenEstimator.needsTruncation(longText, 100));
        }

        @Test
        @DisplayName("文本 token 数等于阈值时应该返回 false")
        void shouldReturnFalseWhenEqualsLimit() {
            // 67 个中文字符 ≈ 100.5 tokens, 向上取整 = 101
            String exactText = "你".repeat(67);
            assertFalse(TokenEstimator.needsTruncation(exactText, 101));
        }

        @ParameterizedTest
        @NullAndEmptySource
        @DisplayName("null 和空字符串应该返回 false")
        void shouldReturnFalseForNullOrEmpty(String input) {
            assertFalse(TokenEstimator.needsTruncation(input, 100));
        }
    }

    @Nested
    @DisplayName("safeTruncate 方法测试")
    class SafeTruncateTests {

        @ParameterizedTest
        @NullAndEmptySource
        @DisplayName("null 和空字符串应该原样返回")
        void shouldReturnSameForNullOrEmpty(String input) {
            assertEquals(input, TokenEstimator.safeTruncate(input, 100));
        }

        @Test
        @DisplayName("未超限文本应该原样返回")
        void shouldReturnOriginalWhenUnderLimit() {
            String shortText = "短文本";
            assertEquals(shortText, TokenEstimator.safeTruncate(shortText, 100));
        }

        @Test
        @DisplayName("超限文本应该被截断")
        void shouldTruncateWhenOverLimit() {
            // 100 个中文字符 = 150 tokens
            String longText = "你".repeat(100);
            String truncated = TokenEstimator.safeTruncate(longText, 100);

            assertNotNull(truncated);
            assertTrue(truncated.length() < longText.length());
            assertTrue(TokenEstimator.estimateTokens(truncated) <= 100);
        }

        @Test
        @DisplayName("截断后的文本应该保持在 token 限制内")
        void shouldKeepTruncatedTextWithinLimit() {
            int maxTokens = 50;
            String longText = "这是一段很长的中文文本".repeat(20);
            String truncated = TokenEstimator.safeTruncate(longText, maxTokens);

            assertTrue(TokenEstimator.estimateTokens(truncated) <= maxTokens);
        }
    }

    @Nested
    @DisplayName("calculateUsageRatio 方法测试")
    class CalculateUsageRatioTests {

        @Test
        @DisplayName("应该正确计算使用率")
        void shouldCalculateRatioCorrectly() {
            assertEquals(0.5, TokenEstimator.calculateUsageRatio(50, 100), 0.001);
        }

        @Test
        @DisplayName("满使用应该返回 1.0")
        void shouldReturnOneForFullUsage() {
            assertEquals(1.0, TokenEstimator.calculateUsageRatio(100, 100), 0.001);
        }

        @Test
        @DisplayName("零使用应该返回 0.0")
        void shouldReturnZeroForNoUsage() {
            assertEquals(0.0, TokenEstimator.calculateUsageRatio(0, 100), 0.001);
        }

        @Test
        @DisplayName("最大上下文为 0 时应该返回 0.0")
        void shouldReturnZeroForZeroMaxContext() {
            assertEquals(0.0, TokenEstimator.calculateUsageRatio(50, 0), 0.001);
        }

        @Test
        @DisplayName("最大上下文为负数时应该返回 0.0")
        void shouldReturnZeroForNegativeMaxContext() {
            assertEquals(0.0, TokenEstimator.calculateUsageRatio(50, -100), 0.001);
        }
    }

    @Nested
    @DisplayName("getSafeMaxTokens 方法测试")
    class GetSafeMaxTokensTests {

        @Test
        @DisplayName("应该返回上下文窗口的 80%")
        void shouldReturn80PercentOfContext() {
            assertEquals(8000, TokenEstimator.getSafeMaxTokens(10000));
        }

        @Test
        @DisplayName("零上下文应该返回 0")
        void shouldReturnZeroForZeroContext() {
            assertEquals(0, TokenEstimator.getSafeMaxTokens(0));
        }

        @Test
        @DisplayName("负数上下文应该返回负数")
        void shouldReturnNegativeForNegativeContext() {
            assertEquals(-80, TokenEstimator.getSafeMaxTokens(-100));
        }
    }

    @Nested
    @DisplayName("常量测试")
    class ConstantsTests {

        @Test
        @DisplayName("默认最大 token 数应该是 8192")
        void shouldHaveCorrectDefaultMaxTokens() {
            assertEquals(8192, TokenEstimator.DEFAULT_MAX_TOKENS);
        }

        @Test
        @DisplayName("上下文窗口比率应该是 0.8")
        void shouldHaveCorrectContextWindowRatio() {
            assertEquals(0.8, TokenEstimator.CONTEXT_WINDOW_RATIO);
        }
    }

    @Nested
    @DisplayName("estimateTokensSafeTruncate 方法测试")
    class EstimateTokensSafeTruncateTests {

        @ParameterizedTest
        @NullAndEmptySource
        @DisplayName("null 和空字符串应该返回 0")
        void shouldReturnZeroForNullOrEmpty(String input) {
            assertEquals(0, TokenEstimator.estimateTokensSafeTruncate(input, 100));
        }

        @Test
        @DisplayName("未超限文本应该返回原始 token 数")
        void shouldReturnOriginalTokensWhenUnderLimit() {
            String shortText = "短文本"; // 5 tokens
            assertEquals(5, TokenEstimator.estimateTokensSafeTruncate(shortText, 100));
        }

        @Test
        @DisplayName("超限文本应该返回截断后的 token 数")
        void shouldReturnTruncatedTokensWhenOverLimit() {
            String longText = "你".repeat(100); // 150 tokens
            int result = TokenEstimator.estimateTokensSafeTruncate(longText, 100);

            assertTrue(result <= 100);
            assertTrue(result > 0);
        }
    }

    @Nested
    @DisplayName("字符类型判断测试")
    class CharacterTypeTests {

        @Test
        @DisplayName("中文标点符号应该按中文字符处理")
        void shouldTreatChinesePunctuationAsChinese() {
            // 中文句号、顿号属于 CJK_SYMBOLS_AND_PUNCTUATION
            String chinesePunctuation = "。、";
            // 2 个中文标点 * 1.5 = 3.0, 向上取整 = 3
            assertEquals(3, TokenEstimator.estimateTokens(chinesePunctuation));
        }

        @Test
        @DisplayName("英文字母大小写都应该正确识别")
        void shouldRecognizeBothCases() {
            // "ABCabc" = 6 个英文字符 * 0.25 = 1.5, 向上取整 = 2
            assertEquals(2, TokenEstimator.estimateTokens("ABCabc"));
        }

        @Test
        @DisplayName("下划线应该按代码符号处理")
        void shouldTreatUnderscoreAsCode() {
            // 下划线是代码符号 * 0.35
            assertEquals(1, TokenEstimator.estimateTokens("_"));
        }

        @Test
        @DisplayName("换行符应该按其他字符处理")
        void shouldTreatNewlineAsOther() {
            // 换行符按英文字母系数 0.25 计算
            assertEquals(1, TokenEstimator.estimateTokens("\n"));
        }
    }
}
