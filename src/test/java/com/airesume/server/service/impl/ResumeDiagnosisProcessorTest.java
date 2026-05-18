package com.airesume.server.service.impl;

import com.airesume.server.common.constants.ResumeDiagnosisConstants;
import com.airesume.server.dto.resume.ResumeDiagnosisResult;
import com.airesume.server.service.NotificationService;
import com.airesume.server.service.ResumeAiService;
import com.airesume.server.service.ResumeContentExtractor;
import com.airesume.server.service.ResumeDiagnosisTaskService;
import com.airesume.server.service.ResumeInfoExtractor;
import com.airesume.server.service.UserQuotaService;
import com.airesume.server.service.resume.ResumeParseResult;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

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

    @Test
    void processTaskShouldNormalizeMinimalDiagnosisResult() throws Exception {
        ResumeDiagnosisTaskService taskService = mock(ResumeDiagnosisTaskService.class);
        ResumeContentExtractor resumeContentExtractor = mock(ResumeContentExtractor.class);
        ResumeAiService resumeAiService = mock(ResumeAiService.class);
        ResumeInfoExtractor resumeInfoExtractor = mock(ResumeInfoExtractor.class);
        UserQuotaService quotaService = mock(UserQuotaService.class);
        NotificationService notificationService = mock(NotificationService.class);

        when(taskService.getTaskStatus(1L)).thenReturn(ResumeDiagnosisConstants.STATUS_PENDING);
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

        ResumeDiagnosisProcessor processor = new ResumeDiagnosisProcessor(
                taskService,
                resumeContentExtractor,
                resumeAiService,
                resumeInfoExtractor,
                objectMapper,
                quotaService,
                notificationService);

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
    void processTaskShouldFailAndRefundOnTimeout() {
        ResumeDiagnosisTaskService taskService = mock(ResumeDiagnosisTaskService.class);
        ResumeContentExtractor resumeContentExtractor = mock(ResumeContentExtractor.class);
        ResumeAiService resumeAiService = mock(ResumeAiService.class);
        ResumeInfoExtractor resumeInfoExtractor = mock(ResumeInfoExtractor.class);
        UserQuotaService quotaService = mock(UserQuotaService.class);
        NotificationService notificationService = mock(NotificationService.class);

        when(taskService.getTaskStatus(1L)).thenReturn(ResumeDiagnosisConstants.STATUS_PENDING);
        when(taskService.updateStatusToProcessing(1L)).thenReturn(true);
        when(resumeContentExtractor.extract("/resume.pdf")).thenReturn(
                ResumeParseResult.builder().text("简历内容").parseMode("TEXT").build());
        when(resumeAiService.diagnose(anyString())).thenThrow(
                new RuntimeException("call failed", new SocketTimeoutException("Read timed out")));

        ResumeDiagnosisProcessor processor = new ResumeDiagnosisProcessor(
                taskService,
                resumeContentExtractor,
                resumeAiService,
                resumeInfoExtractor,
                objectMapper,
                quotaService,
                notificationService);

        processor.processTask(1L, 2L, "/resume.pdf");

        verify(quotaService).refundResumeQuota(2L);
        verify(taskService).updateStatusToFailed(1L, "AI分析超时，请稍后重试");
    }

    @Test
    void processTaskShouldSkipWhenTaskAlreadyClaimedByAnotherWorker() {
        ResumeDiagnosisTaskService taskService = mock(ResumeDiagnosisTaskService.class);
        ResumeContentExtractor resumeContentExtractor = mock(ResumeContentExtractor.class);
        ResumeAiService resumeAiService = mock(ResumeAiService.class);
        ResumeInfoExtractor resumeInfoExtractor = mock(ResumeInfoExtractor.class);
        UserQuotaService quotaService = mock(UserQuotaService.class);
        NotificationService notificationService = mock(NotificationService.class);

        when(taskService.getTaskStatus(1L)).thenReturn(ResumeDiagnosisConstants.STATUS_PENDING);
        when(taskService.updateStatusToProcessing(1L)).thenReturn(false);

        ResumeDiagnosisProcessor processor = new ResumeDiagnosisProcessor(
                taskService,
                resumeContentExtractor,
                resumeAiService,
                resumeInfoExtractor,
                objectMapper,
                quotaService,
                notificationService);

        processor.processTask(1L, 2L, "/resume.pdf");

        verify(resumeContentExtractor, never()).extract(anyString());
        verify(resumeAiService, never()).diagnose(anyString());
        verify(quotaService, never()).refundResumeQuota(anyLong());
    }
}
