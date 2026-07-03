package com.airesume.server.dto.interview;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * 创建面试会话请求DTO
 * 用于接收前端创建面试会话的请求
 */
@Data
public class CreateSessionRequest {

    /**
     * 面试岗位
     */
    @NotBlank(message = "面试岗位不能为空")
    @Size(max = 100, message = "面试岗位名称不能超过 100 个字符")
    private String jobRole;

    /**
     * 岗位编码（可选，用于关联 prompt 配置）
     */
    @Size(max = 100, message = "岗位编码不能超过 100 个字符")
    private String jobRoleCode;

    /**
     * 难度级别：1-初级，2-中级，3-高级
     */
    @NotNull(message = "难度级别不能为空")
    @Min(value = 1, message = "难度级别必须在 1-3 之间")
    @Max(value = 3, message = "难度级别必须在 1-3 之间")
    private Integer difficulty;

    /**
     * 面试模式：normal / stress / job_targeted / big_company_hr / tech_leader / foreign_interviewer。
     * 通过白名单约束避免任意字符串注入到 AI System Prompt。
     */
    @Pattern(
            regexp = "normal|stress|job_targeted|big_company_hr|tech_leader|foreign_interviewer",
            message = "面试模式只支持 normal/stress/job_targeted/big_company_hr/tech_leader/foreign_interviewer"
    )
    private String interviewMode;

    /**
     * 是否开启岗位定向模拟。
     */
    private Boolean jobTargeted;

    /**
     * 关联的简历诊断任务 ID。
     * 普通模拟面试和岗位定向模拟面试都可以携带，后端也会兜底查找最近一次简历。
     */
    private String resumeTaskId;

    /**
     * 用户手动输入的岗位 JD 文本。
     * 限制最大长度防止异常超长输入推高 Prompt Token。
     */
    @Size(max = 8000, message = "JD 文本不能超过 8000 个字符")
    private String jdText;

    /**
     * 是否优先复用最近一次岗位 JD 对比分析结果。
     */
    private Boolean useLatestJobMatch;

    /**
     * 指定复用的岗位 JD 对比分析记录 ID。
     */
    private String jobMatchRecordId;

    /**
     * 反馈模式：immediate-每题即时反馈，after_interview-面完统一复盘
     * 创建时选定后存入会话表，会话详情和历史记录中返回。
     */
    @Pattern(
            regexp = "immediate|after_interview",
            message = "反馈模式只支持 immediate/after_interview"
    )
    private String feedbackMode;

    /**
     * 交互方式：0-文字面试（默认），1-语音面试。
     * 后端会校验只允许 0/1，避免未知交互模式进入会话主链路。
     */
    @Min(value = 0, message = "交互方式只能是文字(0)或语音(1)")
    @Max(value = 1, message = "交互方式只能是文字(0)或语音(1)")
    private Integer interactionType;

    /**
     * 是否显式回退到平台 AI。默认 false，只有用户点击回退按钮时才消耗平台额度。
     */
    private Boolean fallbackToPlatform;
}
