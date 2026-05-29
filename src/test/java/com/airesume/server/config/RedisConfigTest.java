package com.airesume.server.config;

import org.junit.jupiter.api.Test;
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;
import org.springframework.cache.transaction.TransactionAwareCacheDecorator;
import org.springframework.data.redis.cache.RedisCacheConfiguration;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.serializer.RedisSerializer;

import java.time.Duration;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.Mockito.mock;

class RedisConfigTest {

    @Test
    void shouldNotWrapCachesWithTransactionAwareDecorator() {
        RedisConfig redisConfig = new RedisConfig();
        RedisConnectionFactory connectionFactory = mock(RedisConnectionFactory.class);
        RedisSerializer<String> keySerializer = redisConfig.redisKeySerializer();
        RedisSerializer<Object> valueSerializer = redisConfig.redisValueSerializer();

        CacheManager cacheManager = redisConfig.cacheManager(connectionFactory, keySerializer, valueSerializer);
        Cache cache = cacheManager.getCache("interview:jobTarget");

        // 缓存写入必须由 CacheErrorHandler 兜底，不能延迟到事务 afterCommit 后绕过异常处理。
        assertNotNull(cache);
        assertFalse(cache instanceof TransactionAwareCacheDecorator);
    }

    @Test
    void shouldConfigureDashboardTrendsCacheTtlForTenMinutes() {
        RedisConfig redisConfig = new RedisConfig();
        RedisCacheConfiguration defaultConfig = RedisCacheConfiguration.defaultCacheConfig()
                .entryTtl(Duration.ofMinutes(5));
        Map<String, RedisCacheConfiguration> cacheConfigurations = redisConfig.initialCacheConfigurations(defaultConfig);

        // 看板趋势查询显式注册缓存区 TTL，避免依赖默认策略时被后续调整误伤。
        assertEquals(Duration.ofMinutes(5), cacheConfigurations.get("admin:dashboardTrends").getTtl());
    }
    @Test
    void shouldConfigureUserStatsCacheTtlForFiveMinutes() {
        RedisConfig redisConfig = new RedisConfig();
        RedisCacheConfiguration defaultConfig = RedisCacheConfiguration.defaultCacheConfig()
                .entryTtl(Duration.ofMinutes(5));
        Map<String, RedisCacheConfiguration> cacheConfigurations = redisConfig.initialCacheConfigurations(defaultConfig);

        assertEquals(Duration.ofMinutes(5), cacheConfigurations.get("admin:userStats").getTtl());
    }
}
