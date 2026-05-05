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

    private ResumeDiagnosisConstants() {
    }
}
