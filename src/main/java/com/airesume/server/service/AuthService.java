package com.airesume.server.service;

import com.airesume.server.dto.auth.LoginRequest;
import com.airesume.server.dto.auth.LoginResponse;
import com.airesume.server.dto.auth.PasswordUpdateRequest;
import com.airesume.server.dto.auth.RegisterRequest;
import com.airesume.server.dto.auth.ResetPasswordRequest;
import com.airesume.server.dto.auth.SecurityQuestionResponse;
import com.airesume.server.dto.auth.SecurityQuestionUpdateRequest;
import com.airesume.server.dto.auth.UserInfoResponse;

/**
 * 认证服务接口
 * 定义用户注册、登录和获取用户信息的核心业务方法
 */
public interface AuthService {

    /**
     * 用户注册
     * 创建新用户账号，并初始化用户额度记录
     *
     * @param request 注册请求参数，包含用户名和密码
     */
    void register(RegisterRequest request);

    /**
     * 用户登录
     * 验证用户名和密码，验证成功后生成 JWT token
     *
     * @param request 登录请求参数，包含用户名和密码
     * @return 登录响应，包含 JWT token 及相关信息
     */
    LoginResponse login(LoginRequest request);

    /**
     * 获取当前登录用户信息
     *
     * @param userId 用户ID
     * @return 用户详细信息
     */
    UserInfoResponse getCurrentUserInfo(Long userId);

    /**
     * 更新用户昵称
     *
     * @param userId 用户ID
     * @param nickname 新昵称
     */
    void updateNickname(Long userId, String nickname);

    /**
     * 修改用户密码
     * 验证原密码后更新为新密码
     *
     * @param userId 用户ID
     * @param request 密码修改请求参数，包含原密码和新密码
     */
    void updatePassword(Long userId, PasswordUpdateRequest request);

    /**
     * 获取用户的安全问题（忘记密码流程第一步）
     *
     * @param username 用户名
     * @return 安全问题响应
     */
    SecurityQuestionResponse getSecurityQuestion(String username);

    /**
     * 通过安全问题验证重置密码（忘记密码流程第二步）
     *
     * @param request 重置密码请求参数
     */
    void resetPasswordBySecurityQuestion(ResetPasswordRequest request);

    /**
     * 修改安全问题和答案
     * 验证原密码后更新安全问题和答案
     *
     * @param userId  当前用户ID
     * @param request 修改请求参数，包含原密码、安全问题和安全答案
     */
    void updateSecurityQuestion(Long userId, SecurityQuestionUpdateRequest request);

}
