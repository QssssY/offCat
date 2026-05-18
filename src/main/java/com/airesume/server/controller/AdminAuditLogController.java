package com.airesume.server.controller;

import com.airesume.server.common.result.Result;
import com.airesume.server.dto.admin.AuditLogResponse;
import com.airesume.server.entity.SysUser;
import com.airesume.server.entity.UserRightsChangeLog;
import com.airesume.server.service.SysUserService;
import com.airesume.server.service.UserRightsChangeLogService;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Slf4j
@RestController
@RequestMapping("/api/admin/audit-logs")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')")
public class AdminAuditLogController {

    private final UserRightsChangeLogService userRightsChangeLogService;
    private final SysUserService sysUserService;

    @GetMapping
    public Result<Map<String, Object>> getAuditLogs(
            @RequestParam(required = false) Long userId,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int size,
            Authentication authentication) {
        log.info("Admin get audit logs, userId: {}, page: {}, size: {}", userId, page, size);

        Page<UserRightsChangeLog> pageParam = new Page<>(page, size);
        LambdaQueryWrapper<UserRightsChangeLog> wrapper = new LambdaQueryWrapper<>();
        if (userId != null) {
            wrapper.eq(UserRightsChangeLog::getUserId, userId);
        }
        wrapper.orderByDesc(UserRightsChangeLog::getCreateTime);
        Page<UserRightsChangeLog> result = userRightsChangeLogService.page(pageParam, wrapper);

        List<AuditLogResponse> records = result.getRecords().stream()
                .map(this::buildResponse)
                .collect(Collectors.toList());

        Map<String, Object> data = new HashMap<>();
        data.put("records", records);
        data.put("total", (int) result.getTotal());
        data.put("page", (int) result.getCurrent());
        data.put("size", (int) result.getSize());
        return Result.success(data);
    }

    private AuditLogResponse buildResponse(UserRightsChangeLog logEntry) {
        SysUser targetUser = sysUserService.getById(logEntry.getUserId());
        SysUser operator = sysUserService.getById(logEntry.getOperatorUserId());
        return AuditLogResponse.builder()
                .id(logEntry.getId())
                .userId(logEntry.getUserId())
                .username(targetUser != null ? targetUser.getUsername() : "未知")
                .operatorUserId(logEntry.getOperatorUserId())
                .operatorName(operator != null ? operator.getUsername() : "未知")
                .beforeRoleDesc(getRoleDesc(logEntry.getBeforeRole()))
                .afterRoleDesc(getRoleDesc(logEntry.getAfterRole()))
                .beforeMembershipPlanCode(logEntry.getBeforeMembershipPlanCode())
                .afterMembershipPlanCode(logEntry.getAfterMembershipPlanCode())
                .beforeVipExpireTime(logEntry.getBeforeVipExpireTime() != null ?
                        logEntry.getBeforeVipExpireTime().toString() : null)
                .afterVipExpireTime(logEntry.getAfterVipExpireTime() != null ?
                        logEntry.getAfterVipExpireTime().toString() : null)
                .remark(logEntry.getRemark())
                .createTime(logEntry.getCreateTime())
                .build();
    }

    private String getRoleDesc(Integer role) {
        if (role == null) return "无";
        return switch (role) {
            case 0 -> "普通用户";
            case 1 -> "会员用户";
            case 9 -> "管理员";
            default -> "未知";
        };
    }
}
