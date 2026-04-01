package com.airesume.server.common.constants;

/**
 * 简历诊断模块常量类
 * 定义任务状态、队列名称等常量
 */
public class ResumeDiagnosisConstants {

    /**
     * 任务状态：排队中
     */
    public static final int STATUS_PENDING = 0;

    /**
     * 任务状态：解析分析中
     */
    public static final int STATUS_PROCESSING = 1;

    /**
     * 任务状态：完成
     */
    public static final int STATUS_COMPLETED = 2;

    /**
     * 任务状态：失败
     */
    public static final int STATUS_FAILED = 3;

    /**
     * RabbitMQ 队列名称：简历诊断任务队列
     */
    public static final String QUEUE_RESUME_DIAGNOSIS = "queue.resume.diagnosis";

    /**
     * RabbitMQ 交换机名称：简历诊断交换机
     */
    public static final String EXCHANGE_RESUME_DIAGNOSIS = "exchange.resume.diagnosis";

    /**
     * RabbitMQ 路由键：简历诊断任务
     */
    public static final String ROUTING_KEY_RESUME_DIAGNOSIS = "routing.key.resume.diagnosis";

    private ResumeDiagnosisConstants() {
    }
}
