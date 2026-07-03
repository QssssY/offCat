package com.airesume.server.entity;

import com.airesume.server.common.entity.BaseEntity;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.time.LocalDateTime;

/**
 * 管理端用户权益变更日志实体。
 *
 * 该表记录管理员手工调整行为，便于后续审计页面追踪
 * 用户角色、套餐编码和会员到期时间是谁在何时改动。
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("user_rights_change_log")
public class UserRightsChangeLog extends BaseEntity {

    /**
     * 被调整权益的目标用户 ID。
     */
    @TableField("user_id")
    private Long userId;

    /**
     * 操作管理员用户 ID。
     */
    @TableField("operator_user_id")
    private Long operatorUserId;

    /**
     * 变更前角色。
     */
    @TableField("before_role")
    private Integer beforeRole;

    /**
     * 变更后角色。
     */
    @TableField("after_role")
    private Integer afterRole;

    /**
     * 变更前套餐编码。
     */
    @TableField("before_membership_plan_code")
    private String beforeMembershipPlanCode;

    /**
     * 变更后套餐编码。
     */
    @TableField("after_membership_plan_code")
    private String afterMembershipPlanCode;

    /**
     * 变更前会员到期时间。
     */
    @TableField("before_vip_expire_time")
    private LocalDateTime beforeVipExpireTime;

    /**
     * 变更后会员到期时间。
     */
    @TableField("after_vip_expire_time")
    private LocalDateTime afterVipExpireTime;

    /**
     * 本次变更备注（可选）。
     */
    @TableField("remark")
    private String remark;
}
