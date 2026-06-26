package com.airesume.server.infrastructure.logging;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * 日志脱敏回归测试，锁定 OSS 签名、鉴权凭据和 AI 请求正文不能原样落盘。
 */
class SensitiveLogMaskerTest {

    @Test
    void shouldMaskOssSignedUrlCredentials() {
        String message = "OSS 图片访问失败, objectKey: community/2026/avatar.png, url: "
                + "https://bucket.oss-cn-hangzhou.aliyuncs.com/community/2026/avatar.png"
                + "?OSSAccessKeyId=LTAI5tVerySensitiveKey&Expires=1780000000"
                + "&Signature=rawSignatureValueShouldNotLeak&security-token=stsTokenShouldNotLeak";

        String masked = SensitiveLogMasker.mask(message);

        assertThat(masked).contains("objectKey: community/2026/avatar.png");
        assertThat(masked).doesNotContain("LTAI5tVerySensitiveKey");
        assertThat(masked).doesNotContain("1780000000");
        assertThat(masked).doesNotContain("rawSignatureValueShouldNotLeak");
        assertThat(masked).doesNotContain("stsTokenShouldNotLeak");
        assertThat(masked).doesNotContainPattern("OSSAccessKeyId=[^*&\\s]{8,}");
        assertThat(masked).doesNotContainPattern("Signature=[^*&\\s]{8,}");
    }

    @Test
    void shouldMaskAuthorizationApiKeyAndAccessKey() {
        String message = """
                AI 请求失败, Authorization: Bearer sk-live-secret-token, apiKey=sk-user-secret,
                payload={"accessKeyId":"LTAI_ACCESS_KEY_ID","accessKeySecret":"aliyun-secret-value","api_key":"sk-json-secret"}
                """;

        String masked = SensitiveLogMasker.mask(message);

        assertThat(masked).doesNotContain("Bearer sk-live-secret-token");
        assertThat(masked).doesNotContain("sk-user-secret");
        assertThat(masked).doesNotContain("LTAI_ACCESS_KEY_ID");
        assertThat(masked).doesNotContain("aliyun-secret-value");
        assertThat(masked).doesNotContain("sk-json-secret");
        assertThat(masked).doesNotContain("Authorization: Bearer");
    }

    @Test
    void shouldMaskAiRequestBodyMessagesAndPrompts() {
        String message = """
                AI 请求正文: {"provider":"openai","model":"gpt-4.1","messages":[
                {"role":"system","content":"你是面试官，必须严格按照系统 prompt 提问"},
                {"role":"user","content":"候选人简历正文：张三，五年 Java 后端经验"}
                ],"prompt":"系统 prompt 原文","resumeText":"完整简历正文","diagnosisResult":"诊断报告正文"}
                """;

        String masked = SensitiveLogMasker.mask(message);

        assertThat(masked).contains("\"provider\":\"openai\"");
        assertThat(masked).contains("\"model\":\"gpt-4.1\"");
        assertThat(masked).doesNotContain("你是面试官");
        assertThat(masked).doesNotContain("候选人简历正文");
        assertThat(masked).doesNotContain("系统 prompt 原文");
        assertThat(masked).doesNotContain("完整简历正文");
        assertThat(masked).doesNotContain("诊断报告正文");
        assertThat(masked).doesNotContainPattern("\"messages\"\\s*:\\s*\\[");
    }

    @Test
    void shouldKeepSafeOperationalFields() {
        String message = """
                AI 调用失败, provider=openai, baseUrlHost=api.openai.com, endpoint=/v1/chat/completions,
                model=gpt-4.1, configType=interview, status=429, elapsedMs=1532, objectKey=community/a.png,
                errorType=TooManyRequests, apiKey=sk-secret
                """;

        String masked = SensitiveLogMasker.mask(message);

        assertThat(masked).contains("provider=openai");
        assertThat(masked).contains("baseUrlHost=api.openai.com");
        assertThat(masked).contains("endpoint=/v1/chat/completions");
        assertThat(masked).contains("model=gpt-4.1");
        assertThat(masked).contains("configType=interview");
        assertThat(masked).contains("status=429");
        assertThat(masked).contains("elapsedMs=1532");
        assertThat(masked).contains("objectKey=community/a.png");
        assertThat(masked).contains("errorType=TooManyRequests");
        assertThat(masked).doesNotContain("sk-secret");
    }
}
