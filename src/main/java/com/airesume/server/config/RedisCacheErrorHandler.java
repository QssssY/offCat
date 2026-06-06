package com.airesume.server.config;

import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.Cache;
import org.springframework.cache.interceptor.CacheErrorHandler;
import org.springframework.data.redis.serializer.SerializationException;

import java.time.Duration;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Redis 缓存异常兜底处理器。
 * <p>
 * 缓存属于性能优化手段，不应该成为业务请求的单点故障。
 * 当 Redis 中存在旧序列化格式、脏数据或临时访问异常时，
 * 这里统一降级为“记录日志 + 回源数据库”，避免接口直接返回 500。
 */
@Slf4j
public class RedisCacheErrorHandler implements CacheErrorHandler {

    private static final Duration WARNING_THROTTLE_WINDOW = Duration.ofSeconds(30);

    private static final int MAX_TRACKED_WARNINGS = 4096;

    private final Map<String, Long> lastWarningTimes = new ConcurrentHashMap<>();

    private final long warningThrottleMillis = WARNING_THROTTLE_WINDOW.toMillis();

    @Override
    public void handleCacheGetError(RuntimeException exception, Cache cache, Object key) {
        if (exception instanceof SerializationException) {
            warnIfAllowed("读取缓存失败，检测到序列化不兼容，已降级回源。", exception, cache, key, "database");
            safeEvict(cache, key);
            return;
        }

        warnIfAllowed("读取缓存失败，已降级回源。", exception, cache, key, "database");
    }

    @Override
    public void handleCachePutError(RuntimeException exception, Cache cache, Object key, Object value) {
        warnIfAllowed("写入缓存失败，已忽略本次缓存写入。", exception, cache, key, "ignore");
    }

    @Override
    public void handleCacheEvictError(RuntimeException exception, Cache cache, Object key) {
        warnIfAllowed("删除缓存失败，已忽略本次缓存删除。", exception, cache, key, "ignore");
    }

    @Override
    public void handleCacheClearError(RuntimeException exception, Cache cache) {
        warnIfAllowed("清空缓存失败，已忽略本次缓存清空。", exception, cache, "<all>", "ignore");
    }

    /**
     * 尝试删除当前坏缓存，避免下一次请求继续命中同一份脏数据。
     */
    private void safeEvict(Cache cache, Object key) {
        try {
            cache.evict(key);
        } catch (RuntimeException evictException) {
            warnIfAllowed("删除坏缓存失败，等待后续 TTL 自然过期。", evictException, cache, key, "ttl");
        }
    }

    /**
     * 对同一缓存、同一 key、同一根因类型做短窗口限频，避免 Redis 故障时 warn 日志刷屏。
     */
    private void warnIfAllowed(String message,
                               RuntimeException exception,
                               Cache cache,
                               Object key,
                               String fallback) {
        String exceptionType = rootCauseType(exception);
        if (!shouldLog(cache.getName(), key, exceptionType)) {
            return;
        }

        log.warn("{}cache={}, key={}, exceptionType={}, fallback={}",
                message, cache.getName(), key, exceptionType, fallback, exception);
    }

    private boolean shouldLog(String cacheName, Object key, String exceptionType) {
        if (lastWarningTimes.size() > MAX_TRACKED_WARNINGS) {
            lastWarningTimes.clear();
        }

        String warningKey = cacheName + "::" + key + "::" + exceptionType;
        long now = System.currentTimeMillis();
        AtomicBoolean allowed = new AtomicBoolean(false);
        lastWarningTimes.compute(warningKey, (ignored, lastWarningTime) -> {
            if (lastWarningTime == null || now - lastWarningTime >= warningThrottleMillis) {
                allowed.set(true);
                return now;
            }
            return lastWarningTime;
        });
        return allowed.get();
    }

    private String rootCauseType(RuntimeException exception) {
        Throwable current = exception;
        while (current.getCause() != null && current.getCause() != current) {
            current = current.getCause();
        }
        return current.getClass().getSimpleName();
    }
}
