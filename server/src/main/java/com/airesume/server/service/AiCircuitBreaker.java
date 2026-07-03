package com.airesume.server.service;

import com.airesume.server.config.AiCircuitBreakerConfig;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Flux;

import java.time.Duration;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Supplier;

/**
 * 轻量级 AI 熔断器。
 * 目标是对外部 AI 请求做最小可行的进程内保护，避免上游连续故障时重复耗尽超时。
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class AiCircuitBreaker {

    private final AiCircuitBreakerConfig config;
    private final ConcurrentHashMap<String, CircuitState> stateMap = new ConcurrentHashMap<>();

    /**
     * 包装同步 AI 调用。
     */
    public <T> T execute(String breakerName, Supplier<T> action) {
        if (!config.isEnabled()) {
            return action.get();
        }

        CircuitAttempt attempt = beginAttempt(breakerName);
        try {
            T result = action.get();
            recordSuccess(attempt);
            return result;
        } catch (RuntimeException ex) {
            recordFailure(attempt, ex);
            throw ex;
        } catch (Error ex) {
            recordFailure(attempt, ex);
            throw ex;
        }
    }

    /**
     * 包装流式 AI 调用。
     * 仅当上游在首包前就失败时计入失败；一旦已经拿到有效输出，则视为本次调用已经通过可用性探测。
     */
    public <T> Flux<T> executeFlux(String breakerName, Supplier<Flux<T>> action) {
        if (!config.isEnabled()) {
            return Flux.defer(action);
        }

        return Flux.defer(() -> {
            CircuitAttempt attempt = beginAttempt(breakerName);
            AtomicBoolean outcomeRecorded = new AtomicBoolean(false);
            try {
                return action.get()
                        .doOnNext(item -> {
                            if (outcomeRecorded.compareAndSet(false, true)) {
                                recordSuccess(attempt);
                            }
                        })
                        .doOnComplete(() -> {
                            if (outcomeRecorded.compareAndSet(false, true)) {
                                recordSuccess(attempt);
                            }
                        })
                        .doOnError(ex -> {
                            if (outcomeRecorded.compareAndSet(false, true)) {
                                recordFailure(attempt, ex);
                            }
                        });
            } catch (RuntimeException ex) {
                if (outcomeRecorded.compareAndSet(false, true)) {
                    recordFailure(attempt, ex);
                }
                return Flux.error(ex);
            } catch (Error ex) {
                if (outcomeRecorded.compareAndSet(false, true)) {
                    recordFailure(attempt, ex);
                }
                return Flux.error(ex);
            }
        });
    }

    private CircuitAttempt beginAttempt(String breakerName) {
        CircuitState state = stateMap.computeIfAbsent(breakerName, key -> new CircuitState());
        long now = System.currentTimeMillis();
        synchronized (state) {
            if (state.openUntilEpochMs > now) {
                long remainingMs = state.openUntilEpochMs - now;
                throw new AiCircuitBreakerOpenException(
                        "AI 服务暂时不可用，熔断冷却中，请 "
                                + Duration.ofMillis(remainingMs).toSeconds()
                                + " 秒后重试");
            }
            boolean halfOpen = state.openUntilEpochMs > 0;
            if (halfOpen && state.halfOpenProbeRunning) {
                throw new AiCircuitBreakerOpenException("AI 服务正在半开探测中，请稍后再试");
            }
            state.halfOpenProbeRunning = halfOpen;
            return new CircuitAttempt(breakerName, state, halfOpen);
        }
    }

    private void recordSuccess(CircuitAttempt attempt) {
        synchronized (attempt.state()) {
            attempt.state().consecutiveFailures = 0;
            attempt.state().openUntilEpochMs = 0L;
            attempt.state().halfOpenProbeRunning = false;
        }
    }

    private void recordFailure(CircuitAttempt attempt, Throwable throwable) {
        synchronized (attempt.state()) {
            attempt.state().halfOpenProbeRunning = false;
            attempt.state().consecutiveFailures++;
            if (attempt.halfOpen() || attempt.state().consecutiveFailures >= config.getFailureThreshold()) {
                attempt.state().openUntilEpochMs = System.currentTimeMillis() + config.getOpenDurationMs();
                log.warn("AI 熔断器已打开, breakerName: {}, failures: {}, cause: {}",
                        attempt.breakerName(),
                        attempt.state().consecutiveFailures,
                        throwable.getClass().getSimpleName());
            }
        }
    }

    private record CircuitAttempt(String breakerName, CircuitState state, boolean halfOpen) {
    }

    private static final class CircuitState {
        private int consecutiveFailures;
        private long openUntilEpochMs;
        @SuppressWarnings("unused")
        private boolean halfOpenProbeRunning;
    }

    /**
     * 熔断器打开时抛出的快速失败异常。
     */
    public static class AiCircuitBreakerOpenException extends RuntimeException {
        public AiCircuitBreakerOpenException(String message) {
            super(message);
        }
    }
}
