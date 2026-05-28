package com.airesume.server.config;

import org.junit.jupiter.api.Test;
import org.yaml.snakeyaml.Yaml;

import java.io.IOException;
import java.io.InputStream;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;

class RuntimeProtectionConfigTest {

    @Test
    void shouldConfigureRedisPoolMaxWaitInDefaultDevAndProdProfiles() throws IOException {
        assertEquals("3000ms", redisPoolMaxWait("application.yml"));
        assertEquals("3000ms", redisPoolMaxWait("application-dev.yml"));
        assertEquals("3000ms", redisPoolMaxWait("application-prod.yml"));
    }

    @SuppressWarnings("unchecked")
    private String redisPoolMaxWait(String resourceName) throws IOException {
        try (InputStream inputStream = Thread.currentThread()
                .getContextClassLoader()
                .getResourceAsStream(resourceName)) {
            Map<String, Object> root = new Yaml().load(inputStream);
            Map<String, Object> spring = (Map<String, Object>) root.get("spring");
            Map<String, Object> data = (Map<String, Object>) spring.get("data");
            Map<String, Object> redis = (Map<String, Object>) data.get("redis");
            Map<String, Object> lettuce = (Map<String, Object>) redis.get("lettuce");
            Map<String, Object> pool = (Map<String, Object>) lettuce.get("pool");
            return String.valueOf(pool.get("max-wait"));
        }
    }
}
