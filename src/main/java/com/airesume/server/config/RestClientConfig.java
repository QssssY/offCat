package com.airesume.server.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestClient;

/**
 * RestClient 配置类
 * 为 AI 服务调用提供 RestClient.Builder Bean
 */
@Configuration
public class RestClientConfig {

    @Bean
    public RestClient.Builder restClientBuilder() {
        return RestClient.builder();
    }
}
