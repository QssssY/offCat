package com.airesume.server.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.http.CacheControl;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import java.nio.file.Paths;
import java.util.concurrent.TimeUnit;

@Configuration
public class WebMvcConfig implements WebMvcConfigurer {

    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        // 只暴露社区图片目录，简历等敏感上传文件继续通过鉴权接口下载。
        String uploadPath = Paths.get(System.getProperty("user.dir"), "uploads", "community").toUri().toString();
        if (!uploadPath.endsWith("/")) {
            uploadPath = uploadPath + "/";
        }
        registry.addResourceHandler("/uploads/community/**")
                .addResourceLocations(uploadPath)
                // 社区图片是公开静态资源，设置 1 天浏览器缓存，降低重复图片访问对后端的压力。
                .setCacheControl(communityUploadCacheControl());
    }

    CacheControl communityUploadCacheControl() {
        return CacheControl.maxAge(1, TimeUnit.DAYS).cachePublic();
    }
}
