package com.airesume.server.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter;
import org.springframework.web.client.RestClient;

import java.util.ArrayList;

/**
 * RestClient 配置类
 * 为 AI 服务调用提供 RestClient.Builder Bean，配置默认 HTTP 超时
 */
@Configuration
public class RestClientConfig {

    @Bean
    public RestClient.Builder restClientBuilder() {
        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(10000);  // 10秒连接超时
        factory.setReadTimeout(120000);    // 120秒读取超时（AI生成需要较长时间）

        // 注册支持 application/octet-stream 的 Jackson 转换器
        // 部分 AI 供应商（如 DeepSeek）返回 octet-stream 而非 application/json
        MappingJackson2HttpMessageConverter octetStreamConverter = new MappingJackson2HttpMessageConverter();
        octetStreamConverter.setSupportedMediaTypes(new ArrayList<>(
                java.util.List.of(
                        org.springframework.http.MediaType.APPLICATION_JSON,
                        org.springframework.http.MediaType.APPLICATION_OCTET_STREAM)));

        return RestClient.builder()
                .requestFactory(factory)
                .messageConverters(converters -> converters.add(0, octetStreamConverter));
    }
}
