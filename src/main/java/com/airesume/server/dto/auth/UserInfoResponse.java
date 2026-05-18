package com.airesume.server.dto.auth;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

import java.io.Serializable;
import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@SuperBuilder
public class UserInfoResponse implements Serializable {

    private static final long serialVersionUID = 1L;

    private Long id;
    private String username;
    private String nickname;
    private Integer role;
    private Integer status;
    private String membershipPlanCode;
    private LocalDateTime vipExpireTime;
    /** 用户注册时间，用于用户侧首页和设置中心展示账号创建日期。 */
    private LocalDateTime createTime;
    private Integer resumeQuota;
    private Integer interviewQuota;
    private Integer vipDailyResumeQuota;
    private Integer vipDailyInterviewQuota;

}
