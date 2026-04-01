package com.airesume.server.entity;

import com.airesume.server.common.entity.BaseEntity;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 简历诊断任务实体类
 * 对应数据库表 resume_diagnosis_task
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("resume_diagnosis_task")
public class ResumeDiagnosisTask extends BaseEntity {

    /**
     * 提交用户ID
     */
    @TableField("user_id")
    private Long userId;

    /**
     * PDF简历存储地址
     */
    @TableField("file_url")
    private String fileUrl;

    /**
     * 任务状态：0-排队中，1-解析分析中，2-完成，3-失败
     */
    @TableField("status")
    private Integer status;

    /**
     * AI返回的结构化诊断报告（JSON格式）
     */
    @TableField("diagnosis_result")
    private String diagnosisResult;

    /**
     * 失败时的异常记录
     */
    @TableField("error_msg")
    private String errorMsg;
}
