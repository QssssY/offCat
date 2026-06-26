package com.airesume.server.config;

import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;
import org.junit.jupiter.api.Test;
import org.slf4j.LoggerFactory;
import org.springframework.cache.Cache;
import org.springframework.data.redis.RedisConnectionFailureException;
import org.springframework.data.redis.serializer.SerializationException;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class RedisCacheErrorHandlerTest {

    @Test
    void shouldLogCacheAndRootCauseWithoutThrowing() {
        Logger logger = (Logger) LoggerFactory.getLogger(RedisCacheErrorHandler.class);
        ListAppender<ILoggingEvent> appender = new ListAppender<>();
        appender.start();
        logger.addAppender(appender);

        try {
            RedisCacheErrorHandler handler = new RedisCacheErrorHandler();
            Cache cache = cache("sys_user");

            assertDoesNotThrow(() -> handler.handleCacheGetError(
                    new RedisConnectionFailureException("redis down"), cache, 100L));
            assertDoesNotThrow(() -> handler.handleCachePutError(
                    new RedisConnectionFailureException("redis down"), cache, 101L, "value"));

            List<String> messages = formattedMessages(appender);
            assertEquals(2, messages.size());
            assertTrue(messages.get(0).contains("cache=sys_user"));
            assertTrue(messages.get(0).contains("key=100"));
            assertTrue(messages.get(0).contains("exceptionType=RedisConnectionFailureException"));
            assertTrue(messages.get(0).contains("fallback=database"));
            assertTrue(messages.get(1).contains("cache=sys_user"));
            assertTrue(messages.get(1).contains("key=101"));
            assertTrue(messages.get(1).contains("exceptionType=RedisConnectionFailureException"));
            assertTrue(messages.get(1).contains("fallback=ignore"));
        } finally {
            logger.detachAppender(appender);
        }
    }

    @Test
    void shouldThrottleRepeatedWarningsForSameCacheKey() {
        Logger logger = (Logger) LoggerFactory.getLogger(RedisCacheErrorHandler.class);
        ListAppender<ILoggingEvent> appender = new ListAppender<>();
        appender.start();
        logger.addAppender(appender);

        try {
            RedisCacheErrorHandler handler = new RedisCacheErrorHandler();
            Cache cache = cache("sys_user");
            RuntimeException exception = new RedisConnectionFailureException("redis down");

            handler.handleCacheGetError(exception, cache, 100L);
            handler.handleCacheGetError(exception, cache, 100L);
            handler.handleCacheGetError(exception, cache, 100L);

            assertEquals(1, formattedMessages(appender).size());
        } finally {
            logger.detachAppender(appender);
        }
    }

    @Test
    void shouldEvictDirtyCacheWhenSerializationFails() {
        Logger logger = (Logger) LoggerFactory.getLogger(RedisCacheErrorHandler.class);
        ListAppender<ILoggingEvent> appender = new ListAppender<>();
        appender.start();
        logger.addAppender(appender);

        try {
            RedisCacheErrorHandler handler = new RedisCacheErrorHandler();
            Cache cache = cache("auth:userInfo");

            handler.handleCacheGetError(new SerializationException("old payload"), cache, 200L);

            verify(cache).evict(200L);
            List<String> messages = formattedMessages(appender);
            assertEquals(1, messages.size());
            assertTrue(messages.get(0).contains("exceptionType=SerializationException"));
            assertTrue(messages.get(0).contains("fallback=database"));
        } finally {
            logger.detachAppender(appender);
        }
    }

    private Cache cache(String cacheName) {
        Cache cache = mock(Cache.class);
        when(cache.getName()).thenReturn(cacheName);
        return cache;
    }

    private List<String> formattedMessages(ListAppender<ILoggingEvent> appender) {
        return appender.list.stream()
                .map(ILoggingEvent::getFormattedMessage)
                .toList();
    }
}
