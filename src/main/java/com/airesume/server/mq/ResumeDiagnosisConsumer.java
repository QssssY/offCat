package com.airesume.server.mq;

import com.airesume.server.common.constants.ResumeDiagnosisConstants;
import com.airesume.server.dto.resume.ResumeDiagnosisResult;
import com.airesume.server.service.NotificationService;
import com.airesume.server.service.PdfTextExtractor;
import com.airesume.server.service.ResumeAiService;
import com.airesume.server.service.ResumeDiagnosisTaskService;
import com.airesume.server.service.ResumeInfoExtractor;
import com.airesume.server.service.UserQuotaService;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

import java.net.SocketTimeoutException;
import java.net.http.HttpTimeoutException;
import java.util.concurrent.TimeoutException;

/**
 * 简历诊断消息消费者。
 *
 * 核心链路：
 * 1. 从队列中读取待处理任务。
 * 2. 提取 PDF 文本并缓存到任务表。
 * 3. 调用 AI 完成诊断。
 * 4. 用后端规则补齐 basicInfoDetails。
 * 5. 更新任务状态并发送完成通知。
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class ResumeDiagnosisConsumer {

    private final ResumeDiagnosisTaskService resumeDiagnosisTaskService;
    private final PdfTextExtractor pdfTextExtractor;
    private final ResumeAiService resumeAiService;
    private final ResumeInfoExtractor resumeInfoExtractor;
    private final ObjectMapper objectMapper;
    private final UserQuotaService userQuotaService;
    private final NotificationService notificationService;

    @RabbitListener(queues = ResumeDiagnosisConstants.QUEUE_RESUME_DIAGNOSIS)
    public void handleResumeDiagnosisTask(ResumeDiagnosisMessage message) {
        Long taskId = message.getTaskId();
        long taskStartTime = System.currentTimeMillis();
        log.info("收到简历诊断任务, taskId: {}, userId: {}", taskId, message.getUserId());

        try {
            // 前置检查：只有待处理任务才允许消费，避免重复消费或把已失败任务再次拉起。
            Integer currentStatus = resumeDiagnosisTaskService.getTaskStatus(taskId);
            if (currentStatus == null) {
                log.warn("任务不存在, 跳过处理, taskId: {}", taskId);
                return;
            }
            if (currentStatus != ResumeDiagnosisConstants.STATUS_PENDING) {
                log.warn("任务状态不是待处理, 跳过消费, taskId: {}, status: {}", taskId, currentStatus);
                return;
            }

            resumeDiagnosisTaskService.updateStatusToProcessing(taskId);

            // 分段记录 PDF 提取耗时，方便判断慢在文本提取还是慢在 AI 生成。
            long pdfStartTime = System.currentTimeMillis();
            String resumeText = pdfTextExtractor.extractText(message.getFileUrl());
            long pdfElapsed = System.currentTimeMillis() - pdfStartTime;
            log.info("PDF 文本提取完成, taskId: {}, textLength: {}, elapsedMs: {}",
                    taskId, resumeText.length(), pdfElapsed);

            try {
                resumeDiagnosisTaskService.updateTaskResumeText(taskId, resumeText);
            } catch (Exception e) {
                log.warn("缓存简历文本失败, taskId: {}, 不影响主流程", taskId, e);
            }

            long aiStartTime = System.currentTimeMillis();
            String diagnosisResult = resumeAiService.diagnose(resumeText);
            long aiElapsed = System.currentTimeMillis() - aiStartTime;
            if (diagnosisResult == null || diagnosisResult.isBlank()) {
                throw new RuntimeException("AI 诊断返回结果为空");
            }

            long enhanceStartTime = System.currentTimeMillis();
            String enhancedResult = enhanceDiagnosisResult(diagnosisResult, resumeText);
            long enhanceElapsed = System.currentTimeMillis() - enhanceStartTime;

            // 清理控制字符，防止 JSON 入库时出现非法编码问题。
            enhancedResult = enhancedResult.replaceAll("[\\x00-\\x08\\x0B\\x0C\\x0E-\\x1F]", "");

            long persistStartTime = System.currentTimeMillis();
            resumeDiagnosisTaskService.updateStatusToCompleted(taskId, enhancedResult);
            long persistElapsed = System.currentTimeMillis() - persistStartTime;

            long totalElapsed = System.currentTimeMillis() - taskStartTime;
            log.info("简历诊断任务完成, taskId: {}, pdfMs: {}, aiMs: {}, enhanceMs: {}, persistMs: {}, totalMs: {}",
                    taskId, pdfElapsed, aiElapsed, enhanceElapsed, persistElapsed, totalElapsed);

            notificationService.createNotification(
                    message.getUserId(),
                    "resume",
                    "简历诊断完成",
                    "你的简历诊断报告已生成，点击查看结果。",
                    "resume_diagnosis",
                    String.valueOf(taskId));
        } catch (PdfTextExtractor.PdfExtractionException e) {
            log.error("PDF 解析失败, taskId: {}", taskId, e);
            refundQuotaIfNeeded(message.getUserId(), taskId);
            markTaskFailed(taskId, "PDF解析失败: " + e.getMessage());
        } catch (RuntimeException e) {
            logDiagnosisFailure(taskId, taskStartTime, e);
            refundQuotaIfNeeded(message.getUserId(), taskId);
            markTaskFailed(taskId, buildUserFriendlyErrorMessage(e));
        } catch (Exception e) {
            logDiagnosisFailure(taskId, taskStartTime, e);
            refundQuotaIfNeeded(message.getUserId(), taskId);
            markTaskFailed(taskId, buildUserFriendlyErrorMessage(e));
        } catch (Throwable t) {
            // 终极兜底：避免未预期的 Throwable 让消费线程直接崩掉。
            logDiagnosisFailure(taskId, taskStartTime, t);
            try {
                refundQuotaIfNeeded(message.getUserId(), taskId);
            } catch (Exception ignored) {
                // 退配额失败只记日志，不覆盖主异常。
            }
            markTaskFailed(taskId, buildUserFriendlyErrorMessage(t));
        }
    }

    /**
     * 增强诊断结果，补充 basicInfoDetails 字段。
     *
     * 处理逻辑：
     * 1. 先用后端规则从简历原文提取基础信息。
     * 2. 如果 AI 没返回 basicInfoDetails，则直接补齐。
     * 3. 如果 AI 已返回，则仅补空值，保留 AI 已识别出的内容。
     */
    private String enhanceDiagnosisResult(String aiDiagnosisResult, String resumeText) {
        try {
            ResumeDiagnosisResult.BasicInfoDetails extractedDetails =
                    resumeInfoExtractor.extractBasicInfo(resumeText);
            log.info("后端规则提取基础信息完成");

            JsonNode rootNode = objectMapper.readTree(aiDiagnosisResult);
            JsonNode aiDetailsNode = rootNode.path("basicInfoDetails");

            ObjectNode detailsNode;
            if (aiDetailsNode.isMissingNode() || aiDetailsNode.isNull()) {
                detailsNode = objectMapper.createObjectNode();
                detailsNode.put("name", extractedDetails.getName());
                detailsNode.put("email", extractedDetails.getEmail());
                detailsNode.put("phone", extractedDetails.getPhone());
                detailsNode.put("location", extractedDetails.getLocation());
                detailsNode.put("currentCompany", extractedDetails.getCurrentCompany());
                detailsNode.put("github", extractedDetails.getGithub());
                detailsNode.put("blog", extractedDetails.getBlog());
                log.info("AI 未返回 basicInfoDetails，已使用后端提取结果补齐");
            } else {
                detailsNode = (ObjectNode) aiDetailsNode;
                mergeDetailsValue(detailsNode, "name", extractedDetails.getName());
                mergeDetailsValue(detailsNode, "email", extractedDetails.getEmail());
                mergeDetailsValue(detailsNode, "phone", extractedDetails.getPhone());
                mergeDetailsValue(detailsNode, "location", extractedDetails.getLocation());
                mergeDetailsValue(detailsNode, "currentCompany", extractedDetails.getCurrentCompany());
                mergeDetailsValue(detailsNode, "github", extractedDetails.getGithub());
                mergeDetailsValue(detailsNode, "blog", extractedDetails.getBlog());
                log.info("AI 已返回 basicInfoDetails，已完成空值补齐");
            }

            ((ObjectNode) rootNode).set("basicInfoDetails", detailsNode);
            String finalResult = objectMapper.writeValueAsString(rootNode);
            log.info("诊断结果增强完成，已补充 basicInfoDetails");
            return finalResult;
        } catch (Exception e) {
            log.warn("增强诊断结果失败，回退到 AI 原始结果: {}", e.getMessage());
            return aiDiagnosisResult;
        }
    }

    /**
     * 合并单个字段值：
     * - AI 有值时保留 AI 结果。
     * - AI 为空时再补后端规则提取的值。
     */
    private void mergeDetailsValue(ObjectNode node, String fieldName, String fallbackValue) {
        JsonNode existingValue = node.path(fieldName);
        boolean needSet = false;

        if (existingValue.isMissingNode() || existingValue.isNull()) {
            needSet = true;
        } else if (existingValue.isTextual()) {
            String textValue = existingValue.asText().trim();
            if (textValue.isEmpty() || "null".equals(textValue)) {
                needSet = true;
            }
        }

        if (needSet && fallbackValue != null) {
            node.put(fieldName, fallbackValue);
        }
    }

    /**
     * 统一记录失败日志，明确区分超时类异常与普通业务异常。
     */
    private void logDiagnosisFailure(Long taskId, long taskStartTime, Throwable throwable) {
        long elapsed = System.currentTimeMillis() - taskStartTime;
        if (isTimeoutException(throwable)) {
            log.error("简历诊断超时失败, taskId: {}, elapsedMs: {}", taskId, elapsed, throwable);
            return;
        }
        log.error("简历诊断处理失败, taskId: {}, elapsedMs: {}", taskId, elapsed, throwable);
    }

    /**
     * 面向用户的错误提示做收敛，避免把底层超时栈直接暴露给前端。
     */
    private String buildUserFriendlyErrorMessage(Throwable throwable) {
        if (isTimeoutException(throwable)) {
            return "AI分析超时，请稍后重试";
        }
        String message = throwable == null ? "" : throwable.getMessage();
        if (message == null || message.isBlank()) {
            return "系统异常，请稍后重试";
        }
        return message;
    }

    /**
     * 沿着 cause 链识别超时异常，覆盖网络层、HTTP 层和 Future 层常见超时来源。
     */
    private boolean isTimeoutException(Throwable throwable) {
        Throwable current = throwable;
        while (current != null) {
            if (current instanceof TimeoutException
                    || current instanceof SocketTimeoutException
                    || current instanceof HttpTimeoutException) {
                return true;
            }
            String message = current.getMessage();
            if (message != null && message.toLowerCase().contains("timeout")) {
                return true;
            }
            current = current.getCause();
        }
        return false;
    }

    private void markTaskFailed(Long taskId, String errorMsg) {
        try {
            String safeMsg = (errorMsg != null && errorMsg.length() > 500)
                    ? errorMsg.substring(0, 500) + "..."
                    : errorMsg;
            resumeDiagnosisTaskService.updateStatusToFailed(taskId, safeMsg);
            log.info("任务已标记失败, taskId: {}, error: {}", taskId, safeMsg);
        } catch (Exception ex) {
            log.error("标记任务失败失败, taskId: {}", taskId, ex);
        }
    }

    /**
     * 任务失败时退还已扣除配额，避免用户因系统异常损失次数。
     */
    private void refundQuotaIfNeeded(Long userId, Long taskId) {
        try {
            userQuotaService.refundResumeQuota(userId);
            log.info("任务失败，已退还配额, userId: {}, taskId: {}", userId, taskId);
        } catch (Exception e) {
            log.error("退还配额失败, userId: {}, taskId: {}", userId, taskId, e);
        }
    }
}
