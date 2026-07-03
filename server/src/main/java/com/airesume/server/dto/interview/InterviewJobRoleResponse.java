package com.airesume.server.dto.interview;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 用户端面试岗位选项响应
 *
 * 作用：
 * 用户端面试入口页直接读取这个接口结果，不再写死岗位列表。
 * 这样岗位配置的唯一来源就是后台岗位表。
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class InterviewJobRoleResponse {

    /**
     * 岗位编码
     */
    private String roleCode;

    /**
     * 岗位名称
     * 前端下拉展示和创建面试时使用这个值。
     */
    private String roleName;

    /**
     * 面试入口标签文案
     */
    private String interviewTag;

    /**
     * 标签样式类型
     */
    private String tagType;
}
