package com.airesume.server.entity;

import com.airesume.server.common.entity.BaseEntity;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.time.LocalDateTime;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName("sys_user")
public class SysUser extends BaseEntity {

    private static final long serialVersionUID = 1L;

    private String username;

    private String nickname;

    private String password;

    private Integer role;

    private Integer status;

    private String membershipPlanCode;

    private LocalDateTime vipExpireTime;

    /** 安全问题（忘记密码用） */
    private String securityQuestion;

    /** 安全问题答案（BCrypt加密存储） */
    private String securityAnswer;

}
