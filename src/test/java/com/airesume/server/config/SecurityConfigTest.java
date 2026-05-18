package com.airesume.server.config;

import jakarta.servlet.DispatcherType;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SecurityConfigTest {

    @Test
    void shouldIncludeAsyncDispatcherTypeInSecurityMatcher() {
        assertTrue(SecurityConfig.supportsSecurityDispatcherType(DispatcherType.REQUEST));
        assertTrue(SecurityConfig.supportsSecurityDispatcherType(DispatcherType.FORWARD));
        assertTrue(SecurityConfig.supportsSecurityDispatcherType(DispatcherType.INCLUDE));
        assertTrue(SecurityConfig.supportsSecurityDispatcherType(DispatcherType.ASYNC));
    }

    @Test
    void shouldExcludeErrorDispatcherTypeFromSecurityMatcher() {
        assertFalse(SecurityConfig.supportsSecurityDispatcherType(DispatcherType.ERROR));
    }
}
