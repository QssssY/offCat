package com.airesume.server.controller;

import com.airesume.server.common.exception.BusinessException;
import com.airesume.server.common.result.Result;
import com.airesume.server.dto.notification.NotificationListResponse;
import com.airesume.server.service.NotificationService;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.Authentication;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.util.List;
import java.util.Map;

/**
 * 站内消息通知控制器
 * 提供通知列表查询、未读数量、标记已读等接口
 */
@Slf4j
@Validated
@RestController
@RequestMapping("/api/user/notifications")
@RequiredArgsConstructor
public class NotificationController {

    private final NotificationService notificationService;

    /**
     * 查询当前用户通知列表（分页+筛选）
     *
     * @param authentication 当前登录用户身份
     * @param pageNum        页码，默认1
     * @param size           每页大小，默认5，最大100
     * @param readStatus     已读状态筛选（可选）
     * @param type           通知类型筛选（可选）
     * @return 通知列表响应
     */
    @GetMapping
    public Result<NotificationListResponse> listNotifications(
            Authentication authentication,
            @RequestParam(defaultValue = "1") @Min(1) Integer pageNum,
            @RequestParam(defaultValue = "5") @Min(1) @Max(100) Integer size,
            @RequestParam(required = false) Integer readStatus,
            @RequestParam(required = false) String type) {
        Long userId = (Long) authentication.getPrincipal();
        // 限制每页大小上限，避免恶意大查询
        int safeSize = Math.min(Math.max(size, 1), 100);
        log.info("[通知] 查询通知列表, userId: {}, pageNum: {}, size: {}", userId, pageNum, safeSize);
        NotificationListResponse response = notificationService.listNotifications(userId, pageNum, safeSize, readStatus, type);
        return Result.success(response);
    }

    /**
     * 获取当前用户未读通知数量
     *
     * @param authentication 当前登录用户身份
     * @return 未读数量
     */
    @GetMapping("/unread-count")
    public Result<Map<String, Long>> getUnreadCount(Authentication authentication) {
        Long userId = (Long) authentication.getPrincipal();
        long unreadCount = notificationService.countUnread(userId);
        return Result.success(Map.of("unreadCount", unreadCount));
    }

    /**
     * 单条通知标记已读
     *
     * @param authentication 当前登录用户身份
     * @param id             通知ID
     * @return 操作结果
     */
    @PostMapping("/{id}/read")
    public Result<Void> markAsRead(Authentication authentication, @PathVariable Long id) {
        Long userId = (Long) authentication.getPrincipal();
        log.info("[通知] 标记已读, userId: {}, notificationId: {}", userId, id);
        notificationService.markAsRead(userId, id);
        return Result.success();
    }

    /**
     * 全部通知标记已读
     *
     * @param authentication 当前登录用户身份
     * @return 操作结果
     */
    @PostMapping("/read-all")
    public Result<Void> markAllAsRead(Authentication authentication) {
        Long userId = (Long) authentication.getPrincipal();
        log.info("[通知] 全部标记已读, userId: {}", userId);
        notificationService.markAllAsRead(userId);
        return Result.success();
    }

    /**
     * 删除单条通知（逻辑删除）
     *
     * @param authentication 当前登录用户身份
     * @param id             通知ID
     * @return 操作结果
     */
    @DeleteMapping("/{id}")
    public Result<Void> deleteNotification(Authentication authentication, @PathVariable Long id) {
        Long userId = (Long) authentication.getPrincipal();
        log.info("[通知] 删除单条通知, userId: {}, notificationId: {}", userId, id);
        notificationService.deleteNotification(userId, id);
        return Result.success();
    }

    /**
     * 批量删除通知（逻辑删除）
     * 使用 POST 而非 DELETE，避免部分代理/网关剥离 DELETE 请求体
     *
     * @param authentication 当前登录用户身份
     * @param ids            通知ID列表
     * @return 操作结果
     */
    @PostMapping("/batch-delete")
    public Result<Void> batchDelete(Authentication authentication, @RequestBody List<Long> ids) {
        Long userId = (Long) authentication.getPrincipal();
        if (ids == null || ids.isEmpty()) {
            return Result.success();
        }
        if (ids.size() > 100) {
            throw new BusinessException("批量删除最多支持100条");
        }
        int safeSize = Math.min(ids.size(), 100);
        List<Long> safeIds = ids.subList(0, safeSize);
        log.info("[通知] 批量删除通知, userId: {}, count: {}", userId, safeIds.size());
        notificationService.batchDeleteNotifications(userId, safeIds);
        return Result.success();
    }

    /**
     * SSE 通知推送流
     * 建立长连接后，新通知会实时推送给用户
     *
     * @param authentication 当前登录用户身份
     * @return SSE 发射器
     */
    @GetMapping("/stream")
    public SseEmitter stream(Authentication authentication) {
        Long userId = (Long) authentication.getPrincipal();
        log.info("[SSE] 用户建立通知流连接, userId: {}", userId);
        return notificationService.registerEmitter(userId);
    }

    /**
     * 断开 SSE 连接（用户退出登录时调用）
     *
     * @param authentication 当前登录用户身份
     * @return 操作结果
     */
    @DeleteMapping("/stream")
    public Result<Void> disconnectStream(Authentication authentication) {
        Long userId = (Long) authentication.getPrincipal();
        log.info("[SSE] 用户断开通知流连接, userId: {}", userId);
        notificationService.unregisterEmitter(userId);
        return Result.success();
    }
}
