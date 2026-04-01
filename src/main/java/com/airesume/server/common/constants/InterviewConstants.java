package com.airesume.server.common.constants;

/**
 * 模拟面试模块常量类
 * 定义会话状态、消息角色等常量
 */
public class InterviewConstants {

    /**
     * 会话状态：进行中
     */
    public static final int STATUS_IN_PROGRESS = 0;

    /**
     * 会话状态：已结束
     */
    public static final int STATUS_ENDED = 1;

    /**
     * 难度级别：初级
     */
    public static final int DIFFICULTY_EASY = 1;

    /**
     * 难度级别：中级
     */
    public static final int DIFFICULTY_MEDIUM = 2;

    /**
     * 难度级别：高级
     */
    public static final int DIFFICULTY_HARD = 3;

    /**
     * 消息角色：用户
     */
    public static final String ROLE_USER = "user";

    /**
     * 消息角色：面试官（AI助手）
     */
    public static final String ROLE_ASSISTANT = "assistant";

    /**
     * 消息角色：系统
     */
    public static final String ROLE_SYSTEM = "system";

    private InterviewConstants() {
    }
}
