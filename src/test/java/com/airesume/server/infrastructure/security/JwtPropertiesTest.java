package com.airesume.server.infrastructure.security;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.mock.env.MockEnvironment;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

@DisplayName("JwtProperties 配置测试")
class JwtPropertiesTest {

    @Test
    @DisplayName("开发环境应允许默认占位 secret")
    void shouldAllowWeakPlaceholderInDevProfile() {
        JwtProperties properties = new JwtProperties();
        properties.setSecret("dev-secret-key-change-in-production-123456");
        properties.setEnvironment(new MockEnvironment().withProperty("spring.profiles.active", "dev"));

        assertDoesNotThrow(properties::validate);
    }

    @Test
    @DisplayName("非开发环境不应允许默认占位 secret")
    void shouldRejectWeakPlaceholderOutsideDevProfile() {
        JwtProperties properties = new JwtProperties();
        properties.setSecret("dev-secret-key-change-in-production-123456");
        properties.setEnvironment(new MockEnvironment().withProperty("spring.profiles.active", "prod"));

        assertThrows(IllegalStateException.class, properties::validate);
    }

    @Test
    @DisplayName("默认 token 有效期应为 7 天")
    void shouldUseSevenDayDefaultExpiration() {
        JwtProperties properties = new JwtProperties();

        assertEquals(604800000L, properties.getExpiration());
    }
}
