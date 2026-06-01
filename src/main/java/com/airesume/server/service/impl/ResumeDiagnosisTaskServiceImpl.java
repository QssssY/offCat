package com.airesume.server.service.impl;

import com.airesume.server.common.constants.ResumeDiagnosisConstants;
import com.airesume.server.common.constants.UserAiConstants;
import com.airesume.server.common.exception.BusinessException;
import com.airesume.server.common.result.PageResult;
import com.airesume.server.common.result.ResultCode;
import com.airesume.server.dto.resume.ResumeDiagnosisHistoryResponse;
import com.airesume.server.dto.resume.ResumeJobMatchAnalyzeResponse;
import com.airesume.server.dto.resume.ResumePolishAnalyzeResponse;
import com.airesume.server.dto.resume.ResumeDiagnosisTaskResponse;
import com.airesume.server.entity.ResumeDiagnosisTask;
import com.airesume.server.mapper.ResumeDiagnosisTaskMapper;
import com.airesume.server.mapper.ResumeJobMatchRecordMapper;
import com.airesume.server.mapper.ResumePolishRecordMapper;
import com.airesume.server.mq.DirectProcessRouter;
import com.airesume.server.mq.ResumeDiagnosisProducer;
import com.airesume.server.service.NotificationService;
import com.airesume.server.service.ResumeContentExtractor;
import com.airesume.server.service.ResumeDiagnosisTaskService;
import com.airesume.server.service.ResumeJobMatchService;
import com.airesume.server.service.ResumePolishService;
import com.airesume.server.service.UserQuotaService;
import com.airesume.server.service.UserAiConfigResolver;
import com.airesume.server.service.UserAiUsageLimitService;
import com.airesume.server.service.resume.ResumeParseResult;
import org.springframework.context.annotation.Lazy;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;
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

    private static final String RESUME_TASK_CACHE = "resume:task";

    private final UserQuotaService userQuotaService;
    private final ResumeDiagnosisProducer resumeDiagnosisProducer;
    private final DirectProcessRouter directProcessRouter;
    private final ResumeContentExtractor resumeContentExtractor;
    private final NotificationService notificationService;
    private final ResumeJobMatchService resumeJobMatchService;
    private final ResumePolishService resumePolishService;
    private final ResumeJobMatchRecordMapper resumeJobMatchRecordMapper;
    private final ResumePolishRecordMapper resumePolishRecordMapper;
    private final UserAiConfigResolver userAiConfigResolver;
    private final UserAiUsageLimitService userAiUsageLimitService;
    @Autowired(required = false)
    private CacheManager cacheManager;

    /**
     * 手动构造器注入，@Lazy 打破 TaskServiceImpl ↔ DirectProcessRouter ↔ Processor ↔ TaskService 循环依赖
     */
    @Autowired
    public ResumeDiagnosisTaskServiceImpl(
            UserQuotaService userQuotaService,
            ResumeDiagnosisProducer resumeDiagnosisProducer,
            @Lazy DirectProcessRouter directProcessRouter,
            ResumeContentExtractor resumeContentExtractor,
            NotificationService notificationService,
            ResumeJobMatchService resumeJobMatchService,
            ResumePolishService resumePolishService,
            ResumeJobMatchRecordMapper resumeJobMatchRecordMapper,
            ResumePolishRecordMapper resumePolishRecordMapper,
            UserAiConfigResolver userAiConfigResolver,
            UserAiUsageLimitService userAiUsageLimitService) {
        this.userQuotaService = userQuotaService;
        this.resumeDiagnosisProducer = resumeDiagnosisProducer;
        this.directProcessRouter = directProcessRouter;
        this.resumeContentExtractor = resumeContentExtractor;
        this.notificationService = notificationService;
        this.resumeJobMatchService = resumeJobMatchService;
        this.resumePolishService = resumePolishService;
        this.resumeJobMatchRecordMapper = resumeJobMatchRecordMapper;
        this.resumePolishRecordMapper = resumePolishRecordMapper;
        this.userAiConfigResolver = userAiConfigResolver;
        this.userAiUsageLimitService = userAiUsageLimitService;
    }

    /**
     * 兼容既有单元测试的构造器，生产注入使用包含自定义 AI 依赖的完整构造器。
     */
    public ResumeDiagnosisTaskServiceImpl(
            UserQuotaService userQuotaService,
            ResumeDiagnosisProducer resumeDiagnosisProducer,
            @Lazy DirectProcessRouter directProcessRouter,
            ResumeContentExtractor resumeContentExtractor,
            NotificationService notificationService,
            ResumeJobMatchService resumeJobMatchService,
            ResumePolishService resumePolishService,
            ResumeJobMatchRecordMapper resumeJobMatchRecordMapper,
            ResumePolishRecordMapper resumePolishRecordMapper) {
        this(userQuotaService, resumeDiagnosisProducer, directProcessRouter, resumeContentExtractor,
                notificationService, resumeJobMatchService, resumePolishService, resumeJobMatchRecordMapper,
                resumePolishRecordMapper, null, null);
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

    /** 上传目录至少保留的可用空间，默认 1GB，防止低磁盘空间时继续接收简历源文件。 */
    @Value("${app.upload.min-free-space-mb:1024}")
    private long minFreeSpaceMb;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Long createTask(Long userId, String fileUrl) {
        return createTask(userId, fileUrl, false);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Long createTask(Long userId, String fileUrl, boolean fallbackToPlatform) {
        log.info("Creating resume diagnosis task, userId: {}, fileUrl: {}", userId, fileUrl);

        boolean useCustomAi = userAiConfigResolver != null
                && userAiConfigResolver.resolve(userId, UserAiConstants.CONFIG_TYPE_RESUME, fallbackToPlatform) != null;
        String billingSource = useCustomAi ? UserAiConstants.BILLING_SOURCE_USER_CUSTOM : UserAiConstants.BILLING_SOURCE_PLATFORM;

        // 1. 创建前锁定计费来源：用户自定义 AI 扣独立次数，平台 AI 保持原平台额度。
        if (useCustomAi) {
            userAiUsageLimitService.checkAndIncrement(userId);
        } else {
            boolean hasQuota = userQuotaService.checkResumeQuota(userId);
            if (!hasQuota) {
                notificationService.createQuotaNotificationIfNeeded(userId);
                throw new BusinessException(ResultCode.RESUME_QUOTA_EXHAUSTED);
            }
        }

        // 2. 创建任务记录
        ResumeDiagnosisTask task = new ResumeDiagnosisTask();
        task.setUserId(userId);
        task.setFileUrl(fileUrl);
        task.setStatus(ResumeDiagnosisConstants.STATUS_PENDING);
        task.setStage(null);
        task.setDiagnosisResult(null);
        task.setErrorMsg(null);
        task.setFailedAt(null);
        task.setAiBillingSource(billingSource);
        task.setFallbackToPlatform(fallbackToPlatform ? 1 : 0);
        save(task);

        log.info("Resume diagnosis task created, taskId: {}", task.getId());

        // 3. 平台 AI 在任务创建时扣平台额度；用户自定义 AI 已扣每日次数。
        if (!useCustomAi) {
            userQuotaService.deductResumeQuota(userId);
        }

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
        return createTask(userId, file, false);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public String createTask(Long userId, MultipartFile file, boolean fallbackToPlatform) {
        if (file.isEmpty()) {
            throw new BusinessException(ResultCode.RESUME_FILE_EMPTY);
        }

        String originalFilename = sanitizeOriginalFilename(file.getOriginalFilename());
        if (!originalFilename.toLowerCase().endsWith(".pdf")) {
            throw new BusinessException(ResultCode.RESUME_FORMAT_UNSUPPORTED);
        }

        if (file.getSize() > maxFileSize) {
            throw new BusinessException(ResultCode.RESUME_FILE_TOO_LARGE, "文件大小不能超过 " + (maxFileSize / 1024 / 1024) + "MB");
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
            ensureUploadDirectoryHasEnoughSpace(uploadDirPath);

            Path destPath = uploadDirPath.resolve(fileName).normalize();
            if (!destPath.startsWith(uploadDirPath)) {
                throw new BusinessException(ResultCode.RESUME_FILE_ILLEGAL_PATH);
            }
            file.transferTo(destPath.toFile());

            fileUrl = "/uploads/resumes/" + fileName;
            log.info("Resume file saved, userId: {}, fileName: {}, fileUrl: {}",
                    userId, fileName, fileUrl);
        } catch (BusinessException e) {
            throw e;
        } catch (Exception e) {
            log.error("Failed to save resume file, userId: {}, fileName: {}", userId, fileName, e);
            throw new BusinessException(ResultCode.RESUME_FILE_SAVE_FAILED);
        }

        Long taskId = createTask(userId, fileUrl, fallbackToPlatform);
        return String.valueOf(taskId);
    }

    private String sanitizeOriginalFilename(String originalFilename) {
        if (originalFilename == null) {
            throw new BusinessException(ResultCode.RESUME_FILE_EMPTY);
        }

        String normalizedFilename = originalFilename.replace("\\", "/");
        String safeFilename = Paths.get(normalizedFilename).getFileName().toString();
        if (safeFilename.isBlank()) {
            throw new BusinessException(ResultCode.RESUME_FILE_EMPTY);
        }
        return safeFilename;
    }

    private String buildStoredResumeFileName() {
        return System.currentTimeMillis() + "_" + UUID.randomUUID().toString().replace("-", "") + ".pdf";
    }

    /**
     * 上传前检查目标磁盘可用空间，避免简历源文件继续写入导致磁盘被打满。
     */
    protected void ensureUploadDirectoryHasEnoughSpace(Path uploadDirPath) throws java.io.IOException {
        if (minFreeSpaceMb <= 0) {
            return;
        }
        long minFreeBytes = minFreeSpaceMb * 1024L * 1024L;
        long usableBytes = usableSpace(uploadDirPath);
        if (usableBytes < minFreeBytes) {
            log.warn("Resume upload rejected due to low disk space, uploadDir: {}, usableBytes: {}, minFreeBytes: {}",
                    uploadDirPath, usableBytes, minFreeBytes);
            throw new BusinessException(ResultCode.RESUME_STORAGE_SPACE_LOW);
        }
    }

    protected long usableSpace(Path uploadDirPath) throws java.io.IOException {
        return Files.getFileStore(uploadDirPath).getUsableSpace();
    }

    @Override
    @Cacheable(value = "resume:task", key = "#taskId + '::' + #userId")
    public ResumeDiagnosisTaskResponse getTaskById(Long taskId, Long userId) {
        ResumeDiagnosisTask task = getBaseMapper().selectOne(new LambdaQueryWrapper<ResumeDiagnosisTask>()
                // 详情页需要诊断 JSON 与简历文本，显式补回默认不全列加载的大字段。
                .select(ResumeDiagnosisTask::getId, ResumeDiagnosisTask::getUserId,
                        ResumeDiagnosisTask::getFileUrl, ResumeDiagnosisTask::getStatus,
                        ResumeDiagnosisTask::getStage, ResumeDiagnosisTask::getDiagnosisResult,
                        ResumeDiagnosisTask::getErrorMsg, ResumeDiagnosisTask::getFailedAt,
                        ResumeDiagnosisTask::getResumeText, ResumeDiagnosisTask::getParseMode,
                        ResumeDiagnosisTask::getParseMessage, ResumeDiagnosisTask::getCreateTime,
                        ResumeDiagnosisTask::getUpdateTime, ResumeDiagnosisTask::getIsDeleted)
                .eq(ResumeDiagnosisTask::getId, taskId)
                .last("limit 1"));
        if (task == null) {
            throw new BusinessException(ResultCode.RESUME_TASK_NOT_FOUND);
        }

        // 校验任务归属
        if (!task.getUserId().equals(userId)) {
            throw new BusinessException(ResultCode.RESUME_TASK_ACCESS_DENIED);
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
        evictResumeTaskCache(taskId);
        return true;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void updateStatusToCompleted(Long taskId, String diagnosisResult) {
        LambdaUpdateWrapper<ResumeDiagnosisTask> wrapper = new LambdaUpdateWrapper<>();
        wrapper.eq(ResumeDiagnosisTask::getId, taskId)
                .set(ResumeDiagnosisTask::getStatus, ResumeDiagnosisConstants.STATUS_COMPLETED)
                .set(ResumeDiagnosisTask::getStage, null)
                .set(ResumeDiagnosisTask::getDiagnosisResult, diagnosisResult)
                .set(ResumeDiagnosisTask::getErrorMsg, null)
                .set(ResumeDiagnosisTask::getFailedAt, null)
                .setSql("update_time = NOW()");
        boolean updated = update(wrapper);
        if (updated) {
            evictResumeTaskCache(taskId);
            log.info("Task status updated to completed, taskId: {}", taskId);
        } else {
            log.warn("Task not found when updating to completed, taskId: {}", taskId);
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void updateStatusToFailed(Long taskId, String errorMsg) {
        LambdaUpdateWrapper<ResumeDiagnosisTask> wrapper = new LambdaUpdateWrapper<>();
        wrapper.eq(ResumeDiagnosisTask::getId, taskId)
                .set(ResumeDiagnosisTask::getStatus, ResumeDiagnosisConstants.STATUS_FAILED)
                .set(ResumeDiagnosisTask::getStage, null)
                .set(ResumeDiagnosisTask::getDiagnosisResult, null)
                .set(ResumeDiagnosisTask::getErrorMsg, errorMsg)
                .set(ResumeDiagnosisTask::getFailedAt, LocalDateTime.now())
                .setSql("update_time = NOW()");
        boolean updated = update(wrapper);
        if (updated) {
            evictResumeTaskCache(taskId);
            log.error("Task status updated to failed, taskId: {}, errorMsg: {}", taskId, errorMsg);
        } else {
            log.warn("Task not found when updating to failed, taskId: {}", taskId);
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void updateTaskResumeText(Long taskId, String resumeText) {
        LambdaUpdateWrapper<ResumeDiagnosisTask> wrapper = new LambdaUpdateWrapper<>();
        wrapper.eq(ResumeDiagnosisTask::getId, taskId)
                .set(ResumeDiagnosisTask::getResumeText, resumeText)
                .setSql("update_time = NOW()");
        boolean updated = update(wrapper);
        if (updated) {
            evictResumeTaskCache(taskId);
            log.info("Task resume text updated, taskId: {}", taskId);
        } else {
            log.warn("Task not found when updating resume text, taskId: {}", taskId);
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void updateTaskResumeParseResult(Long taskId, String resumeText, String parseMode, String parseMessage) {
        LambdaUpdateWrapper<ResumeDiagnosisTask> wrapper = new LambdaUpdateWrapper<>();
        wrapper.eq(ResumeDiagnosisTask::getId, taskId)
                .set(ResumeDiagnosisTask::getResumeText, resumeText)
                .set(ResumeDiagnosisTask::getParseMode, parseMode)
                .set(ResumeDiagnosisTask::getParseMessage, parseMessage)
                .setSql("update_time = NOW()");
        boolean updated = update(wrapper);
        if (updated) {
            evictResumeTaskCache(taskId);
            log.info("Task resume parse result updated, taskId: {}, parseMode: {}", taskId, parseMode);
        } else {
            log.warn("Task not found when updating resume parse result, taskId: {}", taskId);
        }
    }


    /**
     * 按 getTaskById 使用的 taskId::userId key 精准驱逐，避免状态轮询更新时清空整个任务缓存区。
     */
    private void evictResumeTaskCache(Long taskId) {
        if (taskId == null) {
            return;
        }
        ResumeDiagnosisTask task = getBaseMapper().selectOne(new LambdaQueryWrapper<ResumeDiagnosisTask>()
                .select(ResumeDiagnosisTask::getId, ResumeDiagnosisTask::getUserId)
                .eq(ResumeDiagnosisTask::getId, taskId)
                .last("limit 1"));
        if (task != null) {
            evictResumeTaskCache(task.getId(), task.getUserId());
        }
    }

    private void evictResumeTaskCache(Long taskId, Long userId) {
        if (taskId == null || userId == null || cacheManager == null) {
            return;
        }
        Cache cache = cacheManager.getCache(RESUME_TASK_CACHE);
        if (cache != null) {
            cache.evict(taskId + "::" + userId);
        }
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
     * 获取子阶段中文描述。
     */
    private String getStageDescription(String stage) {
        if (stage == null) return null;
        return switch (stage) {
            case ResumeDiagnosisConstants.STAGE_EXTRACTING -> "正在提取简历文本";
            case ResumeDiagnosisConstants.STAGE_AI_ANALYZING -> "AI 正在分析简历";
            case ResumeDiagnosisConstants.STAGE_ENHANCING -> "正在生成诊断报告";
            default -> null;
        };
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void updateStage(Long taskId, String stage) {
        LambdaUpdateWrapper<ResumeDiagnosisTask> wrapper = new LambdaUpdateWrapper<>();
        wrapper.eq(ResumeDiagnosisTask::getId, taskId)
                .eq(ResumeDiagnosisTask::getStatus, ResumeDiagnosisConstants.STATUS_PROCESSING)
                .set(ResumeDiagnosisTask::getStage, stage)
                .setSql("update_time = NOW()");
        boolean updated = update(wrapper);
        if (updated) {
            evictResumeTaskCache(taskId);
            log.info("Task stage updated, taskId: {}, stage: {}", taskId, stage);
        } else {
            log.warn("Task not processing when updating stage, taskId: {}, stage: {}", taskId, stage);
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public String retryFailedTask(Long taskId, Long userId) {
        ResumeDiagnosisTask task = getBaseMapper().selectOne(new LambdaQueryWrapper<ResumeDiagnosisTask>()
                // 重试需要复用 file_url，并在源文件已清理时预填 resume_text。
                .select(ResumeDiagnosisTask::getId, ResumeDiagnosisTask::getUserId,
                        ResumeDiagnosisTask::getFileUrl, ResumeDiagnosisTask::getStatus,
                        ResumeDiagnosisTask::getResumeText, ResumeDiagnosisTask::getFailedAt,
                        ResumeDiagnosisTask::getUpdateTime, ResumeDiagnosisTask::getIsDeleted)
                .eq(ResumeDiagnosisTask::getId, taskId)
                .last("limit 1"));
        if (task == null) {
            throw new BusinessException(ResultCode.RESUME_TASK_NOT_FOUND);
        }
        if (!task.getUserId().equals(userId)) {
            throw new BusinessException(ResultCode.RESUME_TASK_ACCESS_DENIED);
        }
        // 只有失败状态的任务才允许重试
        if (task.getStatus() == null || task.getStatus() != ResumeDiagnosisConstants.STATUS_FAILED) {
            throw new BusinessException(ResultCode.RESUME_TASK_NOT_RETRYABLE);
        }
        // 失败时间超过 24 小时不允许重试，引导用户重新上传。历史数据没有 failed_at 时兼容使用 update_time。
        LocalDateTime failedAt = task.getFailedAt() != null ? task.getFailedAt() : task.getUpdateTime();
        if (failedAt != null && failedAt.isBefore(LocalDateTime.now().minusHours(24))) {
            throw new BusinessException(ResultCode.RESUME_TASK_RETRY_EXPIRED);
        }

        // 复用原文件创建新任务
        Long newTaskId = createTask(userId, task.getFileUrl());

        // 预填缓存文本：重试场景下原文件可能已被定时清理，处理器会优先使用缓存跳过PDF提取
        if (task.getResumeText() != null && !task.getResumeText().isBlank()) {
            updateTaskResumeText(newTaskId, task.getResumeText());
            log.info("重试任务已预填缓存简历文本, 原taskId: {}, 新taskId: {}", taskId, newTaskId);
        }

        log.info("重试失败任务, 原taskId: {}, 新taskId: {}, userId: {}", taskId, newTaskId, userId);
        return String.valueOf(newTaskId);
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
                .stage(task.getStage())
                .stageDesc(getStageDescription(task.getStage()))
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
                .errorMsg(task.getErrorMsg())
                .createTime(task.getCreateTime())
                .updateTime(task.getUpdateTime())
                .build();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public int recoverOrphanedTasks(int timeoutMinutes) {
        // 查询超时的处理中任务：状态为PROCESSING且updateTime早于阈值时间
        LocalDateTime timeoutThreshold = LocalDateTime.now().minusMinutes(timeoutMinutes);

        // 先统计符合条件的任务数量（用于返回值）
        LambdaQueryWrapper<ResumeDiagnosisTask> countWrapper = new LambdaQueryWrapper<>();
        countWrapper.eq(ResumeDiagnosisTask::getStatus, ResumeDiagnosisConstants.STATUS_PROCESSING)
                .lt(ResumeDiagnosisTask::getUpdateTime, timeoutThreshold);
        long orphanCount = count(countWrapper);

        if (orphanCount == 0) {
            return 0;
        }

        // 批量更新：单条 SQL 将所有超时任务标记为失败
        LambdaUpdateWrapper<ResumeDiagnosisTask> updateWrapper = new LambdaUpdateWrapper<>();
        updateWrapper.eq(ResumeDiagnosisTask::getStatus, ResumeDiagnosisConstants.STATUS_PROCESSING)
                .lt(ResumeDiagnosisTask::getUpdateTime, timeoutThreshold)
                .set(ResumeDiagnosisTask::getStatus, ResumeDiagnosisConstants.STATUS_FAILED)
                .set(ResumeDiagnosisTask::getStage, null)
                .set(ResumeDiagnosisTask::getErrorMsg, "任务处理超时，系统自动回收。可能原因：服务重启或消费者异常")
                .set(ResumeDiagnosisTask::getFailedAt, LocalDateTime.now());
        update(updateWrapper);

        log.warn("孤儿任务批量回收完成, 共处理 {} 个任务", orphanCount);
        return (int) orphanCount;
    }

    /**
     * 清理当前用户的全部简历诊断历史。
     * 先读取文件路径再逻辑删除主记录，确保上传文件清理不会因任务不可见而丢失路径来源。
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public int clearHistory(Long userId) {
        List<String> fileUrls = getBaseMapper().selectActiveFileUrlsByUserId(userId);
        List<ResumeDiagnosisTask> cachedTasks = getBaseMapper().selectList(new LambdaQueryWrapper<ResumeDiagnosisTask>()
                .select(ResumeDiagnosisTask::getId, ResumeDiagnosisTask::getUserId)
                .eq(ResumeDiagnosisTask::getUserId, userId));
        resumeJobMatchRecordMapper.logicalDeleteByUserId(userId);
        resumePolishRecordMapper.logicalDeleteByUserId(userId);
        int deletedCount = getBaseMapper().logicalDeleteByUserId(userId);

        if (cachedTasks != null) {
            for (ResumeDiagnosisTask cachedTask : cachedTasks) {
                evictResumeTaskCache(cachedTask.getId(), cachedTask.getUserId());
            }
        }
        for (String fileUrl : fileUrls) {
            deleteResumeFileIfExists(fileUrl);
        }
        return deletedCount;
    }

    /**
     * 删除单条简历诊断记录及其衍生数据（JD 对比记录、AI 润色记录）。
     * 先读取文件路径再逻辑删除，确保上传文件清理不会丢失路径来源。
     */
    @Override
    @Transactional(rollbackFor = Exception.class, noRollbackFor = BusinessException.class)
    public boolean deleteTask(Long userId, Long taskId) {
        ResumeDiagnosisTask task = getById(taskId);
        if (task == null) {
            throw new BusinessException(ResultCode.RESUME_TASK_NOT_FOUND);
        }
        if (!task.getUserId().equals(userId)) {
            throw new BusinessException(ResultCode.RESUME_TASK_ACCESS_DENIED);
        }

        List<String> fileUrls = getBaseMapper().selectActiveFileUrlsByTaskIds(List.of(taskId));
        resumeJobMatchRecordMapper.logicalDeleteByResumeTaskIds(List.of(taskId));
        resumePolishRecordMapper.logicalDeleteByResumeTaskIds(List.of(taskId));
        getBaseMapper().logicalDeleteByTaskIds(List.of(taskId));
        evictResumeTaskCache(taskId, userId);

        // 数据库逻辑删除已完成，文件清理失败不影响事务提交
        boolean fileCleanupFailed = false;
        for (String fileUrl : fileUrls) {
            try {
                deleteResumeFileIfExists(fileUrl);
            } catch (Exception e) {
                fileCleanupFailed = true;
                log.warn("删除简历文件失败，不影响记录删除, taskId: {}, fileUrl: {}", taskId, fileUrl, e);
            }
        }
        if (fileCleanupFailed) {
            throw new BusinessException(ResultCode.RESUME_FILE_CLEANUP_FAILED);
        }
        return true;
    }

    /**
     * 删除用户上传的简历文件。
     * 文件路径必须解析到项目 uploads/resumes 目录内；文件不存在视为已经清理，不阻断数据库逻辑删除。
     */
    private void deleteResumeFileIfExists(String fileUrl) {
        if (fileUrl == null || fileUrl.isBlank()) {
            return;
        }
        Path uploadRoot = Paths.get(System.getProperty("user.dir"), "uploads", "resumes")
                .toAbsolutePath()
                .normalize();
        String normalized = fileUrl.replace("\\", "/").trim();
        String prefix = "/uploads/resumes/";
        if (!normalized.startsWith(prefix)) {
            throw new BusinessException(ResultCode.RESUME_FILE_ILLEGAL_PATH);
        }

        Path resolvedPath = uploadRoot.resolve(normalized.substring(prefix.length())).normalize();
        if (!resolvedPath.startsWith(uploadRoot)) {
            throw new BusinessException(ResultCode.RESUME_FILE_ILLEGAL_PATH);
        }

        try {
            Files.deleteIfExists(resolvedPath);
        } catch (Exception e) {
            log.warn("删除简历上传文件失败, fileUrl: {}", fileUrl, e);
            throw new BusinessException(ResultCode.RESUME_FILE_CLEANUP_FAILED);
        }
    }
}
