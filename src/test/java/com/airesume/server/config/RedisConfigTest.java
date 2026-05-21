package com.airesume.server.config;

import org.junit.jupiter.api.Test;
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;
import org.springframework.cache.transaction.TransactionAwareCacheDecorator;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.serializer.RedisSerializer;

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
}
