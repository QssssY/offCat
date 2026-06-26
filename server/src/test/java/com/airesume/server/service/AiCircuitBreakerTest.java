package com.airesume.server.service;

import com.airesume.server.config.AiCircuitBreakerConfig;
import org.junit.jupiter.api.Test;
import reactor.core.publisher.Flux;

import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class AiCircuitBreakerTest {

    @Test
    void shouldOpenAfterConfiguredFailures() {
        AiCircuitBreaker breaker = new AiCircuitBreaker(buildConfig(2, 60_000L));

        assertThrows(RuntimeException.class,
                () -> breaker.execute("resume-ai", () -> {
                    throw new RuntimeException("boom-1");
                }));
        assertThrows(RuntimeException.class,
                () -> breaker.execute("resume-ai", () -> {
                    throw new RuntimeException("boom-2");
                }));

        assertThrows(AiCircuitBreaker.AiCircuitBreakerOpenException.class,
                () -> breaker.execute("resume-ai", () -> "should-not-run"));
    }

    @Test
    void shouldRecoverAfterCooldownAndSuccessfulProbe() throws InterruptedException {
        AiCircuitBreaker breaker = new AiCircuitBreaker(buildConfig(1, 30L));

        assertThrows(RuntimeException.class,
                () -> breaker.execute("interview-ai", () -> {
                    throw new RuntimeException("boom");
                }));

        Thread.sleep(50L);
        assertEquals("ok", breaker.execute("interview-ai", () -> "ok"));
        assertEquals("ok-2", breaker.execute("interview-ai", () -> "ok-2"));
    }

    @Test
    void executeFluxShouldOpenAfterEarlyStreamFailures() {
        AiCircuitBreaker breaker = new AiCircuitBreaker(buildConfig(1, 60_000L));
        AtomicInteger invocations = new AtomicInteger();

        assertThrows(RuntimeException.class, () -> breaker.executeFlux(
                "interview-stream",
                () -> {
                    invocations.incrementAndGet();
                    return Flux.error(new RuntimeException("stream-down"));
                }).blockLast());

        assertThrows(AiCircuitBreaker.AiCircuitBreakerOpenException.class, () -> breaker.executeFlux(
                "interview-stream",
                () -> {
                    invocations.incrementAndGet();
                    return Flux.just("should-not-run");
                }).blockLast());

        assertEquals(1, invocations.get());
    }

    private AiCircuitBreakerConfig buildConfig(int failureThreshold, long openDurationMs) {
        AiCircuitBreakerConfig config = new AiCircuitBreakerConfig();
        config.setEnabled(true);
        config.setFailureThreshold(failureThreshold);
        config.setOpenDurationMs(openDurationMs);
        return config;
    }
}
