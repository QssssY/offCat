package com.airesume.server.entity;

import com.airesume.server.common.entity.BaseEntity;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 岗位 JD 对比分析记录实体。
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("resume_job_match_record")
@Entity
@Table(name = "resume_job_match_record")
public class ResumeJobMatchRecord extends BaseEntity {

    /**
     * 用户 ID。
     */
    @TableField("user_id")
    private Long userId;

    /**
     * 关联的简历诊断任务 ID。
     */
    @TableField("resume_task_id")
    private Long resumeTaskId;

    /**
     * 简历文本快照。
     */
    @TableField("resume_text")
    private String resumeText;

    /**
     * JD 文本快照。
     */
    @TableField("jd_text")
    private String jdText;

    /**
     * 匹配度评分。
     */
    @TableField("match_score")
    private Integer matchScore;

    /**
     * 结构化分析结果。
     */
    @TableField("analysis_result")
    private String analysisResult;
}
