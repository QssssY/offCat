package com.airesume.server.controller;

import com.airesume.server.common.constants.UserRoleConstants;
import com.airesume.server.common.exception.BusinessException;
import com.airesume.server.common.result.Result;
import com.airesume.server.dto.admin.CustomAiUsageStatsResponse;
import com.airesume.server.dto.admin.CustomAiUsageTrendResponse;
import com.airesume.server.entity.SysUser;
import com.airesume.server.service.SysUserService;
import com.airesume.server.service.UserAiUsageStatsService;
import java.time.LocalDate;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * 管理端用户自定义 AI 统计接口。
 */
@Slf4j
@RestController
@RequestMapping("/api/admin/custom-ai")
@RequiredArgsConstructor
public class AdminCustomAiStatsController {

    private static final int MAX_PAGE = 10_000;
    private static final int MAX_PAGE_SIZE = 100;

    private final UserAiUsageStatsService userAiUsageStatsService;
    private final SysUserService sysUserService;

    /**
     * 查询用户自定义 AI 日期范围统计。
     *
     * 统计口径：
     * - date 为旧版单日参数，传入时优先按 date 单日查询。
     * - startDate/endDate 为新版范围参数，功能分布和用户明细共享同一范围。
     */
    @GetMapping("/usage-stats")
    public Result<CustomAiUsageStatsResponse> getCustomAiUsageStats(
            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date,
            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate,
            @RequestParam(defaultValue = "1") Integer page,
            @RequestParam(defaultValue = "20") Integer pageSize,
            Authentication authentication) {
        Long userId = (Long) authentication.getPrincipal();
        checkAdminPermission(userId);
        // 管理端分页参数来自请求边界，页码和页大小都做上限收敛，避免极端输入造成 offset 溢出。
        int normalizedPage = Math.min(MAX_PAGE, Math.max(1, page == null ? 1 : page));
        int normalizedPageSize = Math.min(MAX_PAGE_SIZE, Math.max(1, pageSize == null ? 20 : pageSize));
        log.info("Admin query custom AI usage stats, adminUserId: {}, date: {}, startDate: {}, endDate: {}, page: {}, pageSize: {}",
                userId, date, startDate, endDate, normalizedPage, normalizedPageSize);
        return Result.success(userAiUsageStatsService.getDailyStats(
                date, startDate, endDate, normalizedPage, normalizedPageSize));
    }

    /**
     * 查询用户自定义 AI 按日趋势。
     *
     * 日期范围规则由 Service 统一归一化：默认近 7 天、单侧日期按单日、最大 90 天。
     */
    @GetMapping("/usage-trends")
    public Result<CustomAiUsageTrendResponse> getCustomAiUsageTrends(
            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate,
            Authentication authentication) {
        Long userId = (Long) authentication.getPrincipal();
        checkAdminPermission(userId);
        log.info("Admin query custom AI usage trends, adminUserId: {}, startDate: {}, endDate: {}",
                userId, startDate, endDate);
        return Result.success(userAiUsageStatsService.getUsageTrends(startDate, endDate));
    }

    private void checkAdminPermission(Long userId) {
        SysUser user = sysUserService.getById(userId);
        Integer role = user != null ? user.getRole() : null;
        if (role == null || role != UserRoleConstants.ROLE_ADMIN) {
            log.warn("Non-admin user access custom AI stats denied, userId: {}, role: {}", userId, role);
            throw new BusinessException("无权限访问");
        }
    }
}
