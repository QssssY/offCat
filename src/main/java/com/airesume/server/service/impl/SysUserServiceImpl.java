package com.airesume.server.service.impl;

import com.airesume.server.common.constants.UserRoleConstants;
import com.airesume.server.entity.SysUser;
import com.airesume.server.mapper.SysUserMapper;
import com.airesume.server.service.SysUserService;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

/**
 * 用户服务实现类
 * 提供用户查询、校验等基础功能
 */
@Slf4j
@Service
public class SysUserServiceImpl extends ServiceImpl<SysUserMapper, SysUser> implements SysUserService {

    /**
     * 根据用户名查询用户
     *
     * @param username 用户名
     * @return 用户实体，不存在返回null
     */
    @Override
    public SysUser getByUsername(String username) {
        log.debug("Querying user by username: {}", username);
        LambdaQueryWrapper<SysUser> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(SysUser::getUsername, username);
        SysUser user = getOne(wrapper);
        log.debug("User query result, username: {}, found: {}", username, user != null);
        return user;
    }

    /**
     * 检查用户名是否已存在
     *
     * @param username 用户名
     * @return 存在返回true，否则返回false
     */
    @Override
    public boolean existsByUsername(String username) {
        log.debug("Checking if username exists: {}", username);
        LambdaQueryWrapper<SysUser> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(SysUser::getUsername, username);
        boolean exists = count(wrapper) > 0;
        log.debug("Username existence check, username: {}, exists: {}", username, exists);
        return exists;
    }

    /**
     * 判断用户是否为有效VIP用户
     * 同时检查用户角色和VIP到期时间
     *
     * @param userId 用户ID
     * @return 是有效VIP返回true，否则返回false
     */
    @Override
    public boolean isVipUser(Long userId) {
        log.debug("Checking if user is VIP, userId: {}", userId);
        SysUser user = getById(userId);
        if (user == null) {
            log.warn("User not found when checking VIP status, userId: {}", userId);
            return false;
        }
        // 检查用户角色是否为VIP
        if (user.getRole() == null || user.getRole() != UserRoleConstants.ROLE_VIP) {
            log.debug("User is not VIP role, userId: {}, role: {}", userId, user.getRole());
            return false;
        }
        // 检查VIP是否过期
        if (user.getVipExpireTime() == null) {
            log.debug("User has no VIP expire time, userId: {}", userId);
            return false;
        }
        boolean isValidVip = user.getVipExpireTime().isAfter(LocalDateTime.now());
        log.debug("VIP status check completed, userId: {}, isValidVip: {}", userId, isValidVip);
        return isValidVip;
    }

}
