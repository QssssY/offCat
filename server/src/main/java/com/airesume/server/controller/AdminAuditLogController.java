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
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.time.LocalDateTime;
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
            @RequestParam(required = false) String operatorName,
            @RequestParam(required = false) String targetUsername,
            @RequestParam(required = false) @DateTimeFormat(pattern = "yyyy-MM-dd") LocalDate startDate,
            @RequestParam(required = false) @DateTimeFormat(pattern = "yyyy-MM-dd") LocalDate endDate,
            @RequestParam(required = false) Integer roleChangeType,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int size,
            Authentication authentication) {
        log.info("Admin get audit logs, userId: {}, operatorName: {}, targetUsername: {}, dateRange: [{}~{}], roleChangeType: {}, page: {}, size: {}",
                userId, operatorName, targetUsername, startDate, endDate, roleChangeType, page, size);

        Page<UserRightsChangeLog> pageParam = new Page<>(page, size);
        LambdaQueryWrapper<UserRightsChangeLog> wrapper = new LambdaQueryWrapper<>();

        // 目标用户ID筛选（直接传userId或通过用户名模糊查找）
        if (userId != null) {
            wrapper.eq(UserRightsChangeLog::getUserId, userId);
        } else if (targetUsername != null && !targetUsername.isBlank()) {
            List<Long> matchedUserIds = findUserIdsByUsername(targetUsername);
            if (matchedUserIds.isEmpty()) {
                // 用户名不匹配任何用户，返回空结果
                return Result.success(buildEmptyPage(pageParam));
            }
            wrapper.in(UserRightsChangeLog::getUserId, matchedUserIds);
        }

        // 操作人用户名模糊筛选
        if (operatorName != null && !operatorName.isBlank()) {
            List<Long> matchedOperatorIds = findUserIdsByUsername(operatorName);
            if (matchedOperatorIds.isEmpty()) {
                return Result.success(buildEmptyPage(pageParam));
            }
            wrapper.in(UserRightsChangeLog::getOperatorUserId, matchedOperatorIds);
        }

        // 日期范围筛选
        if (startDate != null) {
            wrapper.ge(UserRightsChangeLog::getCreateTime, startDate.atStartOfDay());
        }
        if (endDate != null) {
            wrapper.lt(UserRightsChangeLog::getCreateTime, endDate.plusDays(1).atStartOfDay());
        }

        // 角色变更类型筛选
        if (roleChangeType != null) {
            applyRoleChangeTypeFilter(wrapper, roleChangeType);
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

    /**
     * 根据角色变更类型添加筛选条件。
     * 1=升级会员(before=0,after=1), 2=降级普通(before=1,after=0), 3=设为管理员(after=9)
     */
    private void applyRoleChangeTypeFilter(LambdaQueryWrapper<UserRightsChangeLog> wrapper, int type) {
        switch (type) {
            case 1 -> {
                wrapper.eq(UserRightsChangeLog::getBeforeRole, 0);
                wrapper.eq(UserRightsChangeLog::getAfterRole, 1);
            }
            case 2 -> {
                wrapper.eq(UserRightsChangeLog::getBeforeRole, 1);
                wrapper.eq(UserRightsChangeLog::getAfterRole, 0);
            }
            case 3 -> wrapper.eq(UserRightsChangeLog::getAfterRole, 9);
            default -> log.warn("未知的角色变更类型: {}", type);
        }
    }

    /**
     * 通过用户名模糊查找用户ID列表，最多返回100个。
     */
    private List<Long> findUserIdsByUsername(String username) {
        return sysUserService.list(
                        new LambdaQueryWrapper<SysUser>()
                                .like(SysUser::getUsername, username.trim())
                                .select(SysUser::getId)
                ).stream()
                .map(SysUser::getId)
                .limit(100)
                .collect(Collectors.toList());
    }

    /** 构建空分页结果 */
    private Map<String, Object> buildEmptyPage(Page<?> pageParam) {
        Map<String, Object> data = new HashMap<>();
        data.put("records", List.of());
        data.put("total", 0);
        data.put("page", (int) pageParam.getCurrent());
        data.put("size", (int) pageParam.getSize());
        return data;
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
