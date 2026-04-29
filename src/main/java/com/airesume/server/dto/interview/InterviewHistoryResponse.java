package com.airesume.server.dto.interview;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * 面试历史响应
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class InterviewHistoryResponse {

    /**
     * 记录ID
     */
    private Long id;

    /**
     * 会话ID（内部使用，不展示给用户）
     */
    private String sessionId;

    /**
     * 面试岗位
     */
    private String jobRole;

    /**
     * 难度等级（1=初级，2=中级，3=高级）
     */
    private Integer difficulty;

    /**
     * 难度描述
     */
    private String difficultyDesc;

    /**
     * 面试模式（normal=普通面试，stress=压力面试，job_targeted=岗位定向模拟）
     */
    private String interviewMode;

    /**
     * 面试模式描述
     */
    private String interviewModeDesc;

    /**
     * 状态（0=进行中，1=已结束）
     */
    private Integer status;

    /**
     * 状态描述
     */
    private String statusDesc;

    /**
     * 综合评分
     */
    private Integer comprehensiveScore;

    /**
     * 消息数量
     */
    private Integer messageCount;

    /**
     * 是否为岗位定向模拟面试。
     */
    private Boolean jobTargeted;

    /**
     * 岗位定向来源类型。
     */
    private String sourceType;

    /**
     * 创建时间
     */
    private LocalDateTime createTime;

    /**
     * 更新时间
     */
    private LocalDateTime updateTime;
}
