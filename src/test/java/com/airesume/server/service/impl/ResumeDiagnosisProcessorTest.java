package com.airesume.server.service.impl;

import com.airesume.server.common.constants.ResumeDiagnosisConstants;
import com.airesume.server.common.exception.BusinessException;
import com.airesume.server.common.result.ResultCode;
import com.airesume.server.dto.resume.ResumeDiagnosisResult;
import com.airesume.server.entity.ResumeDiagnosisTask;
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
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class ResumeDiagnosisProcessorTest {

    private final ObjectMapper objectMapper = new ObjectMapper();
    private ResumeDiagnosisTaskService taskService;
    private ResumeContentExtractor resumeContentExtractor;
    private ResumeAiService resumeAiService;
    private ResumeInfoExtractor resumeInfoExtractor;
    private UserQuotaService quotaService;
    private NotificationService notificationService;
    private ResumeDiagnosisProcessor processor;

    @BeforeEach
    void setUp() {
        taskService = mock(ResumeDiagnosisTaskService.class);
        resumeContentExtractor = mock(ResumeContentExtractor.class);
        resumeAiService = mock(ResumeAiService.class);
        resumeInfoExtractor = mock(ResumeInfoExtractor.class);
        quotaService = mock(UserQuotaService.class);
        notificationService = mock(NotificationService.class);
        processor = new ResumeDiagnosisProcessor(
                taskService,
                resumeContentExtractor,
                resumeAiService,
                resumeInfoExtractor,
                objectMapper,
                quotaService,
                notificationService);
    }

    @Test
    void processTaskShouldNormalizeMinimalDiagnosisResult() throws Exception {
        when(taskService.getById(1L)).thenReturn(newPendingTask());
        when(taskService.updateStatusToProcessing(1L)).thenReturn(true);
        when(resumeContentExtractor.extract("/resume.pdf")).thenReturn(
                ResumeParseResult.builder().text("张三 13800000000 zhangsan@test.com").parseMode("TEXT").build());
        when(resumeAiService.diagnose(anyString())).thenReturn("""
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
        when(taskService.getById(1L)).thenReturn(newPendingTask());
        when(taskService.updateStatusToProcessing(1L)).thenReturn(true);
        when(resumeContentExtractor.extract("/resume.pdf")).thenReturn(
                ResumeParseResult.builder().text("Java 后端开发 简历内容").parseMode("TEXT").build());
        when(resumeAiService.diagnose(anyString())).thenReturn("""
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
        when(taskService.getById(1L)).thenReturn(newPendingTask());
        when(taskService.updateStatusToProcessing(1L)).thenReturn(true);
        when(resumeContentExtractor.extract("/resume.pdf")).thenReturn(
                ResumeParseResult.builder().text("Java 后端开发 简历内容").parseMode("TEXT").build());
        when(resumeAiService.diagnose(anyString())).thenReturn("""
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
    void processTaskShouldFailAndRefundOnTimeout() {
        when(taskService.getById(1L)).thenReturn(newPendingTask());
        when(taskService.updateStatusToProcessing(1L)).thenReturn(true);
        when(resumeContentExtractor.extract("/resume.pdf")).thenReturn(
                ResumeParseResult.builder().text("简历内容").parseMode("TEXT").build());
        when(resumeAiService.diagnose(anyString())).thenThrow(
                new RuntimeException("call failed", new SocketTimeoutException("Read timed out")));

        processor.processTask(1L, 2L, "/resume.pdf");

        verify(quotaService).refundResumeQuota(2L);
        verify(taskService).updateStatusToFailed(1L, "AI分析超时，请稍后重试");
    }

    @Test
    void processTaskShouldMapStructuredAiBusinessException() {
        when(taskService.getById(1L)).thenReturn(newPendingTask());
        when(taskService.updateStatusToProcessing(1L)).thenReturn(true);
        when(resumeContentExtractor.extract("/resume.pdf")).thenReturn(
                ResumeParseResult.builder().text("简历内容").parseMode("TEXT").build());
        when(resumeAiService.diagnose(anyString()))
                .thenThrow(new BusinessException(ResultCode.AI_SERVICE_UNAVAILABLE));

        processor.processTask(1L, 2L, "/resume.pdf");

        verify(quotaService).refundResumeQuota(2L);
        verify(taskService).updateStatusToFailed(1L, "AI服务暂时不可用，请稍后重试");
    }

    @Test
    void processTaskShouldMapBlankAiResponseToStructuredMessage() {
        when(taskService.getById(1L)).thenReturn(newPendingTask());
        when(taskService.updateStatusToProcessing(1L)).thenReturn(true);
        when(resumeContentExtractor.extract("/resume.pdf")).thenReturn(
                ResumeParseResult.builder().text("简历内容").parseMode("TEXT").build());
        when(resumeAiService.diagnose(anyString())).thenReturn(" ");

        processor.processTask(1L, 2L, "/resume.pdf");

        verify(taskService).updateStatusToFailed(1L, "AI分析返回结果为空，请稍后重试");
    }

    @Test
    void processTaskShouldSkipWhenTaskAlreadyClaimedByAnotherWorker() {
        when(taskService.getById(1L)).thenReturn(newPendingTask());
        when(taskService.updateStatusToProcessing(1L)).thenReturn(false);

        processor.processTask(1L, 2L, "/resume.pdf");

        verify(resumeContentExtractor, never()).extract(anyString());
        verify(resumeAiService, never()).diagnose(anyString());
        verify(quotaService, never()).refundResumeQuota(anyLong());
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
