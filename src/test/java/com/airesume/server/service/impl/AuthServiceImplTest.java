package com.airesume.server.service.impl;

import com.airesume.server.common.exception.BusinessException;
import com.airesume.server.common.result.ResultCode;
import com.airesume.server.dto.auth.LoginRequest;
import com.airesume.server.dto.auth.LoginResponse;
import com.airesume.server.dto.auth.RegisterRequest;
import com.airesume.server.dto.auth.UserInfoResponse;
import com.airesume.server.entity.SysUser;
import com.airesume.server.entity.UserQuota;
import com.airesume.server.infrastructure.security.JwtProperties;
import com.airesume.server.infrastructure.security.JwtUtil;
import com.airesume.server.service.SysUserService;
import com.airesume.server.service.UserQuotaService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * AuthService 认证服务测试
 *
 * 测试覆盖：
 * - 用户注册
 * - 用户登录
 * - 登录失败限制
 * - 获取用户信息
 * - 密码验证
 * - 账号状态检查
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("AuthService 认证服务测试")
class AuthServiceImplTest {

    @InjectMocks
    private AuthServiceImpl authService;

    @Mock
    private SysUserService sysUserService;

    @Mock
    private UserQuotaService userQuotaService;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private JwtUtil jwtUtil;

    @Mock
    private JwtProperties jwtProperties;

    @Mock
    private StringRedisTemplate stringRedisTemplate;

    @Mock
    private ValueOperations<String, String> valueOperations;

    private static final String TEST_USERNAME = "testUser";
    private static final String TEST_PASSWORD = "Test@123456";
    private static final String ENCODED_PASSWORD = "$2a$10$encodedPassword";
    private static final Long TEST_USER_ID = 12345L;
    private static final String TEST_TOKEN = "test.jwt.token";

    @BeforeEach
    void setUp() {
        lenient().when(stringRedisTemplate.opsForValue()).thenReturn(valueOperations);
        lenient().when(jwtProperties.getPrefix()).thenReturn("Bearer ");
        lenient().when(jwtProperties.getExpiration()).thenReturn(3600000L);
    }

    @Nested
    @DisplayName("register 方法测试")
    class RegisterTests {

        @Test
        @DisplayName("应该成功注册新用户")
        void shouldRegisterNewUserSuccessfully() {
            // Arrange
            RegisterRequest request = new RegisterRequest();
            request.setUsername(TEST_USERNAME);
            request.setPassword(TEST_PASSWORD);
            request.setSecurityQuestion("你的名字是？");
            request.setSecurityAnswer("测试用户");

            when(sysUserService.existsByUsername(TEST_USERNAME)).thenReturn(false);
            when(passwordEncoder.encode(TEST_PASSWORD)).thenReturn(ENCODED_PASSWORD);
            when(passwordEncoder.encode("测试用户")).thenReturn("encodedAnswer");
            when(sysUserService.save(any(SysUser.class))).thenAnswer(invocation -> {
                SysUser user = invocation.getArgument(0);
                user.setId(TEST_USER_ID);
                return true;
            });

            // Act
            authService.register(request);

            // Assert
            verify(sysUserService).existsByUsername(TEST_USERNAME);
            verify(sysUserService).save(any(SysUser.class));
            verify(userQuotaService).initUserQuota(TEST_USER_ID);
            verify(passwordEncoder).encode(TEST_PASSWORD);
        }

        @Test
        @DisplayName("用户名已存在时应该抛出异常")
        void shouldThrowExceptionWhenUsernameExists() {
            // Arrange
            RegisterRequest request = new RegisterRequest();
            request.setUsername(TEST_USERNAME);
            request.setPassword(TEST_PASSWORD);

            when(sysUserService.existsByUsername(TEST_USERNAME)).thenReturn(true);

            // Act & Assert
            BusinessException exception = assertThrows(BusinessException.class,
                    () -> authService.register(request));

            assertEquals("用户名已存在", exception.getMessage());
            verify(sysUserService).existsByUsername(TEST_USERNAME);
            verify(sysUserService, never()).save(any());
            verify(userQuotaService, never()).initUserQuota(any());
        }

        @Test
        @DisplayName("注册时应该设置默认角色和状态")
        void shouldSetDefaultRoleAndStatus() {
            // Arrange
            RegisterRequest request = new RegisterRequest();
            request.setUsername(TEST_USERNAME);
            request.setPassword(TEST_PASSWORD);

            when(sysUserService.existsByUsername(TEST_USERNAME)).thenReturn(false);
            when(passwordEncoder.encode(TEST_PASSWORD)).thenReturn(ENCODED_PASSWORD);
            when(sysUserService.save(any(SysUser.class))).thenAnswer(invocation -> {
                SysUser user = invocation.getArgument(0);
                user.setId(TEST_USER_ID);
                return true;
            });

            // Act
            authService.register(request);

            // Assert
            verify(sysUserService).save(argThat(user -> {
                assertEquals(0, user.getRole());
                assertEquals(1, user.getStatus());
                assertEquals(TEST_USERNAME, user.getUsername());
                assertNotNull(user.getNickname());
                return true;
            }));
        }

        @Test
        @DisplayName("注册时应该保存安全问题和答案")
        void shouldSaveSecurityQuestionAndAnswer() {
            // Arrange
            RegisterRequest request = new RegisterRequest();
            request.setUsername(TEST_USERNAME);
            request.setPassword(TEST_PASSWORD);
            request.setSecurityQuestion("你的名字是？");
            request.setSecurityAnswer("测试用户");

            when(sysUserService.existsByUsername(TEST_USERNAME)).thenReturn(false);
            when(passwordEncoder.encode(anyString())).thenReturn("encoded");
            when(sysUserService.save(any(SysUser.class))).thenAnswer(invocation -> {
                SysUser user = invocation.getArgument(0);
                user.setId(TEST_USER_ID);
                return true;
            });

            // Act
            authService.register(request);

            // Assert
            verify(sysUserService).save(argThat(user -> {
                assertEquals("你的名字是？", user.getSecurityQuestion());
                assertNotNull(user.getSecurityAnswer());
                return true;
            }));
        }

        @Test
        @DisplayName("注册时不提供安全问题应该正常工作")
        void shouldWorkWithoutSecurityQuestion() {
            // Arrange
            RegisterRequest request = new RegisterRequest();
            request.setUsername(TEST_USERNAME);
            request.setPassword(TEST_PASSWORD);

            when(sysUserService.existsByUsername(TEST_USERNAME)).thenReturn(false);
            when(passwordEncoder.encode(TEST_PASSWORD)).thenReturn(ENCODED_PASSWORD);
            when(sysUserService.save(any(SysUser.class))).thenAnswer(invocation -> {
                SysUser user = invocation.getArgument(0);
                user.setId(TEST_USER_ID);
                return true;
            });

            // Act
            authService.register(request);

            // Assert
            verify(sysUserService).save(argThat(user -> {
                assertNull(user.getSecurityQuestion());
                assertNull(user.getSecurityAnswer());
                return true;
            }));
        }
    }

    @Nested
    @DisplayName("login 方法测试")
    class LoginTests {

        @Test
        @DisplayName("应该成功登录并返回 token")
        void shouldLoginSuccessfullyAndReturnToken() {
            // Arrange
            LoginRequest request = new LoginRequest();
            request.setUsername(TEST_USERNAME);
            request.setPassword(TEST_PASSWORD);

            SysUser user = new SysUser();
            user.setId(TEST_USER_ID);
            user.setUsername(TEST_USERNAME);
            user.setPassword(ENCODED_PASSWORD);
            user.setRole(0);
            user.setStatus(1);

            when(valueOperations.get(anyString())).thenReturn(null);
            when(sysUserService.getByUsername(TEST_USERNAME)).thenReturn(user);
            when(passwordEncoder.matches(TEST_PASSWORD, ENCODED_PASSWORD)).thenReturn(true);
            when(jwtUtil.generateToken(TEST_USER_ID, TEST_USERNAME, 0)).thenReturn(TEST_TOKEN);

            // Act
            LoginResponse response = authService.login(request);

            // Assert
            assertNotNull(response);
            assertEquals(TEST_TOKEN, response.getToken());
            assertEquals("Bearer", response.getTokenType());
            assertEquals(3600, response.getExpiresIn());
            verify(stringRedisTemplate).delete(anyString());
        }

        @Test
        @DisplayName("用户不存在时应该抛出异常")
        void shouldThrowExceptionWhenUserNotFound() {
            // Arrange
            LoginRequest request = new LoginRequest();
            request.setUsername(TEST_USERNAME);
            request.setPassword(TEST_PASSWORD);

            when(valueOperations.get(anyString())).thenReturn(null);
            when(sysUserService.getByUsername(TEST_USERNAME)).thenReturn(null);

            // Act & Assert
            BusinessException exception = assertThrows(BusinessException.class,
                    () -> authService.login(request));

            assertEquals("用户名或密码错误", exception.getMessage());
            verify(valueOperations).increment(anyString());
        }

        @Test
        @DisplayName("密码错误时应该抛出异常")
        void shouldThrowExceptionWhenPasswordIncorrect() {
            // Arrange
            LoginRequest request = new LoginRequest();
            request.setUsername(TEST_USERNAME);
            request.setPassword("wrongPassword");

            SysUser user = new SysUser();
            user.setId(TEST_USER_ID);
            user.setUsername(TEST_USERNAME);
            user.setPassword(ENCODED_PASSWORD);

            when(valueOperations.get(anyString())).thenReturn(null);
            when(sysUserService.getByUsername(TEST_USERNAME)).thenReturn(user);
            when(passwordEncoder.matches("wrongPassword", ENCODED_PASSWORD)).thenReturn(false);

            // Act & Assert
            BusinessException exception = assertThrows(BusinessException.class,
                    () -> authService.login(request));

            assertEquals("用户名或密码错误", exception.getMessage());
            verify(valueOperations).increment(anyString());
        }

        @Test
        @DisplayName("账号被封禁时应该抛出异常")
        void shouldThrowExceptionWhenAccountBanned() {
            // Arrange
            LoginRequest request = new LoginRequest();
            request.setUsername(TEST_USERNAME);
            request.setPassword(TEST_PASSWORD);

            SysUser user = new SysUser();
            user.setId(TEST_USER_ID);
            user.setUsername(TEST_USERNAME);
            user.setPassword(ENCODED_PASSWORD);
            user.setStatus(0); // 封禁状态

            when(valueOperations.get(anyString())).thenReturn(null);
            when(sysUserService.getByUsername(TEST_USERNAME)).thenReturn(user);
            when(passwordEncoder.matches(TEST_PASSWORD, ENCODED_PASSWORD)).thenReturn(true);

            // Act & Assert
            BusinessException exception = assertThrows(BusinessException.class,
                    () -> authService.login(request));

            assertEquals("账号已被封禁", exception.getMessage());
        }

        @Test
        @DisplayName("登录失败次数过多时应该抛出异常")
        void shouldThrowExceptionWhenTooManyFailedAttempts() {
            // Arrange
            LoginRequest request = new LoginRequest();
            request.setUsername(TEST_USERNAME);
            request.setPassword(TEST_PASSWORD);

            when(valueOperations.get(anyString())).thenReturn("5");

            // Act & Assert
            BusinessException exception = assertThrows(BusinessException.class,
                    () -> authService.login(request));

            assertTrue(exception.getMessage().contains("登录失败次数过多"));
        }

        @Test
        @DisplayName("登录成功后应该清除失败计数")
        void shouldClearFailedAttemptsAfterSuccessfulLogin() {
            // Arrange
            LoginRequest request = new LoginRequest();
            request.setUsername(TEST_USERNAME);
            request.setPassword(TEST_PASSWORD);

            SysUser user = new SysUser();
            user.setId(TEST_USER_ID);
            user.setUsername(TEST_USERNAME);
            user.setPassword(ENCODED_PASSWORD);
            user.setRole(0);
            user.setStatus(1);

            when(valueOperations.get(anyString())).thenReturn("3");
            when(sysUserService.getByUsername(TEST_USERNAME)).thenReturn(user);
            when(passwordEncoder.matches(TEST_PASSWORD, ENCODED_PASSWORD)).thenReturn(true);
            when(jwtUtil.generateToken(TEST_USER_ID, TEST_USERNAME, 0)).thenReturn(TEST_TOKEN);

            // Act
            authService.login(request);

            // Assert
            verify(stringRedisTemplate).delete(anyString());
        }
    }

    @Nested
    @DisplayName("getCurrentUserInfo 方法测试")
    class GetCurrentUserInfoTests {

        @Test
        @DisplayName("应该成功获取用户信息")
        void shouldGetUserInfoSuccessfully() {
            // Arrange
            SysUser user = new SysUser();
            user.setId(TEST_USER_ID);
            user.setUsername(TEST_USERNAME);
            user.setNickname("测试用户");
            user.setRole(0);
            user.setStatus(1);
            user.setMembershipPlanCode("NORMAL");
            user.setVipExpireTime(null);

            UserQuota quota = new UserQuota();
            quota.setDailyResumeUsed(0);
            quota.setDailyInterviewUsed(0);

            when(sysUserService.getById(TEST_USER_ID)).thenReturn(user);
            when(userQuotaService.getRemainingResumeQuota(TEST_USER_ID)).thenReturn(5);
            when(userQuotaService.getRemainingInterviewQuota(TEST_USER_ID)).thenReturn(3);
            when(userQuotaService.getByUserId(TEST_USER_ID)).thenReturn(quota);

            // Act
            UserInfoResponse response = authService.getCurrentUserInfo(TEST_USER_ID);

            // Assert
            assertNotNull(response);
            assertEquals(TEST_USER_ID, response.getId());
            assertEquals(TEST_USERNAME, response.getUsername());
            assertEquals("测试用户", response.getNickname());
            assertEquals(0, response.getRole());
            assertEquals(1, response.getStatus());
            assertEquals(5, response.getResumeQuota());
            assertEquals(3, response.getInterviewQuota());
        }

        @Test
        @DisplayName("用户不存在时应该抛出异常")
        void shouldThrowExceptionWhenUserNotFound() {
            // Arrange
            when(sysUserService.getById(TEST_USER_ID)).thenReturn(null);

            // Act & Assert
            BusinessException exception = assertThrows(BusinessException.class,
                    () -> authService.getCurrentUserInfo(TEST_USER_ID));

            assertEquals(ResultCode.NOT_FOUND.getCode(), exception.getCode());
        }
    }

    @Nested
    @DisplayName("Redis 异常处理测试")
    class RedisExceptionTests {

        @Test
        @DisplayName("Redis 读取失败时应该跳过限流")
        void shouldSkipRateLimitingWhenRedisFails() {
            // Arrange
            LoginRequest request = new LoginRequest();
            request.setUsername(TEST_USERNAME);
            request.setPassword(TEST_PASSWORD);

            SysUser user = new SysUser();
            user.setId(TEST_USER_ID);
            user.setUsername(TEST_USERNAME);
            user.setPassword(ENCODED_PASSWORD);
            user.setRole(0);
            user.setStatus(1);

            when(valueOperations.get(anyString())).thenThrow(new RuntimeException("Redis connection failed"));
            when(sysUserService.getByUsername(TEST_USERNAME)).thenReturn(user);
            when(passwordEncoder.matches(TEST_PASSWORD, ENCODED_PASSWORD)).thenReturn(true);
            when(jwtUtil.generateToken(TEST_USER_ID, TEST_USERNAME, 0)).thenReturn(TEST_TOKEN);

            // Act
            LoginResponse response = authService.login(request);

            // Assert
            assertNotNull(response);
            assertEquals(TEST_TOKEN, response.getToken());
        }

        @Test
        @DisplayName("Redis 写入失败时应该继续登录流程")
        void shouldContinueLoginWhenRedisWriteFails() {
            // Arrange
            LoginRequest request = new LoginRequest();
            request.setUsername(TEST_USERNAME);
            request.setPassword("wrongPassword");

            SysUser user = new SysUser();
            user.setId(TEST_USER_ID);
            user.setUsername(TEST_USERNAME);
            user.setPassword(ENCODED_PASSWORD);

            when(valueOperations.get(anyString())).thenReturn(null);
            when(sysUserService.getByUsername(TEST_USERNAME)).thenReturn(user);
            when(passwordEncoder.matches("wrongPassword", ENCODED_PASSWORD)).thenReturn(false);
            doThrow(new RuntimeException("Redis connection failed")).when(valueOperations).increment(anyString());

            // Act & Assert
            BusinessException exception = assertThrows(BusinessException.class,
                    () -> authService.login(request));

            assertEquals("用户名或密码错误", exception.getMessage());
        }
    }
}
