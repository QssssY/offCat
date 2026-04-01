package com.airesume.server.dto.interview;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 发送面试消息响应DTO
 * 用于返回面试官的回复
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SendMessageResponse {

    /**
     * 会话ID
     */
    private String sessionId;

    /**
     * 面试官回复内容
     */
    private String replyContent;
}
