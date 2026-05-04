package com.airesume.server.service.impl;

import com.airesume.server.config.AiTokenLimitConfig;
import com.airesume.server.entity.SysAiEngineConfig;
import com.airesume.server.service.SysAiEngineConfigService;
import com.airesume.server.service.SysPromptService;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class ResumeAiServiceImplTest {

    private final ObjectMapper objectMapper = new ObjectMapper();
    private HttpServer server;

    @AfterEach
    void tearDown() {
        if (server != null) {
            server.stop(0);
            server = null;
        }
    }

    @Test
    void resolveReadTimeoutMsShouldRespectConfiguredValue() {
        assertEquals(600_000, ResumeAiServiceImpl.resolveReadTimeoutMs(600_000, 180_000));
        assertEquals(180_000, ResumeAiServiceImpl.resolveReadTimeoutMs(null, 180_000));
    }

    @Test
    void diagnoseShouldAssembleStreamingResponse() throws Exception {
        String finalJson = """
                {"overallEvaluation":{"totalScore":88,"level":"A","summary":"诊断成功","strengths":[],"weaknesses":[]},
                "highlights":["亮点1"],
                "basicInfoEvaluation":{"score":90,"hasName":true,"hasPhone":true,"hasEmail":true,"hasGithub":false,"hasBlog":false,"evaluation":"信息完整","strengths":[],"weaknesses":[],"suggestions":[]},
                "skillEvaluation":{"score":85,"skillList":["Java"],"evaluation":"技能明确","strengths":[],"weaknesses":[],"suggestions":[]},
                "workExperienceEvaluation":{"score":70,"totalYears":1,"companyCount":1,"hasQuantifiableResults":false,"experiences":[],"evaluation":"经历一般","strengths":[],"weaknesses":[],"suggestions":[]},
                "projectExperienceEvaluation":{"score":80,"projectCount":1,"hasTechStack":true,"hasResponsibilities":true,"projects":[],"evaluation":"项目较清晰","strengths":[],"weaknesses":[],"suggestions":[]},
                "educationEvaluation":{"score":75,"degree":"本科","school":"测试大学","major":"软件工程","hasRelevantMajor":true,"evaluation":"教育背景匹配","strengths":[],"weaknesses":[],"suggestions":[]},
                "optimizationSuggestions":["建议1"]}
                """.replace("\r", "").replace("\n", "");

        server = startServer(exchange -> {
            String chunkJson = "{\"choices\":[{\"delta\":{\"content\":"
                    + objectMapper.writeValueAsString(finalJson) + "}}]}";
            String response = "data: " + chunkJson + "\n\n" + "data: [DONE]\n\n";
            writeResponse(exchange, 200, "text/event-stream", response);
        });

        ResumeAiServiceImpl service = createService(server.getAddress().getPort(), 1_000, null);
        String result = service.diagnose("张三 Java 工程师简历");

        assertEquals(finalJson, result);
    }

    @Test
    void diagnoseShouldRetryWithLeanPromptAfterTimeout() throws Exception {
        AtomicInteger requestCount = new AtomicInteger();
        List<String> requestBodies = new ArrayList<>();
        String finalJson = """
                {"overallEvaluation":{"totalScore":76,"level":"B","summary":"重试成功","strengths":[],"weaknesses":[]},
                "highlights":[],
                "basicInfoEvaluation":{"score":80,"hasName":true,"hasPhone":true,"hasEmail":true,"hasGithub":false,"hasBlog":false,"evaluation":"信息完整","strengths":[],"weaknesses":[],"suggestions":[]},
                "skillEvaluation":{"score":78,"skillList":[],"evaluation":"技能尚可","strengths":[],"weaknesses":[],"suggestions":[]},
                "workExperienceEvaluation":{"score":65,"totalYears":0,"companyCount":0,"hasQuantifiableResults":false,"experiences":[],"evaluation":"缺少实习","strengths":[],"weaknesses":[],"suggestions":[]},
                "projectExperienceEvaluation":{"score":82,"projectCount":1,"hasTechStack":true,"hasResponsibilities":true,"projects":[],"evaluation":"项目清晰","strengths":[],"weaknesses":[],"suggestions":[]},
                "educationEvaluation":{"score":72,"degree":"本科","school":"测试大学","major":"软件工程","hasRelevantMajor":true,"evaluation":"教育匹配","strengths":[],"weaknesses":[],"suggestions":[]},
                "optimizationSuggestions":["建议1"]}
                """.replace("\r", "").replace("\n", "");

        server = startServer(exchange -> {
            requestBodies.add(new String(exchange.getRequestBody().readAllBytes(), StandardCharsets.UTF_8));
            int current = requestCount.incrementAndGet();
            if (current == 1) {
                sleep(300);
                writeResponse(exchange, 200, "text/event-stream", "data: [DONE]\n\n");
                return;
            }
            String chunkJson = "{\"choices\":[{\"delta\":{\"content\":"
                    + objectMapper.writeValueAsString(finalJson) + "}}]}";
            String response = "data: " + chunkJson + "\n\n" + "data: [DONE]\n\n";
            writeResponse(exchange, 200, "text/event-stream", response);
        });

        ResumeAiServiceImpl service = createService(server.getAddress().getPort(), 100, null);
        String result = service.diagnose("李四 前端工程师简历");

        assertEquals(finalJson, result);
        assertEquals(2, requestCount.get());
        assertTrue(requestBodies.get(1).contains("返回精简 JSON"));
    }

    private ResumeAiServiceImpl createService(int port, int timeoutMs, String dbPrompt) {
        SysPromptService sysPromptService = mock(SysPromptService.class);
        when(sysPromptService.getActivePromptContent(anyInt())).thenReturn(dbPrompt);

        SysAiEngineConfigService aiEngineConfigService = mock(SysAiEngineConfigService.class);
        SysAiEngineConfig config = new SysAiEngineConfig();
        config.setProviderType("deepseek");
        config.setModelName("test-model");
        config.setBaseUrl("http://localhost:" + port);
        config.setApiKey("test-key");
        config.setTimeoutMs(timeoutMs);
        config.setEngineCode("resume-test");
        when(aiEngineConfigService.getActiveByBusinessType("resume")).thenReturn(config);

        AiTokenLimitConfig tokenLimitConfig = new AiTokenLimitConfig();
        tokenLimitConfig.setCompressionEnabled(false);
        tokenLimitConfig.setTokenLimitEnabled(true);
        tokenLimitConfig.setResumeDiagnosisMax(6000);

        return new ResumeAiServiceImpl(
                "deepseek",
                "http://localhost:" + port,
                "test-model",
                "none",
                sysPromptService,
                aiEngineConfigService,
                objectMapper,
                tokenLimitConfig,
                org.springframework.web.client.RestClient.builder(),
                org.springframework.web.reactive.function.client.WebClient.builder());
    }

    private HttpServer startServer(ThrowingHandler handler) throws IOException {
        HttpServer httpServer = HttpServer.create(new InetSocketAddress(0), 0);
        httpServer.setExecutor(Executors.newCachedThreadPool());
        httpServer.createContext("/chat/completions", exchange -> {
            try {
                handler.handle(exchange);
            } finally {
                exchange.close();
            }
        });
        httpServer.start();
        return httpServer;
    }

    private void writeResponse(HttpExchange exchange, int status, String contentType, String body) throws IOException {
        byte[] bytes = body.getBytes(StandardCharsets.UTF_8);
        exchange.getResponseHeaders().add("Content-Type", contentType);
        exchange.sendResponseHeaders(status, bytes.length);
        try (OutputStream outputStream = exchange.getResponseBody()) {
            outputStream.write(bytes);
        }
    }

    private void sleep(long millis) {
        try {
            Thread.sleep(millis);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new RuntimeException(e);
        }
    }

    @FunctionalInterface
    private interface ThrowingHandler {
        void handle(HttpExchange exchange) throws IOException;
    }
}
