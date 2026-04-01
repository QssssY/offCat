package com.airesume.server.dto.resume;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * 简历诊断任务响应DTO
 * 用于返回任务状态和结果信息
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ResumeDiagnosisTaskResponse {

    /**
     * 任务ID
     */
    private Long taskId;

    /**
     * 用户ID
     */
    private Long userId;

    /**
     * PDF简历存储地址
     */
    private String fileUrl;

    /**
     * 任务状态：0-排队中，1-解析分析中，2-完成，3-失败
     */
    private Integer status;

    /**
     * 任务状态描述
     */
    private String statusDesc;

    /**
     * AI返回的结构化诊断报告（JSON格式）
     */
    private String diagnosisResult;

    /**
     * 失败时的异常记录
     */
    private String errorMsg;

    /**
     * 创建时间
     */
    private LocalDateTime createTime;

    /**
     * 更新时间
     */
    private LocalDateTime updateTime;
}
