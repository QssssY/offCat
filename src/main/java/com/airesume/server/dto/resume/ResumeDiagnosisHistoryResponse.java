package com.airesume.server.dto.resume;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * 简历诊断历史记录响应DTO
 * 用于返回用户的简历诊断历史列表
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ResumeDiagnosisHistoryResponse {

    /**
     * 任务ID
     * 使用String类型避免JavaScript精度丢失问题
     */
    private String taskId;

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
     * 创建时间
     */
    private LocalDateTime createTime;

    /**
     * 更新时间
     */
    private LocalDateTime updateTime;
}
