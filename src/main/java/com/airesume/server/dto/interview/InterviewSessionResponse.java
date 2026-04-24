package com.airesume.server.dto.interview;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 面试会话详情响应DTO
 * 用于返回面试会话的详细信息
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class InterviewSessionResponse {

    /**
     * 主键ID
     */
    private Long id;

    /**
     * 会话ID
     */
    private String sessionId;

    /**
     * 用户ID
     */
    private Long userId;

    /**
     * 面试岗位
     */
    private String jobRole;

    /**
     * 岗位编码
     */
    private String jobRoleCode;

    /**
     * 难度级别
     */
    private Integer difficulty;

    /**
     * 难度描述
     */
    private String difficultyDesc;

    /**
     * 面试模式：normal-普通面试，stress-压力面试
     */
    private String interviewMode;

    /**
     * 面试模式描述
     */
    private String interviewModeDesc;

    /**
     * 会话状态：0-进行中，1-已结束
     */
    private Integer status;

    /**
     * 状态描述
     */
    private String statusDesc;

    /**
     * AI综合打分
     */
    private Integer comprehensiveScore;

    /**
     * 面试结束后的综合评价报告（JSON格式）
     */
    private String evaluationReport;

    /**
     * 聊天记录列表
     */
    private List<ChatMessageResponse> chatLogs;

    /**
     * 创建时间
     */
    private LocalDateTime createTime;

    /**
     * 更新时间
     */
    private LocalDateTime updateTime;
}
