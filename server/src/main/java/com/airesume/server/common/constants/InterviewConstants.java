package com.airesume.server.common.constants;

import java.util.Map;

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
     * 获取难度级别的中文描述。
     * @param difficulty 难度级别（1/2/3）
     * @return 中文描述
     */
    public static String getDifficultyLabel(Integer difficulty) {
        if (difficulty == null) return "未知";
        return switch (difficulty) {
            case DIFFICULTY_EASY -> "初级";
            case DIFFICULTY_MEDIUM -> "中级";
            case DIFFICULTY_HARD -> "高级";
            default -> "未知";
        };
    }

    /**
     * 获取难度级别的详细描述（含经验年限），用于 AI Prompt 构建。
     * @param difficulty 难度级别（1/2/3），null 时按中级处理
     * @return 详细描述
     */
    public static String getDifficultyDescription(Integer difficulty) {
        return switch (difficulty == null ? DIFFICULTY_MEDIUM : difficulty) {
            case DIFFICULTY_EASY -> "初级（1-3年经验）";
            case DIFFICULTY_HARD -> "高级（5年以上经验）";
            default -> "中级（3-5年经验）";
        };
    }

    /**
     * 获取按难度分级的维度权重映射。
     * 不同难度侧重不同能力维度，确保总分与岗位要求相符。
     * @param difficulty 难度级别（1/2/3），null 时按中级处理
     * @return 维度 -> 权重的不可变 Map
     */
    public static Map<String, Double> getDimensionWeights(Integer difficulty) {
        return switch (difficulty == null ? DIFFICULTY_MEDIUM : difficulty) {
            case DIFFICULTY_EASY -> Map.of(
                    "jobMatch", 0.20,
                    "communication", 0.25,
                    "pressureResistance", 0.20,
                    "problemSolving", 0.15,
                    "technicalDepth", 0.10,
                    "projectExpression", 0.10
            );
            case DIFFICULTY_HARD -> Map.of(
                    "technicalDepth", 0.25,
                    "projectExpression", 0.25,
                    "problemSolving", 0.25,
                    "jobMatch", 0.10,
                    "communication", 0.10,
                    "pressureResistance", 0.05
            );
            default -> Map.of(
                    "technicalDepth", 0.20,
                    "projectExpression", 0.20,
                    "problemSolving", 0.20,
                    "jobMatch", 0.15,
                    "communication", 0.15,
                    "pressureResistance", 0.10
            );
        };
    }

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

    /**
     * 普通模拟面试模式。
     */
    public static final String MODE_NORMAL = "normal";

    /**
     * 压力模拟面试模式。
     */
    public static final String MODE_STRESS = "stress";

    /**
     * 岗位定向模拟面试模式。
     * 说明：该模式用于前后端展示语义区分，真实会话仍可结合岗位定向上下文继续追问。
     */
    public static final String MODE_JOB_TARGETED = "job_targeted";

    /**
     * 大厂 HR 面试官人设。
     */
    public static final String MODE_BIG_COMPANY_HR = "big_company_hr";

    /**
     * 技术 Leader 面试官人设。
     */
    public static final String MODE_TECH_LEADER = "tech_leader";

    /**
     * 外企面试官人设。
     */
    public static final String MODE_FOREIGN_INTERVIEWER = "foreign_interviewer";

    /**
     * 每题即时反馈模式。
     */
    public static final String FEEDBACK_MODE_IMMEDIATE = "immediate";

    /**
     * 面完统一复盘模式。
     */
    public static final String FEEDBACK_MODE_AFTER_INTERVIEW = "after_interview";

    /**
     * 反馈模式默认值。
     */
    public static final String FEEDBACK_MODE_DEFAULT = FEEDBACK_MODE_AFTER_INTERVIEW;

    /**
     * 交互方式：文字面试。
     */
    public static final int INTERACTION_TYPE_TEXT = 0;

    /**
     * 交互方式：语音面试。
     */
    public static final int INTERACTION_TYPE_VOICE = 1;

    /**
     * 面试开场白模板。
     * 参数：{0}=难度描述, {1}=岗位名称, {2}=简历提示
     */
    public static final String OPENING_TEMPLATE = "你好，欢迎参加%s%s面试。我是今天的面试官，%s请你先介绍一下自己吧。";

    /**
     * 判断是否为本轮支持的有限面试模式。
     * 说明：只开放固定人设，避免前端传入任意字符串影响 AI Prompt。
     */
    public static boolean isSupportedInterviewMode(String interviewMode) {
        return MODE_NORMAL.equals(interviewMode)
                || MODE_STRESS.equals(interviewMode)
                || MODE_JOB_TARGETED.equals(interviewMode)
                || MODE_BIG_COMPANY_HR.equals(interviewMode)
                || MODE_TECH_LEADER.equals(interviewMode)
                || MODE_FOREIGN_INTERVIEWER.equals(interviewMode);
    }

    /**
     * 判断是否为新增面试官人设模式。
     */
    public static boolean isInterviewerPersonaMode(String interviewMode) {
        return MODE_BIG_COMPANY_HR.equals(interviewMode)
                || MODE_TECH_LEADER.equals(interviewMode)
                || MODE_FOREIGN_INTERVIEWER.equals(interviewMode);
    }

    /**
     * 判断是否为支持的交互方式。
     * 说明：交互方式会写入会话表，必须拒绝未知值，避免后续前端和 AI Prompt 分支不可控。
     */
    public static boolean isSupportedInteractionType(Integer interactionType) {
        return interactionType != null
                && (interactionType == INTERACTION_TYPE_TEXT || interactionType == INTERACTION_TYPE_VOICE);
    }

    private InterviewConstants() {
    }
}
