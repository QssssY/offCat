package com.airesume.server.common.constants;

/**
 * Prompt模板常量类
 * 定义场景类型、启用状态等常量
 */
public class PromptConstants {

    /**
     * 场景类型：面试系统设定
     */
    public static final int SCENARIO_INTERVIEW = 1;

    /**
     * 场景类型：简历诊断设定
     */
    public static final int SCENARIO_RESUME_DIAGNOSIS = 2;

    /**
     * 启用状态：禁用
     */
    public static final int INACTIVE = 0;

    /**
     * 启用状态：启用
     */
    public static final int ACTIVE = 1;

    private PromptConstants() {
    }
}
