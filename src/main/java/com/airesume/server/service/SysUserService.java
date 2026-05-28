package com.airesume.server.service;

import com.airesume.server.entity.SysUser;
import com.baomidou.mybatisplus.extension.service.IService;

/**
 * 用户服务接口
 */
public interface SysUserService extends IService<SysUser> {

    /**
     * 根据用户名查询用户
     *
     * @param username 用户名
     * @return 用户信息
     */
    SysUser getByUsername(String username);

    /**
     * 检查用户名是否已存在
     *
     * @param username 用户名
     * @return 存在返回true，否则返回false
     */
    boolean existsByUsername(String username);

    /**
     * 判断用户是否为VIP会员
     *
     * @param userId 用户ID
     * @return 是VIP返回true，否则返回false
     */
    boolean isVipUser(Long userId);

    int getVipDailyResumeLimit(Long userId);

    int getVipDailyInterviewLimit(Long userId);

}
