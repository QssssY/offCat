package com.airesume.server.service.impl;

import com.airesume.server.common.constants.ResumeDiagnosisConstants;
import com.airesume.server.common.constants.UserAiConstants;
import com.airesume.server.common.exception.BusinessException;
import com.airesume.server.common.result.ResultCode;
import com.airesume.server.dto.resume.ResumeDiagnosisResult;
import com.airesume.server.entity.ResumeDiagnosisTask;
import com.airesume.server.mapper.ResumeDiagnosisTaskMapper;
import com.airesume.server.service.NotificationService;
import com.airesume.server.service.ResumeAiService;
import com.airesume.server.service.ResumeContentExtractor;
import com.airesume.server.service.ResumeDiagnosisTaskService;
import com.airesume.server.service.ResumeInfoExtractor;
import com.airesume.server.service.UserQuotaService;
import com.airesume.server.service.resume.ResumeParseResult;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.test.util.ReflectionTestUtils;

import java.net.SocketTimeoutException;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class ResumeDiagnosisProcessorTest {

    private final ObjectMapper objectMapper = new ObjectMapper();
    private ResumeDiagnosisTaskService taskService;
    private ResumeDiagnosisTaskMapper resumeDiagnosisTaskMapper;
    private ResumeContentExtractor resumeContentExtractor;
    private ResumeAiService resumeAiService;
    private ResumeInfoExtractor resumeInfoExtractor;
    private UserQuotaService quotaService;
    private NotificationService notificationService;
    private ResumeDiagnosisProcessor processor;

    @BeforeEach
    void setUp() {
        taskService = mock(ResumeDiagnosisTaskService.class);
        resumeDiagnosisTaskMapper = mock(ResumeDiagnosisTaskMapper.class);
        resumeContentExtractor = mock(ResumeContentExtractor.class);
        resumeAiService = mock(ResumeAiService.class);
        resumeInfoExtractor = mock(ResumeInfoExtractor.class);
        quotaService = mock(UserQuotaService.class);
        notificationService = mock(NotificationService.class);
        processor = new ResumeDiagnosisProcessor(
                taskService,
                resumeDiagnosisTaskMapper,
                resumeContentExtractor,
                resumeAiService,
                resumeInfoExtractor,
                objectMapper,
                quotaService,
                notificationService);
    }

    @Test
    void processTaskShouldNormalizeMinimalDiagnosisResult() throws Exception {
        when(resumeDiagnosisTaskMapper.selectOne(any())).thenReturn(newPendingTask());
        when(taskService.updateStatusToProcessing(1L)).thenReturn(true);
        when(resumeContentExtractor.extract("/resume.pdf", 2L, false, false)).thenReturn(
                ResumeParseResult.builder().text("张三 13800000000 zhangsan@test.com").parseMode("TEXT").build());
        when(resumeAiService.diagnose(anyString(), eq(2L), eq(false), eq(false))).thenReturn("""
                {"overallEvaluation":{"totalScore":72,"level":"B","summary":"基础还可以"}}
                """);
        when(resumeInfoExtractor.extractBasicInfo(anyString())).thenReturn(
                ResumeDiagnosisResult.BasicInfoDetails.builder()
                        .name("张三")
                        .email("zhangsan@test.com")
                        .phone("13800000000")
                        .github("")
                        .blog("")
                        .build());

        processor.processTask(1L, 2L, "/resume.pdf");

        ArgumentCaptor<String> resultCaptor = ArgumentCaptor.forClass(String.class);
        verify(taskService).updateStatusToCompleted(org.mockito.ArgumentMatchers.eq(1L), resultCaptor.capture());
        verify(quotaService, never()).refundResumeQuota(anyLong());

        JsonNode result = objectMapper.readTree(resultCaptor.getValue());
        assertTrue(result.has("basicInfoEvaluation"));
        assertTrue(result.has("skillEvaluation"));
        assertTrue(result.has("workExperienceEvaluation"));
        assertTrue(result.has("projectExperienceEvaluation"));
        assertTrue(result.has("educationEvaluation"));
        assertEquals("张三", result.path("basicInfoDetails").path("name").asText());
        assertEquals("zhangsan@test.com", result.path("basicInfoDetails").path("email").asText());
        assertTrue(result.path("basicInfoEvaluation").path("hasName").asBoolean());
    }

    @Test
    void processTaskShouldRecalculateOverallScoreFromDimensionScores() throws Exception {
        when(resumeDiagnosisTaskMapper.selectOne(any())).thenReturn(newPendingTask());
        when(taskService.updateStatusToProcessing(1L)).thenReturn(true);
        when(resumeContentExtractor.extract("/resume.pdf", 2L, false, false)).thenReturn(
                ResumeParseResult.builder().text("Java 后端开发 简历内容").parseMode("TEXT").build());
        when(resumeAiService.diagnose(anyString(), eq(2L), eq(false), eq(false))).thenReturn("""
                {
                  "overallEvaluation":{"totalScore":86,"level":"A","summary":"模型原始总分偏高"},
                  "basicInfoEvaluation":{"score":80},
                  "skillEvaluation":{"score":70},
                  "workExperienceEvaluation":{"score":60},
                  "projectExperienceEvaluation":{"score":75},
                  "educationEvaluation":{"score":90},
                  "positioningEvaluation":{"score":65}
                }
                """);
        when(resumeInfoExtractor.extractBasicInfo(anyString())).thenReturn(
                ResumeDiagnosisResult.BasicInfoDetails.builder().build());

        processor.processTask(1L, 2L, "/resume.pdf");

        ArgumentCaptor<String> resultCaptor = ArgumentCaptor.forClass(String.class);
        verify(taskService).updateStatusToCompleted(org.mockito.ArgumentMatchers.eq(1L), resultCaptor.capture());

        JsonNode result = objectMapper.readTree(resultCaptor.getValue());
        assertEquals(71, result.path("overallEvaluation").path("totalScore").asInt());
        assertEquals("B", result.path("overallEvaluation").path("level").asText());
    }

    @Test
    void processTaskShouldRenormalizeOverallScoreWhenDimensionScoreMissing() throws Exception {
        when(resumeDiagnosisTaskMapper.selectOne(any())).thenReturn(newPendingTask());
        when(taskService.updateStatusToProcessing(1L)).thenReturn(true);
        when(resumeContentExtractor.extract("/resume.pdf", 2L, false, false)).thenReturn(
                ResumeParseResult.builder().text("Java 后端开发 简历内容").parseMode("TEXT").build());
        when(resumeAiService.diagnose(anyString(), eq(2L), eq(false), eq(false))).thenReturn("""
                {
                  "overallEvaluation":{"totalScore":86,"level":"A","summary":"模型原始总分偏高"},
                  "basicInfoEvaluation":{"score":80},
                  "skillEvaluation":{"score":80},
                  "workExperienceEvaluation":{"score":80},
                  "projectExperienceEvaluation":{"score":80},
                  "educationEvaluation":{"score":80}
                }
                """);
        when(resumeInfoExtractor.extractBasicInfo(anyString())).thenReturn(
                ResumeDiagnosisResult.BasicInfoDetails.builder().build());

        processor.processTask(1L, 2L, "/resume.pdf");

        ArgumentCaptor<String> resultCaptor = ArgumentCaptor.forClass(String.class);
        verify(taskService).updateStatusToCompleted(org.mockito.ArgumentMatchers.eq(1L), resultCaptor.capture());

        JsonNode result = objectMapper.readTree(resultCaptor.getValue());
        assertEquals(80, result.path("overallEvaluation").path("totalScore").asInt());
        assertEquals("A", result.path("overallEvaluation").path("level").asText());
    }

    @Test
    void processTaskShouldPreserveCommonAiFieldAliasesDuringNormalization() throws Exception {
        when(resumeDiagnosisTaskMapper.selectOne(any())).thenReturn(newPendingTask());
        when(taskService.updateStatusToProcessing(1L)).thenReturn(true);
        when(resumeContentExtractor.extract("/resume.pdf", 2L, false, false)).thenReturn(
                ResumeParseResult.builder().text("Java 后端开发 简历内容").parseMode("TEXT").build());
        when(resumeAiService.diagnose(anyString(), eq(2L), eq(false), eq(false))).thenReturn("""
                {
                  "overallEvaluation":{"totalScore":82,"level":"A","summary":"模型按别名字段返回了完整诊断"},
                  "skills":{"score":68,"skillList":["Java","Spring Boot"],"evaluation":"技能覆盖基础后端栈，但缺少场景和熟练度说明。","strengths":["技术栈覆盖 Java 后端基础"],"weaknesses":["缺少技能使用场景"],"suggestions":["补充技术在项目中的具体应用"]},
                  "workExperience":{"score":55,"experiences":[{"company":"某科技公司","position":"Java 实习生","duration":"2025.01-2025.03","highlights":["参与接口开发"]}],"evaluation":"实习信息存在但成果量化不足。","strengths":["有真实实习经历"],"weaknesses":["缺少业务结果"],"suggestions":["补充接口性能或业务指标"]},
                  "projectExperience":{"score":62,"projects":[{"name":"智能简历系统","role":"后端开发","techStack":"Spring Boot","highlights":["完成诊断链路"]}],"evaluation":"项目有技术栈但个人贡献边界还不够清晰。","strengths":["项目方向贴近岗位"],"weaknesses":["个人贡献描述偏笼统"],"suggestions":["写清个人负责模块"]},
                  "education":{"score":90,"degree":"本科","school":"测试大学","major":"软件工程","hasRelevantMajor":true,"evaluation":"专业相关度高。","strengths":["专业匹配"],"weaknesses":["课程成果未展开"],"suggestions":["补充课程项目"]},
                  "positioning":{"score":50,"evaluation":"求职方向存在但差异化不足。","strengths":["岗位方向明确"],"weaknesses":["核心优势不突出"],"suggestions":["增加个人定位摘要"]},
                  "suggestions":["优先补充量化成果"]
                }
                """);
        when(resumeInfoExtractor.extractBasicInfo(anyString())).thenReturn(
                ResumeDiagnosisResult.BasicInfoDetails.builder().build());

        processor.processTask(1L, 2L, "/resume.pdf");

        ArgumentCaptor<String> resultCaptor = ArgumentCaptor.forClass(String.class);
        verify(taskService).updateStatusToCompleted(org.mockito.ArgumentMatchers.eq(1L), resultCaptor.capture());

        JsonNode result = objectMapper.readTree(resultCaptor.getValue());
        assertEquals(68, result.path("skillEvaluation").path("score").asInt());
        assertEquals("Java", result.path("skillEvaluation").path("skillList").get(0).asText());
        assertEquals(55, result.path("workExperienceEvaluation").path("score").asInt());
        assertEquals("某科技公司", result.path("workExperienceEvaluation").path("experiences").get(0).path("company").asText());
        assertEquals(62, result.path("projectExperienceEvaluation").path("score").asInt());
        assertEquals("智能简历系统", result.path("projectExperienceEvaluation").path("projects").get(0).path("name").asText());
        assertEquals(90, result.path("educationEvaluation").path("score").asInt());
        assertEquals(50, result.path("positioningEvaluation").path("score").asInt());
        assertEquals("优先补充量化成果", result.path("optimizationSuggestions").get(0).asText());
    }

    @Test
    void processTaskShouldFailAndRefundOnTimeout() {
        when(resumeDiagnosisTaskMapper.selectOne(any())).thenReturn(newPendingTask());
        when(taskService.updateStatusToProcessing(1L)).thenReturn(true);
        when(resumeContentExtractor.extract("/resume.pdf", 2L, false, false)).thenReturn(
                ResumeParseResult.builder().text("简历内容").parseMode("TEXT").build());
        when(resumeAiService.diagnose(anyString(), eq(2L), eq(false), eq(false))).thenThrow(
                new RuntimeException("call failed", new SocketTimeoutException("Read timed out")));

        processor.processTask(1L, 2L, "/resume.pdf");

        verify(quotaService).refundResumeQuota(2L);
        verify(taskService).updateStatusToFailed(1L, "AI分析超时，请稍后重试");
    }

    @Test
    void processTaskShouldMapStructuredAiBusinessException() {
        when(resumeDiagnosisTaskMapper.selectOne(any())).thenReturn(newPendingTask());
        when(taskService.updateStatusToProcessing(1L)).thenReturn(true);
        when(resumeContentExtractor.extract("/resume.pdf", 2L, false, false)).thenReturn(
                ResumeParseResult.builder().text("简历内容").parseMode("TEXT").build());
        when(resumeAiService.diagnose(anyString(), eq(2L), eq(false), eq(false)))
                .thenThrow(new BusinessException(ResultCode.AI_SERVICE_UNAVAILABLE));

        processor.processTask(1L, 2L, "/resume.pdf");

        verify(quotaService).refundResumeQuota(2L);
        verify(taskService).updateStatusToFailed(1L, "AI服务暂时不可用，请稍后重试");
    }

    @Test
    void processTaskShouldMapBlankAiResponseToStructuredMessage() {
        when(resumeDiagnosisTaskMapper.selectOne(any())).thenReturn(newPendingTask());
        when(taskService.updateStatusToProcessing(1L)).thenReturn(true);
        when(resumeContentExtractor.extract("/resume.pdf", 2L, false, false)).thenReturn(
                ResumeParseResult.builder().text("简历内容").parseMode("TEXT").build());
        when(resumeAiService.diagnose(anyString(), eq(2L), eq(false), eq(false))).thenReturn(" ");

        processor.processTask(1L, 2L, "/resume.pdf");

        verify(taskService).updateStatusToFailed(1L, "AI分析返回结果为空，请稍后重试");
    }

    @Test
    void processTaskShouldSkipWhenTaskAlreadyClaimedByAnotherWorker() {
        when(resumeDiagnosisTaskMapper.selectOne(any())).thenReturn(newPendingTask());
        when(taskService.updateStatusToProcessing(1L)).thenReturn(false);

        processor.processTask(1L, 2L, "/resume.pdf");

        verify(resumeContentExtractor, never()).extract(anyString(), anyLong(), anyBoolean(), anyBoolean());
        verify(resumeAiService, never()).diagnose(anyString(), anyLong(), anyBoolean(), anyBoolean());
        verify(quotaService, never()).refundResumeQuota(anyLong());
    }

    @Test
    void processTaskShouldUseLockedCustomAiContextForExtractionAndDiagnosis() throws Exception {
        ResumeDiagnosisTask task = newPendingTask();
        task.setAiBillingSource(UserAiConstants.BILLING_SOURCE_USER_CUSTOM);
        task.setFallbackToPlatform(0);
        when(resumeDiagnosisTaskMapper.selectOne(any())).thenReturn(task);
        when(taskService.updateStatusToProcessing(1L)).thenReturn(true);
        when(resumeContentExtractor.extract("/resume.pdf", 2L, false, true)).thenReturn(
                ResumeParseResult.builder().text("图片简历识别文本").parseMode("MULTIMODAL").build());
        when(resumeAiService.diagnose("图片简历识别文本", 2L, false, true)).thenReturn("""
                {"overallEvaluation":{"totalScore":70,"level":"B","summary":"已按自定义AI诊断"}}
                """);
        when(resumeInfoExtractor.extractBasicInfo(anyString())).thenReturn(
                ResumeDiagnosisResult.BasicInfoDetails.builder().build());

        processor.processTask(1L, 2L, "/resume.pdf");

        verify(resumeContentExtractor).extract("/resume.pdf", 2L, false, true);
        verify(resumeAiService).diagnose("图片简历识别文本", 2L, false, true);
        ArgumentCaptor<String> resultCaptor = ArgumentCaptor.forClass(String.class);
        verify(taskService).updateStatusToCompleted(eq(1L), resultCaptor.capture());
        JsonNode result = objectMapper.readTree(resultCaptor.getValue());
        assertEquals(70, result.path("overallEvaluation").path("totalScore").asInt());
    }

    private ResumeDiagnosisTask newPendingTask() {
        ResumeDiagnosisTask task = new ResumeDiagnosisTask();
        task.setId(1L);
        task.setStatus(ResumeDiagnosisConstants.STATUS_PENDING);
        return task;
    }

    @Test
    void normalizeOverallEvaluationShouldFallbackToDWhenTotalScoreMissing() {
        ResumeDiagnosisResult.OverallEvaluation source = ResumeDiagnosisResult.OverallEvaluation.builder()
                .level("S - 优秀")
                .build();

        ResumeDiagnosisResult diagnosisResult = ResumeDiagnosisResult.builder()
                .overallEvaluation(source)
                .build();

        ResumeDiagnosisResult.OverallEvaluation result = ReflectionTestUtils.invokeMethod(
                processor,
                "normalizeOverallEvaluation",
                source,
                diagnosisResult);

        assertEquals("D", result.getLevel());
    }
}
