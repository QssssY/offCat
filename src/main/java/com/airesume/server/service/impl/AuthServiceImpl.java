package com.airesume.server.service.impl;

import com.airesume.server.common.exception.BusinessException;
import com.airesume.server.common.result.ResultCode;
import com.airesume.server.dto.auth.LoginRequest;
import com.airesume.server.dto.auth.LoginResponse;
import com.airesume.server.dto.auth.RegisterRequest;
import com.airesume.server.dto.auth.UserInfoResponse;
import com.airesume.server.entity.SysUser;
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
 * 认证服务实现类
 * 实现用户注册、登录和获取用户信息的核心业务逻辑
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

    /**
     * 用户注册
     * 1. 检查用户名是否已存在
     * 2. 创建新用户并加密密码
     * 3. 初始化用户额度记录
     *
     * @param request 注册请求参数，包含用户名和密码
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public void register(RegisterRequest request) {
        String username = request.getUsername();
        log.info("Processing user registration, username: {}", username);

        // 检查用户名是否已存在
        if (sysUserService.existsByUsername(username)) {
            log.warn("Registration failed, username already exists: {}", username);
            throw new BusinessException("用户名已存在");
        }

        // 创建新用户
        SysUser user = new SysUser();
        user.setUsername(username);
        user.setPassword(passwordEncoder.encode(request.getPassword()));
        user.setRole(0); // 0-普通用户
        user.setStatus(1); // 1-正常状态
        sysUserService.save(user);
        log.info("User created, userId: {}", user.getId());

        // 初始化用户额度记录
        userQuotaService.initUserQuota(user.getId());

        log.info("User registered successfully: {}", username);
    }

    /**
     * 用户登录
     * 1. 根据用户名查询用户
     * 2. 验证密码是否正确
     * 3. 检查用户状态是否正常
     * 4. 生成 JWT token
     *
     * @param request 登录请求参数，包含用户名和密码
     * @return 登录响应，包含 JWT token 及相关信息
     */
    @Override
    public LoginResponse login(LoginRequest request) {
        String username = request.getUsername();
        log.info("Processing user login, username: {}", username);

        // 根据用户名查询用户
        SysUser user = sysUserService.getByUsername(username);
        if (user == null) {
            log.warn("Login failed, user not found: {}", username);
            throw new BusinessException(ResultCode.PARAM_ERROR.getCode(), "用户名或密码错误");
        }

        // 验证密码
        if (!passwordEncoder.matches(request.getPassword(), user.getPassword())) {
            log.warn("Login failed, password incorrect, username: {}", username);
            throw new BusinessException(ResultCode.PARAM_ERROR.getCode(), "用户名或密码错误");
        }

        // 检查用户状态
        if (user.getStatus() == 0) {
            log.warn("Login failed, account banned, username: {}", username);
            throw new BusinessException("账号已被封禁");
        }

        // 生成 JWT token
        String token = jwtUtil.generateToken(user.getId(), user.getUsername(), user.getRole());
        log.info("User logged in successfully: {}", user.getUsername());

        return new LoginResponse(token, jwtProperties.getPrefix().trim(), jwtProperties.getExpiration() / 1000);
    }

    /**
     * 获取当前登录用户信息
     *
     * @param userId 用户ID
     * @return 用户详细信息
     */
    @Override
    public UserInfoResponse getCurrentUserInfo(Long userId) {
        log.debug("Fetching user info, userId: {}", userId);

        SysUser user = sysUserService.getById(userId);
        if (user == null) {
            log.warn("User not found when getting current user info, userId: {}", userId);
            throw new BusinessException(ResultCode.NOT_FOUND);
        }

        log.debug("User info fetched successfully, userId: {}, username: {}", userId, user.getUsername());
        return new UserInfoResponse(
                user.getId(),
                user.getUsername(),
                user.getRole(),
                user.getStatus(),
                user.getVipExpireTime()
        );
    }

}
