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
}
