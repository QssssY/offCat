package com.airesume.server.service.impl;

import com.airesume.server.common.constants.InterviewConstants;
import com.airesume.server.common.constants.ResumeDiagnosisConstants;
import com.airesume.server.entity.UserSettings;
import com.airesume.server.mapper.MockInterviewJobTargetRecordMapper;
import com.airesume.server.mapper.ResumeDiagnosisTaskMapper;
import com.airesume.server.mapper.ResumeJobMatchRecordMapper;
import com.airesume.server.mapper.ResumePolishRecordMapper;
import com.airesume.server.mapper.UserSettingsMapper;
import com.airesume.server.repository.InterviewMessageRepository;
import com.airesume.server.repository.InterviewSessionRepository;
import com.airesume.server.service.UserDataRetentionCleanupService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;

/**
 * 用户数据保留期自动清理服务实现。
 * 每个用户每类数据最多处理固定批次数，避免单次定时任务把数据库和文件系统压力拉满。
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class UserDataRetentionCleanupServiceImpl implements UserDataRetentionCleanupService {

    private static final int BATCH_SIZE = 200;
    private static final int MAX_BATCHES_PER_USER = 10;
    private static final List<Integer> RESUME_TERMINAL_STATUSES = List.of(
            ResumeDiagnosisConstants.STATUS_COMPLETED,
            ResumeDiagnosisConstants.STATUS_FAILED
    );

    private final UserSettingsMapper userSettingsMapper;
    private final InterviewSessionRepository interviewSessionRepository;
    private final InterviewMessageRepository interviewMessageRepository;
    private final MockInterviewJobTargetRecordMapper mockInterviewJobTargetRecordMapper;
    private final ResumeDiagnosisTaskMapper resumeDiagnosisTaskMapper;
    private final ResumeJobMatchRecordMapper resumeJobMatchRecordMapper;
    private final ResumePolishRecordMapper resumePolishRecordMapper;

    @Override
    public int cleanupExpiredInterviewRecords() {
        int total = 0;
        for (UserSettings settings : userSettingsMapper.selectInterviewRetentionEnabled()) {
            try {
                total += cleanupExpiredInterviewRecordsForUser(settings.getUserId(), settings.getInterviewRetentionDays());
            } catch (Exception e) {
                log.warn("面试记录自动清理失败, userId: {}", settings.getUserId(), e);
            }
        }
        return total;
    }

    @Override
    public int cleanupExpiredResumeRecords() {
        int total = 0;
        for (UserSettings settings : userSettingsMapper.selectResumeRetentionEnabled()) {
            try {
                total += cleanupExpiredResumeRecordsForUser(settings.getUserId(), settings.getResumeRetentionDays());
            } catch (Exception e) {
                log.warn("简历诊断记录自动清理失败, userId: {}", settings.getUserId(), e);
            }
        }
        return total;
    }

    /**
     * 按用户分批清理过期面试记录。
     * 只清理已结束会话，聊天记录和岗位定向上下文跟随主会话一起逻辑删除。
     */
    @Transactional(rollbackFor = Exception.class)
    public int cleanupExpiredInterviewRecordsForUser(Long userId, Integer retentionDays) {
        if (userId == null || retentionDays == null || retentionDays <= 0) {
            return 0;
        }

        int deleted = 0;
        LocalDateTime cutoffTime = LocalDateTime.now().minusDays(retentionDays);
        for (int batchNo = 0; batchNo < MAX_BATCHES_PER_USER; batchNo++) {
            List<String> sessionIds = interviewSessionRepository.findExpiredSessionIds(
                    userId,
                    InterviewConstants.STATUS_ENDED,
                    cutoffTime,
                    PageRequest.of(0, BATCH_SIZE)
            );
            if (sessionIds.isEmpty()) {
                break;
            }

            LocalDateTime now = LocalDateTime.now();
            interviewMessageRepository.logicalDeleteBySessionIdIn(sessionIds, now);
            mockInterviewJobTargetRecordMapper.logicalDeleteBySessionIds(sessionIds);
            deleted += interviewSessionRepository.logicalDeleteBySessionIdIn(sessionIds, now);
        }
        return deleted;
    }

    /**
     * 按用户分批清理过期简历诊断记录。
     * 只清理完成或失败的终态任务，处理中任务由任务恢复逻辑单独处理。
     */
    @Transactional(rollbackFor = Exception.class)
    public int cleanupExpiredResumeRecordsForUser(Long userId, Integer retentionDays) {
        if (userId == null || retentionDays == null || retentionDays <= 0) {
            return 0;
        }

        int deleted = 0;
        LocalDateTime cutoffTime = LocalDateTime.now().minusDays(retentionDays);
        for (int batchNo = 0; batchNo < MAX_BATCHES_PER_USER; batchNo++) {
            List<Long> taskIds = resumeDiagnosisTaskMapper.selectExpiredTerminalTaskIds(
                    userId,
                    RESUME_TERMINAL_STATUSES,
                    cutoffTime,
                    BATCH_SIZE);
            if (taskIds.isEmpty()) {
                break;
            }

            List<String> fileUrls = resumeDiagnosisTaskMapper.selectActiveFileUrlsByTaskIds(taskIds);
            resumeJobMatchRecordMapper.logicalDeleteByResumeTaskIds(taskIds);
            resumePolishRecordMapper.logicalDeleteByResumeTaskIds(taskIds);
            deleted += resumeDiagnosisTaskMapper.logicalDeleteByTaskIds(taskIds);
            deleteResumeFilesIfExists(fileUrls);
        }
        return deleted;
    }

    private void deleteResumeFilesIfExists(Collection<String> fileUrls) {
        if (fileUrls == null || fileUrls.isEmpty()) {
            return;
        }
        for (String fileUrl : fileUrls) {
            deleteResumeFileIfExists(fileUrl);
        }
    }

    /**
     * 自动清理只允许删除 uploads/resumes 目录内的文件。
     * 非法路径会被跳过并记录日志，避免定时任务误删目录外文件。
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
            log.warn("跳过非法简历文件路径, fileUrl: {}", fileUrl);
            return;
        }

        Path resolvedPath = uploadRoot.resolve(normalized.substring(prefix.length())).normalize();
        if (!resolvedPath.startsWith(uploadRoot)) {
            log.warn("跳过越界简历文件路径, fileUrl: {}", fileUrl);
            return;
        }

        try {
            Files.deleteIfExists(resolvedPath);
        } catch (Exception e) {
            log.warn("自动清理简历上传文件失败, fileUrl: {}", fileUrl, e);
        }
    }
}
