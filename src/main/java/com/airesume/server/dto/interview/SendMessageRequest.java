package com.airesume.server.dto.interview;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

/**
 * 发送面试消息请求DTO
 * 用于接收用户发送的面试消息
 */
@Data
public class SendMessageRequest {

    /**
     * 会话ID
     */
    @NotBlank(message = "会话ID不能为空")
    private String sessionId;

    /**
     * 用户消息内容
     */
    @NotBlank(message = "消息内容不能为空")
    private String content;
}
