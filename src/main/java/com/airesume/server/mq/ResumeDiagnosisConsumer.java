package com.airesume.server.mq;

import com.airesume.server.common.constants.ResumeDiagnosisConstants;
import com.airesume.server.service.PdfTextExtractor;
import com.airesume.server.service.ResumeAiService;
import com.airesume.server.service.ResumeDiagnosisTaskService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class ResumeDiagnosisConsumer {

    private final ResumeDiagnosisTaskService resumeDiagnosisTaskService;
    private final PdfTextExtractor pdfTextExtractor;
    private final ResumeAiService resumeAiService;

    @RabbitListener(queues = ResumeDiagnosisConstants.QUEUE_RESUME_DIAGNOSIS)
    public void handleResumeDiagnosisTask(ResumeDiagnosisMessage message) {
        Long taskId = message.getTaskId();
        log.info("收到简历诊断任务, taskId: {}, userId: {}", taskId, message.getUserId());

        try {
            resumeDiagnosisTaskService.updateStatusToProcessing(taskId);

            String resumeText = pdfTextExtractor.extractText(message.getFileUrl());
            log.info("PDF 文本提取成功, taskId: {}, textLength: {}", taskId, resumeText.length());

            String diagnosisResult = resumeAiService.diagnose(resumeText);

            if (diagnosisResult == null || diagnosisResult.isBlank()) {
                throw new RuntimeException("AI 诊断返回结果为空");
            }

            resumeDiagnosisTaskService.updateStatusToCompleted(taskId, diagnosisResult);
            log.info("简历诊断任务完成, taskId: {}", taskId);

        } catch (PdfTextExtractor.PdfExtractionException e) {
            log.error("PDF 解析失败, taskId: {}", taskId, e);
            markTaskFailed(taskId, "PDF解析失败: " + e.getMessage());

        } catch (RuntimeException e) {
            log.error("简历诊断业务异常, taskId: {}", taskId, e);
            markTaskFailed(taskId, e.getMessage());

        } catch (Exception e) {
            log.error("简历诊断未知异常, taskId: {}", taskId, e);
            markTaskFailed(taskId, "系统异常: " + e.getMessage());
        }
    }

    private void markTaskFailed(Long taskId, String errorMsg) {
        try {
            resumeDiagnosisTaskService.updateStatusToFailed(taskId, errorMsg);
            log.info("任务已标记失败, taskId: {}, error: {}", taskId, errorMsg);
        } catch (Exception ex) {
            log.error("标记任务失败失败, taskId: {}", taskId, ex);
        }
    }
}
