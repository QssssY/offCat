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
import com.airesume.server.service.UserAiUsageLimitService;
import com.airesume.server.service.resume.ResumeParseResult;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
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
@RequiredArgsConstructor(onConstructor_ = @org.springframework.beans.factory.annotation.Autowired)
public class ResumeDiagnosisProcessor {

    private final ResumeDiagnosisTaskService resumeDiagnosisTaskService;
    private final ResumeDiagnosisTaskMapper resumeDiagnosisTaskMapper;
    private final ResumeContentExtractor resumeContentExtractor;
    private final ResumeAiService resumeAiService;
    private final ResumeInfoExtractor resumeInfoExtractor;
    private final ObjectMapper objectMapper;
    private final UserQuotaService userQuotaService;
    private final UserAiUsageLimitService userAiUsageLimitService;
    private final NotificationService notificationService;

    /**
     * 兼容既有单元测试的构造器，生产注入使用 Lombok 生成的完整构造器。
     */
    public ResumeDiagnosisProcessor(
            ResumeDiagnosisTaskService resumeDiagnosisTaskService,
            ResumeDiagnosisTaskMapper resumeDiagnosisTaskMapper,
            ResumeContentExtractor resumeContentExtractor,
            ResumeAiService resumeAiService,
            ResumeInfoExtractor resumeInfoExtractor,
            ObjectMapper objectMapper,
            UserQuotaService userQuotaService,
            NotificationService notificationService) {
        this.resumeDiagnosisTaskService = resumeDiagnosisTaskService;
        this.resumeDiagnosisTaskMapper = resumeDiagnosisTaskMapper;
        this.resumeContentExtractor = resumeContentExtractor;
        this.resumeAiService = resumeAiService;
        this.resumeInfoExtractor = resumeInfoExtractor;
        this.objectMapper = objectMapper;
        this.userQuotaService = userQuotaService;
        this.userAiUsageLimitService = null;
        this.notificationService = notificationService;
    }

    public void processTask(Long taskId, Long userId, String fileUrl) {
        long taskStartTime = System.currentTimeMillis();
        log.info("开始处理简历诊断任务, taskId: {}, userId: {}", taskId, userId);

        try {
            // 状态校验与缓存文本检查合并为一次查询，避免 getTaskStatus + getById 两次 SELECT
            ResumeDiagnosisTask currentTask = resumeDiagnosisTaskMapper.selectOne(new QueryWrapper<ResumeDiagnosisTask>()
                    // 处理器需要读取缓存简历文本，必须显式补回 resume_text 大字段。
                    .select("id", "status", "resume_text", "ai_billing_source", "fallback_to_platform", "is_deleted")
                    .eq("id", taskId)
                    .last("limit 1"));
            if (currentTask == null) {
                log.warn("任务不存在，跳过处理, taskId: {}", taskId);
                refundQuotaIfNeeded(userId, taskId);
                return;
            }
            if (currentTask.getStatus() == null || currentTask.getStatus() != ResumeDiagnosisConstants.STATUS_PENDING) {
                log.warn("任务状态不是待处理，跳过处理, taskId: {}, status: {}", taskId, currentTask.getStatus());
                return;
            }

            // 只有抢占到待处理任务的消费者才能继续执行，避免同一任务被重复解析和重复退款。
            boolean claimed = resumeDiagnosisTaskService.updateStatusToProcessing(taskId);
            if (!claimed) {
                log.warn("Task was claimed by another worker, skip current processing, taskId: {}", taskId);
                return;
            }
            boolean fallbackToPlatform = Integer.valueOf(1).equals(currentTask.getFallbackToPlatform());
            boolean requireUserCustom = UserAiConstants.BILLING_SOURCE_USER_CUSTOM.equals(currentTask.getAiBillingSource());

            // 阶段1：提取简历文本（优先使用缓存，支持文件过期后的重试场景）
            long pdfStartTime = System.currentTimeMillis();
            String resumeText = (currentTask.getResumeText() != null
                    && !currentTask.getResumeText().isBlank())
                    ? currentTask.getResumeText() : null;

            if (resumeText != null) {
                log.info("使用缓存简历文本（跳过PDF提取）, taskId: {}, textLength: {}", taskId, resumeText.length());
                // 使用缓存文本时仍需设置提取阶段，确保前端阶段链路可见
                resumeDiagnosisTaskService.updateStage(taskId, ResumeDiagnosisConstants.STAGE_EXTRACTING);
            } else {
                resumeDiagnosisTaskService.updateStage(taskId, ResumeDiagnosisConstants.STAGE_EXTRACTING);
                // 图片页多模态识别必须沿用任务创建时锁定的 AI 来源，避免自定义 AI 任务在提取阶段误走平台配置。
                ResumeParseResult parseResult = resumeContentExtractor.extract(
                        fileUrl, userId, fallbackToPlatform, requireUserCustom);
                resumeText = parseResult.getText();
                log.info("PDF 文本提取完成, taskId: {}, textLength: {}, elapsedMs: {}",
                        taskId, resumeText.length(), System.currentTimeMillis() - pdfStartTime);
                try {
                    resumeDiagnosisTaskService.updateTaskResumeParseResult(
                            taskId, resumeText,
                            parseResult.getParseMode(), parseResult.getParseMessage());
                } catch (Exception e) {
                    log.warn("缓存简历文本失败, taskId: {}, 不影响主流程", taskId, e);
                }
            }
            long pdfElapsed = System.currentTimeMillis() - pdfStartTime;

            long aiStartTime = System.currentTimeMillis();
            // 阶段2：AI 分析简历
            resumeDiagnosisTaskService.updateStage(taskId, ResumeDiagnosisConstants.STAGE_AI_ANALYZING);
            log.info("开始 AI 诊断调用, taskId: {}", taskId);
            String diagnosisResult = resumeAiService.diagnose(resumeText, userId, fallbackToPlatform, requireUserCustom);
            long aiElapsed = System.currentTimeMillis() - aiStartTime;
            log.info("AI 诊断调用返回, taskId: {}, elapsedMs: {}", taskId, aiElapsed);
            if (diagnosisResult == null || diagnosisResult.isBlank()) {
                throw new BusinessException(ResultCode.AI_RESPONSE_EMPTY);
            }

            long enhanceStartTime = System.currentTimeMillis();
            // 阶段3：生成诊断报告
            resumeDiagnosisTaskService.updateStage(taskId, ResumeDiagnosisConstants.STAGE_ENHANCING);
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
            } catch (Exception refundEx) {
                // 配额退还失败，保留原始异常作为主要错误
                log.warn("诊断失败后配额退还异常, taskId: {}, error: {}", taskId, refundEx.getMessage());
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

        parsedResult.setHighlights(defaultStringList(parsedResult.getHighlights()));
        parsedResult.setBasicInfoEvaluation(normalizeBasicInfoEvaluation(parsedResult.getBasicInfoEvaluation(), normalizedDetails));
        parsedResult.setBasicInfoDetails(normalizedDetails);
        parsedResult.setSkillEvaluation(normalizeSkillEvaluation(parsedResult.getSkillEvaluation()));
        parsedResult.setWorkExperienceEvaluation(normalizeWorkExperienceEvaluation(parsedResult.getWorkExperienceEvaluation()));
        parsedResult.setProjectExperienceEvaluation(normalizeProjectExperienceEvaluation(parsedResult.getProjectExperienceEvaluation()));
        parsedResult.setEducationEvaluation(normalizeEducationEvaluation(parsedResult.getEducationEvaluation()));
        parsedResult.setPositioningEvaluation(normalizePositioningEvaluation(parsedResult.getPositioningEvaluation()));
        parsedResult.setOverallEvaluation(normalizeOverallEvaluation(parsedResult.getOverallEvaluation(), parsedResult));
        parsedResult.setOptimizationSuggestions(defaultStringList(parsedResult.getOptimizationSuggestions()));
        return parsedResult;
    }

    private ResumeDiagnosisResult.OverallEvaluation normalizeOverallEvaluation(
            ResumeDiagnosisResult.OverallEvaluation source,
            ResumeDiagnosisResult result) {
        if (source == null) {
            source = ResumeDiagnosisResult.OverallEvaluation.builder().build();
        }
        source.setSummary(defaultString(source.getSummary()));
        source.setStrengths(defaultStringList(source.getStrengths()));
        source.setWeaknesses(defaultStringList(source.getWeaknesses()));
        // 总分不再直接信任大模型自由给出的 totalScore，改为按维度分固定权重汇总，降低同一简历重复诊断的随机波动。
        int totalScore = calculateStableTotalScore(result, source.getTotalScore());
        source.setTotalScore(totalScore);
        source.setLevel(resolveLevel(totalScore));
        return source;
    }

    private int calculateStableTotalScore(ResumeDiagnosisResult result, Integer aiTotalScore) {
        double weightedScore = 0;
        double availableWeight = 0;

        if (result.getBasicInfoEvaluation() != null && result.getBasicInfoEvaluation().getScore() != null) {
            weightedScore += readScore(result.getBasicInfoEvaluation().getScore()) * 0.05;
            availableWeight += 0.05;
        }
        if (result.getSkillEvaluation() != null && result.getSkillEvaluation().getScore() != null) {
            weightedScore += readScore(result.getSkillEvaluation().getScore()) * 0.23;
            availableWeight += 0.23;
        }
        if (result.getWorkExperienceEvaluation() != null && result.getWorkExperienceEvaluation().getScore() != null) {
            weightedScore += readScore(result.getWorkExperienceEvaluation().getScore()) * 0.30;
            availableWeight += 0.30;
        }
        if (result.getProjectExperienceEvaluation() != null && result.getProjectExperienceEvaluation().getScore() != null) {
            weightedScore += readScore(result.getProjectExperienceEvaluation().getScore()) * 0.27;
            availableWeight += 0.27;
        }
        if (result.getEducationEvaluation() != null && result.getEducationEvaluation().getScore() != null) {
            weightedScore += readScore(result.getEducationEvaluation().getScore()) * 0.10;
            availableWeight += 0.10;
        }
        if (result.getPositioningEvaluation() != null && result.getPositioningEvaluation().getScore() != null) {
            weightedScore += readScore(result.getPositioningEvaluation().getScore()) * 0.05;
            availableWeight += 0.05;
        }
        if (availableWeight == 0) {
            return readScore(aiTotalScore);
        }

        // AI 偶发漏掉某个维度时，仅按实际返回的维度重新归一化，避免缺失项被当成 0 分拉低总分。
        return (int) Math.round(weightedScore / availableWeight);
    }

    private int readScore(Integer score) {
        if (score == null) {
            return 0;
        }
        return Math.max(0, Math.min(score, 100));
    }

    private String resolveLevel(int totalScore) {
        if (totalScore >= 90) return "S";
        if (totalScore >= 75) return "A";
        if (totalScore >= 60) return "B";
        if (totalScore >= 40) return "C";
        return "D";
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

    private ResumeDiagnosisResult.PositioningEvaluation normalizePositioningEvaluation(
            ResumeDiagnosisResult.PositioningEvaluation source) {
        if (source == null) {
            source = ResumeDiagnosisResult.PositioningEvaluation.builder().build();
        }
        source.setEvaluation(defaultString(source.getEvaluation()));
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
        if (throwable == null) {
            return "系统异常，请稍后重试";
        }
        if (isTimeoutException(throwable)) {
            return "AI分析超时，请稍后重试";
        }
        // PDF 解析类错误
        if (throwable instanceof com.airesume.server.service.PdfTextExtractor.PdfExtractionException) {
            return "PDF文件解析失败，请检查文件是否损坏后重新上传";
        }
        BusinessException businessException = findBusinessException(throwable);
        if (businessException != null) {
            return buildBusinessErrorMessage(businessException);
        }
        return "诊断处理异常，请稍后重试";
    }

    private BusinessException findBusinessException(Throwable throwable) {
        Throwable current = throwable;
        while (current != null) {
            if (current instanceof BusinessException businessException) {
                return businessException;
            }
            current = current.getCause();
        }
        return null;
    }

    private String buildBusinessErrorMessage(BusinessException exception) {
        Integer code = exception.getCode();
        if (ResultCode.AI_SERVICE_UNAVAILABLE.getCode().equals(code)) {
            return "AI服务暂时不可用，请稍后重试";
        }
        if (ResultCode.AI_RESPONSE_EMPTY.getCode().equals(code)) {
            return "AI分析返回结果为空，请稍后重试";
        }
        if (ResultCode.AI_RESPONSE_PARSE_FAILED.getCode().equals(code)) {
            return "AI响应解析失败，请稍后重试";
        }
        if (ResultCode.AI_QUOTA_INSUFFICIENT.getCode().equals(code)) {
            return "AI调用配额不足，请联系客服或稍后重试";
        }
        return "诊断处理异常，请稍后重试";
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
            ResumeDiagnosisTask task = resumeDiagnosisTaskMapper.selectOne(new QueryWrapper<ResumeDiagnosisTask>()
                    .select("id", "ai_billing_source", "is_deleted")
                    .eq("id", taskId)
                    .last("limit 1"));
            if (task != null && UserAiConstants.BILLING_SOURCE_USER_CUSTOM.equals(task.getAiBillingSource())) {
                if (userAiUsageLimitService != null) {
                    userAiUsageLimitService.rollback(userId);
                }
                log.info("任务失败，已回滚自定义 AI 次数, userId: {}, taskId: {}", userId, taskId);
                return;
            }
            userQuotaService.refundResumeQuota(userId);
            log.info("任务失败，已退还平台配额, userId: {}, taskId: {}", userId, taskId);
        } catch (Exception e) {
            log.error("退还配额失败, userId: {}, taskId: {}", userId, taskId, e);
        }
    }
}
