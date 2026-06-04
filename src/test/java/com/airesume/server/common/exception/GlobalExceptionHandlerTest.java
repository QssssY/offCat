package com.airesume.server.common.exception;

import com.airesume.server.common.result.Result;
import com.airesume.server.common.result.ResultCode;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpMethod;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.servlet.resource.NoResourceFoundException;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * GlobalExceptionHandler 测试。
 * 验证各类异常被正确转换为 Result 响应，不泄露内部细节。
 */
class GlobalExceptionHandlerTest {

    private GlobalExceptionHandler handler;

    @BeforeEach
    void setUp() {
        handler = new GlobalExceptionHandler();
    }

    @Test
    @DisplayName("BusinessException(ResultCode) 正确传播 code 和 message")
    void shouldHandleBusinessExceptionWithResultCode() {
        BusinessException ex = new BusinessException(ResultCode.RESUME_FILE_EMPTY);
        Result<Void> result = handler.handleBusinessException(ex);
        assertEquals(ResultCode.RESUME_FILE_EMPTY.getCode(), result.getCode());
        assertEquals(ResultCode.RESUME_FILE_EMPTY.getMessage(), result.getMessage());
        assertNull(result.getData());
    }

    @Test
    @DisplayName("BusinessException(ResultCode, 动态消息) 正确传播 code 和动态消息")
    void shouldHandleBusinessExceptionWithDynamicMessage() {
        BusinessException ex = new BusinessException(ResultCode.RESUME_FILE_TOO_LARGE, "文件大小不能超过 10MB");
        Result<Void> result = handler.handleBusinessException(ex);
        assertEquals(ResultCode.RESUME_FILE_TOO_LARGE.getCode(), result.getCode());
        assertEquals("文件大小不能超过 10MB", result.getMessage());
        assertNull(result.getData());
    }

    @Test
    @DisplayName("BusinessException(纯消息) 使用 BUSINESS_ERROR code")
    void shouldHandleBusinessExceptionWithPlainMessage() {
        BusinessException ex = new BusinessException("某个中文错误");
        Result<Void> result = handler.handleBusinessException(ex);
        assertEquals(ResultCode.BUSINESS_ERROR.getCode(), result.getCode());
        assertEquals("某个中文错误", result.getMessage());
    }

    @Test
    @DisplayName("通用异常返回系统错误，不泄露堆栈")
    void shouldHandleGenericExceptionWithSystemError() {
        Exception ex = new RuntimeException("数据库连接失败: jdbc:mysql://secret-host/db");
        Result<Void> result = handler.handleException(ex);
        assertEquals(ResultCode.SYSTEM_ERROR.getCode(), result.getCode());
        // 不应包含原始异常消息中的敏感信息
        assertFalse(result.getMessage().contains("secret-host"));
        assertFalse(result.getMessage().contains("jdbc"));
    }

    @Test
    @DisplayName("缺失静态资源返回 404，不按系统异常处理")
    void shouldHandleMissingStaticResourceAsNotFound() {
        NoResourceFoundException ex = new NoResourceFoundException(HttpMethod.GET, "/uploads/community/missing.png");
        Result<Void> result = handler.handleNoResourceFoundException(ex);
        assertEquals(ResultCode.NOT_FOUND.getCode(), result.getCode());
        assertEquals("资源不存在", result.getMessage());
    }

    @Test
    @DisplayName("不支持的请求方法返回参数错误，不按系统异常处理")
    void shouldHandleUnsupportedRequestMethodAsClientError() {
        HttpRequestMethodNotSupportedException ex =
                new HttpRequestMethodNotSupportedException("GET", List.of("POST"));
        Result<Void> result = handler.handleHttpRequestMethodNotSupportedException(ex);
        assertEquals(ResultCode.PARAM_ERROR.getCode(), result.getCode());
        assertEquals("请求方法不支持", result.getMessage());
    }
}
