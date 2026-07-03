package com.airesume.server.config;

import org.junit.jupiter.api.Test;

import java.util.Arrays;

import static org.junit.jupiter.api.Assertions.assertFalse;

class WebMvcConfigTest {

    @Test
    void shouldNotExposeLegacyLocalCommunityUploadCacheControl() {
        boolean hasLegacyCacheControlMethod = Arrays.stream(WebMvcConfig.class.getDeclaredMethods())
                .anyMatch(method -> "communityUploadCacheControl".equals(method.getName()));

        assertFalse(hasLegacyCacheControlMethod);
    }
}
