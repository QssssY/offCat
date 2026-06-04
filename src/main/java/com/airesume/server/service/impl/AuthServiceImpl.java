package com.airesume.server.service.impl;

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
import com.airesume.server.entity.MembershipPlan;
import com.airesume.server.entity.SysUser;
import com.airesume.server.entity.UserQuota;
import com.airesume.server.infrastructure.security.JwtProperties;
import com.airesume.server.infrastructure.security.JwtUtil;
import com.airesume.server.service.AuthService;
import com.airesume.server.service.CaptchaService;
import com.airesume.server.service.SysUserService;
import com.airesume.server.service.UserQuotaService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.LocalDateTime;
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
    private static final String RESET_PASSWORD_FAILURE_MESSAGE = "用户名或凭证信息不正确";
    private static final String SECURITY_QUESTION_LOOKUP_MESSAGE = "若账户已配置安全问题，可继续输入答案并重置密码";

    private final SysUserService sysUserService;
    private final UserQuotaService userQuotaService;
    private final PasswordEncoder passwordEncoder;
    private final JwtUtil jwtUtil;
    private final JwtProperties jwtProperties;
    private final CaptchaService captchaService;

    @Autowired(required = false)
    private StringRedisTemplate stringRedisTemplate;

    /**
     * Redis 故障时使用本地计数兜底，避免登录限流完全失效。
     */
    private final Map<String, LoginAttemptRecord> localLoginAttempts = new ConcurrentHashMap<>();

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void register(RegisterRequest request) {
        captchaService.verify(request.getCaptchaId(), request.getCaptchaCode());

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

    /** 连续失败多少次后要求输入验证码 */
    private static final int CAPTCHA_REQUIRED_ATTEMPTS = 3;

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

        // 渐进式验证码：前 2 次失败不要求验证码，第 3 次起必须输入
        if (attempts >= CAPTCHA_REQUIRED_ATTEMPTS) {
            if (request.getCaptchaId() == null || request.getCaptchaId().isBlank()
                    || request.getCaptchaCode() == null || request.getCaptchaCode().isBlank()) {
                throw new BusinessException("请先完成验证码验证");
            }
            captchaService.verify(request.getCaptchaId(), request.getCaptchaCode());
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

        autoUnbanIfExpired(user);
        if (user.getStatus() == 0) {
            log.warn("Login failed, account banned, username: {}", username);
            throw new BusinessException(ResultCode.PARAM_ERROR.getCode(), LOGIN_FAILURE_MESSAGE);
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
     * 登录失败次数优先写入 Redis，Redis 不可用时自动切换到本地兜底。
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
    private void autoUnbanIfExpired(SysUser user) {
        if (user == null
                || !Integer.valueOf(0).equals(user.getStatus())
                || user.getBannedUntil() == null
                || user.getBannedUntil().isAfter(LocalDateTime.now())) {
            return;
        }
        user.setStatus(1);
        user.setBanReason(null);
        user.setBannedUntil(null);
        user.setBannedBy(null);
        user.setBannedTime(null);
        sysUserService.updateById(user);
        log.info("Expired temporary ban auto-unlocked during login, userId: {}", user.getId());
    }

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

    /**
     * 定时清理 Redis 故障兜底时产生的本地登录失败记录，避免冷门账号的过期记录长期留在内存中。
     */
    @Scheduled(fixedDelayString = "${app.auth.local-login-attempt-cleanup-interval-ms:1800000}")
    void cleanupExpiredLocalLoginAttempts() {
        localLoginAttempts.entrySet().removeIf(entry -> entry.getValue().isExpired());
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

        // 一次查询 user_quota，避免 getRemainingResumeQuota / getRemainingInterviewQuota / getByUserId 各查一次
        UserQuota userQuota = userQuotaService.getByUserId(userId);
        userQuotaService.refreshDailyQuotaIfNeeded(userId, userQuota);
        int resumeQuota = userQuota == null ? 0 : Math.max(0, safeValue(userQuota.getResumeQuota()));
        int interviewQuota = userQuota == null ? 0 : Math.max(0, safeValue(userQuota.getInterviewQuota()));

        // 一次查询会员套餐，提取所有每日限额，避免 6 次重复 VIP + plan 查询
        MembershipPlan plan = sysUserService.getActiveMembershipPlan(userId);
        int vipDailyResumeLimit = plan == null || plan.getResumeQuota() == null ? 0 : Math.max(0, plan.getResumeQuota());
        int vipDailyInterviewLimit = plan == null || plan.getInterviewQuota() == null ? 0 : Math.max(0, plan.getInterviewQuota());
        int vipDailyResumeQuota = userQuota == null
                ? 0
                : Math.max(0, vipDailyResumeLimit - safeValue(userQuota.getDailyResumeUsed()));
        int vipDailyInterviewQuota = userQuota == null
                ? 0
                : Math.max(0, vipDailyInterviewLimit - safeValue(userQuota.getDailyInterviewUsed()));

        int vipDailyPolishQuota = userQuota == null
                ? 0
                : Math.max(0, (plan == null || plan.getDailyPolishLimit() == null ? 0 : plan.getDailyPolishLimit()) - safeValue(userQuota.getDailyPolishUsed()));
        int vipDailyJdMatchQuota = userQuota == null
                ? 0
                : Math.max(0, (plan == null || plan.getDailyJdMatchLimit() == null ? 0 : plan.getDailyJdMatchLimit()) - safeValue(userQuota.getDailyJdMatchUsed()));
        int vipDailyTemplateQuota = userQuota == null
                ? 0
                : Math.max(0, (plan == null || plan.getDailyTemplateLimit() == null ? 0 : plan.getDailyTemplateLimit()) - safeValue(userQuota.getDailyTemplateUsed()));
        int vipDailyOfferQuota = userQuota == null
                ? 0
                : Math.max(0, (plan == null || plan.getDailyOfferLimit() == null ? 0 : plan.getDailyOfferLimit()) - safeValue(userQuota.getDailyOfferUsed()));

        // 非VIP用户（或VIP已过期）的4种免费额度剩余
        boolean isNonVip = plan == null;
        Integer freePolishLeft = isNonVip && userQuota != null ? Math.max(0, safeValue(userQuota.getFreePolishLeft())) : null;
        Integer freeJdMatchLeft = isNonVip && userQuota != null ? Math.max(0, safeValue(userQuota.getFreeJdMatchLeft())) : null;
        Integer freeTemplateLeft = isNonVip && userQuota != null ? Math.max(0, safeValue(userQuota.getFreeTemplateLeft())) : null;
        Integer freeOfferLeft = isNonVip && userQuota != null ? Math.max(0, safeValue(userQuota.getFreeOfferLeft())) : null;

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
                .createTime(user.getCreateTime())
                .resumeQuota(resumeQuota)
                .interviewQuota(interviewQuota)
                .vipDailyResumeQuota(vipDailyResumeQuota)
                .vipDailyInterviewQuota(vipDailyInterviewQuota)
                .vipDailyPolishQuota(vipDailyPolishQuota)
                .vipDailyJdMatchQuota(vipDailyJdMatchQuota)
                .vipDailyTemplateQuota(vipDailyTemplateQuota)
                .vipDailyOfferQuota(vipDailyOfferQuota)
                .freePolishLeft(freePolishLeft)
                .freeJdMatchLeft(freeJdMatchLeft)
                .freeTemplateLeft(freeTemplateLeft)
                .freeOfferLeft(freeOfferLeft)
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
        SecurityQuestionResponse response = new SecurityQuestionResponse();
        response.setSecurityQuestion(SECURITY_QUESTION_LOOKUP_MESSAGE);
        return response;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void resetPasswordBySecurityQuestion(ResetPasswordRequest request) {
        captchaService.verify(request.getCaptchaId(), request.getCaptchaCode());

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

        // 统一失败文案，避免通过差异化错误枚举用户名或安全问题状态。
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
