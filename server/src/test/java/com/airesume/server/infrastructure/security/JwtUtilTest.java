package com.airesume.server.infrastructure.security;

import io.jsonwebtoken.Claims;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * JwtUtil 工具类测试
 *
 * 测试覆盖：
 * - Token 生成
 * - Token 解析
 * - Token 验证
 * - Token 过期检测
 * - 用户信息提取
 * - 异常情况处理
 */
@DisplayName("JwtUtil 工具类测试")
class JwtUtilTest {

    private JwtUtil jwtUtil;
    private JwtProperties jwtProperties;

    private static final Long TEST_USER_ID = 12345L;
    private static final String TEST_USERNAME = "testUser";
    private static final Integer TEST_ROLE = 0;

    @BeforeEach
    void setUp() {
        jwtProperties = new JwtProperties();
        jwtProperties.setSecret("testSecretKeyForJwtTokenGenerationMustBeLongEnoughForHS256Algorithm");
        jwtProperties.setExpiration(3600000); // 1 hour
        jwtUtil = new JwtUtil(jwtProperties);
    }

    @Nested
    @DisplayName("generateToken 方法测试")
    class GenerateTokenTests {

        @Test
        @DisplayName("应该成功生成 token")
        void shouldGenerateTokenSuccessfully() {
            String token = jwtUtil.generateToken(TEST_USER_ID, TEST_USERNAME, TEST_ROLE);

            assertNotNull(token);
            assertFalse(token.isEmpty());
            // JWT token 应该有 3 个部分，用 . 分隔
            assertEquals(3, token.split("\\.").length);
        }

        @Test
        @DisplayName("生成的 token 应该包含正确的用户信息")
        void shouldContainCorrectUserInfo() {
            String token = jwtUtil.generateToken(TEST_USER_ID, TEST_USERNAME, TEST_ROLE);
            Claims claims = jwtUtil.parseToken(token);

            assertNotNull(claims);
            assertEquals(TEST_USER_ID.toString(), claims.getSubject());
            assertEquals(TEST_USERNAME, claims.get("username", String.class));
            assertEquals(TEST_ROLE, claims.get("role", Integer.class));
        }

        @Test
        @DisplayName("生成的 token 应该有正确的过期时间")
        void shouldHaveCorrectExpiration() {
            long beforeGeneration = System.currentTimeMillis();
            String token = jwtUtil.generateToken(TEST_USER_ID, TEST_USERNAME, TEST_ROLE);
            long afterGeneration = System.currentTimeMillis();

            Claims claims = jwtUtil.parseToken(token);
            assertNotNull(claims);

            long expirationTime = claims.getExpiration().getTime();
            // 允许 1 秒的误差
            assertTrue(expirationTime >= beforeGeneration + jwtProperties.getExpiration() - 1000);
            assertTrue(expirationTime <= afterGeneration + jwtProperties.getExpiration() + 1000);
        }

        @Test
        @DisplayName("生成的 token 应该有签发时间")
        void shouldHaveIssuedAtTime() {
            long beforeGeneration = System.currentTimeMillis();
            String token = jwtUtil.generateToken(TEST_USER_ID, TEST_USERNAME, TEST_ROLE);
            long afterGeneration = System.currentTimeMillis();

            Claims claims = jwtUtil.parseToken(token);
            assertNotNull(claims);

            long issuedAtTime = claims.getIssuedAt().getTime();
            // 允许 1 秒的误差
            assertTrue(issuedAtTime >= beforeGeneration - 1000);
            assertTrue(issuedAtTime <= afterGeneration + 1000);
        }

        @Test
        @DisplayName("不同用户生成的 token 应该不同")
        void shouldGenerateDifferentTokensForDifferentUsers() {
            String token1 = jwtUtil.generateToken(1L, "user1", 0);
            String token2 = jwtUtil.generateToken(2L, "user2", 0);

            assertNotEquals(token1, token2);
        }
    }

    @Nested
    @DisplayName("parseToken 方法测试")
    class ParseTokenTests {

        @Test
        @DisplayName("应该成功解析有效 token")
        void shouldParseValidTokenSuccessfully() {
            String token = jwtUtil.generateToken(TEST_USER_ID, TEST_USERNAME, TEST_ROLE);
            Claims claims = jwtUtil.parseToken(token);

            assertNotNull(claims);
        }

        @Test
        @DisplayName("解析无效 token 应该返回 null")
        void shouldReturnNullForInvalidToken() {
            String invalidToken = "invalid.token.here";
            Claims claims = jwtUtil.parseToken(invalidToken);

            assertNull(claims);
        }

        @Test
        @DisplayName("解析空 token 应该返回 null")
        void shouldReturnNullForEmptyToken() {
            Claims claims = jwtUtil.parseToken("");

            assertNull(claims);
        }

        @Test
        @DisplayName("解析 null token 应该返回 null")
        void shouldReturnNullForNullToken() {
            Claims claims = jwtUtil.parseToken(null);

            assertNull(claims);
        }

        @Test
        @DisplayName("解析篡改的 token 应该返回 null")
        void shouldReturnNullForTamperedToken() {
            String token = jwtUtil.generateToken(TEST_USER_ID, TEST_USERNAME, TEST_ROLE);
            // 篡改 token 的 payload 部分
            String[] parts = token.split("\\.");
            String tamperedToken = parts[0] + ".tamperedPayload." + parts[2];

            Claims claims = jwtUtil.parseToken(tamperedToken);

            assertNull(claims);
        }
    }

    @Nested
    @DisplayName("getUserIdFromToken 方法测试")
    class GetUserIdFromTokenTests {

        @Test
        @DisplayName("应该正确提取用户 ID")
        void shouldExtractUserIdCorrectly() {
            String token = jwtUtil.generateToken(TEST_USER_ID, TEST_USERNAME, TEST_ROLE);
            Long userId = jwtUtil.getUserIdFromToken(token);

            assertEquals(TEST_USER_ID, userId);
        }

        @Test
        @DisplayName("无效 token 应该返回 null")
        void shouldReturnNullForInvalidToken() {
            Long userId = jwtUtil.getUserIdFromToken("invalid.token.here");

            assertNull(userId);
        }

        @Test
        @DisplayName("空 token 应该返回 null")
        void shouldReturnNullForEmptyToken() {
            Long userId = jwtUtil.getUserIdFromToken("");

            assertNull(userId);
        }
    }

    @Nested
    @DisplayName("getUsernameFromToken 方法测试")
    class GetUsernameFromTokenTests {

        @Test
        @DisplayName("应该正确提取用户名")
        void shouldExtractUsernameCorrectly() {
            String token = jwtUtil.generateToken(TEST_USER_ID, TEST_USERNAME, TEST_ROLE);
            String username = jwtUtil.getUsernameFromToken(token);

            assertEquals(TEST_USERNAME, username);
        }

        @Test
        @DisplayName("无效 token 应该返回 null")
        void shouldReturnNullForInvalidToken() {
            String username = jwtUtil.getUsernameFromToken("invalid.token.here");

            assertNull(username);
        }
    }

    @Nested
    @DisplayName("getRoleFromToken 方法测试")
    class GetRoleFromTokenTests {

        @Test
        @DisplayName("应该正确提取用户角色")
        void shouldExtractRoleCorrectly() {
            String token = jwtUtil.generateToken(TEST_USER_ID, TEST_USERNAME, TEST_ROLE);
            Integer role = jwtUtil.getRoleFromToken(token);

            assertEquals(TEST_ROLE, role);
        }

        @Test
        @DisplayName("管理员角色应该正确提取")
        void shouldExtractAdminRoleCorrectly() {
            Integer adminRole = 1;
            String token = jwtUtil.generateToken(TEST_USER_ID, TEST_USERNAME, adminRole);
            Integer role = jwtUtil.getRoleFromToken(token);

            assertEquals(adminRole, role);
        }

        @Test
        @DisplayName("无效 token 应该返回 null")
        void shouldReturnNullForInvalidToken() {
            Integer role = jwtUtil.getRoleFromToken("invalid.token.here");

            assertNull(role);
        }
    }

    @Nested
    @DisplayName("isTokenExpired 方法测试")
    class IsTokenExpiredTests {

        @Test
        @DisplayName("未过期的 token 应该返回 false")
        void shouldReturnFalseForValidToken() {
            String token = jwtUtil.generateToken(TEST_USER_ID, TEST_USERNAME, TEST_ROLE);
            boolean expired = jwtUtil.isTokenExpired(token);

            assertFalse(expired);
        }

        @Test
        @DisplayName("过期的 token 应该返回 true")
        void shouldReturnTrueForExpiredToken() {
            // 创建一个过期时间为 0 的 JwtUtil
            JwtProperties expiredProperties = new JwtProperties();
            expiredProperties.setSecret(jwtProperties.getSecret());
            expiredProperties.setExpiration(0); // 立即过期
            JwtUtil expiredJwtUtil = new JwtUtil(expiredProperties);

            String token = expiredJwtUtil.generateToken(TEST_USER_ID, TEST_USERNAME, TEST_ROLE);

            // 等待一小段时间确保 token 过期
            try {
                Thread.sleep(10);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }

            boolean expired = jwtUtil.isTokenExpired(token);
            assertTrue(expired);
        }

        @Test
        @DisplayName("无效 token 应该返回 true")
        void shouldReturnTrueForInvalidToken() {
            boolean expired = jwtUtil.isTokenExpired("invalid.token.here");

            assertTrue(expired);
        }

        @Test
        @DisplayName("空 token 应该返回 true")
        void shouldReturnTrueForEmptyToken() {
            boolean expired = jwtUtil.isTokenExpired("");

            assertTrue(expired);
        }
    }

    @Nested
    @DisplayName("validateToken 方法测试")
    class ValidateTokenTests {

        @Test
        @DisplayName("有效 token 应该返回 true")
        void shouldReturnTrueForValidToken() {
            String token = jwtUtil.generateToken(TEST_USER_ID, TEST_USERNAME, TEST_ROLE);
            boolean valid = jwtUtil.validateToken(token);

            assertTrue(valid);
        }

        @Test
        @DisplayName("无效 token 应该返回 false")
        void shouldReturnFalseForInvalidToken() {
            boolean valid = jwtUtil.validateToken("invalid.token.here");

            assertFalse(valid);
        }

        @Test
        @DisplayName("空 token 应该返回 false")
        void shouldReturnFalseForEmptyToken() {
            boolean valid = jwtUtil.validateToken("");

            assertFalse(valid);
        }

        @Test
        @DisplayName("null token 应该返回 false")
        void shouldReturnFalseForNullToken() {
            boolean valid = jwtUtil.validateToken(null);

            assertFalse(valid);
        }

        @Test
        @DisplayName("过期 token 应该返回 false")
        void shouldReturnFalseForExpiredToken() {
            // 创建一个过期时间为 0 的 JwtUtil
            JwtProperties expiredProperties = new JwtProperties();
            expiredProperties.setSecret(jwtProperties.getSecret());
            expiredProperties.setExpiration(0);
            JwtUtil expiredJwtUtil = new JwtUtil(expiredProperties);

            String token = expiredJwtUtil.generateToken(TEST_USER_ID, TEST_USERNAME, TEST_ROLE);

            try {
                Thread.sleep(10);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }

            boolean valid = jwtUtil.validateToken(token);
            assertFalse(valid);
        }

        @Test
        @DisplayName("篡改的 token 应该返回 false")
        void shouldReturnFalseForTamperedToken() {
            String token = jwtUtil.generateToken(TEST_USER_ID, TEST_USERNAME, TEST_ROLE);
            String[] parts = token.split("\\.");
            String tamperedToken = parts[0] + ".tamperedPayload." + parts[2];

            boolean valid = jwtUtil.validateToken(tamperedToken);
            assertFalse(valid);
        }
    }

    @Nested
    @DisplayName("边界条件测试")
    class BoundaryTests {

        @Test
        @DisplayName("用户 ID 为 0 应该正常工作")
        void shouldHandleZeroUserId() {
            String token = jwtUtil.generateToken(0L, TEST_USERNAME, TEST_ROLE);
            Long userId = jwtUtil.getUserIdFromToken(token);

            assertEquals(0L, userId);
        }

        @Test
        @DisplayName("用户 ID 为最大值应该正常工作")
        void shouldHandleMaxUserId() {
            Long maxUserId = Long.MAX_VALUE;
            String token = jwtUtil.generateToken(maxUserId, TEST_USERNAME, TEST_ROLE);
            Long userId = jwtUtil.getUserIdFromToken(token);

            assertEquals(maxUserId, userId);
        }

        @Test
        @DisplayName("用户名包含特殊字符应该正常工作")
        void shouldHandleSpecialCharactersInUsername() {
            String specialUsername = "user@name.test_123";
            String token = jwtUtil.generateToken(TEST_USER_ID, specialUsername, TEST_ROLE);
            String username = jwtUtil.getUsernameFromToken(token);

            assertEquals(specialUsername, username);
        }

        @Test
        @DisplayName("角色为负数应该正常工作")
        void shouldHandleNegativeRole() {
            Integer negativeRole = -1;
            String token = jwtUtil.generateToken(TEST_USER_ID, TEST_USERNAME, negativeRole);
            Integer role = jwtUtil.getRoleFromToken(token);

            assertEquals(negativeRole, role);
        }

        @Test
        @DisplayName("角色为最大值应该正常工作")
        void shouldHandleMaxRole() {
            Integer maxRole = Integer.MAX_VALUE;
            String token = jwtUtil.generateToken(TEST_USER_ID, TEST_USERNAME, maxRole);
            Integer role = jwtUtil.getRoleFromToken(token);

            assertEquals(maxRole, role);
        }
    }
}
