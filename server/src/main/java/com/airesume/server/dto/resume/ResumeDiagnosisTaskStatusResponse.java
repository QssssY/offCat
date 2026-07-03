package com.airesume.server.dto.resume;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 简历诊断任务轻量状态响应。
 * 仅用于等待页轮询，不返回简历原文、诊断 JSON 等大字段。
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ResumeDiagnosisTaskStatusResponse implements Serializable {

    private static final long serialVersionUID = 1L;

    /** 任务 ID，使用字符串避免前端长整型精度丢失。 */
    private String taskId;

    /** 任务归属用户 ID，用于前端必要展示和调试。 */
    private Long userId;

    /** 任务状态：0-排队中，1-处理中，2-完成，3-失败。 */
    private Integer status;

    /** 任务状态中文描述。 */
    private String statusDesc;

    /** 当前处理子阶段：extracting / ai_analyzing / enhancing。 */
    private String stage;

    /** 当前处理子阶段中文描述。 */
    private String stageDesc;

    /** 失败原因，仅失败状态返回。 */
    private String errorMsg;

    /** 任务失败时间。 */
    private LocalDateTime failedAt;

    /** 创建时间。 */
    private LocalDateTime createTime;

    /** 最近更新时间。 */
    private LocalDateTime updateTime;
}
