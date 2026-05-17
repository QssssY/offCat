package com.airesume.server.dto.admin;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Builder
public class AuditLogResponse {

    private Long id;
    private Long userId;
    private String username;
    private Long operatorUserId;
    private String operatorName;
    private String beforeRoleDesc;
    private String afterRoleDesc;
    private String beforeMembershipPlanCode;
    private String afterMembershipPlanCode;
    private String beforeVipExpireTime;
    private String afterVipExpireTime;
    private String remark;
    private LocalDateTime createTime;
}
