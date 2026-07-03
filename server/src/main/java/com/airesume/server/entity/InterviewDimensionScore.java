package com.airesume.server.entity;

import com.airesume.server.common.entity.BaseEntity;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 面试维度评分实体。
 * 每次面试产生 6 行维度记录，用于成长中心雷达图和盲区分析。
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("interview_dimension_score")
public class InterviewDimensionScore extends BaseEntity {

    /** 用户ID */
    @TableField("user_id")
    private Long userId;

    /** 面试会话ID */
    @TableField("session_id")
    private String sessionId;

    /** 维度标识：technicalDepth/projectExpression/communication/problemSolving/pressureResistance/jobMatch */
    @TableField("dimension_key")
    private String dimensionKey;

    /** 维度分数 0-100 */
    @TableField("score")
    private Integer score;

    /** 维度评价说明 */
    @TableField("comment")
    private String comment;

    /** 加分项列表（JSON 字符串） */
    @TableField("strengths")
    private String strengths;

    /** 扣分项列表（JSON 字符串） */
    @TableField("weaknesses")
    private String weaknesses;
}
