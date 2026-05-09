package com.airesume.server.config;

import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.Cache;
import org.springframework.cache.interceptor.CacheErrorHandler;
import org.springframework.data.redis.serializer.SerializationException;

/**
 * Redis 缓存异常兜底处理器。
 * <p>
 * 缓存属于性能优化手段，不应该成为业务请求的单点故障。
 * 当 Redis 中存在旧序列化格式、脏数据或临时访问异常时，
 * 这里统一降级为“记录日志 + 回源数据库”，避免接口直接返回 500。
 */
@Slf4j
public class RedisCacheErrorHandler implements CacheErrorHandler {

    @Override
    public void handleCacheGetError(RuntimeException exception, Cache cache, Object key) {
        if (exception instanceof SerializationException) {
            log.warn("读取缓存失败，检测到序列化不兼容，已降级回源。cache={}, key={}", cache.getName(), key, exception);
            safeEvict(cache, key);
            return;
        }

        log.warn("读取缓存失败，已降级回源。cache={}, key={}", cache.getName(), key, exception);
    }

    @Override
    public void handleCachePutError(RuntimeException exception, Cache cache, Object key, Object value) {
        log.warn("写入缓存失败，已忽略本次缓存写入。cache={}, key={}", cache.getName(), key, exception);
    }

    @Override
    public void handleCacheEvictError(RuntimeException exception, Cache cache, Object key) {
        log.warn("删除缓存失败，已忽略本次缓存删除。cache={}, key={}", cache.getName(), key, exception);
    }

    @Override
    public void handleCacheClearError(RuntimeException exception, Cache cache) {
        log.warn("清空缓存失败，已忽略本次缓存清空。cache={}", cache.getName(), exception);
    }

    /**
     * 尝试删除当前坏缓存，避免下一次请求继续命中同一份脏数据。
     */
    private void safeEvict(Cache cache, Object key) {
        try {
            cache.evict(key);
        } catch (RuntimeException evictException) {
            log.warn("删除坏缓存失败，等待后续 TTL 自然过期。cache={}, key={}", cache.getName(), key, evictException);
        }
    }
}
