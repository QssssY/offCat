package com.airesume.server.scheduler;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.concurrent.atomic.AtomicLong;
import java.util.stream.Stream;

/**
 * 简历上传目录存储观测任务。
 * 该任务只统计文件数量和大小并写日志，不做任何删除动作。
 */
@Slf4j
@Component
public class ResumeUploadStorageMonitor {

    @Value("${app.upload.resume-dir:}")
    private String configuredUploadDir;

    @Scheduled(fixedDelayString = "${app.upload.storage-monitor-interval-ms:3600000}", initialDelayString = "${app.upload.storage-monitor-initial-delay-ms:300000}")
    public void logResumeUploadStorageUsage() {
        Path uploadRoot = resolveUploadRoot();
        if (!Files.exists(uploadRoot)) {
            log.info("简历上传目录尚不存在, path: {}", uploadRoot);
            return;
        }

        AtomicLong fileCount = new AtomicLong();
        AtomicLong totalBytes = new AtomicLong();
        try (Stream<Path> paths = Files.walk(uploadRoot)) {
            paths.filter(Files::isRegularFile).forEach(path -> {
                fileCount.incrementAndGet();
                try {
                    totalBytes.addAndGet(Files.size(path));
                } catch (Exception e) {
                    log.warn("统计简历上传文件大小失败, path: {}", path, e);
                }
            });
            log.info("简历上传目录统计完成, path: {}, fileCount: {}, totalBytes: {}",
                    uploadRoot, fileCount.get(), totalBytes.get());
        } catch (Exception e) {
            log.warn("统计简历上传目录失败, path: {}", uploadRoot, e);
        }
    }

    private Path resolveUploadRoot() {
        String uploadDir = configuredUploadDir;
        if (uploadDir == null || uploadDir.isBlank()) {
            uploadDir = System.getProperty("user.dir") + "/uploads/resumes/";
        }
        return Paths.get(uploadDir).toAbsolutePath().normalize();
    }
}
