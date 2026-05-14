package com.airesume.server.config;

import org.springframework.cache.CacheManager;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Lazy;
import org.springframework.data.redis.cache.RedisCacheConfiguration;
import org.springframework.data.redis.cache.RedisCacheManager;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.serializer.JdkSerializationRedisSerializer;
import org.springframework.data.redis.serializer.RedisSerializationContext;
import org.springframework.data.redis.serializer.RedisSerializer;
import org.springframework.data.redis.serializer.StringRedisSerializer;

import java.time.Duration;
import java.util.HashMap;
import java.util.Map;

/**
 * Redis 配置类。
 * <p>
 * 这里将 Spring Cache 与 RedisTemplate 的值序列化统一切换为 JDK 序列化，
 * 避免 Jackson 在根对象为 List 时出现“能写不能读”的反序列化问题。
 */
@Configuration
public class RedisConfig {

    /**
     * Redis Key 统一使用字符串序列化，便于排查缓存键。
     */
    @Bean
    public RedisSerializer<String> redisKeySerializer() {
        return new StringRedisSerializer();
    }

    /**
     * Redis Value 统一使用 JDK 序列化。
     * <p>
     * Spring Cache 的缓存对象大多是 DTO、VO、Map、List 等复杂结构，
     * 使用 JDK 序列化可以稳定处理根对象为 List 的场景。
     */
    @Bean
    public RedisSerializer<Object> redisValueSerializer() {
        return new JdkSerializationRedisSerializer(getClass().getClassLoader());
    }

    /**
     * 提供给手动 Redis 操作使用的模板。
     * PublicStatsController 读写公开统计缓存时也复用这一套序列化配置。
     */
    @Bean
    @Lazy
    public RedisTemplate<String, Object> redisTemplate(@Lazy RedisConnectionFactory connectionFactory,
                                                        RedisSerializer<String> redisKeySerializer,
                                                        RedisSerializer<Object> redisValueSerializer) {
        RedisTemplate<String, Object> template = new RedisTemplate<>();
        template.setConnectionFactory(connectionFactory);
        template.setKeySerializer(redisKeySerializer);
        template.setHashKeySerializer(redisKeySerializer);
        template.setValueSerializer(redisValueSerializer);
        template.setHashValueSerializer(redisValueSerializer);
        template.afterPropertiesSet();
        return template;
    }

    /**
     * Spring Cache 缓存管理器。
     * 不同缓存区域继续保留独立 TTL，避免一次性把所有缓存策略混成同一个周期。
     */
    @Bean
    public CacheManager cacheManager(@Lazy RedisConnectionFactory connectionFactory,
                                     RedisSerializer<String> redisKeySerializer,
                                     RedisSerializer<Object> redisValueSerializer) {
        RedisCacheConfiguration defaultConfig = RedisCacheConfiguration.defaultCacheConfig()
                .entryTtl(Duration.ofMinutes(5))
                .serializeKeysWith(RedisSerializationContext.SerializationPair.fromSerializer(redisKeySerializer))
                .serializeValuesWith(RedisSerializationContext.SerializationPair.fromSerializer(redisValueSerializer))
                .disableCachingNullValues();

        Map<String, RedisCacheConfiguration> cacheConfigMap = new HashMap<>();
        cacheConfigMap.put("auth:userInfo", defaultConfig.entryTtl(Duration.ofMinutes(10)));
        cacheConfigMap.put("notification:unreadCount", defaultConfig.entryTtl(Duration.ofMinutes(2)));
        cacheConfigMap.put("config:jobRoles", defaultConfig.entryTtl(Duration.ofMinutes(30)));
        cacheConfigMap.put("config:membershipPlans", defaultConfig.entryTtl(Duration.ofMinutes(30)));
        cacheConfigMap.put("resume:task", defaultConfig.entryTtl(Duration.ofSeconds(10)));
        cacheConfigMap.put("user:monthlyStats", defaultConfig.entryTtl(Duration.ofMinutes(5)));
        cacheConfigMap.put("user:growthOverview", defaultConfig.entryTtl(Duration.ofMinutes(5)));

        return RedisCacheManager.builder(connectionFactory)
                .cacheDefaults(defaultConfig)
                .withInitialCacheConfigurations(cacheConfigMap)
                .transactionAware()
                .build();
    }

}
