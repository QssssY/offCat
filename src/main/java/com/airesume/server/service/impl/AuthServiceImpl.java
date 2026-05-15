package com.airesume.server.service.impl;

import com.airesume.server.common.constants.QuotaConstants;
import com.airesume.server.common.exception.BusinessException;
import com.airesume.server.common.result.ResultCode;
import com.airesume.server.dto.auth.LoginRequest;
import com.airesume.server.dto.auth.LoginResponse;
import com.airesume.server.dto.auth.PasswordUpdateRequest;
import com.airesume.server.dto.auth.RegisterRequest;
import com.airesume.server.dto.auth.ResetPasswordRequest;
import com.airesume.server.dto.auth.SecurityQuestionResponse;
import com.airesume.server.dto.auth.SecurityQuestionUpdateRequest;
import com.airesume.server.dto.auth.UserInfoResponse;
import com.airesume.server.entity.SysUser;
import com.airesume.server.entity.UserQuota;
import com.airesume.server.infrastructure.security.JwtProperties;
import com.airesume.server.infrastructure.security.JwtUtil;
import com.airesume.server.service.AuthService;
import com.airesume.server.service.SysUserService;
import com.airesume.server.service.UserQuotaService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;

/**
 * 认证服务实现。
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AuthServiceImpl implements AuthService {

    private static final String LOGIN_ATTEMPTS_KEY_PREFIX = "login:attempts:";
    private static final int MAX_LOGIN_ATTEMPTS = 5;
    private static final long LOGIN_LOCKOUT_MINUTES = 15;
    private static final String LOGIN_FAILURE_MESSAGE = "用户名或密码错误";
    private static final String RESET_PASSWORD_FAILURE_MESSAGE = "用户名或安全问题答案不正确";

    private final SysUserService sysUserService;
    private final UserQuotaService userQuotaService;
    private final PasswordEncoder passwordEncoder;
    private final JwtUtil jwtUtil;
    private final JwtProperties jwtProperties;

    @Autowired(required = false)
    private StringRedisTemplate stringRedisTemplate;

    /**
     * Redis 故障时使用本地计数兜底，避免登录限流完全失效。
     */
    private final Map<String, LoginAttemptRecord> localLoginAttempts = new ConcurrentHashMap<>();

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void register(RegisterRequest request) {
        String username = request.getUsername();
        log.info("Processing user registration, username: {}", username);

        if (sysUserService.existsByUsername(username)) {
            log.warn("Registration failed, username already exists: {}", username);
            throw new BusinessException("用户名已存在");
        }

        SysUser user = new SysUser();
        user.setUsername(username);
        user.setNickname(generateRandomNickname());
        user.setPassword(passwordEncoder.encode(request.getPassword()));
        user.setRole(0);
        user.setStatus(1);

        if (request.getSecurityQuestion() != null && !request.getSecurityQuestion().isBlank()
                && request.getSecurityAnswer() != null && !request.getSecurityAnswer().isBlank()) {
            user.setSecurityQuestion(request.getSecurityQuestion().trim());
            user.setSecurityAnswer(passwordEncoder.encode(request.getSecurityAnswer().trim()));
        }

        sysUserService.save(user);
        log.info("User created, userId: {}", user.getId());

        userQuotaService.initUserQuota(user.getId());
        log.info("User registered successfully: {}", username);
    }

    @Override
    public LoginResponse login(LoginRequest request) {
        String username = request.getUsername();
        log.info("Processing user login, username: {}", username);

        String attemptsKey = LOGIN_ATTEMPTS_KEY_PREFIX + username;
        int attempts = getLoginAttempts(attemptsKey);
        if (attempts >= MAX_LOGIN_ATTEMPTS) {
            log.warn("Login blocked due to too many failed attempts, username: {}", username);
            throw new BusinessException("登录失败次数过多，请 " + LOGIN_LOCKOUT_MINUTES + " 分钟后再试");
        }

        SysUser user = sysUserService.getByUsername(username);
        if (user == null) {
            incrementLoginAttempts(attemptsKey);
            log.warn("Login failed, user not found: {}", username);
            throw new BusinessException(ResultCode.PARAM_ERROR.getCode(), LOGIN_FAILURE_MESSAGE);
        }

        if (!passwordEncoder.matches(request.getPassword(), user.getPassword())) {
            incrementLoginAttempts(attemptsKey);
            log.warn("Login failed, password incorrect, username: {}", username);
            throw new BusinessException(ResultCode.PARAM_ERROR.getCode(), LOGIN_FAILURE_MESSAGE);
        }

        if (user.getStatus() == 0) {
            log.warn("Login failed, account banned, username: {}", username);
            throw new BusinessException("账号已被封禁");
        }

        clearLoginAttempts(attemptsKey);
        String token = jwtUtil.generateToken(user.getId(), user.getUsername(), user.getRole());
        log.info("User logged in successfully: {}", user.getUsername());
        return new LoginResponse(token, jwtProperties.getPrefix().trim(), jwtProperties.getExpiration() / 1000);
    }

    private int getLoginAttempts(String key) {
        if (stringRedisTemplate == null) {
            log.warn("Redis unavailable, fallback to in-memory login rate limit, key={}", key);
            return getLocalLoginAttempts(key);
        }
        try {
            String attemptsStr = stringRedisTemplate.opsForValue().get(key);
            return attemptsStr != null ? Integer.parseInt(attemptsStr) : 0;
        } catch (NumberFormatException e) {
            log.warn("Redis login attempts format is invalid, reset local state, key={}", key);
            clearLoginAttempts(key);
            return 0;
        } catch (Exception e) {
            log.warn("Redis read failed, fallback to in-memory login rate limit, key={}", key, e);
            return getLocalLoginAttempts(key);
        }
    }

    /**
     * 登录失败次数优先写 Redis；若 Redis 不可用则自动切换到本地兜底。
     */
    private void incrementLoginAttempts(String key) {
        if (stringRedisTemplate == null) {
            incrementLocalLoginAttempts(key);
            return;
        }
        try {
            Long newAttempts = stringRedisTemplate.opsForValue().increment(key);
            if (newAttempts != null && newAttempts == 1) {
                stringRedisTemplate.expire(key, LOGIN_LOCKOUT_MINUTES, TimeUnit.MINUTES);
            }
        } catch (Exception e) {
            log.warn("Redis write failed, fallback to in-memory login rate limit, key={}", key, e);
            incrementLocalLoginAttempts(key);
        }
    }

    private void clearLoginAttempts(String key) {
        localLoginAttempts.remove(key);
        if (stringRedisTemplate == null) {
            return;
        }
        try {
            stringRedisTemplate.delete(key);
        } catch (Exception e) {
            log.warn("Failed to clear login attempts from Redis, key={}", key, e);
        }
    }

    /**
     * Redis 故障时，用本地带过期时间的计数保护登录接口。
     */
    private int getLocalLoginAttempts(String key) {
        LoginAttemptRecord record = localLoginAttempts.get(key);
        if (record == null) {
            return 0;
        }
        if (record.isExpired()) {
            localLoginAttempts.remove(key);
            return 0;
        }
        return record.attempts();
    }

    private void incrementLocalLoginAttempts(String key) {
        localLoginAttempts.compute(key, (ignored, existingRecord) -> {
            if (existingRecord == null || existingRecord.isExpired()) {
                return new LoginAttemptRecord(1, Instant.now().plusSeconds(LOGIN_LOCKOUT_MINUTES * 60));
            }
            return new LoginAttemptRecord(existingRecord.attempts() + 1, existingRecord.expireAt());
        });
    }

    @Override
    @Cacheable(value = "auth:userInfo", key = "#userId", sync = true)
    public UserInfoResponse getCurrentUserInfo(Long userId) {
        log.debug("Fetching user info, userId: {}", userId);

        SysUser user = sysUserService.getById(userId);
        if (user == null) {
            log.warn("User not found when getting current user info, userId: {}", userId);
            throw new BusinessException(ResultCode.NOT_FOUND);
        }

        int resumeQuota = userQuotaService.getRemainingResumeQuota(userId);
        int interviewQuota = userQuotaService.getRemainingInterviewQuota(userId);
        UserQuota userQuota = userQuotaService.getByUserId(userId);
        userQuotaService.refreshDailyQuotaIfNeeded(userId, userQuota);
        int vipDailyResumeQuota = userQuota == null
                ? 0
                : Math.max(0, QuotaConstants.VIP_USER_DAILY_RESUME_LIMIT - safeValue(userQuota.getDailyResumeUsed()));
        int vipDailyInterviewQuota = userQuota == null
                ? 0
                : Math.max(0, QuotaConstants.VIP_USER_DAILY_INTERVIEW_LIMIT - safeValue(userQuota.getDailyInterviewUsed()));

        log.debug("User info fetched successfully, userId: {}, username: {}, resumeQuota: {}, interviewQuota: {}",
                userId, user.getUsername(), resumeQuota, interviewQuota);

        return UserInfoResponse.builder()
                .id(user.getId())
                .username(user.getUsername())
                .nickname(user.getNickname())
                .role(user.getRole())
                .status(user.getStatus())
                .membershipPlanCode(user.getMembershipPlanCode())
                .vipExpireTime(user.getVipExpireTime())
                .resumeQuota(resumeQuota)
                .interviewQuota(interviewQuota)
                .vipDailyResumeQuota(vipDailyResumeQuota)
                .vipDailyInterviewQuota(vipDailyInterviewQuota)
                .build();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    @CacheEvict(value = "auth:userInfo", key = "#userId")
    public void updateNickname(Long userId, String nickname) {
        log.info("Updating nickname, userId: {}, nickname: {}", userId, nickname);
        SysUser user = sysUserService.getById(userId);
        if (user == null) {
            throw new BusinessException(ResultCode.NOT_FOUND);
        }
        user.setNickname(nickname);
        sysUserService.updateById(user);
        log.info("Nickname updated successfully, userId: {}", userId);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    @CacheEvict(value = "auth:userInfo", key = "#userId")
    public void updatePassword(Long userId, PasswordUpdateRequest request) {
        log.info("Updating password, userId: {}", userId);
        SysUser user = sysUserService.getById(userId);
        if (user == null) {
            throw new BusinessException(ResultCode.NOT_FOUND);
        }

        if (!passwordEncoder.matches(request.getOldPassword(), user.getPassword())) {
            log.warn("Password update failed, old password incorrect, userId: {}", userId);
            throw new BusinessException("原密码不正确");
        }

        if (passwordEncoder.matches(request.getNewPassword(), user.getPassword())) {
            log.warn("Password update failed, new password same as old, userId: {}", userId);
            throw new BusinessException("新密码不能与原密码相同");
        }

        user.setPassword(passwordEncoder.encode(request.getNewPassword()));
        sysUserService.updateById(user);
        log.info("Password updated successfully, userId: {}", userId);
    }

    @Override
    public SecurityQuestionResponse getSecurityQuestion(String username) {
        log.info("Getting security question for username: {}", username);
        SysUser user = sysUserService.getByUsername(username);
        if (user == null) {
            log.warn("Get security question failed, user not found or no question: {}", username);
            throw new BusinessException("该用户未设置安全问题，无法找回密码");
        }
        if (user.getSecurityQuestion() == null || user.getSecurityQuestion().isBlank()) {
            log.warn("Get security question failed, user has no security question set: {}", username);
            throw new BusinessException("该用户未设置安全问题，无法找回密码");
        }
        SecurityQuestionResponse response = new SecurityQuestionResponse();
        response.setSecurityQuestion(user.getSecurityQuestion());
        return response;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void resetPasswordBySecurityQuestion(ResetPasswordRequest request) {
        String username = request.getUsername();
        log.info("Resetting password by security question, username: {}", username);

        SysUser user = sysUserService.getByUsername(username);
        if (user == null) {
            log.warn("Reset password failed, user not found: {}", username);
            throw new BusinessException(RESET_PASSWORD_FAILURE_MESSAGE);
        }
        if (user.getSecurityQuestion() == null || user.getSecurityAnswer() == null) {
            log.warn("Reset password failed, user has no security question set: {}", username);
            throw new BusinessException(RESET_PASSWORD_FAILURE_MESSAGE);
        }

        // 统一密码找回失败文案，避免通过错误差异枚举用户名或安全问题状态。
        if (!passwordEncoder.matches(request.getSecurityAnswer(), user.getSecurityAnswer())) {
            log.warn("Reset password failed, security answer incorrect, username: {}", username);
            throw new BusinessException(RESET_PASSWORD_FAILURE_MESSAGE);
        }

        if (passwordEncoder.matches(request.getNewPassword(), user.getPassword())) {
            log.warn("Reset password failed, new password same as old, username: {}", username);
            throw new BusinessException("新密码不能与原密码相同");
        }

        user.setPassword(passwordEncoder.encode(request.getNewPassword()));
        sysUserService.updateById(user);
        log.info("Password reset successfully by security question, username: {}", username);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    @CacheEvict(value = "auth:userInfo", key = "#userId")
    public void updateSecurityQuestion(Long userId, SecurityQuestionUpdateRequest request) {
        log.info("Updating security question, userId: {}", userId);
        SysUser user = sysUserService.getById(userId);
        if (user == null) {
            throw new BusinessException(ResultCode.NOT_FOUND);
        }

        if (!passwordEncoder.matches(request.getOldPassword(), user.getPassword())) {
            log.warn("Update security question failed, old password incorrect, userId: {}", userId);
            throw new BusinessException("原密码不正确");
        }

        user.setSecurityQuestion(request.getSecurityQuestion().trim());
        user.setSecurityAnswer(passwordEncoder.encode(request.getSecurityAnswer().trim()));
        sysUserService.updateById(user);
        log.info("Security question updated successfully, userId: {}", userId);
    }

    private int safeValue(Integer value) {
        return value == null ? 0 : value;
    }

    private String generateRandomNickname() {
        String prefix = "用户_";
        String chars = "ABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789";
        StringBuilder sb = new StringBuilder(prefix);
        java.util.concurrent.ThreadLocalRandom random = java.util.concurrent.ThreadLocalRandom.current();
        for (int i = 0; i < 6; i++) {
            sb.append(chars.charAt(random.nextInt(chars.length())));
        }
        return sb.toString();
    }

    private record LoginAttemptRecord(int attempts, Instant expireAt) {
        private boolean isExpired() {
            return Instant.now().isAfter(expireAt);
        }
    }
}
