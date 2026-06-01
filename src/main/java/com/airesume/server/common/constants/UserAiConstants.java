package com.airesume.server.common.constants;

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

    /** 系统配置中的每日调用次数上限 key。 */
    public static final String CUSTOM_AI_DAILY_LIMIT_KEY = "custom_ai_daily_limit";

    /** 默认每日调用次数上限。 */
    public static final int DEFAULT_DAILY_LIMIT = 50;

    public static final Set<String> SUPPORTED_CONFIG_TYPES = Set.of(
            CONFIG_TYPE_DEFAULT,
            CONFIG_TYPE_RESUME,
            CONFIG_TYPE_INTERVIEW
    );

    private UserAiConstants() {
    }
}
