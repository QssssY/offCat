package com.airesume.server.controller;

import com.airesume.server.common.result.Result;
import com.airesume.server.dto.auth.SecurityQuestionResponse;
import com.airesume.server.dto.user.AccountDeleteRequest;
import com.airesume.server.service.UserAccountService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 用户账号管理控制器。
 * 提供设置中心账号注销等当前用户高风险操作入口。
 */
@Slf4j
@RestController
@RequestMapping("/api/user/account")
@RequiredArgsConstructor
public class UserAccountController {

    private final UserAccountService userAccountService;

    /**
     * 获取当前登录账号的安全问题。
     * 仅用于注销账号前的已登录身份校验，不暴露给未登录忘记密码流程。
     */
    @GetMapping("/security-question")
    public Result<SecurityQuestionResponse> getCurrentSecurityQuestion(Authentication authentication) {
        Long userId = (Long) authentication.getPrincipal();
        SecurityQuestionResponse response = new SecurityQuestionResponse();
        response.setSecurityQuestion(userAccountService.getCurrentSecurityQuestion(userId));
        return Result.success(response);
    }

    /**
     * 注销当前账号。
     * 该接口必须登录，并通过当前密码、确认密码和安全问题答案后才执行数据清理与账号匿名化。
     */
    @PostMapping("/delete")
    public Result<Void> deleteAccount(@Valid @RequestBody AccountDeleteRequest request,
                                      Authentication authentication) {
        Long userId = (Long) authentication.getPrincipal();
        log.info("账号注销请求, userId: {}", userId);
        userAccountService.deleteAccount(userId, request);
        return Result.success();
    }
}
