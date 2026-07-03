package com.airesume.server.config;

import jakarta.servlet.DispatcherType;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpMethod;
import org.springframework.security.core.userdetails.UserDetailsService;

import java.nio.file.Files;
import java.nio.file.Path;

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
        assertTrue(SecurityConfig.supportsPublicUploadPath("/api/community/images/community/1/20260605/image.png"));
        assertTrue(SecurityConfig.supportsPublicUploadPath("/api/community/images/community/1/20260605/nested.webp"));
        assertFalse(SecurityConfig.supportsPublicUploadPath("/api/community/images/upload"));
        assertFalse(SecurityConfig.supportsPublicUploadPath("/uploads/resumes/resume.pdf"));
        assertFalse(SecurityConfig.supportsPublicUploadPath("/uploads/community/image.png"));
        assertFalse(SecurityConfig.supportsPublicUploadPath("/api/community/posts"));
    }

    @Test
    void shouldOnlyExposeCommunityImageGetEndpointWithoutAuthentication() {
        assertTrue(SecurityConfig.supportsPublicCommunityImageEndpoint(
                HttpMethod.GET, "/api/community/images/community/1/20260605/image.png"));

        assertFalse(SecurityConfig.supportsPublicCommunityImageEndpoint(
                HttpMethod.POST, "/api/community/images/upload"));
        assertFalse(SecurityConfig.supportsPublicCommunityImageEndpoint(
                HttpMethod.POST, "/api/community/images/community/1/20260605/image.png"));
        assertFalse(SecurityConfig.supportsPublicCommunityImageEndpoint(
                HttpMethod.GET, "/api/community/images/upload"));
    }

    @Test
    void shouldNotExposeOfflineSttFallbackPath() throws Exception {
        String source = Files.readString(Path.of("src/main/java/com/airesume/server/config/SecurityConfig.java"));

        assertFalse(source.contains("/api/offline-stt"));
        assertFalse(source.contains("supportsPublicOfflineSttModelPath"));
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

    @Test
    void shouldProvideEmptyUserDetailsServiceToSuppressGeneratedPassword() {
        UserDetailsService userDetailsService = SecurityConfig.emptyUserDetailsService();

        assertTrue(userDetailsService instanceof org.springframework.security.provisioning.InMemoryUserDetailsManager);
        assertFalse(((org.springframework.security.provisioning.InMemoryUserDetailsManager) userDetailsService)
                .userExists("user"));
    }
}
