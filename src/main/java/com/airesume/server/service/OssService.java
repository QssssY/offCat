package com.airesume.server.service;

import org.springframework.web.multipart.MultipartFile;

/**
 * 阿里云 OSS 存储服务接口
 * 提供社区图片上传与签名 URL 生成能力
 */
public interface OssService {

    /**
     * 将图片上传到 OSS
     *
     * @param file      图片文件
     * @param userId    上传者用户 ID
     * @param extension 文件扩展名（不含点号），例如 "jpg"、"png"
     * @return OSS 对象键，例如 "community/123/20260604/uuid.jpg"
     */
    String upload(MultipartFile file, Long userId, String extension);

    /**
     * 生成 OSS 对象的签名访问 URL
     *
     * @param objectKey OSS 对象键
     * @return 带签名和过期时间的完整 URL
     */
    String generateSignedUrl(String objectKey);

    /**
     * 判断 OSS 是否已正确配置并可用
     *
     * @return true 表示可用，false 表示未配置
     */
    boolean isEnabled();
}
