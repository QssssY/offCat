package com.airesume.server.dto.interview;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * 面试历史回放单轮响应。
 * 用于把 interview_chat_log 中的面试官提问、候选人回答和下一条 AI 反馈组织成时间线。
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class InterviewReplayRoundResponse {

    /**
     * 回放轮次，从 1 开始。
     */
    private Integer roundNo;

    /**
     * 本轮面试官问题消息 ID。
     */
    private Long questionMessageId;

    /**
     * 本轮面试官问题内容。
     */
    private String questionContent;

    /**
     * 候选人回答消息 ID。
     */
    private Long answerMessageId;

    /**
     * 候选人回答内容。
     */
    private String answerContent;

    /**
     * AI 对本轮回答的反馈或下一轮追问消息 ID。
     */
    private Long feedbackMessageId;

    /**
     * AI 对本轮回答的反馈和追问内容。
     */
    private String feedbackContent;

    /**
     * 本轮回答时间，用于前端时间线排序与展示。
     */
    private LocalDateTime answerTime;

    /**
     * AI 反馈时间，用于前端展示反馈发生顺序。
     */
    private LocalDateTime feedbackTime;
}
