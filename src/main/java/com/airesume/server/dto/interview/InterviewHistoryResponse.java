package com.airesume.server.dto.interview;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * 面试历史记录响应DTO
 * 用于返回用户的面试历史列表
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class InterviewHistoryResponse {

    /**
     * 主键ID
     */
    private Long id;

    /**
     * 会话ID
     */
    private String sessionId;

    /**
     * 面试岗位
     */
    private String jobRole;

    /**
     * 难度级别
     */
    private Integer difficulty;

    /**
     * 难度描述
     */
    private String difficultyDesc;

    /**
     * 会话状态：0-进行中，1-已结束
     */
    private Integer status;

    /**
     * 状态描述
     */
    private String statusDesc;

    /**
     * AI综合打分（已结束时才有）
     */
    private Integer comprehensiveScore;

    /**
     * 创建时间
     */
    private LocalDateTime createTime;

    /**
     * 更新时间
     */
    private LocalDateTime updateTime;
}
