package com.airesume.server.config;

import org.junit.jupiter.api.Test;
import org.yaml.snakeyaml.Yaml;

import java.io.IOException;
import java.io.InputStream;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

class RuntimeProtectionConfigTest {

    @Test
    void shouldConfigureRedisPoolMaxWaitInDefaultDevAndProdProfiles() throws IOException {
        assertEquals("3000ms", redisPoolMaxWait("application.yml"));
        assertEquals("3000ms", redisPoolMaxWait("application-dev.yml"));
        assertEquals("3000ms", redisPoolMaxWait("application-prod.yml"));
    }

    @Test
    void shouldConfigureConservativeProductionCapacityForSmallServers() throws IOException {
        Map<String, Object> root = loadYaml("application-prod.yml");
        Map<String, Object> spring = getMap(root, "spring");
        Map<String, Object> datasource = getMap(spring, "datasource");
        Map<String, Object> hikari = getMap(datasource, "hikari");
        assertEquals(8, hikari.get("maximum-pool-size"));
        assertEquals(2, hikari.get("minimum-idle"));
        assertEquals(30000, hikari.get("connection-timeout"));
        assertEquals(300000, hikari.get("idle-timeout"));
        assertEquals(1800000, hikari.get("max-lifetime"));

        Map<String, Object> data = getMap(spring, "data");
        Map<String, Object> redis = getMap(data, "redis");
        assertEquals("3000ms", String.valueOf(redis.get("timeout")));
        Map<String, Object> lettuce = getMap(redis, "lettuce");
        Map<String, Object> pool = getMap(lettuce, "pool");
        assertEquals(8, pool.get("max-active"));
        assertEquals("3000ms", String.valueOf(pool.get("max-wait")));
        assertEquals(8, pool.get("max-idle"));
        assertEquals(2, pool.get("min-idle"));

        Map<String, Object> server = getMap(root, "server");
        Map<String, Object> tomcat = getMap(server, "tomcat");
        Map<String, Object> threads = getMap(tomcat, "threads");
        assertEquals(100, threads.get("max"));
        assertEquals(10, threads.get("min-spare"));
        assertEquals(2000, tomcat.get("max-connections"));
        assertEquals(100, tomcat.get("accept-count"));
        assertEquals(20000, tomcat.get("connection-timeout"));

        Map<String, Object> app = getMap(root, "app");
        Map<String, Object> diagnosis = getMap(app, "diagnosis");
        assertEquals(1, diagnosis.get("direct-threshold"));
    }

    @SuppressWarnings("unchecked")
    private String redisPoolMaxWait(String resourceName) throws IOException {
        Map<String, Object> root = loadYaml(resourceName);
        Map<String, Object> spring = getMap(root, "spring");
        Map<String, Object> data = getMap(spring, "data");
        Map<String, Object> redis = getMap(data, "redis");
        Map<String, Object> lettuce = getMap(redis, "lettuce");
        Map<String, Object> pool = getMap(lettuce, "pool");
        return String.valueOf(pool.get("max-wait"));
    }

    private Map<String, Object> loadYaml(String resourceName) throws IOException {
        try (InputStream inputStream = Thread.currentThread()
                .getContextClassLoader()
                .getResourceAsStream(resourceName)) {
            assertNotNull(inputStream, "Missing test resource: " + resourceName);
            return new Yaml().load(inputStream);
        }
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> getMap(Map<String, Object> source, String key) {
        Object value = source.get(key);
        assertNotNull(value, "Missing yaml key: " + key);
        return (Map<String, Object>) value;
    }
}
