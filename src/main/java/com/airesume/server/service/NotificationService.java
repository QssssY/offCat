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

import java.time.LocalDateTime;
import java.util.List;

/**
 * 站内消息通知服务
 * 提供通知创建、查询、标记已读等能力
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class NotificationService {

    private final UserNotificationMapper userNotificationMapper;

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
        LambdaUpdateWrapper<UserNotification> updateWrapper = new LambdaUpdateWrapper<>();
        updateWrapper.eq(UserNotification::getId, notificationId)
                .eq(UserNotification::getUserId, userId)
                .eq(UserNotification::getReadStatus, 0)
                .set(UserNotification::getReadStatus, 1)
                .set(UserNotification::getReadTime, LocalDateTime.now());
        int updated = userNotificationMapper.update(null, updateWrapper);
        if (updated > 0) {
            log.info("通知已标记已读, notificationId: {}, userId: {}", notificationId, userId);
        }
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
