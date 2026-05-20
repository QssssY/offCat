package com.airesume.server.entity;

import com.airesume.server.common.entity.BaseEntity;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * AI 简历润色记录实体。
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("resume_polish_record")
@Entity
@Table(name = "resume_polish_record")
public class ResumePolishRecord extends BaseEntity {

    /**
     * 用户 ID。
     */
    @TableField("user_id")
    private Long userId;

    /**
     * 简历诊断任务 ID。
     */
    @TableField("resume_task_id")
    private Long resumeTaskId;

    /**
     * 原始简历文本快照。
     */
    @TableField("source_resume_text")
    private String sourceResumeText;

    /**
     * JD 文本快照。
     */
    @TableField("jd_text")
    private String jdText;

    /**
     * 润色后的简历文本。
     */
    @TableField("polished_resume_text")
    private String polishedResumeText;

    /**
     * 编辑后的简历文档 JSON（结构化模型：header + sections）。
     */
    @TableField("document_json")
    private String documentJson;

    /**
     * 编辑后的简历纯文本。
     */
    @TableField("edited_plain_text")
    private String editedPlainText;

    /**
     * 修改说明。
     */
    @TableField("modification_notes")
    private String modificationNotes;

    /**
     * 来源类型。
     */
    @TableField("source_type")
    private String sourceType;
}
