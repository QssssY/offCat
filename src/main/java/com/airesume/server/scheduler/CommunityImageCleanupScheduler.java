package com.airesume.server.scheduler;

import com.airesume.server.service.CommunityImageRegistryService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;

/**
 * 社区未绑定图片清理任务。
 * 上传后 24 小时仍未绑定到帖子/评论的图片会被视为图床滥用风险并清理。
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class CommunityImageCleanupScheduler {

    private final CommunityImageRegistryService imageRegistryService;

    @Value("${app.community.image-cleanup.ttl-hours:24}")
    private int ttlHours;

    @Value("${app.community.image-cleanup.batch-size:100}")
    private int batchSize;

    @Scheduled(cron = "${app.community.image-cleanup.cron:0 20 3 * * ?}")
    public void cleanupExpiredUnboundImages() {
        LocalDateTime expireBefore = LocalDateTime.now().minusHours(Math.max(1, ttlHours));
        int cleaned = imageRegistryService.cleanupExpiredUnboundImages(expireBefore, batchSize);
        if (cleaned > 0) {
            log.info("社区未绑定图片清理完成, cleaned: {}", cleaned);
        }
    }
}
