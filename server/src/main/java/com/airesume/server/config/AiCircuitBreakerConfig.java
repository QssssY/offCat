package com.airesume.server.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * AI 熔断器配置。
 * 用于在上游 AI 服务连续失败时快速失败，避免每个请求都把超时跑满。
 */
@Data
@Component
@ConfigurationProperties(prefix = "app.ai-circuit-breaker")
public class AiCircuitBreakerConfig {

    /**
     * 是否启用 AI 熔断器。
     */
    private boolean enabled = true;

    /**
     * 连续失败达到该阈值后打开熔断器。
     */
    private int failureThreshold = 3;

    /**
     * 熔断器打开后的冷却时间，单位毫秒。
     */
    private long openDurationMs = 30_000L;
}
