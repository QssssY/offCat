package com.airesume.server.entity;

import com.airesume.server.common.entity.BaseEntity;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 面试会话实体类
 * 对应数据库表 interview_session
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("interview_session")
@Entity
@Table(name = "interview_session")
public class InterviewSession extends BaseEntity {

    /**
     * 会话ID，对应Redis中的会话Key
     */
    @TableField("session_id")
    private String sessionId;

    /**
     * 面试用户ID
     */
    @TableField("user_id")
    private Long userId;

    /**
     * 面试岗位
     */
    @TableField("job_role")
    private String jobRole;

    /**
     * 难度级别
     */
    @TableField("difficulty")
    private Integer difficulty;

    /**
     * 面试模式：normal-普通面试，stress-压力面试
     */
    @TableField("interview_mode")
    private String interviewMode;

    /**
     * 会话状态：0-进行中，1-已结束
     */
    @TableField("status")
    private Integer status;

    /**
     * AI综合打分
     */
    @TableField("comprehensive_score")
    private Integer comprehensiveScore;

    /**
     * 面试结束后的综合评价报告（JSON格式）
     */
    @TableField("evaluation_report")
    private String evaluationReport;
}
