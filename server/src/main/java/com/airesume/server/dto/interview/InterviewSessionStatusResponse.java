package com.airesume.server.dto.interview;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * 面试会话轻量状态响应。
 * 用于开场白和报告生成轮询，只返回状态字段，避免反复加载聊天记录和评估报告大字段。
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class InterviewSessionStatusResponse {

    /** 会话ID。 */
    private String sessionId;

    /** 会话状态：0-进行中，1-已结束。 */
    private Integer status;

    /** 状态描述。 */
    private String statusDesc;

    /** 综合评分，报告生成前为空。 */
    private Integer comprehensiveScore;

    /** 开场白是否仍在生成中。 */
    private Boolean openingPending;

    /** 评估报告是否已写入。 */
    private Boolean reportReady;

    /** 会话最近更新时间。 */
    private LocalDateTime updateTime;
}