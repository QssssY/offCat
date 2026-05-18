package com.airesume.server.service;

import com.airesume.server.dto.user.AccountDeleteRequest;

/**
 * 用户账号数据管理服务。
 * 承接设置中心的账号注销等高风险操作，统一放在事务内处理。
 */
public interface UserAccountService {

    /**
     * 注销当前用户账号并清理关联业务数据。
     *
     * @param userId 当前登录用户 ID
     * @param request 注销确认请求，包含当前密码
     */
    void deleteAccount(Long userId, AccountDeleteRequest request);
}
