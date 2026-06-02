package com.airesume.server.entity;

import com.airesume.server.common.entity.BaseEntity;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 面试会话实体类
 * 对应数据库表 interview_session
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("interview_session")
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
     * 岗位编码（用于关联 prompt 配置）
     */
    @TableField("job_role_code")
    private String jobRoleCode;

    /**
     * 难度级别
     */
    @TableField("difficulty")
    private Integer difficulty;

    /**
     * 面试模式：normal-普通面试，stress-压力面试，也可保存固定面试官人设。
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
    @TableField(value = "evaluation_report", select = false)
    private String evaluationReport;

    /**
     * 开场白是否已生成：0-未生成，1-已生成
     */
    @TableField("opening_generated")
    private Integer openingGenerated;

    /**
     * 反馈模式：immediate-每题即时反馈，after_interview-面完统一复盘（默认）
     * 用户在创建会话时选择，不影响存量会话。
     */
    @TableField("feedback_mode")
    private String feedbackMode;

    /**
     * 交互方式：0-文字面试，1-语音面试。
     * 该字段只记录创建会话时选择的交互入口，会话进行中不切换。
     */
    @TableField("interaction_type")
    private Integer interactionType;

    /**
     * AI 计费来源：platform / user_custom / platform_fallback。
     * 用于锁定自定义 AI 面试会话首次手动切平台时只扣一次平台面试额度。
     */
    @TableField("ai_billing_source")
    private String aiBillingSource;
}
