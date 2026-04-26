package com.airesume.server.service.impl;

import com.airesume.server.common.constants.QuotaConstants;
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
import com.airesume.server.service.AuthService;
import com.airesume.server.service.SysUserService;
import com.airesume.server.service.UserQuotaService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 认证服务实现类。
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AuthServiceImpl implements AuthService {

    private final SysUserService sysUserService;
    private final UserQuotaService userQuotaService;
    private final PasswordEncoder passwordEncoder;
    private final JwtUtil jwtUtil;
    private final JwtProperties jwtProperties;

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
        sysUserService.save(user);
        log.info("User created, userId: {}", user.getId());

        userQuotaService.initUserQuota(user.getId());
        log.info("User registered successfully: {}", username);
    }

    @Override
    public LoginResponse login(LoginRequest request) {
        String username = request.getUsername();
        log.info("Processing user login, username: {}", username);

        SysUser user = sysUserService.getByUsername(username);
        if (user == null) {
            log.warn("Login failed, user not found: {}", username);
            throw new BusinessException(ResultCode.PARAM_ERROR.getCode(), "用户名或密码错误");
        }

        if (!passwordEncoder.matches(request.getPassword(), user.getPassword())) {
            log.warn("Login failed, password incorrect, username: {}", username);
            throw new BusinessException(ResultCode.PARAM_ERROR.getCode(), "用户名或密码错误");
        }

        if (user.getStatus() == 0) {
            log.warn("Login failed, account banned, username: {}", username);
            throw new BusinessException("账号已被封禁");
        }

        String token = jwtUtil.generateToken(user.getId(), user.getUsername(), user.getRole());
        log.info("User logged in successfully: {}", user.getUsername());
        return new LoginResponse(token, jwtProperties.getPrefix().trim(), jwtProperties.getExpiration() / 1000);
    }

    @Override
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

    /**
     * 安全获取整数值。
     */
    private int safeValue(Integer value) {
        return value == null ? 0 : value;
    }

    /**
     * 生成默认昵称。
     */
    private String generateRandomNickname() {
        String prefix = "用户_";
        String chars = "ABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789";
        StringBuilder sb = new StringBuilder(prefix);
        java.util.Random random = new java.util.Random();
        for (int i = 0; i < 6; i++) {
            sb.append(chars.charAt(random.nextInt(chars.length())));
        }
        return sb.toString();
    }
}
