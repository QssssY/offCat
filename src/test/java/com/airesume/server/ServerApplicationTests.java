package com.airesume.server;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest(properties = {
        "jwt.secret=test-jwt-secret-key-for-spring-context-123456",
        "spring.datasource.password=123456",
        "spring.rabbitmq.listener.simple.auto-startup=false",
        "spring.rabbitmq.listener.direct.auto-startup=false",
        // 项目同时依赖 spring-boot-starter-web 和 spring-boot-starter-webflux，
        // 测试上下文可能以 REACTIVE 模式启动，导致 RestClient.Builder 等 Servlet 相关 bean 不可用，
        // 显式指定 SERVLET 类型与生产环境保持一致，避免 TtsDiscoveryServiceImpl 等注入失败。
        "spring.main.web-application-type=servlet"
})
class ServerApplicationTests {

    @Test
    void contextLoads() {
    }

}
