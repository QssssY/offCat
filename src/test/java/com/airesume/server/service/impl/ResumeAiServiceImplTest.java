package com.airesume.server.service.impl;

import com.airesume.server.common.constants.AiEngineConstants;
import com.airesume.server.common.constants.UserAiConstants;
import com.airesume.server.config.AiTokenLimitConfig;
import com.airesume.server.config.AiCircuitBreakerConfig;
import com.airesume.server.dto.ai.ResolvedAiConfig;
import com.airesume.server.service.AiCircuitBreaker;
import com.airesume.server.service.AiCredentialCrypto;
import com.airesume.server.service.SysAiEngineConfigService;
import com.airesume.server.service.SysPromptService;
import com.airesume.server.service.UserAiConfigResolver;
import com.fasterxml.jackson.databind.ObjectMapper;
import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.web.client.RestClient;
import org.springframework.web.reactive.function.client.WebClient;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class ResumeAiServiceImplTest {

    private ResumeAiServiceImpl service;
    private Method sanitizeMethod;

    @BeforeEach
    void setUp() throws Exception {
        service = createService("doubao", "");
        sanitizeMethod = ResumeAiServiceImpl.class.getDeclaredMethod("sanitizePolishedResumeText", String.class);
        sanitizeMethod.setAccessible(true);
    }

    @Test
    void constructorShouldNotRequireConfiguredBaseUrlDnsAtStartup() {
        assertDoesNotThrow(() -> createService("deepseek", "https://startup-only.invalid/v1"));
    }

    @Test
    void sanitizePolishedResumeTextShouldKeepFirstResumeAndStripMetadata() throws Exception {
        String polluted = """
                个人信息
                温家健
                Java后端开发工程师（初级）
                教育背景
                广东轻工职业技术学院 | 软件技术 | 专科 | 2021.09-2024.06
                专业技能
                • 精通Java OOP/集合/多线程，熟练Spring Boot/MyBatis/MySQL
                荣誉证书
                校级二等奖学金(2023)、Java编程竞赛三等奖(2023)
                个人评价
                具备扎实Java后端开发能力和半年实习经验。(String), 温家健
                个人信息
                温家健
                教育背景
                广东轻工职业技术学院 | 软件技术 | 专科 | 2021.09-2024.06
                仅基于简历(String)
                2026-05-12T22:05:26.181401600(LocalDateTime)
                <==    Updates: 1
                """;

        String sanitized = (String) sanitizeMethod.invoke(service, polluted);

        assertTrue(sanitized.contains("个人评价\n具备扎实Java后端开发能力和半年实习经验。"));
        assertFalse(sanitized.contains("(String)"));
        assertFalse(sanitized.contains("(LocalDateTime)"));
        assertFalse(sanitized.contains("<==    Updates: 1"));
        assertFalse(sanitized.contains("仅基于简历"));
        assertFalse(sanitized.indexOf("个人信息") != sanitized.lastIndexOf("个人信息"));
    }

    @Test
    void sanitizePolishedResumeTextShouldHandleNullInput() throws Exception {
        String sanitized = (String) sanitizeMethod.invoke(service, (Object) null);
        assertEquals("", sanitized);
    }

    @Test
    void applyStableDiagnosisOptionsShouldUseZeroTemperature() throws Exception {
        Class<?> requestBodyClass = Class.forName(
                "com.airesume.server.service.impl.ResumeAiServiceImpl$RequestBody");
        var constructor = requestBodyClass.getDeclaredConstructor();
        constructor.setAccessible(true);
        Object requestBody = constructor.newInstance();
        Method applyMethod = ResumeAiServiceImpl.class.getDeclaredMethod("applyStableDiagnosisOptions", requestBodyClass);
        applyMethod.setAccessible(true);

        applyMethod.invoke(service, requestBody);

        Field temperatureField = requestBodyClass.getDeclaredField("temperature");
        temperatureField.setAccessible(true);
        assertEquals(BigDecimal.ZERO, temperatureField.get(requestBody));
    }

    @Test
    void userCustomResumeRuntimeLogsShouldUseCustomTagAndRouteDetails() throws Exception {
        UserAiConfigResolver userAiConfigResolver = mock(UserAiConfigResolver.class);
        when(userAiConfigResolver.resolve(7L, AiEngineConstants.BUSINESS_TYPE_RESUME, false))
                .thenReturn(ResolvedAiConfig.builder()
                        .provider("openai")
                        .baseUrl("https://token-plan-cn.xiaomimimo.com/v1")
                        .apiKey("custom-key")
                        .model("mimo-v2.5")
                        .configType(UserAiConstants.CONFIG_TYPE_RESUME)
                        .build());
        ResumeAiServiceImpl customService = createService("deepseek", "", userAiConfigResolver);
        Method resolveMethod = ResumeAiServiceImpl.class.getDeclaredMethod(
                "resolveRuntimeConfig", Long.class, boolean.class, boolean.class);
        resolveMethod.setAccessible(true);
        Object runtimeConfig = resolveMethod.invoke(customService, 7L, false, true);

        Method tagMethod = ResumeAiServiceImpl.class.getDeclaredMethod("runtimeLogTag", runtimeConfig.getClass());
        tagMethod.setAccessible(true);
        String tag = (String) tagMethod.invoke(customService, runtimeConfig);

        Logger logger = (Logger) org.slf4j.LoggerFactory.getLogger(ResumeAiServiceImpl.class);
        Level originalLevel = logger.getLevel();
        ListAppender<ILoggingEvent> appender = new ListAppender<>();
        appender.start();
        logger.addAppender(appender);
        logger.setLevel(Level.INFO);

        try {
            Method routeMethod = ResumeAiServiceImpl.class.getDeclaredMethod(
                    "logRuntimeRoute", String.class, runtimeConfig.getClass(), String.class);
            routeMethod.setAccessible(true);
            routeMethod.invoke(customService, tag, runtimeConfig, "resume-diagnosis");
        } finally {
            logger.detachAppender(appender);
            logger.setLevel(originalLevel);
        }

        assertEquals("USER_CUSTOM/openai-compatible", tag);
        assertTrue(appender.list.stream().anyMatch(event -> {
            String message = event.getFormattedMessage();
            return message.startsWith("[USER_CUSTOM/openai-compatible]")
                    && message.contains("stage=resume-diagnosis")
                    && message.contains("source=user_custom")
                    && message.contains("baseUrl=https://token-plan-cn.xiaomimimo.com/v1")
                    && message.contains("model=mimo-v2.5")
                    && message.contains("configType=resume");
        }));
        assertTrue(appender.list.stream().noneMatch(event ->
                event.getFormattedMessage().startsWith("[OPENAI]")));
    }

    @Test
    void buildThinkingConfigShouldUseRuntimeLogTagForWarnings() throws Exception {
        Method method = ResumeAiServiceImpl.class.getDeclaredMethod(
                "buildThinkingConfig", String.class, String.class, String.class);
        method.setAccessible(true);

        Logger logger = (Logger) org.slf4j.LoggerFactory.getLogger(ResumeAiServiceImpl.class);
        Level originalLevel = logger.getLevel();
        ListAppender<ILoggingEvent> appender = new ListAppender<>();
        appender.start();
        logger.addAppender(appender);
        logger.setLevel(Level.WARN);

        try {
            method.invoke(service, "mimo-v2.5", "enabled", "USER_CUSTOM/openai-compatible");
        } finally {
            logger.detachAppender(appender);
            logger.setLevel(originalLevel);
        }

        assertTrue(appender.list.stream().anyMatch(event ->
                event.getFormattedMessage().startsWith("[USER_CUSTOM/openai-compatible]")));
        assertTrue(appender.list.stream().noneMatch(event ->
                event.getFormattedMessage().startsWith("[DOUBAO]")));
    }

    @Test
    void platformLogTagShouldIncludePlatformPrefix() throws Exception {
        Method method = ResumeAiServiceImpl.class.getDeclaredMethod("platformLogTag", String.class);
        method.setAccessible(true);

        assertEquals("PLATFORM/DEEPSEEK", method.invoke(service, "deepseek"));
        assertEquals("PLATFORM/UNKNOWN", method.invoke(service, " "));
    }

    @Test
    void sanitizePolishedResumeTextShouldHandleEmptyString() throws Exception {
        String sanitized = (String) sanitizeMethod.invoke(service, "");
        assertEquals("", sanitized);
    }

    @Test
    void sanitizePolishedResumeTextShouldHandleBlankString() throws Exception {
        String sanitized = (String) sanitizeMethod.invoke(service, "   \n  \n  ");
        assertEquals("", sanitized);
    }

    @Test
    void sanitizePolishedResumeTextShouldStripMarkdownCodeBlocks() throws Exception {
        String input = """
                ```json
                个人信息
                张三
                教育背景
                清华大学
                ```
                """;

        String sanitized = (String) sanitizeMethod.invoke(service, input);

        assertFalse(sanitized.startsWith("```"));
        assertFalse(sanitized.endsWith("```"));
        assertTrue(sanitized.contains("个人信息"));
    }

    @Test
    void sanitizePolishedResumeTextShouldStripGenericCodeBlocks() throws Exception {
        String input = """
                ```
                个人信息
                张三
                教育背景
                清华大学
                ```
                """;

        String sanitized = (String) sanitizeMethod.invoke(service, input);

        assertFalse(sanitized.startsWith("```"));
        assertFalse(sanitized.endsWith("```"));
        assertTrue(sanitized.contains("个人信息"));
    }

    @Test
    void sanitizePolishedResumeTextShouldStripLeadingTitles() throws Exception {
        String input = """
                AI润色简历
                个人信息
                张三
                教育背景
                清华大学
                """;

        String sanitized = (String) sanitizeMethod.invoke(service, input);

        assertFalse(sanitized.startsWith("AI润色简历"));
        assertTrue(sanitized.startsWith("个人信息"));
    }

    @Test
    void sanitizePolishedResumeTextShouldStripLeadingSummaryBlock() throws Exception {
        String input = """
                摘要：这是一份优秀的简历
                包含丰富的工作经验
                个人信息
                张三
                教育背景
                清华大学
                """;

        String sanitized = (String) sanitizeMethod.invoke(service, input);

        assertFalse(sanitized.contains("摘要"));
        assertTrue(sanitized.startsWith("个人信息"));
    }

    @Test
    void sanitizePolishedResumeTextShouldNormalizeInlineSectionTitles() throws Exception {
        String input = "个人信息 张三 教育背景 清华大学 专业技能 Java";

        String sanitized = (String) sanitizeMethod.invoke(service, input);

        // 验证章节标题被正确识别并换行
        assertTrue(sanitized.contains("个人信息"));
        assertTrue(sanitized.contains("张三"));
        assertTrue(sanitized.contains("教育背景"));
        assertTrue(sanitized.contains("清华大学"));
        assertTrue(sanitized.contains("专业技能"));
        assertTrue(sanitized.contains("Java"));
    }

    @Test
    void sanitizePolishedResumeTextShouldKeepFirstResumeBody() throws Exception {
        String input = """
                个人信息
                张三
                教育背景
                清华大学
                个人信息
                李四
                教育背景
                北京大学
                """;

        String sanitized = (String) sanitizeMethod.invoke(service, input);

        assertTrue(sanitized.contains("张三"));
        assertFalse(sanitized.contains("李四"));
        assertEquals(1, countOccurrences(sanitized, "个人信息"));
    }

    @Test
    void sanitizePolishedResumeTextShouldStripTrailingMetadata() throws Exception {
        String input = """
                个人信息
                张三
                教育背景
                清华大学
                (String)
                (LocalDateTime)
                <== Updates: 1
                """;

        String sanitized = (String) sanitizeMethod.invoke(service, input);

        assertFalse(sanitized.contains("(String)"));
        assertFalse(sanitized.contains("(LocalDateTime)"));
        assertFalse(sanitized.contains("<== Updates:"));
    }

    @Test
    void sanitizePolishedResumeTextShouldNormalizeLineEndings() throws Exception {
        String input = "个人信息\r\n张三\r教育背景\n清华大学";

        String sanitized = (String) sanitizeMethod.invoke(service, input);

        assertFalse(sanitized.contains("\r"));
        assertTrue(sanitized.contains("个人信息\n张三"));
    }

    @Test
    void sanitizePolishedResumeTextShouldStripBom() throws Exception {
        String input = "﻿个人信息\n张三";

        String sanitized = (String) sanitizeMethod.invoke(service, input);

        assertFalse(sanitized.contains("﻿"));
        assertTrue(sanitized.startsWith("个人信息"));
    }

    @Test
    void sanitizePolishedResumeTextShouldCollapseMultipleNewlines() throws Exception {
        String input = "个人信息\n\n\n\n张三\n\n\n\n教育背景";

        String sanitized = (String) sanitizeMethod.invoke(service, input);

        // 验证多个换行符被折叠为最多两个
        assertFalse(sanitized.contains("\n\n\n"));
        assertTrue(sanitized.contains("个人信息"));
        assertTrue(sanitized.contains("张三"));
        assertTrue(sanitized.contains("教育背景"));
    }

    @Test
    void sanitizePolishedResumeTextShouldStripMarkdownHeaders() throws Exception {
        String input = """
                # 个人信息
                张三
                ## 教育背景
                清华大学
                """;

        String sanitized = (String) sanitizeMethod.invoke(service, input);

        assertFalse(sanitized.contains("#"));
        assertTrue(sanitized.contains("个人信息"));
        assertTrue(sanitized.contains("教育背景"));
    }

    private int countOccurrences(String text, String substring) {
        int count = 0;
        int index = 0;
        while ((index = text.indexOf(substring, index)) != -1) {
            count++;
            index += substring.length();
        }
        return count;
    }

    private ResumeAiServiceImpl createService(String provider, String configuredBaseUrl) {
        return createService(provider, configuredBaseUrl, null);
    }

    private ResumeAiServiceImpl createService(String provider, String configuredBaseUrl,
                                              UserAiConfigResolver userAiConfigResolver) {
        return new ResumeAiServiceImpl(
                provider,
                configuredBaseUrl,
                "",
                "none",
                mock(SysPromptService.class),
                mock(SysAiEngineConfigService.class),
                new ObjectMapper(),
                new AiTokenLimitConfig(),
                RestClient.builder(),
                WebClient.builder(),
                new AiCircuitBreaker(new AiCircuitBreakerConfig()),
                new AiCredentialCrypto("test-secret-for-ai-key-encryption"),
                userAiConfigResolver);
    }
}
