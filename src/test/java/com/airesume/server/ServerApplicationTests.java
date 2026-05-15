package com.airesume.server;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest(properties = "jwt.secret=test-jwt-secret-key-for-spring-context-123456")
class ServerApplicationTests {

    @Test
    void contextLoads() {
    }

}
