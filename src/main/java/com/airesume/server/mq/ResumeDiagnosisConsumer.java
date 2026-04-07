package com.airesume.server.mq;

import com.airesume.server.common.constants.ResumeDiagnosisConstants;
import com.airesume.server.dto.resume.ResumeDiagnosisResult;
import com.airesume.server.service.PdfTextExtractor;
import com.airesume.server.service.ResumeAiService;
import com.airesume.server.service.ResumeDiagnosisTaskService;
import com.airesume.server.service.ResumeInfoExtractor;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

/**
 * 简历诊断消息消费者
 *
 * 【核心流程】
 * 1. 接收 RabbitMQ 消息
 * 2. 从 PDF 提取文本
 * 3. 调用 AI 生成诊断结果
 * 4. 使用后端规则提取基础信息（作为 AI 结果的补充）
 * 5. 合并并补充 basicInfoDetails 字段
 * 6. 保存最终结果到数据库
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

            // 【关键步骤】补充 basicInfoDetails 字段
            String enhancedResult = enhanceDiagnosisResult(diagnosisResult, resumeText);

            resumeDiagnosisTaskService.updateStatusToCompleted(taskId, enhancedResult);
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

    /**
     * 增强诊断结果，补充 basicInfoDetails 字段
     *
     * 【处理逻辑】
     * 1. 解析 AI 返回的 JSON
     * 2. 使用后端规则从简历原文提取基础信息
     * 3. 如果 AI 已返回 basicInfoDetails 且有值，保留 AI 的值
     * 4. 如果 AI 未返回或值为空，使用后端提取的值补充
     * 5. 保留原有 basicInfoEvaluation 等所有字段不变
     *
     * @param aiDiagnosisResult AI 返回的诊断结果 JSON 字符串
     * @param resumeText 简历原文文本
     * @return 增强后的诊断结果 JSON 字符串
     */
    private String enhanceDiagnosisResult(String aiDiagnosisResult, String resumeText) {
        try {
            // 步骤1：使用后端规则提取基础信息
            ResumeDiagnosisResult.BasicInfoDetails extractedDetails =
                    resumeInfoExtractor.extractBasicInfo(resumeText);
            log.info("后端规则提取基础信息完成");

            // 步骤2：解析 AI 返回的 JSON
            JsonNode rootNode = objectMapper.readTree(aiDiagnosisResult);

            // 步骤3：检查 AI 是否已返回 basicInfoDetails
            JsonNode aiDetailsNode = rootNode.path("basicInfoDetails");

            // 创建或获取 basicInfoDetails 对象
            ObjectNode detailsNode;
            if (aiDetailsNode.isMissingNode() || aiDetailsNode.isNull()) {
                // AI 未返回，使用后端提取的值创建
                detailsNode = objectMapper.createObjectNode();
                detailsNode.put("name", extractedDetails.getName());
                detailsNode.put("email", extractedDetails.getEmail());
                detailsNode.put("phone", extractedDetails.getPhone());
                detailsNode.put("location", extractedDetails.getLocation());
                detailsNode.put("currentCompany", extractedDetails.getCurrentCompany());
                detailsNode.put("github", extractedDetails.getGithub());
                detailsNode.put("blog", extractedDetails.getBlog());
                log.info("AI 未返回 basicInfoDetails，使用后端提取值补充");
            } else {
                // AI 已返回，合并值（AI 值优先，空值才用后端补充）
                detailsNode = (ObjectNode) aiDetailsNode;
                mergeDetailsValue(detailsNode, "name", extractedDetails.getName());
                mergeDetailsValue(detailsNode, "email", extractedDetails.getEmail());
                mergeDetailsValue(detailsNode, "phone", extractedDetails.getPhone());
                mergeDetailsValue(detailsNode, "location", extractedDetails.getLocation());
                mergeDetailsValue(detailsNode, "currentCompany", extractedDetails.getCurrentCompany());
                mergeDetailsValue(detailsNode, "github", extractedDetails.getGithub());
                mergeDetailsValue(detailsNode, "blog", extractedDetails.getBlog());
                log.info("AI 已返回 basicInfoDetails，合并后端提取值");
            }

            // 步骤4：将 detailsNode 写回根节点
            ((ObjectNode) rootNode).set("basicInfoDetails", detailsNode);

            // 步骤5：序列化为 JSON 字符串返回
            String finalResult = objectMapper.writeValueAsString(rootNode);
            log.info("诊断结果增强完成，已包含 basicInfoDetails 字段");

            return finalResult;

        } catch (Exception e) {
            // JSON 解析或处理失败时，返回 AI 原始结果（不影响原有功能）
            log.warn("增强诊断结果失败，返回 AI 原始结果: {}", e.getMessage());
            return aiDiagnosisResult;
        }
    }

    /**
     * 合并单个字段值
     *
     * 【合并规则】
     * - 如果目标节点已有该字段且值不为空，保留原值（AI 优先）
     * - 如果目标节点没有该字段或值为空，使用补充值
     *
     * @param node 目标 JSON 节点
     * @param fieldName 字段名
     * @param fallbackValue 补充值
     */
    private void mergeDetailsValue(ObjectNode node, String fieldName, String fallbackValue) {
        JsonNode existingValue = node.path(fieldName);
        boolean needSet = false;

        if (existingValue.isMissingNode() || existingValue.isNull()) {
            // 字段不存在或为 null，需要设置
            needSet = true;
        } else if (existingValue.isTextual()) {
            String textValue = existingValue.asText().trim();
            if (textValue.isEmpty() || textValue.equals("null")) {
                // 文本值为空，需要设置
                needSet = true;
            }
        }

        if (needSet && fallbackValue != null) {
            node.put(fieldName, fallbackValue);
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
