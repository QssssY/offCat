package com.airesume.server.infrastructure.security;

import jakarta.annotation.PostConstruct;
import lombok.Data;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Component;

import java.util.Arrays;

/**
 * JWT 配置属性。
 */
@Data
@Component
@ConfigurationProperties(prefix = "jwt")
public class JwtProperties {

    static final String WEAK_SECRET_PLACEHOLDER = "dev-secret-key-change-in-production-123456";
    private static final int MIN_SECRET_LENGTH = 32;

    private String secret = "";
    private long expiration = 86400000;
    private String header = "Authorization";
    private String prefix = "Bearer ";
    private Environment environment;

    /**
     * 启动时强制校验 JWT 密钥，避免弱默认值或空值进入运行环境。
     */
    @PostConstruct
    public void validate() {
        if (secret == null || secret.isBlank()) {
            throw new IllegalStateException("JWT_SECRET must be configured and must not be blank");
        }
        if (WEAK_SECRET_PLACEHOLDER.equals(secret) && !isDevProfileActive()) {
            throw new IllegalStateException("JWT_SECRET must not use the default placeholder value");
        }
        if (secret.length() < MIN_SECRET_LENGTH) {
            throw new IllegalStateException("JWT_SECRET must be at least 32 characters long");
        }
    }

    /**
     * 开发环境允许使用仓库中的默认占位密钥，方便本地直接启动。
     */
    @Autowired
    void setEnvironment(Environment environment) {
        this.environment = environment;
    }

    private boolean isDevProfileActive() {
        if (environment == null) {
            return false;
        }
        return Arrays.stream(environment.getActiveProfiles()).anyMatch("dev"::equalsIgnoreCase)
                || Arrays.stream(environment.getDefaultProfiles()).anyMatch("dev"::equalsIgnoreCase);
    }
}
