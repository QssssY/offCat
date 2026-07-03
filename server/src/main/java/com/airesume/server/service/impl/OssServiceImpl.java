package com.airesume.server.service.impl;

import com.aliyun.oss.OSS;
import com.aliyun.oss.OSSClientBuilder;
import com.aliyun.oss.model.ObjectMetadata;
import com.airesume.server.common.exception.BusinessException;
import com.airesume.server.config.OssConfig;
import com.airesume.server.service.OssService;
import jakarta.annotation.PreDestroy;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.lang.reflect.Method;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * 阿里云 OSS 存储服务实现
 * 负责社区图片上传、签名 URL 生成等操作
 * OSS 未配置时 isEnabled() 返回 false，调用方优雅降级
 */
@Slf4j
@Service
public class OssServiceImpl implements OssService {

    /** 扩展名 → Content-Type 映射 */
    private static final Map<String, String> CONTENT_TYPE_MAP = Map.of(
            "jpg", "image/jpeg",
            "jpeg", "image/jpeg",
            "png", "image/png",
            "gif", "image/gif",
            "webp", "image/webp"
    );

    private static final DateTimeFormatter DATE_FORMAT = DateTimeFormatter.ofPattern("yyyyMMdd");

    private final OssConfig ossConfig;

    /** 懒初始化的 OSS 客户端 */
    private volatile OSS ossClient;

    public OssServiceImpl(OssConfig ossConfig) {
        this.ossConfig = ossConfig;
    }

    @Override
    public String upload(MultipartFile file, Long userId, String extension) {
        ensureClient();

        // 构造对象键：community/{userId}/{yyyyMMdd}/{uuid}.{ext}
        String datePart = LocalDate.now().format(DATE_FORMAT);
        String uuid = UUID.randomUUID().toString().replace("-", "");
        String objectKey = String.format("community/%d/%s/%s.%s", userId, datePart, uuid, extension);

        // 设置 Content-Type，确保 OSS 返回正确的 MIME 类型
        ObjectMetadata metadata = new ObjectMetadata();
        String contentType = CONTENT_TYPE_MAP.getOrDefault(extension.toLowerCase(), "application/octet-stream");
        metadata.setContentType(contentType);
        metadata.setContentLength(file.getSize());

        try (InputStream inputStream = file.getInputStream()) {
            ossClient.putObject(ossConfig.getBucketName(), objectKey, inputStream, metadata);
            log.info("OSS上传成功, bucket: {}, objectKey: {}, size: {}",
                    ossConfig.getBucketName(), objectKey, file.getSize());
            return objectKey;
        } catch (IOException e) {
            log.error("OSS上传失败(读取文件流异常), userId: {}, objectKey: {}", userId, objectKey, e);
            throw new BusinessException("图片上传失败，请稍后重试");
        } catch (Exception e) {
            log.error("OSS上传失败, userId: {}, objectKey: {}", userId, objectKey, e);
            throw new BusinessException("图片上传失败，请稍后重试");
        }
    }

    @Override
    public String generateSignedUrl(String objectKey) {
        ensureClient();

        Date expiration = new Date(System.currentTimeMillis()
                + (long) ossConfig.getSignedUrlExpirationMinutes() * 60 * 1000);

        try {
            URL url = ossClient.generatePresignedUrl(ossConfig.getBucketName(), objectKey, expiration);
            return url.toString();
        } catch (Exception e) {
            log.error("OSS签名URL生成失败, objectKey: {}, bucket: {}, accessKeyId: {}, errorCode: {}, errorType: {}",
                    objectKey, ossConfig.getBucketName(), maskAccessKeyId(),
                    resolveOssErrorCode(e), e.getClass().getSimpleName());
            throw new BusinessException("图片获取失败");
        }
    }

    @Override
    public void deleteObject(String objectKey) {
        ensureClient();
        try {
            ossClient.deleteObject(ossConfig.getBucketName(), objectKey);
            log.info("OSS对象删除成功, bucket: {}, objectKey: {}", ossConfig.getBucketName(), objectKey);
        } catch (Exception e) {
            log.error("OSS对象删除失败, objectKey: {}", objectKey, e);
            throw new BusinessException("图片清理失败");
        }
    }

    @Override
    public boolean isEnabled() {
        return ossConfig != null && ossConfig.hasCompleteEnabledConfig();
    }

    /**
     * 懒初始化 OSS 客户端，双重检查锁定保证线程安全
     */
    private void ensureClient() {
        if (ossClient == null) {
            synchronized (this) {
                if (ossClient == null) {
                    validateEnabledConfig();
                    log.info("初始化OSS客户端, endpoint: {}, bucket: {}, accessKeyId: {}",
                            ossConfig.getEndpoint(), ossConfig.getBucketName(), maskAccessKeyId());
                    try {
                        ossClient = buildClient();
                    } catch (Exception e) {
                        log.error("OSS客户端初始化失败, endpoint: {}, bucket: {}, accessKeyId: {}, errorType: {}",
                                ossConfig.getEndpoint(), ossConfig.getBucketName(),
                                maskAccessKeyId(), e.getClass().getSimpleName());
                        throw new BusinessException("OSS 客户端初始化失败，请检查 OSS 凭据和 Bucket 配置");
                    }
                }
            }
        }
    }

    /**
     * 创建 OSS 客户端。
     * 独立成受保护方法，便于单元测试模拟 SDK 初始化失败，同时生产路径仍使用官方 SDK 类型。
     */
    protected OSS buildClient() {
        return new OSSClientBuilder().build(
                ossConfig.getEndpoint(),
                ossConfig.getAccessKeyId(),
                ossConfig.getAccessKeySecret());
    }

    private void validateEnabledConfig() {
        if (ossConfig == null || !ossConfig.isEnabled()) {
            throw new BusinessException("OSS 未启用，无法执行操作");
        }
        List<String> missingFields = ossConfig.missingRequiredFieldsWhenEnabled();
        if (!missingFields.isEmpty()) {
            throw new BusinessException("OSS 配置缺失: " + String.join(", ", missingFields));
        }
    }

    private String maskAccessKeyId() {
        String accessKeyId = ossConfig == null ? null : ossConfig.getAccessKeyId();
        if (accessKeyId == null || accessKeyId.isBlank()) {
            return "***";
        }
        String trimmed = accessKeyId.trim();
        if (trimmed.length() <= 8) {
            return trimmed.charAt(0) + "****" + trimmed.charAt(trimmed.length() - 1);
        }
        return trimmed.substring(0, 4) + "****" + trimmed.substring(trimmed.length() - 4);
    }

    private String resolveOssErrorCode(Exception exception) {
        try {
            // 阿里云 OSSException 暴露 getErrorCode，这里用反射避免把日志逻辑绑死到具体异常类。
            Method method = exception.getClass().getMethod("getErrorCode");
            Object value = method.invoke(exception);
            return value == null ? "UNKNOWN" : value.toString();
        } catch (Exception ignored) {
            return "UNKNOWN";
        }
    }

    /**
     * 应用关闭时释放 OSS 客户端连接池资源
     */
    @PreDestroy
    public void destroy() {
        if (ossClient != null) {
            ossClient.shutdown();
            log.info("OSS客户端已关闭");
        }
    }
}
