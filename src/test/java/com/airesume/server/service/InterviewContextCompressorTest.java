package com.airesume.server.service;

import com.airesume.server.config.AiTokenLimitConfig;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneId;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;

class InterviewContextCompressorTest {

    @Test
    void shouldKeepFreshSummaryCacheWhenCleanupRuns() {
        MutableClock clock = new MutableClock(Instant.parse("2026-05-15T00:00:00Z"));
        InterviewContextCompressor compressor = buildCompressor(clock);

        putCachedSummary(compressor, "session-fresh", clock.instant());

        int cleaned = compressor.cleanupExpiredSummaryCache();

        assertEquals(0, cleaned);
        assertEquals(1, summaryCache(compressor).size());
    }

    @Test
    void shouldRemoveSummaryCacheOlderThanTwoHours() {
        MutableClock clock = new MutableClock(Instant.parse("2026-05-15T03:00:00Z"));
        InterviewContextCompressor compressor = buildCompressor(clock);

        putCachedSummary(compressor, "session-expired", clock.instant().minus(Duration.ofHours(2)).minusSeconds(1));
        putCachedSummary(compressor, "session-fresh", clock.instant().minus(Duration.ofMinutes(30)));

        int cleaned = compressor.cleanupExpiredSummaryCache();

        assertEquals(1, cleaned);
        assertEquals(1, summaryCache(compressor).size());
        org.junit.jupiter.api.Assertions.assertTrue(summaryCache(compressor).containsKey("session-fresh"));
    }

    @Test
    void shouldEvictSpecificSessionCacheImmediately() {
        MutableClock clock = new MutableClock(Instant.parse("2026-05-15T00:00:00Z"));
        InterviewContextCompressor compressor = buildCompressor(clock);
        putCachedSummary(compressor, "session-a", clock.instant());
        putCachedSummary(compressor, "session-b", clock.instant());

        compressor.evictCache("session-a");

        assertEquals(1, summaryCache(compressor).size());
        org.junit.jupiter.api.Assertions.assertTrue(summaryCache(compressor).containsKey("session-b"));
    }

    private InterviewContextCompressor buildCompressor(Clock clock) {
        return new InterviewContextCompressor(new AiTokenLimitConfig(), mock(AiChatClient.class), clock);
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> summaryCache(InterviewContextCompressor compressor) {
        return (Map<String, Object>) ReflectionTestUtils.getField(compressor, "summaryCache");
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    private void putCachedSummary(InterviewContextCompressor compressor, String sessionId, Instant updatedAt) {
        Map cache = summaryCache(compressor);
        Class<?> cachedSummaryClass = List.of(InterviewContextCompressor.class.getDeclaredClasses()).stream()
                .filter(type -> "CachedSummary".equals(type.getSimpleName()))
                .findFirst()
                .orElseThrow();
        try {
            var constructor = cachedSummaryClass.getDeclaredConstructor(String.class, int.class, Instant.class);
            constructor.setAccessible(true);
            cache.put(sessionId, constructor.newInstance("摘要", 3, updatedAt));
        } catch (ReflectiveOperationException e) {
            throw new AssertionError(e);
        }
    }

    private static final class MutableClock extends Clock {
        private final Instant current;

        private MutableClock(Instant current) {
            this.current = current;
        }

        @Override
        public ZoneId getZone() {
            return ZoneId.of("UTC");
        }

        @Override
        public Clock withZone(ZoneId zone) {
            return this;
        }

        @Override
        public Instant instant() {
            return current;
        }
    }
}
