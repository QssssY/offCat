package com.airesume.server.service;

import com.airesume.server.common.constants.AiEngineConstants;
import com.airesume.server.common.constants.UserAiConstants;
import com.airesume.server.config.AiTokenLimitConfig;
import com.airesume.server.dto.ai.ResolvedAiConfig;
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
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

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

    @Test
    void shouldPassUserAiContextWhenCompressingEvaluationHistory() {
        Long userId = 123L;
        AiTokenLimitConfig config = new AiTokenLimitConfig();
        config.setEvaluationRecentMessagesToKeep(2);
        AiChatClient aiChatClient = mock(AiChatClient.class);
        InterviewContextCompressor compressor = new InterviewContextCompressor(
                config, aiChatClient, new MutableClock(Instant.parse("2026-05-15T00:00:00Z")));
        when(aiChatClient.chat(anyString(), anyString(), eq(config.getAiSummaryTimeoutMs()), eq(userId), eq(false)))
                .thenReturn("候选人围绕订单系统、库存扣减和幂等设计进行了回答。");

        compressor.compressForEvaluation(List.of(
                new InterviewAiService.ChatMessageItem("assistant", "请介绍项目。"),
                new InterviewAiService.ChatMessageItem("user", "我负责订单链路。"),
                new InterviewAiService.ChatMessageItem("assistant", "如何处理库存？"),
                new InterviewAiService.ChatMessageItem("user", "使用 Redis Lua。"),
                new InterviewAiService.ChatMessageItem("assistant", "如何做幂等？"),
                new InterviewAiService.ChatMessageItem("user", "用幂等表。"),
                new InterviewAiService.ChatMessageItem("assistant", "如何恢复异常？")
        ), null, userId, false);

        verify(aiChatClient).chat(anyString(), anyString(), eq(config.getAiSummaryTimeoutMs()), eq(userId), eq(false));
    }

    @Test
    void shouldCountCustomAiUsageWhenEvaluationSummaryUsesUserConfig() {
        Long userId = 123L;
        AiTokenLimitConfig config = new AiTokenLimitConfig();
        config.setEvaluationRecentMessagesToKeep(2);
        AiChatClient aiChatClient = mock(AiChatClient.class);
        UserAiConfigResolver userAiConfigResolver = mock(UserAiConfigResolver.class);
        UserAiUsageLimitService userAiUsageLimitService = mock(UserAiUsageLimitService.class);
        InterviewContextCompressor compressor = new InterviewContextCompressor(
                config,
                aiChatClient,
                new MutableClock(Instant.parse("2026-05-15T00:00:00Z")),
                userAiConfigResolver,
                userAiUsageLimitService);
        when(userAiConfigResolver.resolve(userId, AiEngineConstants.BUSINESS_TYPE_INTERVIEW, false))
                .thenReturn(ResolvedAiConfig.builder().configType("interview").build());
        when(aiChatClient.chat(anyString(), anyString(), eq(config.getAiSummaryTimeoutMs()), eq(userId), eq(false)))
                .thenReturn("候选人围绕订单系统、库存扣减和幂等设计进行了回答。");

        compressor.compressForEvaluation(buildLongHistory(), null, userId, false);

        verify(userAiUsageLimitService).checkAndIncrement(userId, UserAiConstants.USAGE_TYPE_INTERVIEW_SUMMARY);
        verify(userAiUsageLimitService, never()).rollback(userId, UserAiConstants.USAGE_TYPE_INTERVIEW_SUMMARY);
    }

    @Test
    void shouldRollbackCustomAiUsageWhenEvaluationSummaryFails() {
        Long userId = 123L;
        AiTokenLimitConfig config = new AiTokenLimitConfig();
        config.setEvaluationRecentMessagesToKeep(2);
        AiChatClient aiChatClient = mock(AiChatClient.class);
        UserAiConfigResolver userAiConfigResolver = mock(UserAiConfigResolver.class);
        UserAiUsageLimitService userAiUsageLimitService = mock(UserAiUsageLimitService.class);
        InterviewContextCompressor compressor = new InterviewContextCompressor(
                config,
                aiChatClient,
                new MutableClock(Instant.parse("2026-05-15T00:00:00Z")),
                userAiConfigResolver,
                userAiUsageLimitService);
        when(userAiConfigResolver.resolve(userId, AiEngineConstants.BUSINESS_TYPE_INTERVIEW, false))
                .thenReturn(ResolvedAiConfig.builder().configType("interview").build());
        when(aiChatClient.chat(anyString(), anyString(), eq(config.getAiSummaryTimeoutMs()), eq(userId), eq(false)))
                .thenThrow(new RuntimeException("AI upstream failed"));

        compressor.compressForEvaluation(buildLongHistory(), null, userId, false);

        verify(userAiUsageLimitService).checkAndIncrement(userId, UserAiConstants.USAGE_TYPE_INTERVIEW_SUMMARY);
        verify(userAiUsageLimitService).rollback(userId, UserAiConstants.USAGE_TYPE_INTERVIEW_SUMMARY);
    }

    private InterviewContextCompressor buildCompressor(Clock clock) {
        return new InterviewContextCompressor(new AiTokenLimitConfig(), mock(AiChatClient.class), clock);
    }

    private List<InterviewAiService.ChatMessageItem> buildLongHistory() {
        return List.of(
                new InterviewAiService.ChatMessageItem("assistant", "请介绍项目。"),
                new InterviewAiService.ChatMessageItem("user", "我负责订单链路。"),
                new InterviewAiService.ChatMessageItem("assistant", "如何处理库存？"),
                new InterviewAiService.ChatMessageItem("user", "使用 Redis Lua。"),
                new InterviewAiService.ChatMessageItem("assistant", "如何做幂等？"),
                new InterviewAiService.ChatMessageItem("user", "用幂等表。"),
                new InterviewAiService.ChatMessageItem("assistant", "如何恢复异常？")
        );
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
