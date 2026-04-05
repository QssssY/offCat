package com.airesume.server.dto.resume;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * 简历诊断历史响应
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ResumeHistoryResponse {

    /**
     * 记录ID
     */
    private Long id;

    /**
     * 任务ID（内部使用，不展示给用户）
     */
    private String taskId;

    /**
     * 文件名
     */
    private String fileName;

    /**
     * 文件URL
     */
    private String fileUrl;

    /**
     * 目标岗位
     */
    private String jobRole;

    /**
     * 状态（0=排队中，1=解析中，2=已完成，3=失败）
     */
    private Integer status;

    /**
     * 状态描述
     */
    private String statusDesc;

    /**
     * 诊断结果摘要
     */
    private String resultSummary;

    /**
     * 综合评分
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

    /**
     * 完成时间
     */
    private LocalDateTime completeTime;
}
