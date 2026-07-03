package com.airesume.server.controller;

import com.airesume.server.common.result.Result;
import com.airesume.server.dto.user.UserSettingsRequest;
import com.airesume.server.dto.user.UserSettingsResponse;
import com.airesume.server.service.UserSettingsService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 用户设置控制器。
 * 这里只暴露当前登录用户自己的设置，避免跨用户读取或修改保留策略。
 */
@RestController
@RequestMapping("/api/user/settings")
@RequiredArgsConstructor
public class UserSettingsController {

    private final UserSettingsService userSettingsService;

    @GetMapping
    public Result<UserSettingsResponse> getSettings(Authentication authentication) {
        Long userId = (Long) authentication.getPrincipal();
        return Result.success(userSettingsService.getSettings(userId));
    }

    @PutMapping
    public Result<UserSettingsResponse> saveSettings(@Valid @RequestBody UserSettingsRequest request,
                                                     Authentication authentication) {
        Long userId = (Long) authentication.getPrincipal();
        return Result.success(userSettingsService.saveSettings(userId, request));
    }
}
