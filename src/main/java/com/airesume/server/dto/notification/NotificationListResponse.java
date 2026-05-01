package com.airesume.server.dto.notification;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * 通知列表响应，包含分页数据和未读数量
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class NotificationListResponse {

    /** 通知列表 */
    private List<NotificationVO> records;

    /** 总记录数 */
    private Long total;

    /** 当前用户未读通知数量 */
    private Long unreadCount;
}
