package com.airesume.server.common.constants;

import java.util.Map;
import java.util.Set;

/**
 * 用户自定义 AI 配置常量。
 */
public final class UserAiConstants {

    /** 通用配置类型，作为 resume/interview 未配置时的兜底。 */
    public static final String CONFIG_TYPE_DEFAULT = "default";

    /** 简历业务配置类型。 */
    public static final String CONFIG_TYPE_RESUME = "resume";

    /** 面试与轻量聊天配置类型。 */
    public static final String CONFIG_TYPE_INTERVIEW = "interview";

    /** 自定义 AI 计费来源。 */
    public static final String BILLING_SOURCE_USER_CUSTOM = "user_custom";

    /** 平台 AI 计费来源。 */
    public static final String BILLING_SOURCE_PLATFORM = "platform";

    /** 面试自定义 AI 会话首次手动回退平台 AI 后的计费来源。 */
    public static final String BILLING_SOURCE_PLATFORM_FALLBACK = "platform_fallback";

    /** 系统配置中的每日调用次数上限 key。 */
    public static final String CUSTOM_AI_DAILY_LIMIT_KEY = "custom_ai_daily_limit";

    /** 默认每日调用次数上限。 */
    public static final int DEFAULT_DAILY_LIMIT = 50;

    /** 未传入明确功能口径时的兜底统计类型，保留旧签名兼容。 */
    public static final String USAGE_TYPE_UNKNOWN = "unknown";

    /** 简历诊断任务使用自定义 AI。 */
    public static final String USAGE_TYPE_RESUME_DIAGNOSIS = "resume_diagnosis";

    /** AI 简历润色使用自定义 AI。 */
    public static final String USAGE_TYPE_RESUME_POLISH = "resume_polish";

    /** JD 匹配分析使用自定义 AI。 */
    public static final String USAGE_TYPE_JD_MATCH = "jd_match";

    /** 面试对话消息使用自定义 AI。 */
    public static final String USAGE_TYPE_INTERVIEW_MESSAGE = "interview_message";

    /** 面试报告生成使用自定义 AI。 */
    public static final String USAGE_TYPE_INTERVIEW_REPORT = "interview_report";

    /** 面试上下文摘要使用自定义 AI。 */
    public static final String USAGE_TYPE_INTERVIEW_SUMMARY = "interview_summary";

    /** Offer 辅助使用自定义 AI。 */
    public static final String USAGE_TYPE_OFFER_ASSIST = "offer_assist";

    public static final Set<String> SUPPORTED_CONFIG_TYPES = Set.of(
            CONFIG_TYPE_DEFAULT,
            CONFIG_TYPE_RESUME,
            CONFIG_TYPE_INTERVIEW
    );

    /** 自定义 AI 用量统计口径白名单。 */
    public static final Set<String> SUPPORTED_USAGE_TYPES = Set.of(
            USAGE_TYPE_RESUME_DIAGNOSIS,
            USAGE_TYPE_RESUME_POLISH,
            USAGE_TYPE_JD_MATCH,
            USAGE_TYPE_INTERVIEW_MESSAGE,
            USAGE_TYPE_INTERVIEW_REPORT,
            USAGE_TYPE_INTERVIEW_SUMMARY,
            USAGE_TYPE_OFFER_ASSIST,
            USAGE_TYPE_UNKNOWN
    );

    /** 管理端展示使用的统计口径中文名称。 */
    public static final Map<String, String> USAGE_TYPE_LABELS = Map.of(
            USAGE_TYPE_RESUME_DIAGNOSIS, "简历诊断",
            USAGE_TYPE_RESUME_POLISH, "简历润色",
            USAGE_TYPE_JD_MATCH, "JD 匹配",
            USAGE_TYPE_INTERVIEW_MESSAGE, "面试消息",
            USAGE_TYPE_INTERVIEW_REPORT, "面试报告",
            USAGE_TYPE_INTERVIEW_SUMMARY, "面试摘要",
            USAGE_TYPE_OFFER_ASSIST, "Offer 辅助",
            USAGE_TYPE_UNKNOWN, "未分类"
    );

    private UserAiConstants() {
    }
}
