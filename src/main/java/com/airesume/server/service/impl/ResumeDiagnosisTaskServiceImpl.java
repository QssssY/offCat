package com.airesume.server.service.impl;

import com.airesume.server.common.constants.ResumeDiagnosisConstants;
import com.airesume.server.common.exception.BusinessException;
import com.airesume.server.common.result.PageResult;
import com.airesume.server.dto.resume.ResumeDiagnosisHistoryResponse;
import com.airesume.server.dto.resume.ResumeDiagnosisTaskResponse;
import com.airesume.server.entity.ResumeDiagnosisTask;
import com.airesume.server.mapper.ResumeDiagnosisTaskMapper;
import com.airesume.server.mq.ResumeDiagnosisProducer;
import com.airesume.server.service.NotificationService;
import com.airesume.server.service.PdfTextExtractor;
import com.airesume.server.service.ResumeDiagnosisTaskService;
import com.airesume.server.service.ResumeJobMatchService;
import com.airesume.server.service.ResumePolishService;
import com.airesume.server.service.UserQuotaService;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.stream.Collectors;

/**
 * 简历诊断任务服务实现类
 * 实现简历诊断任务的创建、查询、状态更新等业务逻辑
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ResumeDiagnosisTaskServiceImpl extends ServiceImpl<ResumeDiagnosisTaskMapper, ResumeDiagnosisTask> implements ResumeDiagnosisTaskService {

    private final UserQuotaService userQuotaService;
    private final ResumeDiagnosisProducer resumeDiagnosisProducer;
    private final PdfTextExtractor pdfTextExtractor;
    private final NotificationService notificationService;
    private final ResumeJobMatchService resumeJobMatchService;
    private final ResumePolishService resumePolishService;

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

        // 4. 发送消息到RabbitMQ
        resumeDiagnosisProducer.sendResumeDiagnosisTask(task.getId(), userId, fileUrl);

        log.info("Resume diagnosis task submitted to queue, taskId: {}", task.getId());

        return task.getId();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public String createTask(Long userId, MultipartFile file) {
        // 1. 校验文件
        if (file.isEmpty()) {
            throw new BusinessException("上传文件不能为空");
        }

        String originalFilename = file.getOriginalFilename();
        if (originalFilename == null || !originalFilename.toLowerCase().endsWith(".pdf")) {
            throw new BusinessException("仅支持 PDF 格式文件");
        }

        // 校验文件大小（防止大文件上传耗尽磁盘空间）
        if (file.getSize() > maxFileSize) {
            throw new BusinessException("文件大小不能超过" + (maxFileSize / 1024 / 1024) + "MB");
        }

        // 2. 保存文件到存储（这里使用本地存储，生产环境建议改为OSS）
        String fileName = System.currentTimeMillis() + "_" + originalFilename;
        String fileUrl;

        try {
            // 使用配置的上传目录，如果未配置则使用默认路径
            String uploadDir;
            if (configuredUploadDir != null && !configuredUploadDir.isBlank()) {
                uploadDir = configuredUploadDir;
            } else {
                // 默认使用项目根目录下的 uploads 目录
                uploadDir = System.getProperty("user.dir") + "/uploads/resumes/";
            }
            // 确保目录路径以分隔符结尾
            if (!uploadDir.endsWith("/") && !uploadDir.endsWith("\\")) {
                uploadDir = uploadDir + "/";
            }
            java.io.File dir = new java.io.File(uploadDir);
            if (!dir.exists()) {
                dir.mkdirs();
            }

            // 保存文件
            java.io.File destFile = new java.io.File(dir, fileName);
            file.transferTo(destFile);

            // 生成文件访问URL（这里使用相对路径，实际部署需要配置域名）
            fileUrl = "/uploads/resumes/" + fileName;

            log.info("Resume file saved, userId: {}, fileName: {}, fileUrl: {}",
                    userId, fileName, fileUrl);

        } catch (Exception e) {
            log.error("Failed to save resume file, userId: {}, fileName: {}", userId, fileName, e);
            throw new BusinessException("文件保存失败，请稍后重试");
        }

        // 3. 调用原有方法创建任务并转换为字符串返回
        Long taskId = createTask(userId, fileUrl);
        return String.valueOf(taskId);
    }

    @Override
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
    public void updateStatusToProcessing(Long taskId) {
        ResumeDiagnosisTask task = getById(taskId);
        if (task == null) {
            log.warn("Task not found when updating to processing, taskId: {}", taskId);
            return;
        }

        task.setStatus(ResumeDiagnosisConstants.STATUS_PROCESSING);
        updateById(task);
        log.info("Task status updated to processing, taskId: {}", taskId);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
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
        String resumeText = null;
        // 优先使用已缓存的简历文本，避免重复解析PDF
        if (task.getResumeText() != null && !task.getResumeText().isBlank()) {
            resumeText = task.getResumeText();
            log.debug("Using cached resume text for taskId: {}", task.getId());
        } else if (task.getFileUrl() != null && !task.getFileUrl().isBlank()) {
            try {
                // 缓存中没有，则解析PDF并缓存
                resumeText = pdfTextExtractor.extractText(task.getFileUrl());
                // 异步缓存到数据库（避免影响响应速度）
                try {
                    updateTaskResumeText(task.getId(), resumeText);
                } catch (Exception e) {
                    log.warn("Failed to cache resume text for taskId: {}", task.getId(), e);
                }
            } catch (Exception e) {
                log.warn("Extract resume text failed when building task response, taskId: {}", task.getId(), e);
            }
        }

        return ResumeDiagnosisTaskResponse.builder()
                .taskId(String.valueOf(task.getId()))
                .userId(task.getUserId())
                .fileUrl(task.getFileUrl())
                .status(task.getStatus())
                .statusDesc(getStatusDescription(task.getStatus()))
                .diagnosisResult(task.getDiagnosisResult())
                .errorMsg(task.getErrorMsg())
                .resumeText(resumeText)
                .latestJobMatchAnalysis(resumeJobMatchService.getLatestAnalysis(task.getUserId(), task.getId()))
                .latestPolishResult(resumePolishService.getLatestPolishResult(task.getUserId(), task.getId()))
                .createTime(task.getCreateTime())
                .updateTime(task.getUpdateTime())
                .build();
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
}
