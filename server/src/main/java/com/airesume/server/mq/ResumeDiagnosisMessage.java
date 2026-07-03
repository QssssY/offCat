package com.airesume.server.mq;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;

/**
 * 简历诊断任务消息体
 * 用于在RabbitMQ中传递简历诊断任务信息
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ResumeDiagnosisMessage implements Serializable {

    private static final long serialVersionUID = 1L;

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
}
