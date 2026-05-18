package com.airesume.server.service.impl;

import com.airesume.server.common.constants.ResumeDiagnosisConstants;
import com.airesume.server.common.exception.BusinessException;
import com.airesume.server.common.result.PageResult;
import com.airesume.server.dto.resume.ResumeDiagnosisHistoryResponse;
import com.airesume.server.dto.resume.ResumeJobMatchAnalyzeResponse;
import com.airesume.server.dto.resume.ResumePolishAnalyzeResponse;
import com.airesume.server.dto.resume.ResumeDiagnosisTaskResponse;
import com.airesume.server.entity.ResumeDiagnosisTask;
import com.airesume.server.mapper.ResumeDiagnosisTaskMapper;
import com.airesume.server.mq.DirectProcessRouter;
import com.airesume.server.mq.ResumeDiagnosisProducer;
import com.airesume.server.service.NotificationService;
import com.airesume.server.service.ResumeContentExtractor;
import com.airesume.server.service.ResumeDiagnosisTaskService;
import com.airesume.server.service.ResumeJobMatchService;
import com.airesume.server.service.ResumePolishService;
import com.airesume.server.service.UserQuotaService;
import com.airesume.server.service.resume.ResumeParseResult;
import org.springframework.context.annotation.Lazy;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.web.multipart.MultipartFile;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * 简历诊断任务服务实现类
 * 实现简历诊断任务的创建、查询、状态更新等业务逻辑
 */
@Slf4j
@Service
public class ResumeDiagnosisTaskServiceImpl extends ServiceImpl<ResumeDiagnosisTaskMapper, ResumeDiagnosisTask> implements ResumeDiagnosisTaskService {

    private final UserQuotaService userQuotaService;
    private final ResumeDiagnosisProducer resumeDiagnosisProducer;
    private final DirectProcessRouter directProcessRouter;
    private final ResumeContentExtractor resumeContentExtractor;
    private final NotificationService notificationService;
    private final ResumeJobMatchService resumeJobMatchService;
    private final ResumePolishService resumePolishService;

    /**
     * 手动构造器注入，@Lazy 打破 TaskServiceImpl ↔ DirectProcessRouter ↔ Processor ↔ TaskService 循环依赖
     */
    public ResumeDiagnosisTaskServiceImpl(
            UserQuotaService userQuotaService,
            ResumeDiagnosisProducer resumeDiagnosisProducer,
            @Lazy DirectProcessRouter directProcessRouter,
            ResumeContentExtractor resumeContentExtractor,
            NotificationService notificationService,
            ResumeJobMatchService resumeJobMatchService,
            ResumePolishService resumePolishService) {
        this.userQuotaService = userQuotaService;
        this.resumeDiagnosisProducer = resumeDiagnosisProducer;
        this.directProcessRouter = directProcessRouter;
        this.resumeContentExtractor = resumeContentExtractor;
        this.notificationService = notificationService;
        this.resumeJobMatchService = resumeJobMatchService;
        this.resumePolishService = resumePolishService;
    }

    /**
     * 上传目录配置
     * 优先使用配置值，默认使用项目根目录下的uploads/resumes目录
     */
    @Value("${app.upload.resume-dir:}")
    private String configuredUploadDir;

    /**
     * 最大文件大小（字节），默认10MB
     */
    @Value("${app.upload.max-file-size:10485760}")
    private long maxFileSize;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Long createTask(Long userId, String fileUrl) {
        log.info("Creating resume diagnosis task, userId: {}, fileUrl: {}", userId, fileUrl);

        // 1. 校验用户额度
        boolean hasQuota = userQuotaService.checkResumeQuota(userId);
        if (!hasQuota) {
            // 额度不足时创建通知（带防重，独立事务不受回滚影响）
            notificationService.createQuotaNotificationIfNeeded(userId);
            throw new BusinessException("简历诊断次数已用完");
        }

        // 2. 创建任务记录
        ResumeDiagnosisTask task = new ResumeDiagnosisTask();
        task.setUserId(userId);
        task.setFileUrl(fileUrl);
        task.setStatus(ResumeDiagnosisConstants.STATUS_PENDING);
        task.setDiagnosisResult(null);
        task.setErrorMsg(null);
        save(task);

        log.info("Resume diagnosis task created, taskId: {}", task.getId());

        // 3. 扣减用户额度
        userQuotaService.deductResumeQuota(userId);

        // 4. 事务提交后智能路由：系统空闲时直连异步处理，繁忙时走 MQ 排队
        Long taskId = task.getId();
        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            @Override
            public void afterCommit() {
                if (directProcessRouter.canProcessDirectly()) {
                    try {
                        directProcessRouter.submitDirect(taskId, userId, fileUrl);
                        log.info("任务路由到直连异步处理, taskId: {}", taskId);
                    } catch (Exception e) {
                        // 线程池满或拒绝时，回退到 MQ 保证任务不丢失
                        log.warn("直连线程池拒绝, 回退到MQ, taskId: {}", taskId, e);
                        resumeDiagnosisProducer.sendResumeDiagnosisTask(taskId, userId, fileUrl);
                    }
                } else {
                    resumeDiagnosisProducer.sendResumeDiagnosisTask(taskId, userId, fileUrl);
                    log.info("任务路由到RabbitMQ（系统繁忙）, taskId: {}", taskId);
                }
            }
        });

        return task.getId();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public String createTask(Long userId, MultipartFile file) {
        if (file.isEmpty()) {
            throw new BusinessException("Uploaded file must not be empty");
        }

        String originalFilename = sanitizeOriginalFilename(file.getOriginalFilename());
        if (!originalFilename.toLowerCase().endsWith(".pdf")) {
            throw new BusinessException("Only PDF files are supported");
        }

        if (file.getSize() > maxFileSize) {
            throw new BusinessException("File size must not exceed " + (maxFileSize / 1024 / 1024) + "MB");
        }

        String fileName = buildStoredResumeFileName();
        String fileUrl;

        try {
            String uploadDir;
            if (configuredUploadDir != null && !configuredUploadDir.isBlank()) {
                uploadDir = configuredUploadDir;
            } else {
                uploadDir = System.getProperty("user.dir") + "/uploads/resumes/";
            }
            if (!uploadDir.endsWith("/") && !uploadDir.endsWith("\\")) {
                uploadDir = uploadDir + "/";
            }

            Path uploadDirPath = Paths.get(uploadDir).toAbsolutePath().normalize();
            Files.createDirectories(uploadDirPath);

            Path destPath = uploadDirPath.resolve(fileName).normalize();
            if (!destPath.startsWith(uploadDirPath)) {
                throw new BusinessException("Illegal file path");
            }
            file.transferTo(destPath.toFile());

            fileUrl = "/uploads/resumes/" + fileName;
            log.info("Resume file saved, userId: {}, fileName: {}, fileUrl: {}",
                    userId, fileName, fileUrl);
        } catch (Exception e) {
            log.error("Failed to save resume file, userId: {}, fileName: {}", userId, fileName, e);
            throw new BusinessException("Failed to save file, please retry later");
        }

        Long taskId = createTask(userId, fileUrl);
        return String.valueOf(taskId);
    }

    private String sanitizeOriginalFilename(String originalFilename) {
        if (originalFilename == null) {
            throw new BusinessException("Filename must not be null");
        }

        String normalizedFilename = originalFilename.replace("\\", "/");
        String safeFilename = Paths.get(normalizedFilename).getFileName().toString();
        if (safeFilename.isBlank()) {
            throw new BusinessException("Filename is invalid");
        }
        return safeFilename;
    }

    private String buildStoredResumeFileName() {
        return System.currentTimeMillis() + "_" + UUID.randomUUID().toString().replace("-", "") + ".pdf";
    }

    @Override
    @Cacheable(value = "resume:task", key = "#taskId + '::' + #userId")
    public ResumeDiagnosisTaskResponse getTaskById(Long taskId, Long userId) {
        ResumeDiagnosisTask task = getById(taskId);
        if (task == null) {
            throw new BusinessException("任务不存在");
        }

        // 校验任务归属
        if (!task.getUserId().equals(userId)) {
            throw new BusinessException("无权访问该任务");
        }

        return buildTaskResponse(task);
    }

    @Override
    public PageResult<ResumeDiagnosisHistoryResponse> getHistoryByUserId(Long userId, Integer pageNum, Integer pageSize) {
        LambdaQueryWrapper<ResumeDiagnosisTask> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(ResumeDiagnosisTask::getUserId, userId);
        wrapper.orderByDesc(ResumeDiagnosisTask::getCreateTime);

        Page<ResumeDiagnosisTask> page = new Page<>(pageNum, pageSize);
        Page<ResumeDiagnosisTask> resultPage = page(page, wrapper);

        List<ResumeDiagnosisHistoryResponse> list = resultPage.getRecords().stream()
                .map(this::buildHistoryResponse)
                .collect(Collectors.toList());

        return PageResult.of(list, resultPage.getTotal(), pageNum, pageSize);
    }

    @Override
    @SuppressWarnings("deprecation")
    public List<ResumeDiagnosisHistoryResponse> getHistoryByUserId(Long userId) {
        LambdaQueryWrapper<ResumeDiagnosisTask> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(ResumeDiagnosisTask::getUserId, userId);
        wrapper.orderByDesc(ResumeDiagnosisTask::getCreateTime);

        List<ResumeDiagnosisTask> tasks = list(wrapper);

        return tasks.stream()
                .map(this::buildHistoryResponse)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    @CacheEvict(value = "resume:task", allEntries = true)
    public boolean updateStatusToProcessing(Long taskId) {
        // 只允许一个消费者把 PENDING 任务原子切换为 PROCESSING，避免重复扣费与重复调用 AI。
        int affected = getBaseMapper().claimPendingTask(
                taskId,
                ResumeDiagnosisConstants.STATUS_PENDING,
                ResumeDiagnosisConstants.STATUS_PROCESSING
        );
        boolean claimed = affected > 0;
        if (!claimed) {
            log.warn("Skip claiming task because it is already processed by another worker, taskId: {}", taskId);
            return false;
        }
        log.info("Task status updated to processing atomically, taskId: {}", taskId);
        return true;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    @CacheEvict(value = "resume:task", allEntries = true)
    public void updateStatusToCompleted(Long taskId, String diagnosisResult) {
        ResumeDiagnosisTask task = getById(taskId);
        if (task == null) {
            log.warn("Task not found when updating to completed, taskId: {}", taskId);
            return;
        }

        task.setStatus(ResumeDiagnosisConstants.STATUS_COMPLETED);
        task.setDiagnosisResult(diagnosisResult);
        task.setErrorMsg(null);
        updateById(task);
        log.info("Task status updated to completed, taskId: {}", taskId);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    @CacheEvict(value = "resume:task", allEntries = true)
    public void updateStatusToFailed(Long taskId, String errorMsg) {
        ResumeDiagnosisTask task = getById(taskId);
        if (task == null) {
            log.warn("Task not found when updating to failed, taskId: {}", taskId);
            return;
        }

        task.setStatus(ResumeDiagnosisConstants.STATUS_FAILED);
        task.setDiagnosisResult(null);
        task.setErrorMsg(errorMsg);
        updateById(task);
        log.error("Task status updated to failed, taskId: {}, errorMsg: {}", taskId, errorMsg);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    @CacheEvict(value = "resume:task", allEntries = true)
    public void updateTaskResumeText(Long taskId, String resumeText) {
        ResumeDiagnosisTask task = getById(taskId);
        if (task == null) {
            log.warn("Task not found when updating resume text, taskId: {}", taskId);
            return;
        }

        task.setResumeText(resumeText);
        updateById(task);
        log.info("Task resume text updated, taskId: {}", taskId);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    @CacheEvict(value = "resume:task", allEntries = true)
    public void updateTaskResumeParseResult(Long taskId, String resumeText, String parseMode, String parseMessage) {
        ResumeDiagnosisTask task = getById(taskId);
        if (task == null) {
            log.warn("Task not found when updating resume parse result, taskId: {}", taskId);
            return;
        }

        // 统一写入缓存文本与解析元信息，供结果页和后续能力复用。
        task.setResumeText(resumeText);
        task.setParseMode(parseMode);
        task.setParseMessage(parseMessage);
        updateById(task);
        log.info("Task resume parse result updated, taskId: {}, parseMode: {}", taskId, parseMode);
    }

    @Override
    public Integer getTaskStatus(Long taskId) {
        ResumeDiagnosisTask task = getById(taskId);
        if (task == null) return null;
        return task.getStatus();
    }

    @Override
    public String getStatusDescription(Integer status) {
        return switch (status) {
            case ResumeDiagnosisConstants.STATUS_PENDING -> "排队中";
            case ResumeDiagnosisConstants.STATUS_PROCESSING -> "解析分析中";
            case ResumeDiagnosisConstants.STATUS_COMPLETED -> "已完成";
            case ResumeDiagnosisConstants.STATUS_FAILED -> "失败";
            default -> "未知状态";
        };
    }

    /**
     * 构建任务详情响应对象
     *
     * @param task 任务实体
     * @return 任务详情响应
     */
    private ResumeDiagnosisTaskResponse buildTaskResponse(ResumeDiagnosisTask task) {
        boolean isCompletedTask = task.getStatus() != null
                && task.getStatus() == ResumeDiagnosisConstants.STATUS_COMPLETED;
        String resumeText = resolveResumeTextForResponse(task, isCompletedTask);

        return ResumeDiagnosisTaskResponse.builder()
                .taskId(String.valueOf(task.getId()))
                .userId(task.getUserId())
                .fileUrl(task.getFileUrl())
                .status(task.getStatus())
                .statusDesc(getStatusDescription(task.getStatus()))
                .diagnosisResult(task.getDiagnosisResult())
                .errorMsg(task.getErrorMsg())
                .resumeText(resumeText)
                .parseMode(task.getParseMode())
                .parseMessage(task.getParseMessage())
                .latestJobMatchAnalysis(resolveLatestJobMatchAnalysis(task, isCompletedTask))
                .latestPolishResult(resolveLatestPolishResult(task, isCompletedTask))
                .createTime(task.getCreateTime())
                .updateTime(task.getUpdateTime())
                .build();
    }

    /**
     * 任务详情响应中的简历文本采用“轮询态轻响应、完成态完整响应”策略：
     * 1. 排队中/处理中：只返回数据库中已经缓存的文本，绝不在轮询接口里兜底解析 PDF。
     * 2. 已完成：优先返回缓存文本；只有缓存缺失时才允许做一次兜底解析并回写。
     */
    private String resolveResumeTextForResponse(ResumeDiagnosisTask task, boolean allowPdfFallback) {
        if (task.getResumeText() != null && !task.getResumeText().isBlank()) {
            log.debug("Using cached resume text for taskId: {}", task.getId());
            return task.getResumeText();
        }
        if (!allowPdfFallback || task.getFileUrl() == null || task.getFileUrl().isBlank()) {
            return null;
        }

        try {
            // 仅在已完成任务缺少缓存文本时才兜底重解析，避免轮询阶段重复开销。
            ResumeParseResult parseResult = resumeContentExtractor.extract(task.getFileUrl());
            String resumeText = parseResult.getText();
            try {
                updateTaskResumeParseResult(
                        task.getId(),
                        resumeText,
                        parseResult.getParseMode(),
                        parseResult.getParseMessage());
            } catch (Exception e) {
                log.warn("Failed to cache resume parse result for completed task, taskId: {}", task.getId(), e);
            }
            return resumeText;
        } catch (Exception e) {
            log.warn("Extract resume text fallback failed for completed task, taskId: {}", task.getId(), e);
            return null;
        }
    }

    /**
     * 岗位匹配与润色结果只在诊断完成后才回填，避免结果页轮询时持续查询附属记录。
     */
    private ResumeJobMatchAnalyzeResponse resolveLatestJobMatchAnalysis(ResumeDiagnosisTask task,
                                                                       boolean includeExtendedData) {
        if (!includeExtendedData) {
            return null;
        }
        return resumeJobMatchService.getLatestAnalysis(task.getUserId(), task.getId());
    }

    /**
     * 润色结果同样只在诊断完成态回填，减少等待阶段的接口负担。
     */
    private ResumePolishAnalyzeResponse resolveLatestPolishResult(ResumeDiagnosisTask task,
                                                                  boolean includeExtendedData) {
        if (!includeExtendedData) {
            return null;
        }
        return resumePolishService.getLatestPolishResult(task.getUserId(), task.getId());
    }

    /**
     * 构建历史记录响应对象
     *
     * @param task 任务实体
     * @return 历史记录响应
     */
    private ResumeDiagnosisHistoryResponse buildHistoryResponse(ResumeDiagnosisTask task) {
        return ResumeDiagnosisHistoryResponse.builder()
                .taskId(String.valueOf(task.getId()))
                .fileUrl(task.getFileUrl())
                .status(task.getStatus())
                .statusDesc(getStatusDescription(task.getStatus()))
                .createTime(task.getCreateTime())
                .updateTime(task.getUpdateTime())
                .build();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public int recoverOrphanedTasks(int timeoutMinutes) {
        // 查询超时的处理中任务：状态为PROCESSING且updateTime早于阈值时间
        LocalDateTime timeoutThreshold = LocalDateTime.now().minusMinutes(timeoutMinutes);
        LambdaQueryWrapper<ResumeDiagnosisTask> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(ResumeDiagnosisTask::getStatus, ResumeDiagnosisConstants.STATUS_PROCESSING)
                .lt(ResumeDiagnosisTask::getUpdateTime, timeoutThreshold);

        List<ResumeDiagnosisTask> orphans = list(wrapper);
        if (orphans.isEmpty()) {
            return 0;
        }

        log.warn("发现 {} 个超时孤儿任务，开始回收...", orphans.size());
        for (ResumeDiagnosisTask task : orphans) {
            task.setStatus(ResumeDiagnosisConstants.STATUS_FAILED);
            task.setErrorMsg("任务处理超时，系统自动回收。可能原因：服务重启或消费者异常");
            updateById(task);
            log.warn("已回收孤儿任务, taskId: {}, userId: {}, 原updateTime: {}",
                    task.getId(), task.getUserId(), task.getUpdateTime());
        }
        log.warn("孤儿任务回收完成, 共处理 {} 个任务", orphans.size());
        return orphans.size();
    }
}
