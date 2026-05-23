package com.airesume.server.config;

import jakarta.servlet.DispatcherType;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpMethod;

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

    @Test
    void shouldOnlyExposeCommunityUploadsAsPublicStaticResources() {
        assertTrue(SecurityConfig.supportsPublicUploadPath("/uploads/community/image.png"));
        assertTrue(SecurityConfig.supportsPublicUploadPath("/uploads/community/nested/image.webp"));
        assertFalse(SecurityConfig.supportsPublicUploadPath("/uploads/resumes/resume.pdf"));
        assertFalse(SecurityConfig.supportsPublicUploadPath("/uploads/other/file.png"));
    }

    @Test
    void shouldOnlyExposePublicAuthEndpointsWithoutAuthentication() {
        assertTrue(SecurityConfig.supportsPublicAuthEndpoint(HttpMethod.POST, "/api/auth/login"));
        assertTrue(SecurityConfig.supportsPublicAuthEndpoint(HttpMethod.POST, "/api/auth/register"));
        assertTrue(SecurityConfig.supportsPublicAuthEndpoint(HttpMethod.POST, "/api/auth/reset-password"));
        assertTrue(SecurityConfig.supportsPublicAuthEndpoint(HttpMethod.GET, "/api/auth/security-question"));

        assertFalse(SecurityConfig.supportsPublicAuthEndpoint(HttpMethod.GET, "/api/auth/me"));
        assertFalse(SecurityConfig.supportsPublicAuthEndpoint(HttpMethod.PUT, "/api/auth/password"));
        assertFalse(SecurityConfig.supportsPublicAuthEndpoint(HttpMethod.PUT, "/api/auth/security-question"));
        assertFalse(SecurityConfig.supportsPublicAuthEndpoint(HttpMethod.POST, "/api/auth/me"));
    }
}
