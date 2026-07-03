package com.airesume.server.service.impl;

import com.airesume.server.common.exception.BusinessException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.test.util.ReflectionTestUtils;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CaptchaServiceImplTest {

    @Mock
    private StringRedisTemplate stringRedisTemplate;

    @Mock
    private ValueOperations<String, String> valueOperations;

    private CaptchaServiceImpl captchaService;

    @BeforeEach
    void setUp() {
        captchaService = new CaptchaServiceImpl();
        ReflectionTestUtils.setField(captchaService, "stringRedisTemplate", stringRedisTemplate);
        when(stringRedisTemplate.opsForValue()).thenReturn(valueOperations);
    }

    @Test
    void shouldConsumeCaptchaAtomicallyWhenVerificationSucceeds() {
        when(valueOperations.getAndDelete("captcha:captcha-1")).thenReturn("A8K2");

        captchaService.verify("captcha-1", "a8k2");

        verify(valueOperations).getAndDelete("captcha:captcha-1");
        verify(stringRedisTemplate, never()).delete(anyString());
    }

    @Test
    void shouldConsumeCaptchaAtomicallyBeforeRejectingMismatch() {
        when(valueOperations.getAndDelete("captcha:captcha-2")).thenReturn("A8K2");

        assertThrows(BusinessException.class, () -> captchaService.verify("captcha-2", "WRONG"));

        verify(valueOperations).getAndDelete("captcha:captcha-2");
        verify(stringRedisTemplate, never()).delete(anyString());
    }
}
