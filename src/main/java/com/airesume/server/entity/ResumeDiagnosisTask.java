package com.airesume.server.entity;

import com.airesume.server.common.entity.BaseEntity;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.time.LocalDateTime;

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
     * 诊断子阶段：extracting / ai_analyzing / enhancing。
     * 仅在 status=1（PROCESSING）时有意义，其余状态为 null。
     */
    @TableField("stage")
    private String stage;

    /**
     * AI返回的结构化诊断报告（JSON格式）
     */
    @TableField(value = "diagnosis_result", select = false)
    private String diagnosisResult;

    /**
     * 失败时的异常记录
     */
    @TableField("error_msg")
    private String errorMsg;

    /**
     * 任务进入失败状态的时间，用于计算 24 小时重试窗口，避免被后续维护更新影响。
     */
    @TableField("failed_at")
    private LocalDateTime failedAt;

    /**
     * 简历提取的文本内容
     * 用于缓存PDF解析结果，避免每次查询都重新解析
     */
    @TableField(value = "resume_text", select = false)
    private String resumeText;

    /**
     * 解析模式：TEXT / MULTIMODAL / OCR / MIXED。
     */
    @TableField("parse_mode")
    private String parseMode;

    /**
     * 解析来源提示信息。
     */
    @TableField("parse_message")
    private String parseMessage;

    /**
     * AI 计费来源：platform / user_custom。
     * 异步任务创建时锁定来源，避免执行时用户配置变化导致误走平台 AI。
     */
    @TableField("ai_billing_source")
    private String aiBillingSource;

    /**
     * 创建任务时是否显式要求回退平台 AI。
     */
    @TableField("fallback_to_platform")
    private Integer fallbackToPlatform;
}
