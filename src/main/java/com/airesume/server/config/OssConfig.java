package com.airesume.server.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

/**
 * 阿里云 OSS 配置
 * 通过 application.yml 中的 app.oss 前缀读取配置
 * 用于社区图片上传与签名访问
 */
@Component
@ConfigurationProperties(prefix = "app.oss")
public class OssConfig {

    /** OSS Endpoint，例如 oss-cn-hangzhou.aliyuncs.com */
    private String endpoint;

    /** 阿里云 AccessKey ID */
    private String accessKeyId;

    /** 阿里云 AccessKey Secret */
    private String accessKeySecret;

    /** OSS Bucket 名称 */
    private String bucketName;

    /** 签名 URL 有效时长（分钟），默认 60 分钟 */
    private int signedUrlExpirationMinutes = 60;

    /** 是否启用 OSS，默认关闭。未启用时社区图片上传返回占位图 */
    private boolean enabled = false;

    public String getEndpoint() {
        return endpoint;
    }

    public void setEndpoint(String endpoint) {
        this.endpoint = endpoint;
    }

    public String getAccessKeyId() {
        return accessKeyId;
    }

    public void setAccessKeyId(String accessKeyId) {
        this.accessKeyId = accessKeyId;
    }

    public String getAccessKeySecret() {
        return accessKeySecret;
    }

    public void setAccessKeySecret(String accessKeySecret) {
        this.accessKeySecret = accessKeySecret;
    }

    public String getBucketName() {
        return bucketName;
    }

    public void setBucketName(String bucketName) {
        this.bucketName = bucketName;
    }

    public int getSignedUrlExpirationMinutes() {
        return signedUrlExpirationMinutes;
    }

    public void setSignedUrlExpirationMinutes(int signedUrlExpirationMinutes) {
        this.signedUrlExpirationMinutes = signedUrlExpirationMinutes;
    }

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    /**
     * 返回启用 OSS 时缺失的必填配置项。
     * 仅列出字段名，不包含任何配置值，避免校验错误被记录时泄露凭据。
     */
    public List<String> missingRequiredFieldsWhenEnabled() {
        List<String> missingFields = new ArrayList<>();
        if (isBlank(endpoint)) {
            missingFields.add("endpoint");
        }
        if (isBlank(bucketName)) {
            missingFields.add("bucketName");
        }
        if (isBlank(accessKeyId)) {
            missingFields.add("accessKeyId");
        }
        if (isBlank(accessKeySecret)) {
            missingFields.add("accessKeySecret");
        }
        return missingFields;
    }

    /**
     * 判断 OSS 是否处于启用且必填配置完整状态。
     */
    public boolean hasCompleteEnabledConfig() {
        return enabled && missingRequiredFieldsWhenEnabled().isEmpty();
    }

    private boolean isBlank(String value) {
        return value == null || value.isBlank();
    }
}
