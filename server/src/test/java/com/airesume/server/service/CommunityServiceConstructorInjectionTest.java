package com.airesume.server.service;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.lang.reflect.Constructor;
import java.util.Arrays;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * 社区服务构造器注入回归测试。
 * 作用：锁定生产环境必须由 Spring 选择完整依赖构造器，避免多构造器场景再次回退到无参构造器导致启动失败。
 */
class CommunityServiceConstructorInjectionTest {

    @Test
    void shouldMarkFullDependencyConstructorAsSpringInjectionEntry() {
        Constructor<?>[] autowiredConstructors = Arrays.stream(CommunityService.class.getConstructors())
                .filter(constructor -> constructor.isAnnotationPresent(Autowired.class))
                .toArray(Constructor[]::new);

        assertEquals(1, autowiredConstructors.length, "社区服务只能有一个显式 Spring 注入构造器");
        assertEquals(10, autowiredConstructors[0].getParameterCount(), "生产注入构造器必须包含完整的 10 个依赖");
        assertEquals(CommunityTextModerationService.class, autowiredConstructors[0].getParameterTypes()[7],
                "第 8 个依赖必须是社区文本审核服务，避免生产环境使用测试兼容构造器");
        assertEquals(NotificationService.class, autowiredConstructors[0].getParameterTypes()[8],
                "第 9 个依赖必须是站内通知服务，确保管理员下架后能通知作者");
        assertEquals(OssService.class, autowiredConstructors[0].getParameterTypes()[9],
                "最后一个依赖必须是 OSS 服务，确保生产环境使用真实对象存储实现");
    }
}
