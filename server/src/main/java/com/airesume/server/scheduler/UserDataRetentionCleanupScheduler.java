package com.airesume.server.scheduler;

import com.airesume.server.service.UserDataRetentionCleanupService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * 用户数据保留期自动清理调度器。
 * 每天低峰执行，实际删除逻辑在服务层按用户和批次限流。
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class UserDataRetentionCleanupScheduler {

    private final UserDataRetentionCleanupService cleanupService;

    @Scheduled(cron = "0 30 3 * * ?")
    public void cleanupExpiredUserData() {
        try {
            int interviewCount = cleanupService.cleanupExpiredInterviewRecords();
            int resumeCount = cleanupService.cleanupExpiredResumeRecords();
            if (interviewCount > 0 || resumeCount > 0) {
                log.info("用户数据保留期自动清理完成, interviewCount: {}, resumeCount: {}", interviewCount, resumeCount);
            }
        } catch (Exception e) {
            log.error("用户数据保留期自动清理任务异常", e);
        }
    }
}
