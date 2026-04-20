package com.airesume.server.dto.admin;

import lombok.Data;

import java.time.LocalDateTime;

/**
 * 管理端手工修改用户权益请求参数。
 *
 * 所有字段均可选，便于管理员按需部分更新。
 */
@Data
public class UserRightsUpdateRequest {

    /**
     * 修改后的目标角色。
     * 支持值：0-普通用户，1-会员用户，9-管理员。
     */
    private Integer role;

    /**
     * 修改后的目标套餐编码。
     *
     * 当该字段传空字符串时，后端按“清空套餐编码”处理。
     */
    private String membershipPlanCode;

    /**
     * 修改后的会员到期时间。
     */
    private LocalDateTime vipExpireTime;

    /**
     * 管理员备注（可选），会写入权益变更日志。
     */
    private String remark;
}
