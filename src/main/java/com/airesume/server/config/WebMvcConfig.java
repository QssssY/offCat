package com.airesume.server.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * Web MVC 配置
 * 社区图片已迁移至阿里云 OSS，通过签名 URL 代理端点（CommunityController）访问，
 * 不再需要本地静态资源映射。
 */
@Configuration
public class WebMvcConfig implements WebMvcConfigurer {

}
