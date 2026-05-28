package com.airesume.server.config;

import com.baomidou.mybatisplus.extension.plugins.MybatisPlusInterceptor;
import com.baomidou.mybatisplus.extension.plugins.inner.BlockAttackInnerInterceptor;
import com.baomidou.mybatisplus.extension.plugins.inner.OptimisticLockerInnerInterceptor;
import com.baomidou.mybatisplus.extension.plugins.inner.PaginationInnerInterceptor;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class MybatisPlusConfigTest {

    @Test
    void shouldKeepRequiredInterceptorsAndSkipUnusedOptimisticLocker() {
        MybatisPlusInterceptor interceptor = new MybatisPlusConfig().mybatisPlusInterceptor();

        assertTrue(hasInnerInterceptor(interceptor, PaginationInnerInterceptor.class));
        assertTrue(hasInnerInterceptor(interceptor, BlockAttackInnerInterceptor.class));
        assertFalse(hasInnerInterceptor(interceptor, OptimisticLockerInnerInterceptor.class));
    }

    private boolean hasInnerInterceptor(MybatisPlusInterceptor interceptor, Class<?> interceptorClass) {
        return interceptor.getInterceptors().stream().anyMatch(interceptorClass::isInstance);
    }
}
