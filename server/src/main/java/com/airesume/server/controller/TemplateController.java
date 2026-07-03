package com.airesume.server.controller;

import com.airesume.server.common.result.Result;
import com.airesume.server.service.UserQuotaService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 模板配额控制器。
 * 模板库为纯前端静态数据，后端仅负责使用配额的检查与扣减。
 */
@Slf4j
@RestController
@RequestMapping("/api/template")
@RequiredArgsConstructor
public class TemplateController {

    private final UserQuotaService userQuotaService;

    /**
     * 使用模板前检查并扣减配额。
     */
    @PostMapping("/use")
    public Result<Void> useTemplate(Authentication authentication) {
        Long userId = (Long) authentication.getPrincipal();
        log.info("模板使用配额检查, userId: {}", userId);

        userQuotaService.checkAndDeductTemplateQuota(userId);
        return Result.success(null);
    }
}
