package com.airesume.server.infrastructure.security;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Data
@Component
@ConfigurationProperties(prefix = "jwt")
public class JwtProperties {

    private String secret = "ai-resume-secret-key-2024-jwt-token-secret-for-authentication";
    private long expiration = 86400000;
    private String header = "Authorization";
    private String prefix = "Bearer ";

}
