package com.airesume.server.dto.interview;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
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
    private String jobRole;

    /**
     * 岗位编码（可选，用于关联 prompt 配置）
     */
    private String jobRoleCode;

    /**
     * 难度级别：1-初级，2-中级，3-高级
     */
    @NotNull(message = "难度级别不能为空")
    private Integer difficulty;

    /**
     * 面试模式：normal-普通面试，stress-压力面试
     */
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
     */
    private String jdText;

    /**
     * 是否优先复用最近一次岗位 JD 对比分析结果。
     */
    private Boolean useLatestJobMatch;

    /**
     * 指定复用的岗位 JD 对比分析记录 ID。
     */
    private String jobMatchRecordId;
}
