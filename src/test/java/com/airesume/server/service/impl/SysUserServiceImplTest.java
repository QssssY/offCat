package com.airesume.server.service.impl;

import org.junit.jupiter.api.Test;
import org.springframework.cache.annotation.CacheEvict;

import java.io.Serializable;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

class SysUserServiceImplTest {

    @Test
    void removeByIdShouldEvictUserCache() throws NoSuchMethodException {
        CacheEvict annotation = SysUserServiceImpl.class
                .getMethod("removeById", Serializable.class)
                .getAnnotation(CacheEvict.class);

        assertNotNull(annotation);
        assertEquals("sys_user", annotation.value()[0]);
        assertEquals("#id", annotation.key());
    }
}
