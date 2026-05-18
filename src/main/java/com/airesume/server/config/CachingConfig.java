package com.airesume.server.config;

import org.springframework.cache.annotation.CachingConfigurer;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.cache.interceptor.CacheErrorHandler;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * 通用缓存配置，与 Redis 解耦。
 * <p>
 * 只负责启用 @EnableCaching 和注册全局 CacheErrorHandler，
 * 不依赖 RedisConnectionFactory。Redis 不可用时，
 * @Cacheable 走 RedisCacheErrorHandler 降级回源数据库，业务不受影响。
 */
@Configuration
@EnableCaching
public class CachingConfig implements CachingConfigurer {

    @Bean
    public CacheErrorHandler cacheErrorHandler() {
        return new RedisCacheErrorHandler();
    }

    @Override
    public CacheErrorHandler errorHandler() {
        return cacheErrorHandler();
    }
}
