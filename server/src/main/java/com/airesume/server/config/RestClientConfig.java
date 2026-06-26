package com.airesume.server.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.MediaType;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter;
import org.springframework.web.client.RestClient;

import java.util.List;

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
        // 仅为错误标注为 octet-stream 的 JSON 响应兜底，避免抢占标准 application/json
        octetStreamConverter.setSupportedMediaTypes(List.of(MediaType.APPLICATION_OCTET_STREAM));

        return RestClient.builder()
                .requestFactory(factory)
                .messageConverters(converters -> converters.add(0, octetStreamConverter));
    }
}
