package com.airesume.server.infrastructure.security;

import jakarta.annotation.PostConstruct;
import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * JWT 配置属性。
 */
@Data
@Component
@ConfigurationProperties(prefix = "jwt")
public class JwtProperties {

    static final String WEAK_SECRET_PLACEHOLDER = "ai-resume-dev-jwt-secret-placeholder";
    private static final int MIN_SECRET_LENGTH = 32;

    private String secret = "";
    private long expiration = 86400000;
    private String header = "Authorization";
    private String prefix = "Bearer ";

    /**
     * 启动时强制校验 JWT 密钥，避免弱默认值或空值进入运行环境。
     */
    @PostConstruct
    public void validate() {
        if (secret == null || secret.isBlank()) {
            throw new IllegalStateException("JWT_SECRET must be configured and must not be blank");
        }
        if (WEAK_SECRET_PLACEHOLDER.equals(secret)) {
            throw new IllegalStateException("JWT_SECRET must not use the default placeholder value");
        }
        if (secret.length() < MIN_SECRET_LENGTH) {
            throw new IllegalStateException("JWT_SECRET must be at least 32 characters long");
        }
    }
}
