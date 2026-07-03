package com.airesume.server.common.exception;

import com.airesume.server.common.result.ResultCode;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * BusinessException 构造函数测试。
 * 验证 4 个构造函数的行为：纯消息、code+消息、ResultCode、ResultCode+动态消息。
 */
class BusinessExceptionTest {

    @Test
    @DisplayName("纯消息构造函数使用 BUSINESS_ERROR code (500)")
    void shouldStringConstructorUseBusinessErrorCode() {
        BusinessException ex = new BusinessException("测试消息");
        assertEquals(ResultCode.BUSINESS_ERROR.getCode(), ex.getCode());
        assertEquals("测试消息", ex.getMessage());
    }

    @Test
    @DisplayName("code + 消息构造函数使用自定义 code")
    void shouldCodeMessageConstructorUseCustomCode() {
        BusinessException ex = new BusinessException(2001, "自定义错误");
        assertEquals(2001, ex.getCode());
        assertEquals("自定义错误", ex.getMessage());
    }

    @Test
    @DisplayName("ResultCode 构造函数使用枚举的 code 和 message")
    void shouldResultCodeConstructorUseEnumCodeAndMessage() {
        BusinessException ex = new BusinessException(ResultCode.RESUME_FILE_EMPTY);
        assertEquals(ResultCode.RESUME_FILE_EMPTY.getCode(), ex.getCode());
        assertEquals(ResultCode.RESUME_FILE_EMPTY.getMessage(), ex.getMessage());
    }

    @Test
    @DisplayName("ResultCode + 动态消息构造函数使用枚举 code 但覆盖 message")
    void shouldResultCodeDynamicMessageConstructorUseEnumCodeButOverrideMessage() {
        BusinessException ex = new BusinessException(ResultCode.RESUME_FILE_TOO_LARGE, "文件大小不能超过 5MB");
        assertEquals(ResultCode.RESUME_FILE_TOO_LARGE.getCode(), ex.getCode());
        assertEquals("文件大小不能超过 5MB", ex.getMessage());
        // 确保动态消息确实不同于枚举原始消息
        assertNotEquals(ResultCode.RESUME_FILE_TOO_LARGE.getMessage(), ex.getMessage());
    }

    @Test
    @DisplayName("BusinessException 是 RuntimeException 的子类")
    void shouldBeRuntimeExceptionSubclass() {
        BusinessException ex = new BusinessException(ResultCode.PARAM_ERROR);
        assertInstanceOf(RuntimeException.class, ex);
    }

    @Test
    @DisplayName("Result.error 可正确使用 BusinessException 的 code 和 message")
    void shouldResultErrorUseBusinessExceptionCodeAndMessage() {
        BusinessException ex = new BusinessException(ResultCode.AI_RESPONSE_EMPTY);
        var result = com.airesume.server.common.result.Result.error(ex.getCode(), ex.getMessage());
        assertEquals(ResultCode.AI_RESPONSE_EMPTY.getCode(), result.getCode());
        assertEquals(ResultCode.AI_RESPONSE_EMPTY.getMessage(), result.getMessage());
    }

    @Test
    @DisplayName("动态消息的 Result.error 也能正确返回 code 和 message")
    void shouldResultErrorWorkWithDynamicMessage() {
        BusinessException ex = new BusinessException(ResultCode.RESUME_FILE_TOO_LARGE, "文件大小不能超过 10MB");
        var result = com.airesume.server.common.result.Result.error(ex.getCode(), ex.getMessage());
        assertEquals(ResultCode.RESUME_FILE_TOO_LARGE.getCode(), result.getCode());
        assertEquals("文件大小不能超过 10MB", result.getMessage());
    }
}
