package com.airesume.server.dto.interview;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * 发送面试消息请求 DTO。
 */
@Data
public class SendMessageRequest {

    /**
     * 会话 ID。
     * 当前主链路已经通过路径参数传递，这里只保留作兼容字段，不再要求必填。
     */
    private String sessionId;

    /**
     * 用户消息内容。
     * 限制最大长度防止异常超长输入推高 AI Token 成本，同时保留语音转写后的合理上限。
     */
    @NotBlank(message = "消息内容不能为空")
    @Size(max = 5000, message = "单条消息内容不能超过 5000 个字符")
    private String content;

    /**
     * 反馈模式：after_interview-面完统一复盘；immediate-每题即时反馈。
     * 不作为会话配置落库，仅影响本次 AI 回复口径。
     */
    @Pattern(
            regexp = "immediate|after_interview",
            message = "反馈模式只支持 immediate/after_interview"
    )
    private String feedbackMode;
}
