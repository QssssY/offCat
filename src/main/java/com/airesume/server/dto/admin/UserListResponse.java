package com.airesume.server.dto.admin;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * 用户列表响应DTO
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserListResponse {

    /**
     * 用户ID
     */
    private Long id;

    /**
     * 用户名
     */
    private String username;

    /**
     * 昵称
     */
    private String nickname;

    /**
     * 角色：0-普通用户，1-会员用户，9-管理员
     */
    private Integer role;

    /**
     * 角色描述
     */
    private String roleDesc;

    /**
     * 状态：1-正常，0-封禁
     */
    private Integer status;

    /**
     * 状态描述
     */
    private String statusDesc;

    /**
     * 会员到期时间
     */
    private LocalDateTime vipExpireTime;

    /**
     * 创建时间
     */
    private LocalDateTime createTime;
}
