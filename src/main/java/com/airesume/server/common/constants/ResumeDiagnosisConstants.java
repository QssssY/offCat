package com.airesume.server.common.constants;

/**
 * Resume diagnosis module constants.
 */
public class ResumeDiagnosisConstants {

    public static final int STATUS_PENDING = 0;

    public static final int STATUS_PROCESSING = 1;

    public static final int STATUS_COMPLETED = 2;

    public static final int STATUS_FAILED = 3;

    public static final String QUEUE_RESUME_DIAGNOSIS = "queue.resume.diagnosis";

    public static final String EXCHANGE_RESUME_DIAGNOSIS = "exchange.resume.diagnosis";

    public static final String ROUTING_KEY_RESUME_DIAGNOSIS = "routing.key.resume.diagnosis";

    public static final String EXCHANGE_RESUME_DIAGNOSIS_DLX = "exchange.resume.diagnosis.dlx";

    public static final String QUEUE_RESUME_DIAGNOSIS_DLQ = "queue.resume.diagnosis.dlq";

    public static final String ROUTING_KEY_RESUME_DIAGNOSIS_DLQ = "routing.key.resume.diagnosis.dlq";

    /**
     * 简历诊断消息存活时间。使用单条消息 TTL，避免给已存在队列追加 x-message-ttl 导致 RabbitMQ 启动声明失败。
     */
    public static final String MESSAGE_TTL_MS = "3600000";

    public static final int SCENARIO_TYPE_RESUME = 2;

    // 诊断子阶段（仅 status=PROCESSING 时有效）
    /** 子阶段：提取简历文本 */
    public static final String STAGE_EXTRACTING = "extracting";
    /** 子阶段：AI 深度分析 */
    public static final String STAGE_AI_ANALYZING = "ai_analyzing";
    /** 子阶段：生成诊断报告 */
    public static final String STAGE_ENHANCING = "enhancing";

    private ResumeDiagnosisConstants() {
    }
}
