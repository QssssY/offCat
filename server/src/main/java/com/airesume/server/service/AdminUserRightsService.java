package com.airesume.server.service;

import com.airesume.server.dto.admin.UserRightsResponse;
import com.airesume.server.dto.admin.UserRightsUpdateRequest;

/**
 * 管理端用户权益聚合与手工调整服务。
 */
public interface AdminUserRightsService {

    /**
     * 查询单个用户的权益聚合详情。
     *
     * @param userId 目标用户 ID
     * @return 用户权益聚合响应
     */
    UserRightsResponse getUserRights(Long userId);

    /**
     * 更新用户会员相关字段，并记录审计日志。
     *
     * @param operatorUserId 操作管理员 ID
     * @param userId 目标用户 ID
     * @param request 更新请求参数
     */
    void updateUserRights(Long operatorUserId, Long userId, UserRightsUpdateRequest request);
}
