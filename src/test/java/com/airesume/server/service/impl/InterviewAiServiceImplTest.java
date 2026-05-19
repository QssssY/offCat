package com.airesume.server.service.impl;

import com.airesume.server.common.constants.InterviewConstants;
import com.airesume.server.dto.interview.InterviewEvaluationReport;
import com.airesume.server.mapper.InterviewSessionMapper;
import com.airesume.server.mapper.MockInterviewJobTargetRecordMapper;
import com.airesume.server.mapper.ResumeDiagnosisTaskMapper;
import com.airesume.server.mock.MockInterviewService;
import com.airesume.server.service.AiCircuitBreaker;
import com.airesume.server.service.AiCredentialCrypto;
import com.airesume.server.service.InterviewContextCompressor;
import com.airesume.server.service.SysAiEngineConfigService;
import com.airesume.server.service.SysPromptService;
import com.airesume.server.config.AiTokenLimitConfig;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.web.client.RestClient;
import org.springframework.web.reactive.function.client.WebClient;

import java.lang.reflect.Method;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class InterviewAiServiceImplTest {

    private InterviewAiServiceImpl service;
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Mock private RestClient.Builder restClientBuilder;
    @Mock private WebClient.Builder webClientBuilder;
    @Mock private SysAiEngineConfigService sysAiEngineConfigService;
    @Mock private SysPromptService sysPromptService;
    @Mock private InterviewContextCompressor contextCompressor;
    @Mock private AiTokenLimitConfig tokenLimitConfig;
    @Mock private MockInterviewService mockInterviewService;
    @Mock private MockInterviewJobTargetRecordMapper mockInterviewJobTargetRecordMapper;
    @Mock private ResumeDiagnosisTaskMapper resumeDiagnosisTaskMapper;
    @Mock private InterviewSessionMapper interviewSessionMapper;
    @Mock private AiCircuitBreaker aiCircuitBreaker;
    @Mock private AiCredentialCrypto aiCredentialCrypto;
    @Mock private RestClient restClient;

    @BeforeEach
    void setUp() {
        when(restClientBuilder.baseUrl(anyString())).thenReturn(restClientBuilder);
        when(restClientBuilder.defaultHeader(anyString(), any())).thenReturn(restClientBuilder);
        when(restClientBuilder.build()).thenReturn(restClient);

        service = new InterviewAiServiceImpl(
                "test", "https://8.8.8.8/v1", "test-model", "none",
                webClientBuilder, restClientBuilder,
                sysAiEngineConfigService, sysPromptService,
                contextCompressor, tokenLimitConfig,
                mockInterviewService, mockInterviewJobTargetRecordMapper,
                resumeDiagnosisTaskMapper, interviewSessionMapper,
                objectMapper, aiCircuitBreaker, aiCredentialCrypto);
    }

    @Test
    void resolveBaseUrlShouldAllowPublicHttpsMimoProvider() throws Exception {
        Method method = InterviewAiServiceImpl.class.getDeclaredMethod(
                "resolveBaseUrl", String.class, String.class);
        method.setAccessible(true);

        assertEquals("https://8.8.8.8/compatible-mode/v1",
                method.invoke(service, "mimo", "https://8.8.8.8/compatible-mode/v1"));
    }

    @Test
    void resolveBaseUrlShouldRejectPrivateNetworkUrl() throws Exception {
        Method method = InterviewAiServiceImpl.class.getDeclaredMethod(
                "resolveBaseUrl", String.class, String.class);
        method.setAccessible(true);

        Exception exception = assertThrows(Exception.class,
                () -> method.invoke(service, "mimo", "https://192.168.1.10/v1"));
        assertTrue(exception.getCause() instanceof IllegalArgumentException);
    }

    @Test
    void resolveBaseUrlShouldUseDefaultWhenBaseUrlIsBlank() throws Exception {
        Method method = InterviewAiServiceImpl.class.getDeclaredMethod(
                "resolveBaseUrl", String.class, String.class);
        method.setAccessible(true);

        assertEquals("https://ark.cn-beijing.volces.com/api/v3", method.invoke(service, "doubao", ""));
    }

    @Test
    void buildSystemPromptShouldNotPolluteAfterInterviewMode() throws Exception {
        Method method = InterviewAiServiceImpl.class.getDeclaredMethod(
                "buildSystemPrompt", String.class, String.class, Integer.class, String.class,
                com.airesume.server.dto.interview.InterviewJobTargetContext.class, String.class, Integer.class);
        method.setAccessible(true);

        String result = (String) method.invoke(
                service,
                "Java工程师",
                "java",
                2,
                InterviewConstants.MODE_NORMAL,
                null,
                InterviewConstants.FEEDBACK_MODE_AFTER_INTERVIEW,
                InterviewConstants.INTERACTION_TYPE_TEXT);

        assertFalse(result.contains("<FEEDBACK>"));
        assertFalse(result.contains("每题反馈模式"));
    }

    @Test
    void buildSystemPromptShouldUseIndependentPromptForImmediate() throws Exception {
        Method method = InterviewAiServiceImpl.class.getDeclaredMethod(
                "buildSystemPrompt", String.class, String.class, Integer.class, String.class,
                com.airesume.server.dto.interview.InterviewJobTargetContext.class, String.class, Integer.class);
        method.setAccessible(true);

        String result = (String) method.invoke(
                service,
                "Java工程师",
                "java",
                2,
                InterviewConstants.MODE_NORMAL,
                null,
                InterviewConstants.FEEDBACK_MODE_IMMEDIATE,
                InterviewConstants.INTERACTION_TYPE_TEXT);

        assertTrue(result.contains("<FEEDBACK>"));
        assertTrue(result.contains("</FEEDBACK>"));
        assertTrue(result.contains("每题反馈模式"));
        assertTrue(result.contains("第一段只输出面试官自然追问"));
        assertTrue(result.contains("不要输出“追问：”“问题：”等标签"));
    }

    @Test
    void buildSystemPromptShouldIncludeVoiceInstructionForVoiceInteraction() throws Exception {
        Method method = InterviewAiServiceImpl.class.getDeclaredMethod(
                "buildSystemPrompt", String.class, String.class, Integer.class, String.class,
                com.airesume.server.dto.interview.InterviewJobTargetContext.class, String.class, Integer.class);
        method.setAccessible(true);

        String result = (String) method.invoke(
                service,
                "Java工程师",
                "java",
                2,
                InterviewConstants.MODE_NORMAL,
                null,
                InterviewConstants.FEEDBACK_MODE_AFTER_INTERVIEW,
                InterviewConstants.INTERACTION_TYPE_VOICE);

        assertTrue(result.contains("语音面试模式"));
        assertTrue(result.contains("口语化"));
        assertTrue(result.contains("适合直接朗读"));
    }

    @Test
    void buildSystemPromptShouldNotContainConcreteResumeMetadataExample() throws Exception {
        Method method = InterviewAiServiceImpl.class.getDeclaredMethod(
                "buildSystemPrompt", String.class, String.class, Integer.class, String.class,
                com.airesume.server.dto.interview.InterviewJobTargetContext.class, String.class, Integer.class);
        method.setAccessible(true);

        String result = (String) method.invoke(
                service,
                "前端开发工程师",
                "frontend",
                2,
                InterviewConstants.MODE_NORMAL,
                null,
                InterviewConstants.FEEDBACK_MODE_AFTER_INTERVIEW,
                InterviewConstants.INTERACTION_TYPE_VOICE);

        assertFalse(result.contains("林映"));
        assertFalse(result.contains("求职简历姓名"));
        assertTrue(result.contains("禁止把简历文件名、姓名、性别、电话、邮箱等元信息说给候选人"));
    }

    @Test
    void buildSystemPromptShouldNotIncludeVoiceInstructionForTextInteraction() throws Exception {
        Method method = InterviewAiServiceImpl.class.getDeclaredMethod(
                "buildSystemPrompt", String.class, String.class, Integer.class, String.class,
                com.airesume.server.dto.interview.InterviewJobTargetContext.class, String.class, Integer.class);
        method.setAccessible(true);

        String result = (String) method.invoke(
                service,
                "Java工程师",
                "java",
                2,
                InterviewConstants.MODE_NORMAL,
                null,
                InterviewConstants.FEEDBACK_MODE_AFTER_INTERVIEW,
                InterviewConstants.INTERACTION_TYPE_TEXT);

        assertFalse(result.contains("语音面试模式"));
    }

    @Test
    void buildInterviewerPersonaInstructionShouldReturnHrPersona() throws Exception {
        Method method = InterviewAiServiceImpl.class.getDeclaredMethod(
                "buildInterviewerPersonaInstruction", String.class);
        method.setAccessible(true);

        String result = (String) method.invoke(service, InterviewConstants.MODE_BIG_COMPANY_HR);
        assertTrue(result.contains("大厂 HR 面"));
        assertTrue(result.contains("行为面试"));
    }

    @Test
    void buildInterviewerPersonaInstructionShouldReturnTechLeaderPersona() throws Exception {
        Method method = InterviewAiServiceImpl.class.getDeclaredMethod(
                "buildInterviewerPersonaInstruction", String.class);
        method.setAccessible(true);

        String result = (String) method.invoke(service, InterviewConstants.MODE_TECH_LEADER);
        assertTrue(result.contains("技术 Leader 面"));
        assertTrue(result.contains("技术深度"));
    }

    @Test
    void buildInterviewerPersonaInstructionShouldReturnForeignPersona() throws Exception {
        Method method = InterviewAiServiceImpl.class.getDeclaredMethod(
                "buildInterviewerPersonaInstruction", String.class);
        method.setAccessible(true);

        String result = (String) method.invoke(service, InterviewConstants.MODE_FOREIGN_INTERVIEWER);
        assertTrue(result.contains("外企面试官"));
        assertTrue(result.contains("international company"));
    }

    @Test
    void buildInterviewerPersonaInstructionShouldReturnEmptyForNormal() throws Exception {
        Method method = InterviewAiServiceImpl.class.getDeclaredMethod(
                "buildInterviewerPersonaInstruction", String.class);
        method.setAccessible(true);

        String result = (String) method.invoke(service, InterviewConstants.MODE_NORMAL);
        assertEquals("", result);
    }

    @Test
    void buildInterviewerPersonaInstructionShouldReturnEmptyForStress() throws Exception {
        Method method = InterviewAiServiceImpl.class.getDeclaredMethod(
                "buildInterviewerPersonaInstruction", String.class);
        method.setAccessible(true);

        String result = (String) method.invoke(service, InterviewConstants.MODE_STRESS);
        assertEquals("", result);
    }

    @Test
    void applyPersonaToMockReplyShouldWorkForAllPersonas() throws Exception {
        Method method = InterviewAiServiceImpl.class.getDeclaredMethod(
                "applyPersonaToMockReply", String.class, String.class);
        method.setAccessible(true);

        String baseReply = "请继续介绍你的项目。";

        String hr = (String) method.invoke(service, baseReply, InterviewConstants.MODE_BIG_COMPANY_HR);
        assertTrue(hr.contains("HR 视角"));

        String tech = (String) method.invoke(service, baseReply, InterviewConstants.MODE_TECH_LEADER);
        assertTrue(tech.contains("技术 Leader 视角"));

        String foreign = (String) method.invoke(service, baseReply, InterviewConstants.MODE_FOREIGN_INTERVIEWER);
        assertTrue(foreign.contains("continue in English"));

        String normal = (String) method.invoke(service, baseReply, InterviewConstants.MODE_NORMAL);
        assertEquals(baseReply, normal);

        String nullMode = (String) method.invoke(service, baseReply, (String) null);
        assertEquals(baseReply, nullMode);
    }

    @Test
    void applyPersonaToMockReplyShouldHandleNullReply() throws Exception {
        Method method = InterviewAiServiceImpl.class.getDeclaredMethod(
                "applyPersonaToMockReply", String.class, String.class);
        method.setAccessible(true);

        String result = (String) method.invoke(service, null, InterviewConstants.MODE_TECH_LEADER);
        assertTrue(result.contains("技术 Leader 视角"));
    }

    @Test
    void applyStructuredImmediateFeedbackToMockReplyShouldAppendFeedbackBlockForImmediate() throws Exception {
        Method method = InterviewAiServiceImpl.class.getDeclaredMethod(
                "applyStructuredImmediateFeedbackToMockReply", String.class, String.class);
        method.setAccessible(true);

        String baseReply = "请具体说说你是怎么做的？";

        String immediate = (String) method.invoke(service, baseReply, InterviewConstants.FEEDBACK_MODE_IMMEDIATE);
        assertTrue(immediate.startsWith(baseReply));
        assertTrue(immediate.contains("<FEEDBACK>"));
        assertTrue(immediate.contains("</FEEDBACK>"));
        assertTrue(immediate.indexOf(baseReply) < immediate.indexOf("<FEEDBACK>"));

        String after = (String) method.invoke(service, baseReply, InterviewConstants.FEEDBACK_MODE_AFTER_INTERVIEW);
        assertEquals(baseReply, after);
    }

    @Test
    void applyStructuredImmediateFeedbackToMockReplyShouldNotDuplicateExistingFeedbackBlock() throws Exception {
        Method method = InterviewAiServiceImpl.class.getDeclaredMethod(
                "applyStructuredImmediateFeedbackToMockReply", String.class, String.class);
        method.setAccessible(true);

        String reply = "继续说说项目。\n\n<FEEDBACK>\n本题反馈：亮点明确；改进点是补充细节。\n</FEEDBACK>";

        String result = (String) method.invoke(service, reply, InterviewConstants.FEEDBACK_MODE_IMMEDIATE);

        assertEquals(reply, result);
    }

    @Test
    void shouldFallbackToLocalMockShouldRecognizeNetworkErrors() throws Exception {
        Method method = InterviewAiServiceImpl.class.getDeclaredMethod(
                "shouldFallbackToLocalMock", Throwable.class);
        method.setAccessible(true);

        assertTrue((boolean) method.invoke(service, new java.net.ConnectException("Connection refused")));
        assertTrue((boolean) method.invoke(service, new java.net.SocketTimeoutException("Read timed out")));
        assertFalse((boolean) method.invoke(service, (Throwable) null));
        assertFalse((boolean) method.invoke(service, new RuntimeException("业务异常")));
    }

    @Test
    void normalizeInterviewModeShouldReturnNullForInvalid() throws Exception {
        Method method = InterviewAiServiceImpl.class.getDeclaredMethod(
                "normalizeInterviewMode", String.class);
        method.setAccessible(true);

        assertNull(method.invoke(service, (Object) null));
        assertNull(method.invoke(service, ""));
        assertEquals("normal", method.invoke(service, "invalid_mode"));
    }

    @Test
    void normalizeInterviewModeShouldReturnNormalizedForValidModes() throws Exception {
        Method method = InterviewAiServiceImpl.class.getDeclaredMethod(
                "normalizeInterviewMode", String.class);
        method.setAccessible(true);

        assertEquals("normal", method.invoke(service, "NORMAL"));
        assertEquals("normal", method.invoke(service, "Normal"));
        assertEquals("stress", method.invoke(service, "Stress"));
        assertEquals("stress", method.invoke(service, "STRESS"));
        assertEquals("big_company_hr", method.invoke(service, "big_company_hr"));
        assertEquals("tech_leader", method.invoke(service, "TECH_LEADER"));
        assertEquals("foreign_interviewer", method.invoke(service, "FOREIGN_INTERVIEWER"));
    }

    @Test
    void normalizeDimensionScoresShouldHandleNullScores() throws Exception {
        Method method = InterviewAiServiceImpl.class.getDeclaredMethod(
                "normalizeDimensionScores", InterviewEvaluationReport.class);
        method.setAccessible(true);

        InterviewEvaluationReport report = InterviewEvaluationReport.builder()
                .overallScore(75)
                .build();

        assertDoesNotThrow(() -> method.invoke(service, report));
    }

    @Test
    void normalizeDimensionScoresShouldFillNullLists() throws Exception {
        Method method = InterviewAiServiceImpl.class.getDeclaredMethod(
                "normalizeDimensionScores", InterviewEvaluationReport.class);
        method.setAccessible(true);

        InterviewEvaluationReport report = InterviewEvaluationReport.builder()
                .overallScore(75)
                .technicalDepth(InterviewEvaluationReport.DimensionScore.builder().score(80).build())
                .projectExpression(InterviewEvaluationReport.DimensionScore.builder().score(75).strengths(List.of("a")).build())
                .communication(InterviewEvaluationReport.DimensionScore.builder().score(70).weaknesses(List.of("b")).build())
                .build();

        method.invoke(service, report);

        assertNotNull(report.getTechnicalDepth().getStrengths());
        assertNotNull(report.getTechnicalDepth().getWeaknesses());
        assertEquals(1, report.getProjectExpression().getStrengths().size());
    }

    @Test
    void parseEvaluationResponseShouldNormalizeNullCollections() throws Exception {
        Method parseMethod = InterviewAiServiceImpl.class.getDeclaredMethod(
                "parseEvaluationResponse", String.class);
        parseMethod.setAccessible(true);
        Method legacyMethod = InterviewAiServiceImpl.class.getDeclaredMethod(
                "mapLegacyFields", InterviewEvaluationReport.class);
        legacyMethod.setAccessible(true);

        String aiResponse = """
                {
                  "overallScore": 82,
                  "level": "A",
                  "finalVerdict": "pass",
                  "summary": "valid report",
                  "strengths": ["clear"],
                  "weaknesses": null,
                  "improvementSuggestions": null,
                  "technicalDepth": {"score": 81, "comment": "depth ok", "strengths": null, "weaknesses": null},
                  "projectExpression": {"score": 83, "comment": "project ok"},
                  "communication": {"score": 82, "comment": "communication ok"},
                  "problemSolving": {"score": 80, "comment": "problem solving ok"},
                  "pressureResistance": {"score": 79, "comment": "pressure ok"},
                  "jobMatch": {"score": 84, "comment": "job match ok"}
                }
                """;

        InterviewEvaluationReport report = (InterviewEvaluationReport) parseMethod.invoke(service, aiResponse);
        assertEquals(82, report.getOverallScore());
        assertEquals("valid report", report.getSummary());
        assertNotNull(report.getWeaknesses());
        assertNotNull(report.getImprovementSuggestions());
        assertNotNull(report.getTechnicalDepth().getStrengths());
        assertNotNull(report.getTechnicalDepth().getWeaknesses());

        assertDoesNotThrow(() -> legacyMethod.invoke(service, report));
        assertNotNull(report.getSuggestions());
        assertNotNull(report.getImprovements());
    }

    @Test
    void extractJsonFromResponseShouldWork() throws Exception {
        Method method = InterviewAiServiceImpl.class.getDeclaredMethod(
                "extractJsonFromResponse", String.class);
        method.setAccessible(true);

        String json = "{\"overallScore\": 80}";
        String wrapped = "一些文本\n" + json + "\n更多文本";
        assertEquals(json, method.invoke(service, wrapped));

        assertEquals("", method.invoke(service, ""));
        assertEquals("abc", method.invoke(service, "abc"));
    }

    @Test
    void buildDefaultEvaluationReportShouldHaveAllRequiredFields() throws Exception {
        Method method = InterviewAiServiceImpl.class.getDeclaredMethod("buildDefaultEvaluationReport");
        method.setAccessible(true);

        InterviewEvaluationReport report = (InterviewEvaluationReport) method.invoke(service);

        assertEquals(60, report.getOverallScore());
        assertEquals("C", report.getLevel());
        assertNotNull(report.getSummary());
        assertNotNull(report.getStrengths());
        assertNotNull(report.getWeaknesses());
        assertNotNull(report.getQuestionPerformance());
        assertNotNull(report.getRoundReviews());
        assertEquals(3, report.getImmediateActions().size());
        assertFalse(report.getFollowUpLossPoints().isEmpty());
        assertFalse(report.getCommonLossPatterns().isEmpty());
        assertNotNull(report.getTechnicalDepth());
        assertNotNull(report.getProjectExpression());
        assertNotNull(report.getCommunication());
        assertNotNull(report.getProblemSolving());
        assertNotNull(report.getPressureResistance());
        assertNotNull(report.getJobMatch());
    }

    @Test
    void generateOpeningShouldReturnFormattedString() {
        String opening = service.generateOpening("Java工程师", "java", 2, null);
        assertTrue(opening.contains("Java工程师"));
        assertTrue(opening.contains("中级"));
    }

    @Test
    void generateOpeningShouldIncludeResumeHintWhenContextExists() {
        com.airesume.server.dto.interview.InterviewJobTargetContext context =
                com.airesume.server.dto.interview.InterviewJobTargetContext.builder()
                        .jobTargeted(true)
                        .resumeText("5年Java经验，精通Spring Cloud")
                        .build();
        String opening = service.generateOpening("Java工程师", "java", 2, context);
        assertTrue(opening.contains("看过你的简历"));
    }

    @Test
    void supportsThinkingShouldHandleNullModel() throws Exception {
        Method method = InterviewAiServiceImpl.class.getDeclaredMethod(
                "supportsThinking", String.class);
        method.setAccessible(true);

        assertFalse((boolean) method.invoke(service, (Object) null));
        assertFalse((boolean) method.invoke(service, "gpt-4"));
    }

    @Test
    void supportsThinkingShouldRecognizeSeedModel() throws Exception {
        Method method = InterviewAiServiceImpl.class.getDeclaredMethod(
                "supportsThinking", String.class);
        method.setAccessible(true);

        assertTrue((boolean) method.invoke(service, "doubao-seed-2.0"));
        assertTrue((boolean) method.invoke(service, "DOUBAO-SEED-2.0"));
    }

    @Test
    void resolveFallbackJobRoleShouldHandleNull() throws Exception {
        Method method = InterviewAiServiceImpl.class.getDeclaredMethod(
                "resolveFallbackJobRole", String.class);
        method.setAccessible(true);

        assertEquals("软件工程师", method.invoke(service, (Object) null));
        assertEquals("软件工程师", method.invoke(service, ""));
        assertEquals("Java工程师", method.invoke(service, "Java工程师"));
    }

    @Test
    void buildThinkingConfigShouldReturnNullForNone() throws Exception {
        Method method = InterviewAiServiceImpl.class.getDeclaredMethod(
                "buildThinkingConfig", String.class, String.class);
        method.setAccessible(true);

        assertNull(method.invoke(service, "gpt-4", "none"));
        assertNull(method.invoke(service, "gpt-4", "unknown_mode"));
    }

    @Test
    void buildThinkingConfigShouldReturnEnabledForEnabledMode() throws Exception {
        Method method = InterviewAiServiceImpl.class.getDeclaredMethod(
                "buildThinkingConfig", String.class, String.class);
        method.setAccessible(true);

        Object result = method.invoke(service, "doubao-seed-2.0", "enabled");
        assertNotNull(result);
    }
}
