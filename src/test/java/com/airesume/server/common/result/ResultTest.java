package com.airesume.server.common.result;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Result 统一响应封装类测试
 *
 * 测试覆盖：
 * - 成功响应（无数据、带数据、自定义消息）
 * - 失败响应（默认错误、自定义消息、自定义错误码、ResultCode 枚举）
 * - 数据完整性
 * - 泛型支持
 */
@DisplayName("Result 统一响应封装类测试")
class ResultTest {

    @Nested
    @DisplayName("success 方法测试")
    class SuccessTests {

        @Test
        @DisplayName("无数据成功响应应该返回正确的状态码和消息")
        void shouldReturnSuccessWithoutData() {
            Result<String> result = Result.success();

            assertEquals(200, result.getCode());
            assertEquals("操作成功", result.getMessage());
            assertNull(result.getData());
        }

        @Test
        @DisplayName("带数据成功响应应该返回正确的数据")
        void shouldReturnSuccessWithData() {
            String testData = "test data";
            Result<String> result = Result.success(testData);

            assertEquals(200, result.getCode());
            assertEquals("操作成功", result.getMessage());
            assertEquals(testData, result.getData());
        }

        @Test
        @DisplayName("自定义消息成功响应应该返回正确的消息")
        void shouldReturnSuccessWithCustomMessage() {
            String customMessage = "自定义成功消息";
            String testData = "test data";
            Result<String> result = Result.success(customMessage, testData);

            assertEquals(200, result.getCode());
            assertEquals(customMessage, result.getMessage());
            assertEquals(testData, result.getData());
        }

        @Test
        @DisplayName("成功响应应该支持 null 数据")
        void shouldSupportNullData() {
            Result<Object> result = Result.success(null);

            assertEquals(200, result.getCode());
            assertNull(result.getData());
        }

        @Test
        @DisplayName("成功响应应该支持复杂对象")
        void shouldSupportComplexObjects() {
            TestUser user = new TestUser("张三", 25);
            Result<TestUser> result = Result.success(user);

            assertEquals(200, result.getCode());
            assertNotNull(result.getData());
            assertEquals("张三", result.getData().getName());
            assertEquals(25, result.getData().getAge());
        }

        @Test
        @DisplayName("成功响应应该支持集合类型")
        void shouldSupportCollections() {
            java.util.List<String> list = java.util.Arrays.asList("a", "b", "c");
            Result<java.util.List<String>> result = Result.success(list);

            assertEquals(200, result.getCode());
            assertEquals(3, result.getData().size());
        }
    }

    @Nested
    @DisplayName("error 方法测试")
    class ErrorTests {

        @Test
        @DisplayName("默认错误响应应该返回正确的状态码和消息")
        void shouldReturnDefaultError() {
            Result<String> result = Result.error();

            assertEquals(500, result.getCode());
            assertEquals("操作失败", result.getMessage());
            assertNull(result.getData());
        }

        @Test
        @DisplayName("自定义消息错误响应应该返回正确的消息")
        void shouldReturnErrorWithCustomMessage() {
            String errorMessage = "自定义错误消息";
            Result<String> result = Result.error(errorMessage);

            assertEquals(500, result.getCode());
            assertEquals(errorMessage, result.getMessage());
            assertNull(result.getData());
        }

        @Test
        @DisplayName("自定义错误码和消息应该返回正确的值")
        void shouldReturnErrorWithCustomCodeAndMessage() {
            Integer errorCode = 400;
            String errorMessage = "参数错误";
            Result<String> result = Result.error(errorCode, errorMessage);

            assertEquals(errorCode, result.getCode());
            assertEquals(errorMessage, result.getMessage());
            assertNull(result.getData());
        }

        @Test
        @DisplayName("ResultCode 枚举错误响应应该返回正确的值")
        void shouldReturnErrorWithResultCode() {
            Result<String> result = Result.error(ResultCode.PARAM_ERROR);

            assertEquals(400, result.getCode());
            assertEquals("参数错误", result.getMessage());
            assertNull(result.getData());
        }

        @Test
        @DisplayName("UNAUTHORIZED ResultCode 应该返回 401")
        void shouldHandleUnauthorizedResultCode() {
            Result<String> result = Result.error(ResultCode.UNAUTHORIZED);

            assertEquals(401, result.getCode());
            assertEquals("未授权", result.getMessage());
        }

        @Test
        @DisplayName("FORBIDDEN ResultCode 应该返回 403")
        void shouldHandleForbiddenResultCode() {
            Result<String> result = Result.error(ResultCode.FORBIDDEN);

            assertEquals(403, result.getCode());
            assertEquals("禁止访问", result.getMessage());
        }

        @Test
        @DisplayName("NOT_FOUND ResultCode 应该返回 404")
        void shouldHandleNotFoundResultCode() {
            Result<String> result = Result.error(ResultCode.NOT_FOUND);

            assertEquals(404, result.getCode());
            assertEquals("资源不存在", result.getMessage());
        }

        @Test
        @DisplayName("SYSTEM_ERROR ResultCode 应该返回 500")
        void shouldHandleSystemErrorResultCode() {
            Result<String> result = Result.error(ResultCode.SYSTEM_ERROR);

            assertEquals(500, result.getCode());
            assertEquals("系统错误", result.getMessage());
        }

        @Test
        @DisplayName("BUSINESS_ERROR ResultCode 应该返回 500")
        void shouldHandleBusinessErrorResultCode() {
            Result<String> result = Result.error(ResultCode.BUSINESS_ERROR);

            assertEquals(500, result.getCode());
            assertEquals("业务异常", result.getMessage());
        }
    }

    @Nested
    @DisplayName("数据完整性测试")
    class DataIntegrityTests {

        @Test
        @DisplayName("成功响应的数据不应该被修改")
        void shouldNotModifySuccessData() {
            String originalData = "original";
            Result<String> result = Result.success(originalData);

            assertEquals(originalData, result.getData());
            // 验证返回的是同一个对象引用
            assertSame(originalData, result.getData());
        }

        @Test
        @DisplayName("错误响应的数据应该始终为 null")
        void shouldAlwaysHaveNullDataForError() {
            Result<String> result1 = Result.error();
            Result<String> result2 = Result.error("error");
            Result<String> result3 = Result.error(400, "error");
            Result<String> result4 = Result.error(ResultCode.ERROR);

            assertNull(result1.getData());
            assertNull(result2.getData());
            assertNull(result3.getData());
            assertNull(result4.getData());
        }

        @Test
        @DisplayName("Result 对象应该正确实现 equals 和 hashCode")
        void shouldImplementEqualsAndHashCode() {
            Result<String> result1 = Result.success("data");
            Result<String> result2 = Result.success("data");

            // Result 类使用了 @Data 注解，会自动生成 equals 和 hashCode
            // 所以两个具有相同内容的 Result 对象应该是相等的
            assertEquals(result1, result2);
            assertEquals(result1.hashCode(), result2.hashCode());
        }

        @Test
        @DisplayName("Result 对象应该有正确的 toString 表示")
        void shouldHaveCorrectToString() {
            Result<String> result = Result.success("test");
            String toString = result.toString();

            assertNotNull(toString);
            assertTrue(toString.contains("200"));
            assertTrue(toString.contains("操作成功"));
            assertTrue(toString.contains("test"));
        }
    }

    @Nested
    @DisplayName("泛型支持测试")
    class GenericsTests {

        @Test
        @DisplayName("应该支持 String 类型")
        void shouldSupportStringType() {
            Result<String> result = Result.success("test");
            assertTrue(result.getData() instanceof String);
        }

        @Test
        @DisplayName("应该支持 Integer 类型")
        void shouldSupportIntegerType() {
            Result<Integer> result = Result.success(123);
            assertTrue(result.getData() instanceof Integer);
        }

        @Test
        @DisplayName("应该支持 Long 类型")
        void shouldSupportLongType() {
            Result<Long> result = Result.success(123L);
            assertTrue(result.getData() instanceof Long);
        }

        @Test
        @DisplayName("应该支持 Boolean 类型")
        void shouldSupportBooleanType() {
            Result<Boolean> result = Result.success(true);
            assertTrue(result.getData() instanceof Boolean);
        }

        @Test
        @DisplayName("应该支持自定义对象类型")
        void shouldSupportCustomObjectType() {
            TestUser user = new TestUser("李四", 30);
            Result<TestUser> result = Result.success(user);

            assertTrue(result.getData() instanceof TestUser);
            assertEquals("李四", result.getData().getName());
        }

        @Test
        @DisplayName("应该支持数组类型")
        void shouldSupportArrayType() {
            String[] array = {"a", "b", "c"};
            Result<String[]> result = Result.success(array);

            assertTrue(result.getData() instanceof String[]);
            assertEquals(3, result.getData().length);
        }

        @Test
        @DisplayName("应该支持 Map 类型")
        void shouldSupportMapType() {
            java.util.Map<String, Object> map = new java.util.HashMap<>();
            map.put("key", "value");
            Result<java.util.Map<String, Object>> result = Result.success(map);

            assertTrue(result.getData() instanceof java.util.Map);
            assertEquals("value", result.getData().get("key"));
        }
    }

    @Nested
    @DisplayName("边界条件测试")
    class BoundaryTests {

        @Test
        @DisplayName("空字符串消息应该正常工作")
        void shouldHandleEmptyMessage() {
            Result<String> result = Result.error("");

            assertEquals(500, result.getCode());
            assertEquals("", result.getMessage());
        }

        @Test
        @DisplayName("超长消息应该正常工作")
        void shouldHandleLongMessage() {
            String longMessage = "a".repeat(10000);
            Result<String> result = Result.error(longMessage);

            assertEquals(500, result.getCode());
            assertEquals(longMessage, result.getMessage());
        }

        @Test
        @DisplayName("特殊字符消息应该正常工作")
        void shouldHandleSpecialCharactersInMessage() {
            String specialMessage = "错误消息：包含特殊字符 !@#$%^&*()_+{}|:\"<>?";
            Result<String> result = Result.error(specialMessage);

            assertEquals(specialMessage, result.getMessage());
        }

        @Test
        @DisplayName("Unicode 字符消息应该正常工作")
        void shouldHandleUnicodeMessage() {
            String unicodeMessage = "错误消息：包含中文和 Emoji 😀";
            Result<String> result = Result.error(unicodeMessage);

            assertEquals(unicodeMessage, result.getMessage());
        }

        @Test
        @DisplayName("负数错误码应该正常工作")
        void shouldHandleNegativeErrorCode() {
            Result<String> result = Result.error(-1, "负数错误码");

            assertEquals(-1, result.getCode());
        }

        @Test
        @DisplayName("零错误码应该正常工作")
        void shouldHandleZeroErrorCode() {
            Result<String> result = Result.error(0, "零错误码");

            assertEquals(0, result.getCode());
        }
    }

    // 测试用的内部类
    static class TestUser {
        private String name;
        private int age;

        public TestUser(String name, int age) {
            this.name = name;
            this.age = age;
        }

        public String getName() {
            return name;
        }

        public int getAge() {
            return age;
        }
    }
}
