package com.airesume.server.service.impl;

import com.airesume.server.common.exception.BusinessException;
import com.airesume.server.common.result.ResultCode;
import com.airesume.server.dto.auth.LoginRequest;
import com.airesume.server.dto.auth.LoginResponse;
import com.airesume.server.dto.auth.RegisterRequest;
import com.airesume.server.dto.auth.ResetPasswordRequest;
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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

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

            authService.register(request);

            verify(sysUserService).existsByUsername(TEST_USERNAME);
            verify(sysUserService).save(any(SysUser.class));
            verify(userQuotaService).initUserQuota(TEST_USER_ID);
            verify(passwordEncoder).encode(TEST_PASSWORD);
        }
    }

    @Nested
    @DisplayName("login 方法测试")
    class LoginTests {

        @Test
        @DisplayName("应该成功登录并返回 token")
        void shouldLoginSuccessfullyAndReturnToken() {
            LoginRequest request = new LoginRequest();
            request.setUsername(TEST_USERNAME);
            request.setPassword(TEST_PASSWORD);

            SysUser user = new SysUser();
            user.setId(TEST_USER_ID);
            user.setUsername(TEST_USERNAME);
            user.setPassword(ENCODED_PASSWORD);
            user.setRole(0);
            user.setStatus(1);

            lenient().when(valueOperations.get(anyString())).thenReturn(null);
            when(sysUserService.getByUsername(TEST_USERNAME)).thenReturn(user);
            when(passwordEncoder.matches(TEST_PASSWORD, ENCODED_PASSWORD)).thenReturn(true);
            when(jwtUtil.generateToken(TEST_USER_ID, TEST_USERNAME, 0)).thenReturn(TEST_TOKEN);

            LoginResponse response = authService.login(request);

            assertNotNull(response);
            assertEquals(TEST_TOKEN, response.getToken());
            assertEquals("Bearer", response.getTokenType());
            assertEquals(3600, response.getExpiresIn());
        }

        @Test
        @DisplayName("Redis 不可用时仍应使用本地限流并最终锁定")
        void shouldUseInMemoryRateLimitWhenRedisUnavailable() {
            LoginRequest request = new LoginRequest();
            request.setUsername(TEST_USERNAME);
            request.setPassword("wrongPassword");

            SysUser user = new SysUser();
            user.setId(TEST_USER_ID);
            user.setUsername(TEST_USERNAME);
            user.setPassword(ENCODED_PASSWORD);

            when(sysUserService.getByUsername(TEST_USERNAME)).thenReturn(user);
            when(passwordEncoder.matches("wrongPassword", ENCODED_PASSWORD)).thenReturn(false);
            lenient().when(valueOperations.get(anyString())).thenThrow(new RuntimeException("Redis read failed"));
            lenient().doThrow(new RuntimeException("Redis write failed")).when(valueOperations).increment(anyString());

            for (int i = 0; i < 5; i++) {
                BusinessException exception = assertThrows(BusinessException.class, () -> authService.login(request));
                assertEquals("用户名或密码错误", exception.getMessage());
            }

            BusinessException blockedException = assertThrows(BusinessException.class, () -> authService.login(request));
            assertTrue(blockedException.getMessage().contains("登录失败次数过多"));
        }
    }

    @Nested
    @DisplayName("getCurrentUserInfo 方法测试")
    class GetCurrentUserInfoTests {

        @Test
        @DisplayName("应该成功获取用户信息")
        void shouldGetUserInfoSuccessfully() {
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

            UserInfoResponse response = authService.getCurrentUserInfo(TEST_USER_ID);

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
            when(sysUserService.getById(TEST_USER_ID)).thenReturn(null);

            BusinessException exception = assertThrows(BusinessException.class,
                    () -> authService.getCurrentUserInfo(TEST_USER_ID));

            assertEquals(ResultCode.NOT_FOUND.getCode(), exception.getCode());
        }
    }

    @Nested
    @DisplayName("resetPasswordBySecurityQuestion 方法测试")
    class ResetPasswordTests {

        @Test
        @DisplayName("用户不存在和答案错误应返回同一错误")
        void shouldUseSameFailureMessageForEnumerationSensitiveCases() {
            ResetPasswordRequest request = new ResetPasswordRequest();
            request.setUsername(TEST_USERNAME);
            request.setSecurityAnswer("wrong");
            request.setNewPassword("NewPass@123456");

            when(sysUserService.getByUsername(TEST_USERNAME)).thenReturn(null);
            BusinessException notFoundException = assertThrows(BusinessException.class,
                    () -> authService.resetPasswordBySecurityQuestion(request));

            SysUser user = new SysUser();
            user.setUsername(TEST_USERNAME);
            user.setSecurityQuestion("你的名字是？");
            user.setSecurityAnswer("encodedAnswer");
            user.setPassword(ENCODED_PASSWORD);
            when(sysUserService.getByUsername(TEST_USERNAME)).thenReturn(user);
            when(passwordEncoder.matches("wrong", "encodedAnswer")).thenReturn(false);

            BusinessException wrongAnswerException = assertThrows(BusinessException.class,
                    () -> authService.resetPasswordBySecurityQuestion(request));

            assertEquals("用户名或安全问题答案不正确", notFoundException.getMessage());
            assertEquals("用户名或安全问题答案不正确", wrongAnswerException.getMessage());
        }
    }

    @Test
    @DisplayName("占位符密钥应在启动校验时失败")
    void jwtPropertiesShouldRejectWeakPlaceholderSecret() {
        JwtProperties properties = new JwtProperties();
        properties.setSecret("ai-resume-dev-jwt-secret-placeholder");

        IllegalStateException exception = assertThrows(IllegalStateException.class, properties::validate);
        assertTrue(exception.getMessage().contains("default placeholder"));
    }

    @Test
    @DisplayName("过短密钥应在启动校验时失败")
    void jwtPropertiesShouldRejectShortSecret() {
        JwtProperties properties = new JwtProperties();
        properties.setSecret("short-secret");

        IllegalStateException exception = assertThrows(IllegalStateException.class, properties::validate);
        assertTrue(exception.getMessage().contains("at least 32 characters"));
    }

    @Test
    @DisplayName("合法密钥应通过启动校验")
    void jwtPropertiesShouldAcceptValidSecret() {
        JwtProperties properties = new JwtProperties();
        properties.setSecret("12345678901234567890123456789012");

        properties.validate();
    }
}
