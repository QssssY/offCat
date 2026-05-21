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
