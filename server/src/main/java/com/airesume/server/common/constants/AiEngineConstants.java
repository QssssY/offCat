package com.airesume.server.common.constants;

import java.util.Set;

/**
 * 管理端 AI 引擎配置常量。
 */
public final class AiEngineConstants {

    /**
     * 启用状态。
     */
    public static final int ACTIVE = 1;

    /**
     * 禁用状态。
     */
    public static final int INACTIVE = 0;

    /**
     * 模拟面试业务类型。
     */
    public static final String BUSINESS_TYPE_INTERVIEW = "interview";

    /**
     * 简历诊断业务类型。
     */
    public static final String BUSINESS_TYPE_RESUME = "resume";

    /**
     * 当前项目支持的业务类型集合。
     */
    public static final Set<String> SUPPORTED_BUSINESS_TYPES = Set.of(
            BUSINESS_TYPE_INTERVIEW,
            BUSINESS_TYPE_RESUME
    );

    private AiEngineConstants() {
    }
}
