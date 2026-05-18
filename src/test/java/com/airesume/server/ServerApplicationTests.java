package com.airesume.server;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest(properties = {
        "jwt.secret=test-jwt-secret-key-for-spring-context-123456",
        "spring.datasource.password=123456",
        "spring.rabbitmq.listener.simple.auto-startup=false",
        "spring.rabbitmq.listener.direct.auto-startup=false"
})
class ServerApplicationTests {

    @Test
    void contextLoads() {
    }

}
