package com.airesume.server.controller;

import com.airesume.server.common.result.Result;
import com.airesume.server.common.util.BatchValidator;
import com.airesume.server.dto.admin.AdminNotificationCreateRequest;
import com.airesume.server.dto.admin.AdminNotificationResponse;
import com.airesume.server.entity.SysAdminNotification;
import com.airesume.server.entity.SysUser;
import com.airesume.server.mapper.UserNotificationMapper;
import com.airesume.server.service.NotificationService;
import com.airesume.server.service.SysAdminNotificationService;
import com.airesume.server.service.SysUserService;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Slf4j
@RestController
@RequestMapping("/api/admin/notifications")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')")
public class AdminNotificationController {

    private final SysAdminNotificationService sysAdminNotificationService;
    private final NotificationService notificationService;
    private final SysUserService sysUserService;
    private final UserNotificationMapper userNotificationMapper;

    @GetMapping
    public Result<Map<String, Object>> getNotificationList(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int size,
            Authentication authentication) {
        log.info("Admin get notification list, page: {}, size: {}", page, size);
        Page<SysAdminNotification> pageParam = new Page<>(page, size);
        Page<SysAdminNotification> result = sysAdminNotificationService.lambdaQuery()
                .orderByDesc(SysAdminNotification::getCreateTime)
                .page(pageParam);
        List<AdminNotificationResponse> responses = result.getRecords().stream()
                .map(this::buildResponse)
                .collect(Collectors.toList());
        Map<String, Object> data = new HashMap<>();
        data.put("records", responses);
        data.put("total", (int) result.getTotal());
        data.put("page", (int) result.getCurrent());
        data.put("size", (int) result.getSize());
        return Result.success(data);
    }

    @GetMapping("/{id}")
    public Result<AdminNotificationResponse> getNotificationDetail(@PathVariable Long id,
                                                                    Authentication authentication) {
        SysAdminNotification notification = sysAdminNotificationService.getById(id);
        if (notification == null) {
            return Result.error("公告不存在");
        }
        return Result.success(buildResponse(notification));
    }

    @PostMapping
    @Transactional(rollbackFor = Exception.class)
    public Result<Long> createAndSendNotification(@Valid @RequestBody AdminNotificationCreateRequest request,
                                                   Authentication authentication) {
        Long operatorUserId = (Long) authentication.getPrincipal();
        log.info("Admin create notification, title: {}, type: {}, targetType: {}",
                request.getTitle(), request.getType(), request.getTargetType());

        SysAdminNotification notification = new SysAdminNotification();
        notification.setTitle(request.getTitle().trim());
        notification.setContent(request.getContent().trim());
        notification.setType(request.getType());
        notification.setTargetType(request.getTargetType());
        int status = request.getStatus() != null ? request.getStatus() : 0;
        notification.setStatus(status);
        if (status == 1) {
            notification.setPublishedAt(LocalDateTime.now());
        }
        sysAdminNotificationService.save(notification);

        if (status == 1) {
            sendToTargetUsers(notification, operatorUserId);
        }

        log.info("Notification created, id: {}, status: {}", notification.getId(), status);
        return Result.success("公告创建成功", notification.getId());
    }

    @PutMapping("/{id}/publish")
    @Transactional(rollbackFor = Exception.class)
    public Result<Void> publishNotification(@PathVariable Long id, Authentication authentication) {
        Long operatorUserId = (Long) authentication.getPrincipal();
        SysAdminNotification notification = sysAdminNotificationService.getById(id);
        if (notification == null) {
            return Result.error("公告不存在");
        }
        if (notification.getStatus() == 1) {
            return Result.error("公告已发布，不能重复发布");
        }

        notification.setStatus(1);
        notification.setPublishedAt(LocalDateTime.now());
        sysAdminNotificationService.updateById(notification);

        sendToTargetUsers(notification, operatorUserId);

        log.info("Notification published, id: {}", id);
        return Result.success("公告发布成功", null);
    }

    @PutMapping("/batch/publish")
    @Transactional(rollbackFor = Exception.class)
    public Result<Void> publishNotificationsBatch(@RequestBody List<Long> ids, Authentication authentication) {
        Long operatorUserId = (Long) authentication.getPrincipal();
        List<Long> safeIds = BatchValidator.validate(ids);
        log.info("Admin batch publish notifications, ids: {}", safeIds);
        List<SysAdminNotification> notifications = sysAdminNotificationService.listByIds(safeIds);
        for (SysAdminNotification notification : notifications) {
            // 批量发布保持幂等：已发布公告跳过，草稿公告才补齐发布时间并广播。
            if (notification.getStatus() == 1) {
                continue;
            }
            notification.setStatus(1);
            notification.setPublishedAt(LocalDateTime.now());
            sysAdminNotificationService.updateById(notification);
            sendToTargetUsers(notification, operatorUserId);
        }
        return Result.success("公告批量发布成功", null);
    }

    @DeleteMapping("/{id}")
    public Result<Void> deleteNotification(@PathVariable Long id, Authentication authentication) {
        log.info("Admin delete notification, id: {}", id);
        sysAdminNotificationService.removeById(id);
        return Result.success("公告删除成功", null);
    }

    @PostMapping("/batch-delete")
    public Result<Void> deleteNotificationsBatch(@RequestBody List<Long> ids, Authentication authentication) {
        List<Long> safeIds = BatchValidator.validate(ids);
        log.info("Admin batch delete notifications, ids: {}", safeIds);
        sysAdminNotificationService.removeByIds(safeIds);
        return Result.success("公告批量删除成功", null);
    }

    private void sendToTargetUsers(SysAdminNotification notification, Long operatorUserId) {
        LambdaQueryWrapper<SysUser> userWrapper = new LambdaQueryWrapper<>();
        userWrapper.eq(SysUser::getStatus, 1);
        if ("vip".equals(notification.getTargetType())) {
            userWrapper.eq(SysUser::getRole, 1);
        } else if ("normal".equals(notification.getTargetType())) {
            userWrapper.eq(SysUser::getRole, 0);
        }
        List<SysUser> targetUsers = sysUserService.list(userWrapper);

        String type = switch (notification.getType()) {
            case "activity" -> "activity";
            case "update" -> "update";
            case "maintenance" -> "maintenance";
            default -> "system";
        };

        for (SysUser user : targetUsers) {
            notificationService.createNotification(
                    user.getId(),
                    type,
                    notification.getTitle(),
                    notification.getContent(),
                    "broadcast",
                    String.valueOf(notification.getId())
            );
        }
        log.info("Broadcast sent to {} users, notificationId: {}", targetUsers.size(), notification.getId());
    }

    private AdminNotificationResponse buildResponse(SysAdminNotification entity) {
        return AdminNotificationResponse.builder()
                .id(entity.getId())
                .title(entity.getTitle())
                .content(entity.getContent())
                .type(entity.getType())
                .typeDesc(getTypeDesc(entity.getType()))
                .targetType(entity.getTargetType())
                .targetTypeDesc(getTargetTypeDesc(entity.getTargetType()))
                .status(entity.getStatus())
                .statusDesc(entity.getStatus() == 1 ? "已发布" : "草稿")
                .publishedAt(entity.getPublishedAt())
                .createTime(entity.getCreateTime())
                .updateTime(entity.getUpdateTime())
                .build();
    }

    private String getTypeDesc(String type) {
        return switch (type) {
            case "system" -> "系统公告";
            case "activity" -> "活动通知";
            case "update" -> "版本更新";
            case "maintenance" -> "维护通知";
            default -> "未知";
        };
    }

    private String getTargetTypeDesc(String targetType) {
        return switch (targetType) {
            case "all" -> "全部用户";
            case "vip" -> "VIP用户";
            case "normal" -> "普通用户";
            default -> "未知";
        };
    }
}
