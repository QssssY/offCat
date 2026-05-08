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
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.net.SocketTimeoutException;
import java.net.http.HttpTimeoutException;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeoutException;

@Slf4j
@Service
@RequiredArgsConstructor
public class ResumeDiagnosisProcessor {

    private final ResumeDiagnosisTaskService resumeDiagnosisTaskService;
    private final ResumeContentExtractor resumeContentExtractor;
    private final ResumeAiService resumeAiService;
    private final ResumeInfoExtractor resumeInfoExtractor;
    private final ObjectMapper objectMapper;
    private final UserQuotaService userQuotaService;
    private final NotificationService notificationService;

    public void processTask(Long taskId, Long userId, String fileUrl) {
        long taskStartTime = System.currentTimeMillis();
        log.info("开始处理简历诊断任务, taskId: {}, userId: {}", taskId, userId);

        try {
            Integer currentStatus = resumeDiagnosisTaskService.getTaskStatus(taskId);
            if (currentStatus == null) {
                log.warn("任务不存在，跳过处理, taskId: {}", taskId);
                refundQuotaIfNeeded(userId, taskId);
                return;
            }
            if (currentStatus != ResumeDiagnosisConstants.STATUS_PENDING) {
                log.warn("任务状态不是待处理，跳过处理, taskId: {}, status: {}", taskId, currentStatus);
                return;
            }

            resumeDiagnosisTaskService.updateStatusToProcessing(taskId);

            long pdfStartTime = System.currentTimeMillis();
            // 统一解析服务会优先复用文本直提，不足时再进入多模态或 OCR。
            ResumeParseResult parseResult = resumeContentExtractor.extract(fileUrl);
            String resumeText = parseResult.getText();
            long pdfElapsed = System.currentTimeMillis() - pdfStartTime;
            log.info("PDF 文本提取完成, taskId: {}, textLength: {}, elapsedMs: {}",
                    taskId, resumeText.length(), pdfElapsed);

            try {
                // 统一缓存解析文本与解析来源，供结果页和后续能力复用。
                resumeDiagnosisTaskService.updateTaskResumeParseResult(
                        taskId,
                        resumeText,
                        parseResult.getParseMode(),
                        parseResult.getParseMessage());
            } catch (Exception e) {
                log.warn("缓存简历文本失败, taskId: {}, 不影响主流程", taskId, e);
            }

            long aiStartTime = System.currentTimeMillis();
            log.info("开始 AI 诊断调用, taskId: {}", taskId);
            String diagnosisResult = resumeAiService.diagnose(resumeText);
            long aiElapsed = System.currentTimeMillis() - aiStartTime;
            log.info("AI 诊断调用返回, taskId: {}, elapsedMs: {}", taskId, aiElapsed);
            if (diagnosisResult == null || diagnosisResult.isBlank()) {
                throw new RuntimeException("AI 诊断返回结果为空");
            }

            long enhanceStartTime = System.currentTimeMillis();
            String enhancedResult = enhanceDiagnosisResult(diagnosisResult, resumeText);
            long enhanceElapsed = System.currentTimeMillis() - enhanceStartTime;
            enhancedResult = enhancedResult.replaceAll("[\\x00-\\x08\\x0B\\x0C\\x0E-\\x1F]", "");

            long persistStartTime = System.currentTimeMillis();
            resumeDiagnosisTaskService.updateStatusToCompleted(taskId, enhancedResult);
            long persistElapsed = System.currentTimeMillis() - persistStartTime;

            long totalElapsed = System.currentTimeMillis() - taskStartTime;
            log.info("简历诊断任务完成, taskId: {}, pdfMs: {}, aiMs: {}, enhanceMs: {}, persistMs: {}, totalMs: {}",
                    taskId, pdfElapsed, aiElapsed, enhanceElapsed, persistElapsed, totalElapsed);

            notificationService.createNotification(
                    userId,
                    "resume",
                    "简历诊断完成",
                    "你的简历诊断报告已生成，点击查看结果。",
                    "resume_diagnosis",
                    String.valueOf(taskId));
        } catch (com.airesume.server.service.PdfTextExtractor.PdfExtractionException e) {
            log.error("PDF 解析失败, taskId: {}", taskId, e);
            refundQuotaIfNeeded(userId, taskId);
            markTaskFailed(taskId, "PDF解析失败: " + e.getMessage());
        } catch (RuntimeException e) {
            logDiagnosisFailure(taskId, taskStartTime, e);
            refundQuotaIfNeeded(userId, taskId);
            markTaskFailed(taskId, buildUserFriendlyErrorMessage(e));
        } catch (Exception e) {
            logDiagnosisFailure(taskId, taskStartTime, e);
            refundQuotaIfNeeded(userId, taskId);
            markTaskFailed(taskId, buildUserFriendlyErrorMessage(e));
        } catch (Throwable t) {
            logDiagnosisFailure(taskId, taskStartTime, t);
            try {
                refundQuotaIfNeeded(userId, taskId);
            } catch (Exception ignored) {
                // Keep the original failure as the primary error.
            }
            markTaskFailed(taskId, buildUserFriendlyErrorMessage(t));
        }
    }

    private String enhanceDiagnosisResult(String aiDiagnosisResult, String resumeText) {
        try {
            ResumeDiagnosisResult normalizedResult = normalizeDiagnosisResult(aiDiagnosisResult, resumeText);
            String finalResult = objectMapper.writeValueAsString(normalizedResult);
            log.info("诊断结果归一化完成，已补齐 basicInfoDetails");
            return finalResult;
        } catch (Exception e) {
            log.warn("诊断结果归一化失败，回退到原始 AI 结果: {}", e.getMessage());
            return aiDiagnosisResult;
        }
    }

    private ResumeDiagnosisResult normalizeDiagnosisResult(String aiDiagnosisResult, String resumeText) throws Exception {
        ResumeDiagnosisResult parsedResult = objectMapper.readValue(aiDiagnosisResult, ResumeDiagnosisResult.class);
        if (parsedResult == null) {
            parsedResult = ResumeDiagnosisResult.builder().build();
        }

        ResumeDiagnosisResult.BasicInfoDetails normalizedDetails =
                normalizeBasicInfoDetails(resumeInfoExtractor.extractBasicInfo(resumeText));

        parsedResult.setOverallEvaluation(normalizeOverallEvaluation(parsedResult.getOverallEvaluation()));
        parsedResult.setHighlights(defaultStringList(parsedResult.getHighlights()));
        parsedResult.setBasicInfoEvaluation(normalizeBasicInfoEvaluation(parsedResult.getBasicInfoEvaluation(), normalizedDetails));
        parsedResult.setBasicInfoDetails(normalizedDetails);
        parsedResult.setSkillEvaluation(normalizeSkillEvaluation(parsedResult.getSkillEvaluation()));
        parsedResult.setWorkExperienceEvaluation(normalizeWorkExperienceEvaluation(parsedResult.getWorkExperienceEvaluation()));
        parsedResult.setProjectExperienceEvaluation(normalizeProjectExperienceEvaluation(parsedResult.getProjectExperienceEvaluation()));
        parsedResult.setEducationEvaluation(normalizeEducationEvaluation(parsedResult.getEducationEvaluation()));
        parsedResult.setOptimizationSuggestions(defaultStringList(parsedResult.getOptimizationSuggestions()));
        return parsedResult;
    }

    private ResumeDiagnosisResult.OverallEvaluation normalizeOverallEvaluation(ResumeDiagnosisResult.OverallEvaluation source) {
        if (source == null) {
            source = ResumeDiagnosisResult.OverallEvaluation.builder().build();
        }
        source.setSummary(defaultString(source.getSummary()));
        source.setStrengths(defaultStringList(source.getStrengths()));
        source.setWeaknesses(defaultStringList(source.getWeaknesses()));
        return source;
    }

    private ResumeDiagnosisResult.BasicInfoEvaluation normalizeBasicInfoEvaluation(
            ResumeDiagnosisResult.BasicInfoEvaluation source,
            ResumeDiagnosisResult.BasicInfoDetails details) {
        if (source == null) {
            source = ResumeDiagnosisResult.BasicInfoEvaluation.builder().build();
        }
        source.setEvaluation(defaultString(source.getEvaluation()));
        source.setHasName(defaultBoolean(source.getHasName(), hasText(details.getName())));
        source.setHasPhone(defaultBoolean(source.getHasPhone(), hasText(details.getPhone())));
        source.setHasEmail(defaultBoolean(source.getHasEmail(), hasText(details.getEmail())));
        source.setHasGithub(defaultBoolean(source.getHasGithub(), hasText(details.getGithub())));
        source.setHasBlog(defaultBoolean(source.getHasBlog(), hasText(details.getBlog())));
        source.setStrengths(defaultStringList(source.getStrengths()));
        source.setWeaknesses(defaultStringList(source.getWeaknesses()));
        source.setSuggestions(defaultStringList(source.getSuggestions()));
        return source;
    }

    private ResumeDiagnosisResult.BasicInfoDetails normalizeBasicInfoDetails(
            ResumeDiagnosisResult.BasicInfoDetails source) {
        ResumeDiagnosisResult.BasicInfoDetails details = source == null
                ? ResumeDiagnosisResult.BasicInfoDetails.builder().build()
                : source;
        details.setName(defaultString(details.getName()));
        details.setEmail(defaultString(details.getEmail()));
        details.setPhone(defaultString(details.getPhone()));
        details.setLocation(defaultString(details.getLocation()));
        details.setCurrentCompany(defaultString(details.getCurrentCompany()));
        details.setGithub(defaultString(details.getGithub()));
        details.setBlog(defaultString(details.getBlog()));
        return details;
    }

    private ResumeDiagnosisResult.SkillEvaluation normalizeSkillEvaluation(
            ResumeDiagnosisResult.SkillEvaluation source) {
        if (source == null) {
            source = ResumeDiagnosisResult.SkillEvaluation.builder().build();
        }
        source.setEvaluation(defaultString(source.getEvaluation()));
        source.setSkillList(defaultStringList(source.getSkillList()));
        source.setStrengths(defaultStringList(source.getStrengths()));
        source.setWeaknesses(defaultStringList(source.getWeaknesses()));
        source.setSuggestions(defaultStringList(source.getSuggestions()));
        return source;
    }

    private ResumeDiagnosisResult.WorkExperienceEvaluation normalizeWorkExperienceEvaluation(
            ResumeDiagnosisResult.WorkExperienceEvaluation source) {
        if (source == null) {
            source = ResumeDiagnosisResult.WorkExperienceEvaluation.builder().build();
        }
        source.setEvaluation(defaultString(source.getEvaluation()));
        source.setExperiences(defaultObjectList(source.getExperiences()));
        source.setStrengths(defaultStringList(source.getStrengths()));
        source.setWeaknesses(defaultStringList(source.getWeaknesses()));
        source.setSuggestions(defaultStringList(source.getSuggestions()));
        return source;
    }

    private ResumeDiagnosisResult.ProjectExperienceEvaluation normalizeProjectExperienceEvaluation(
            ResumeDiagnosisResult.ProjectExperienceEvaluation source) {
        if (source == null) {
            source = ResumeDiagnosisResult.ProjectExperienceEvaluation.builder().build();
        }
        source.setEvaluation(defaultString(source.getEvaluation()));
        source.setProjects(defaultObjectList(source.getProjects()));
        source.setStrengths(defaultStringList(source.getStrengths()));
        source.setWeaknesses(defaultStringList(source.getWeaknesses()));
        source.setSuggestions(defaultStringList(source.getSuggestions()));
        return source;
    }

    private ResumeDiagnosisResult.EducationEvaluation normalizeEducationEvaluation(
            ResumeDiagnosisResult.EducationEvaluation source) {
        if (source == null) {
            source = ResumeDiagnosisResult.EducationEvaluation.builder().build();
        }
        source.setDegree(defaultString(source.getDegree()));
        source.setSchool(defaultString(source.getSchool()));
        source.setMajor(defaultString(source.getMajor()));
        source.setEvaluation(defaultString(source.getEvaluation()));
        source.setStrengths(defaultStringList(source.getStrengths()));
        source.setWeaknesses(defaultStringList(source.getWeaknesses()));
        source.setSuggestions(defaultStringList(source.getSuggestions()));
        return source;
    }

    private List<String> defaultStringList(List<String> values) {
        return values == null ? List.of() : values;
    }

    private List<Map<String, Object>> defaultObjectList(List<Map<String, Object>> values) {
        return values == null ? List.of() : values;
    }

    private String defaultString(String value) {
        return value == null ? "" : value;
    }

    private Boolean defaultBoolean(Boolean value, boolean fallback) {
        return value != null ? value : fallback;
    }

    private boolean hasText(String value) {
        return value != null && !value.isBlank();
    }

    private void logDiagnosisFailure(Long taskId, long taskStartTime, Throwable throwable) {
        long elapsed = System.currentTimeMillis() - taskStartTime;
        if (isTimeoutException(throwable)) {
            log.error("简历诊断超时失败, taskId: {}, elapsedMs: {}", taskId, elapsed, throwable);
            return;
        }
        log.error("简历诊断处理失败, taskId: {}, elapsedMs: {}", taskId, elapsed, throwable);
    }

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
            log.error("标记任务失败时出错, taskId: {}", taskId, ex);
        }
    }

    private void refundQuotaIfNeeded(Long userId, Long taskId) {
        try {
            userQuotaService.refundResumeQuota(userId);
            log.info("任务失败，已退还配额, userId: {}, taskId: {}", userId, taskId);
        } catch (Exception e) {
            log.error("退还配额失败, userId: {}, taskId: {}", userId, taskId, e);
        }
    }
}
