package com.airesume.server.config;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertTrue;

class WebMvcConfigTest {

    @Test
    void shouldConfigureCommunityUploadCacheControlForOneDay() {
        String headerValue = new WebMvcConfig().communityUploadCacheControl().getHeaderValue();

        assertTrue(headerValue.contains("max-age=86400"));
        assertTrue(headerValue.contains("public"));
    }
}
