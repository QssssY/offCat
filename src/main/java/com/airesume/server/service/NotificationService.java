package com.airesume.server.service;

import com.airesume.server.dto.notification.NotificationListResponse;
import com.airesume.server.dto.notification.NotificationVO;
import com.airesume.server.entity.UserNotification;
import com.airesume.server.mapper.UserNotificationMapper;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

/**
 * 站内消息通知服务
 * 提供通知创建、查询、标记已读等能力
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class NotificationService {

    private final UserNotificationMapper userNotificationMapper;

    /** SSE 连接池：userId → SseEmitter */
    private final Map<Long, SseEmitter> emitterMap = new ConcurrentHashMap<>();

    /** SSE 心跳调度器：定期向所有活跃连接发送心跳，防止代理/NLB 断开空闲连接 */
    private final ScheduledExecutorService heartbeatScheduler = Executors.newSingleThreadScheduledExecutor(r -> {
        Thread t = new Thread(r, "sse-heartbeat");
        t.setDaemon(true);
        return t;
    });

    /** 心跳是否已启动（避免重复调度） */
    private volatile boolean heartbeatStarted = false;

    /**
     * 创建通知
     * 使用 REQUIRES_NEW 独立事务，确保即使外层事务回滚，通知也能落库
     * 使用 try-catch 安全降级，不阻断主业务流程
     *
     * @param userId  目标用户ID
     * @param type    通知类型: resume/polish/interview/quota/system
     * @param title   通知标题
     * @param content 通知内容
     * @param bizType 关联业务类型
     * @param bizId   关联业务ID
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void createNotification(Long userId, String type, String title, String content, String bizType, String bizId) {
        try {
            UserNotification notification = new UserNotification();
            notification.setUserId(userId);
            notification.setType(type);
            notification.setTitle(title);
            notification.setContent(content);
            notification.setBizType(bizType);
            notification.setBizId(bizId);
            notification.setReadStatus(0);
            userNotificationMapper.insert(notification);
            log.info("通知创建成功, userId: {}, type: {}, title: {}", userId, type, title);
            // 通过 SSE 实时推送给用户
            sendNotificationToUser(userId, notification);
        } catch (Exception e) {
            log.error("创建通知失败，不影响主业务, userId: {}, type: {}, error: {}", userId, type, e.getMessage());
        }
    }

    /**
     * 创建额度不足通知（带防重）
     * 检查最近 24 小时是否已有同类型未读通知，避免重复创建
     *
     * @param userId 用户ID
     */
    public void createQuotaNotificationIfNeeded(Long userId) {
        if (hasRecentUnreadNotification(userId, "quota")) {
            log.info("最近已有额度不足通知，跳过创建, userId: {}", userId);
            return;
        }
        createNotification(userId, "quota", "额度不足提醒",
                "你的可用次数不足，请查看权益或会员状态。",
                "quota", null);
    }

    /**
     * 统计用户未读通知数量
     *
     * @param userId 用户ID
     * @return 未读数量
     */
    public long countUnread(Long userId) {
        LambdaQueryWrapper<UserNotification> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(UserNotification::getUserId, userId)
                .eq(UserNotification::getReadStatus, 0);
        return userNotificationMapper.selectCount(wrapper);
    }

    /**
     * 单条通知标记已读
     * 通过 userId + id + readStatus=0 三个条件一次 UPDATE 完成，避免额外 SELECT
     * 不存在、不属于当前用户、已读时均幂等返回
     *
     * @param userId         当前用户ID
     * @param notificationId 通知ID
     */
    public void markAsRead(Long userId, Long notificationId) {
        log.info("[通知] 开始标记已读, userId: {}, notificationId: {}", userId, notificationId);
        LambdaUpdateWrapper<UserNotification> updateWrapper = new LambdaUpdateWrapper<>();
        updateWrapper.eq(UserNotification::getId, notificationId)
                .eq(UserNotification::getUserId, userId)
                .eq(UserNotification::getReadStatus, 0)
                .set(UserNotification::getReadStatus, 1)
                .set(UserNotification::getReadTime, LocalDateTime.now());
        int updated = userNotificationMapper.update(null, updateWrapper);
        log.info("[通知] 标记已读结果, notificationId: {}, userId: {}, updatedRows: {}", notificationId, userId, updated);
    }

    /**
     * 全部通知标记已读
     * 只标记当前用户的未读通知，重复调用幂等
     *
     * @param userId 用户ID
     */
    public void markAllAsRead(Long userId) {
        LambdaUpdateWrapper<UserNotification> updateWrapper = new LambdaUpdateWrapper<>();
        updateWrapper.eq(UserNotification::getUserId, userId)
                .eq(UserNotification::getReadStatus, 0)
                .set(UserNotification::getReadStatus, 1)
                .set(UserNotification::getReadTime, LocalDateTime.now());
        userNotificationMapper.update(null, updateWrapper);
        log.info("全部通知已标记已读, userId: {}", userId);
    }

    /**
     * 分页查询当前用户通知列表
     *
     * @param userId     用户ID
     * @param page       页码
     * @param size       每页大小
     * @param readStatus 已读状态筛选（可选）
     * @param type       通知类型筛选（可选）
     * @return 通知列表响应，包含分页数据和未读数量
     */
    public NotificationListResponse listNotifications(Long userId, Integer page, Integer size, Integer readStatus, String type) {
        // 构建查询条件
        LambdaQueryWrapper<UserNotification> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(UserNotification::getUserId, userId);
        if (readStatus != null) {
            wrapper.eq(UserNotification::getReadStatus, readStatus);
        }
        if (type != null && !type.isBlank()) {
            wrapper.eq(UserNotification::getType, type);
        }
        wrapper.orderByDesc(UserNotification::getCreateTime);

        // 分页查询
        Page<UserNotification> pageParam = new Page<>(page, size);
        Page<UserNotification> resultPage = userNotificationMapper.selectPage(pageParam, wrapper);

        // 转换为 VO
        List<NotificationVO> voList = resultPage.getRecords().stream()
                .map(this::toVO)
                .toList();

        // 查询未读数量
        long unreadCount = countUnread(userId);

        return NotificationListResponse.builder()
                .records(voList)
                .total(resultPage.getTotal())
                .unreadCount(unreadCount)
                .build();
    }

    /**
     * 删除单条通知（逻辑删除）
     * 通过 userId + id 双条件确保只能删除自己的通知
     *
     * @param userId         当前用户ID
     * @param notificationId 通知ID
     */
    public void deleteNotification(Long userId, Long notificationId) {
        LambdaUpdateWrapper<UserNotification> wrapper = new LambdaUpdateWrapper<>();
        wrapper.eq(UserNotification::getId, notificationId)
                .eq(UserNotification::getUserId, userId);
        int affected = userNotificationMapper.delete(wrapper);
        if (affected > 0) {
            log.info("通知已删除, userId: {}, notificationId: {}", userId, notificationId);
        } else {
            log.warn("删除通知未命中, userId: {}, notificationId: {}（不存在或不属于当前用户）", userId, notificationId);
        }
    }

    /**
     * 批量删除通知（逻辑删除）
     *
     * @param userId 当前用户ID
     * @param ids    通知ID列表
     */
    public void batchDeleteNotifications(Long userId, List<Long> ids) {
        if (ids == null || ids.isEmpty()) return;
        LambdaUpdateWrapper<UserNotification> wrapper = new LambdaUpdateWrapper<>();
        wrapper.eq(UserNotification::getUserId, userId)
                .in(UserNotification::getId, ids);
        int affected = userNotificationMapper.delete(wrapper);
        log.info("批量删除通知, userId: {}, 请求: {}, 实际删除: {}", userId, ids.size(), affected);
    }

    /**
     * 检查最近24小时是否存在同类型未读通知（用于额度不足防重）
     *
     * @param userId 用户ID
     * @param type   通知类型
     * @return true=已存在，false=不存在
     */
    public boolean hasRecentUnreadNotification(Long userId, String type) {
        LambdaQueryWrapper<UserNotification> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(UserNotification::getUserId, userId)
                .eq(UserNotification::getType, type)
                .eq(UserNotification::getReadStatus, 0)
                .ge(UserNotification::getCreateTime, LocalDateTime.now().minusHours(24));
        return userNotificationMapper.selectCount(wrapper) > 0;
    }

    /**
     * 注册用户的 SSE 连接
     * 如果用户已有连接，会替换旧连接
     *
     * @param userId 用户ID
     * @return SseEmitter 实例
     */
    public SseEmitter registerEmitter(Long userId) {
        // 超时设为 30 分钟，每 30 秒心跳保活
        SseEmitter emitter = new SseEmitter(30 * 60 * 1000L);
        emitterMap.put(userId, emitter);

        // 启动心跳调度（仅首次连接时启动，后续连接共享同一调度器）
        startHeartbeatIfNeeded();

        // 连接完成或超时时清理
        emitter.onCompletion(() -> {
            emitterMap.remove(userId);
            log.info("[SSE] 连接完成, userId: {}", userId);
        });
        emitter.onTimeout(() -> {
            emitterMap.remove(userId);
            log.info("[SSE] 连接超时, userId: {}", userId);
        });
        emitter.onError(e -> {
            emitterMap.remove(userId);
            log.warn("[SSE] 连接异常, userId: {}, error: {}", userId, e.getMessage());
        });

        // 发送初始连接确认 + 心跳保活
        try {
            emitter.send(SseEmitter.event().name("connected").data("ok"));
        } catch (IOException e) {
            log.warn("[SSE] 发送连接确认失败, userId: {}", userId);
        }

        log.info("[SSE] 用户连接注册, userId: {}", userId);
        return emitter;
    }

    /**
     * 注销用户的 SSE 连接
     *
     * @param userId 用户ID
     */
    public void unregisterEmitter(Long userId) {
        SseEmitter emitter = emitterMap.remove(userId);
        if (emitter != null) {
            emitter.complete();
            log.info("[SSE] 用户连接注销, userId: {}", userId);
        }
    }

    /**
     * 通过 SSE 向用户推送新通知
     * 推送失败时自动清理失效连接
     *
     * @param userId       目标用户ID
     * @param notification 新通知实体
     */
    public void sendNotificationToUser(Long userId, UserNotification notification) {
        SseEmitter emitter = emitterMap.get(userId);
        if (emitter == null) {
            return;
        }
        try {
            // 推送未读数和通知摘要
            long unreadCount = countUnread(userId);
            NotificationVO vo = toVO(notification);
            Map<String, Object> payload = Map.of(
                    "unreadCount", unreadCount,
                    "notification", vo
            );
            emitter.send(SseEmitter.event()
                    .name("notification")
                    .data(payload));
            log.info("[SSE] 推送通知成功, userId: {}, notificationId: {}", userId, notification.getId());
        } catch (IOException e) {
            log.warn("[SSE] 推送通知失败，清理连接, userId: {}, error: {}", userId, e.getMessage());
            emitterMap.remove(userId);
        }
    }

    /**
     * 向用户发送未读数更新（用于心跳或同步）
     *
     * @param userId 用户ID
     */
    public void sendUnreadCountUpdate(Long userId) {
        SseEmitter emitter = emitterMap.get(userId);
        if (emitter == null) {
            return;
        }
        try {
            long unreadCount = countUnread(userId);
            emitter.send(SseEmitter.event()
                    .name("unread-count")
                    .data(Map.of("unreadCount", unreadCount)));
        } catch (IOException e) {
            log.warn("[SSE] 推送未读数失败，清理连接, userId: {}, error: {}", userId, e.getMessage());
            emitterMap.remove(userId);
        }
    }

    /**
     * 启动心跳调度（幂理，仅首次调用生效）
     * 每 30 秒向所有活跃连接发送 SSE 注释心跳，防止代理/NLB 断开空闲连接
     */
    private void startHeartbeatIfNeeded() {
        if (heartbeatStarted) {
            return;
        }
        synchronized (this) {
            if (heartbeatStarted) {
                return;
            }
            heartbeatStarted = true;
            heartbeatScheduler.scheduleAtFixedRate(this::sendHeartbeatToAll, 30, 30, TimeUnit.SECONDS);
            log.info("[SSE] 心跳调度已启动，间隔 30 秒");
        }
    }

    /**
     * 向所有活跃 SSE 连接发送心跳
     * 使用 SSE 注释行（以 ':' 开头）作为心跳，不触发前端事件处理
     * 发送失败时清理失效连接
     */
    private void sendHeartbeatToAll() {
        if (emitterMap.isEmpty()) {
            return;
        }
        emitterMap.forEach((userId, emitter) -> {
            try {
                emitter.send(":\n\n");
            } catch (IOException e) {
                log.warn("[SSE] 心跳发送失败，清理连接, userId: {}, error: {}", userId, e.getMessage());
                emitterMap.remove(userId);
            }
        });
    }

    /**
     * 实体转 VO
     */
    private NotificationVO toVO(UserNotification entity) {
        return NotificationVO.builder()
                .id(entity.getId())
                .type(entity.getType())
                .title(entity.getTitle())
                .content(entity.getContent())
                .bizType(entity.getBizType())
                .bizId(entity.getBizId())
                .readStatus(entity.getReadStatus())
                .readTime(entity.getReadTime())
                .createTime(entity.getCreateTime())
                .build();
    }
}
