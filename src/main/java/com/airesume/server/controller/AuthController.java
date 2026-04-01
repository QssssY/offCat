package com.airesume.server.controller;

import com.airesume.server.common.result.Result;
import com.airesume.server.dto.auth.LoginRequest;
import com.airesume.server.dto.auth.LoginResponse;
import com.airesume.server.dto.auth.RegisterRequest;
import com.airesume.server.dto.auth.UserInfoResponse;
import com.airesume.server.service.AuthService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 认证控制器
 * 提供用户注册、登录和获取当前用户信息的接口
 */
@Slf4j
@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;

    /**
     * 用户注册接口
     *
     * @param request 注册请求参数，包含用户名和密码
     * @return 注册成功返回空结果
     */
    @PostMapping("/register")
    public Result<Void> register(@Valid @RequestBody RegisterRequest request) {
        log.info("User registration request, username: {}", request.getUsername());
        authService.register(request);
        return Result.success();
    }

    /**
     * 用户登录接口
     *
     * @param request 登录请求参数，包含用户名和密码
     * @return 登录成功返回 JWT token
     */
    @PostMapping("/login")
    public Result<LoginResponse> login(@Valid @RequestBody LoginRequest request) {
        log.info("User login request, username: {}", request.getUsername());
        LoginResponse response = authService.login(request);
        return Result.success(response);
    }

    /**
     * 获取当前登录用户信息接口
     *
     * @param authentication Spring Security 认证对象，包含当前用户ID
     * @return 当前登录用户的详细信息
     */
    @GetMapping("/me")
    public Result<UserInfoResponse> getCurrentUser(Authentication authentication) {
        Long userId = (Long) authentication.getPrincipal();
        log.info("Get current user info request, userId: {}", userId);
        UserInfoResponse userInfo = authService.getCurrentUserInfo(userId);
        return Result.success(userInfo);
    }

}
