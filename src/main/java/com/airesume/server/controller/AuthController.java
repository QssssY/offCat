package com.airesume.server.controller;

import com.airesume.server.common.result.Result;
import com.airesume.server.dto.auth.LoginRequest;
import com.airesume.server.dto.auth.LoginResponse;
import com.airesume.server.dto.auth.NicknameUpdateRequest;
import com.airesume.server.dto.auth.PasswordUpdateRequest;
import com.airesume.server.dto.auth.RegisterRequest;
import com.airesume.server.dto.auth.ResetPasswordRequest;
import com.airesume.server.dto.auth.SecurityQuestionResponse;
import com.airesume.server.dto.auth.UserInfoResponse;
import com.airesume.server.service.AuthService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
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

    /**
     * 更新用户昵称接口
     *
     * @param request 昵称更新请求参数，包含新昵称
     * @param authentication Spring Security 认证对象，包含当前用户ID
     * @return 更新成功返回空结果
     */
    @PutMapping("/nickname")
    public Result<Void> updateNickname(
            @Valid @RequestBody NicknameUpdateRequest request,
            Authentication authentication) {
        Long userId = (Long) authentication.getPrincipal();
        log.info("Update nickname request, userId: {}, nickname: {}", userId, request.getNickname());
        authService.updateNickname(userId, request.getNickname());
        return Result.success();
    }

    /**
     * 修改密码接口
     *
     * @param request 密码修改请求参数，包含原密码和新密码
     * @param authentication Spring Security 认证对象，包含当前用户ID
     * @return 修改成功返回空结果
     */
    @PutMapping("/password")
    public Result<Void> updatePassword(
            @Valid @RequestBody PasswordUpdateRequest request,
            Authentication authentication) {
        Long userId = (Long) authentication.getPrincipal();
        log.info("Update password request, userId: {}", userId);
        authService.updatePassword(userId, request);
        return Result.success();
    }

    /**
     * 获取用户的安全问题（忘记密码流程第一步）
     *
     * @param username 用户名
     * @return 安全问题文本
     */
    @GetMapping("/security-question")
    public Result<SecurityQuestionResponse> getSecurityQuestion(@RequestParam String username) {
        log.info("Get security question request, username: {}", username);
        return Result.success(authService.getSecurityQuestion(username));
    }

    /**
     * 通过安全问题验证重置密码（忘记密码流程第二步）
     *
     * @param request 重置密码请求参数，包含用户名、安全问题答案和新密码
     * @return 重置成功返回空结果
     */
    @PostMapping("/reset-password")
    public Result<Void> resetPassword(@Valid @RequestBody ResetPasswordRequest request) {
        log.info("Reset password by security question request, username: {}", request.getUsername());
        authService.resetPasswordBySecurityQuestion(request);
        return Result.success();
    }

}
