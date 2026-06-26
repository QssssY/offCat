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
import com.airesume.server.service.CaptchaService;
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
import org.springframework.test.util.ReflectionTestUtils;

import java.lang.reflect.Constructor;
import java.time.Instant;
import java.time.LocalDateTime;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.times;
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
    private CaptchaService captchaService;

    @Mock
    private StringRedisTemplate stringRedisTemplate;

    @Mock
    private ValueOperations<String, String> valueOperations;

    private static final String TEST_USERNAME = "testUser";
    private static final String TEST_PASSWORD = "Test@123456";
    private static final String ENCODED_PASSWORD = "$2a$10$encodedPassword";
    private static final Long TEST_USER_ID = 12345L;
    private static final String TEST_TOKEN = "test.jwt.token";
    private static final long SEVEN_DAY_TOKEN_EXPIRATION_MILLIS = 604800000L;
    private static final long SEVEN_DAY_TOKEN_EXPIRATION_SECONDS = 604800L;

    @BeforeEach
    void setUp() {
        lenient().when(stringRedisTemplate.opsForValue()).thenReturn(valueOperations);
        lenient().when(jwtProperties.getPrefix()).thenReturn("Bearer ");
        lenient().when(jwtProperties.getExpiration()).thenReturn(SEVEN_DAY_TOKEN_EXPIRATION_MILLIS);
    }

    @Nested
    @DisplayName("register")
    class RegisterTests {

        @Test
        @DisplayName("should register new user successfully")
        void shouldRegisterNewUserSuccessfully() {
            RegisterRequest request = new RegisterRequest();
            request.setUsername(TEST_USERNAME);
            request.setPassword(TEST_PASSWORD);
            request.setSecurityQuestion("你的名字是？");
            request.setSecurityAnswer("测试用户");
            request.setCaptchaId("captcha-register");
            request.setCaptchaCode("A8K2");

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
            verify(captchaService).verify("captcha-register", "A8K2");
        }
    }

    @Nested
    @DisplayName("login")
    class LoginTests {

        @Test
        @DisplayName("should login successfully and return token")
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
            assertEquals(SEVEN_DAY_TOKEN_EXPIRATION_SECONDS, response.getExpiresIn());
        }

        @Test
        @DisplayName("should auto unban expired temporary ban before login")
        void shouldAutoUnbanExpiredTemporaryBanBeforeLogin() {
            LoginRequest request = new LoginRequest();
            request.setUsername(TEST_USERNAME);
            request.setPassword(TEST_PASSWORD);

            SysUser user = new SysUser();
            user.setId(TEST_USER_ID);
            user.setUsername(TEST_USERNAME);
            user.setPassword(ENCODED_PASSWORD);
            user.setRole(0);
            user.setStatus(0);
            user.setBanReason("临时封禁");
            user.setBannedBy(1L);
            user.setBannedTime(LocalDateTime.now().minusDays(2));
            user.setBannedUntil(LocalDateTime.now().minusMinutes(1));

            lenient().when(valueOperations.get(anyString())).thenReturn(null);
            when(sysUserService.getByUsername(TEST_USERNAME)).thenReturn(user);
            when(passwordEncoder.matches(TEST_PASSWORD, ENCODED_PASSWORD)).thenReturn(true);
            when(jwtUtil.generateToken(TEST_USER_ID, TEST_USERNAME, 0)).thenReturn(TEST_TOKEN);

            LoginResponse response = authService.login(request);

            assertNotNull(response);
            assertEquals(TEST_TOKEN, response.getToken());
            assertEquals(1, user.getStatus());
            assertEquals(null, user.getBanReason());
            assertEquals(null, user.getBannedUntil());
            verify(sysUserService).updateById(user);
        }

        @Test
        @DisplayName("should reject active banned user login")
        void shouldRejectActiveBannedUserLogin() {
            LoginRequest request = new LoginRequest();
            request.setUsername(TEST_USERNAME);
            request.setPassword(TEST_PASSWORD);

            SysUser user = new SysUser();
            user.setId(TEST_USER_ID);
            user.setUsername(TEST_USERNAME);
            user.setPassword(ENCODED_PASSWORD);
            user.setRole(0);
            user.setStatus(0);
            user.setBannedUntil(LocalDateTime.now().plusDays(1));

            lenient().when(valueOperations.get(anyString())).thenReturn(null);
            when(sysUserService.getByUsername(TEST_USERNAME)).thenReturn(user);
            when(passwordEncoder.matches(TEST_PASSWORD, ENCODED_PASSWORD)).thenReturn(true);

            BusinessException exception = assertThrows(BusinessException.class, () -> authService.login(request));

            assertEquals("用户名或密码错误", exception.getMessage());
        }

        @Test
        @DisplayName("should use in-memory rate limit when redis unavailable")
        void shouldUseInMemoryRateLimitWhenRedisUnavailable() {
            LoginRequest request = new LoginRequest();
            request.setUsername(TEST_USERNAME);
            request.setPassword("wrongPassword");
            request.setCaptchaId("captcha-login");
            request.setCaptchaCode("L8K2");

            SysUser user = new SysUser();
            user.setId(TEST_USER_ID);
            user.setUsername(TEST_USERNAME);
            user.setPassword(ENCODED_PASSWORD);

            when(sysUserService.getByUsername(TEST_USERNAME)).thenReturn(user);
            when(passwordEncoder.matches("wrongPassword", ENCODED_PASSWORD)).thenReturn(false);
            lenient().when(valueOperations.get(anyString())).thenThrow(new RuntimeException("Redis read failed"));
            lenient().when(valueOperations.increment(anyString())).thenThrow(new RuntimeException("Redis write failed"));

            for (int i = 0; i < 5; i++) {
                BusinessException exception = assertThrows(BusinessException.class, () -> authService.login(request));
                assertEquals("用户名或密码错误", exception.getMessage());
            }

            BusinessException blockedException = assertThrows(BusinessException.class, () -> authService.login(request));
            assertTrue(blockedException.getMessage().contains("登录失败次数过多"));
        }
        @Test
        @DisplayName("should clean expired local login attempts and keep active attempts")
        @SuppressWarnings({"unchecked", "rawtypes"})
        void shouldCleanExpiredLocalLoginAttemptsAndKeepActiveAttempts() throws Exception {
            Map localLoginAttempts = (Map) ReflectionTestUtils.getField(authService, "localLoginAttempts");
            Class<?> recordClass = Class.forName(AuthServiceImpl.class.getName() + "$LoginAttemptRecord");
            Constructor<?> constructor = recordClass.getDeclaredConstructor(int.class, Instant.class);
            constructor.setAccessible(true);

            localLoginAttempts.put("login:attempts:expiredUser", constructor.newInstance(3, Instant.now().minusSeconds(1)));
            localLoginAttempts.put("login:attempts:activeUser", constructor.newInstance(2, Instant.now().plusSeconds(60)));

            authService.cleanupExpiredLocalLoginAttempts();

            assertFalse(localLoginAttempts.containsKey("login:attempts:expiredUser"));
            assertTrue(localLoginAttempts.containsKey("login:attempts:activeUser"));
        }
    }

    @Nested
    @DisplayName("getCurrentUserInfo")
    class GetCurrentUserInfoTests {

        @Test
        @DisplayName("should get user info successfully")
        void shouldGetUserInfoSuccessfully() {
            SysUser user = new SysUser();
            user.setId(TEST_USER_ID);
            user.setUsername(TEST_USERNAME);
            user.setNickname("测试用户");
            user.setRole(0);
            user.setStatus(1);
            user.setMembershipPlanCode("NORMAL");
            user.setVipExpireTime(null);
            user.setCreateTime(java.time.LocalDateTime.of(2026, 5, 18, 10, 0));

            UserQuota quota = new UserQuota();
            quota.setResumeQuota(5);
            quota.setInterviewQuota(3);
            quota.setDailyResumeUsed(0);
            quota.setDailyInterviewUsed(0);

            when(sysUserService.getById(TEST_USER_ID)).thenReturn(user);
            when(userQuotaService.getByUserId(TEST_USER_ID)).thenReturn(quota);

            UserInfoResponse response = authService.getCurrentUserInfo(TEST_USER_ID);

            assertNotNull(response);
            assertEquals(TEST_USER_ID, response.getId());
            assertEquals(TEST_USERNAME, response.getUsername());
            assertEquals("测试用户", response.getNickname());
            assertEquals(0, response.getRole());
            assertEquals(1, response.getStatus());
            assertEquals(java.time.LocalDateTime.of(2026, 5, 18, 10, 0), response.getCreateTime());
            assertEquals(5, response.getResumeQuota());
            assertEquals(3, response.getInterviewQuota());
        }

        @Test
        @DisplayName("should throw when user not found")
        void shouldThrowExceptionWhenUserNotFound() {
            when(sysUserService.getById(TEST_USER_ID)).thenReturn(null);

            BusinessException exception = assertThrows(BusinessException.class,
                    () -> authService.getCurrentUserInfo(TEST_USER_ID));

            assertEquals(ResultCode.NOT_FOUND.getCode(), exception.getCode());
        }
    }

    @Nested
    @DisplayName("resetPasswordBySecurityQuestion")
    class ResetPasswordTests {

        @Test
        @DisplayName("should use same failure message for enumeration sensitive cases")
        void shouldUseSameFailureMessageForEnumerationSensitiveCases() {
            ResetPasswordRequest request = new ResetPasswordRequest();
            request.setUsername(TEST_USERNAME);
            request.setSecurityAnswer("wrong");
            request.setNewPassword("NewPass@123456");
            request.setCaptchaId("captcha-reset");
            request.setCaptchaCode("R7T9");

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

            assertEquals("用户名或凭证信息不正确", notFoundException.getMessage());
            assertEquals("用户名或凭证信息不正确", wrongAnswerException.getMessage());
            verify(captchaService, times(2)).verify("captcha-reset", "R7T9");
        }

        @Test
        @DisplayName("should return fallback security question message when account not enumerable")
        void shouldReturnFallbackSecurityQuestionMessageWhenAccountNotEnumerable() {
            assertEquals("若账户已配置安全问题，可继续输入答案并重置密码",
                    authService.getSecurityQuestion(TEST_USERNAME).getSecurityQuestion());
        }
    }

    @Test
    @DisplayName("should reject weak placeholder secret")
    void jwtPropertiesShouldRejectWeakPlaceholderSecret() {
        JwtProperties properties = new JwtProperties();
        properties.setSecret("dev-secret-key-change-in-production-123456");

        IllegalStateException exception = assertThrows(IllegalStateException.class, properties::validate);
        assertTrue(exception.getMessage().contains("default placeholder"));
    }

    @Test
    @DisplayName("should reject short secret")
    void jwtPropertiesShouldRejectShortSecret() {
        JwtProperties properties = new JwtProperties();
        properties.setSecret("short-secret");

        IllegalStateException exception = assertThrows(IllegalStateException.class, properties::validate);
        assertTrue(exception.getMessage().contains("at least 32 characters"));
    }

    @Test
    @DisplayName("should accept valid secret")
    void jwtPropertiesShouldAcceptValidSecret() {
        JwtProperties properties = new JwtProperties();
        properties.setSecret("12345678901234567890123456789012");

        properties.validate();
    }
}
