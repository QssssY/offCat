package com.airesume.server.dto.admin;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * 管理端用户权益聚合响应参数。
 *
 * 该 DTO 把 sys_user 的身份字段与 user_quota 的额度字段合并，
 * 让管理端一次请求即可查看会员状态与额度状态。
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserRightsResponse {

    /**
     * 目标用户 ID。
     */
    private Long userId;

    /**
     * 用户名。
     */
    private String username;

    /**
     * 用户角色编码。
     */
    private Integer role;

    /**
     * 角色描述。
     */
    private String roleDesc;

    /**
     * 当前绑定套餐编码。
     */
    private String membershipPlanCode;

    /**
     * 来自 sys_user 的会员到期时间快照。
     */
    private LocalDateTime vipExpireTime;

    /**
     * 当前是否为有效会员。
     *
     * 必须基于 role + vipExpireTime 计算，
     * 与项目其他位置使用的运行时业务规则保持一致。
     */
    private Boolean isVipActive;

    /**
     * 当前剩余简历诊断额度。
     *
     * 该值是额度服务按现行业务规则实时计算结果，
     * 不是旧累计套餐设计里 resume_quota 的历史存储值。
     */
    private Integer resumeQuota;

    /**
     * 当前剩余模拟面试额度。
     */
    private Integer interviewQuota;

    /**
     * 今日已使用简历诊断次数。
     */
    private Integer dailyResumeUsed;

    /**
     * 今日已使用模拟面试次数。
     */
    private Integer dailyInterviewUsed;

    /**
     * 历史累计已使用简历诊断次数。
     */
    private Integer totalResumeUsed;

    /**
     * 历史累计已使用模拟面试次数。
     */
    private Integer totalInterviewUsed;

    /**
     * 最近一次日额度刷新日期。
     */
    private LocalDate lastRefreshDate;
}
