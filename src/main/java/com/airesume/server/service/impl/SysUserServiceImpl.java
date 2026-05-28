package com.airesume.server.service.impl;

import com.airesume.server.common.constants.QuotaConstants;
import com.airesume.server.common.constants.UserRoleConstants;
import com.airesume.server.entity.MembershipPlan;
import com.airesume.server.entity.SysUser;
import com.airesume.server.mapper.SysUserMapper;
import com.airesume.server.service.MembershipPlanService;
import com.airesume.server.service.SysUserService;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 用户服务实现类
 * 提供用户查询、校验等基础功能
 */
@Slf4j
@Service
public class SysUserServiceImpl extends ServiceImpl<SysUserMapper, SysUser> implements SysUserService {

    private static final String USER_CACHE = "sys_user";

    /**
     * 自注入：通过 Spring 代理调用自身方法，使 @Cacheable 等 AOP 注解在自调用时生效。
     */
    @Lazy
    @Autowired
    private SysUserServiceImpl self;

    @Autowired
    private MembershipPlanService membershipPlanService;

    /**
     * 重写 getById，加 Redis 缓存。
     * JwtAuthenticationFilter 每次请求都调用此方法，缓存可避免频繁查库。
     */
    @Override
    @Cacheable(value = USER_CACHE, key = "#id")
    public SysUser getById(Serializable id) {
        return super.getById(id);
    }

    /**
     * 重写 updateById，更新时清除缓存。
     */
    @Override
    @CacheEvict(value = USER_CACHE, key = "#entity.id")
    public boolean updateById(SysUser entity) {
        return super.updateById(entity);
    }

    /**
     * 重写 save，新建用户时不会命中缓存，但防止极端场景缓存不一致。
     */
    @Override
    @CacheEvict(value = USER_CACHE, key = "#entity.id")
    public boolean save(SysUser entity) {
        return super.save(entity);
    }

    /**
     * 重写 removeById，删除用户时清除用户详情缓存。
     */
    @Override
    @CacheEvict(value = USER_CACHE, key = "#id")
    public boolean removeById(Serializable id) {
        return super.removeById(id);
    }

    /**
     * 根据用户名查询用户
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
     */
    @Override
    public boolean isVipUser(Long userId) {
        log.debug("Checking if user is VIP, userId: {}", userId);
        SysUser user = self.getById(userId);
        if (user == null) {
            log.warn("User not found when checking VIP status, userId: {}", userId);
            return false;
        }
        if (user.getRole() == null || user.getRole() != UserRoleConstants.ROLE_VIP) {
            log.debug("User is not VIP role, userId: {}, role: {}", userId, user.getRole());
            return false;
        }
        if (user.getVipExpireTime() == null) {
            log.debug("User has no VIP expire time, userId: {}", userId);
            return false;
        }
        boolean isValidVip = user.getVipExpireTime().isAfter(LocalDateTime.now());
        log.debug("VIP status check completed, userId: {}, isValidVip: {}", userId, isValidVip);
        return isValidVip;
    }

    @Override
    public int getVipDailyResumeLimit(Long userId) {
        MembershipPlan plan = getCurrentActiveMembershipPlan(userId);
        return Math.max(0, plan == null || plan.getResumeQuota() == null ? 0 : plan.getResumeQuota());
    }

    @Override
    public int getVipDailyInterviewLimit(Long userId) {
        MembershipPlan plan = getCurrentActiveMembershipPlan(userId);
        return Math.max(0, plan == null || plan.getInterviewQuota() == null ? 0 : plan.getInterviewQuota());
    }

    private MembershipPlan getCurrentActiveMembershipPlan(Long userId) {
        SysUser user = self.getById(userId);
        if (user == null || !isVipUser(userId) || user.getMembershipPlanCode() == null) {
            return null;
        }
        return membershipPlanService.getActiveByCode(user.getMembershipPlanCode());
    }

    @Override
    public int getVipDailyPolishLimit(Long userId) {
        MembershipPlan plan = getCurrentActiveMembershipPlan(userId);
        return Math.max(0, plan == null || plan.getDailyPolishLimit() == null ? 0 : plan.getDailyPolishLimit());
    }

    @Override
    public int getVipDailyJdMatchLimit(Long userId) {
        MembershipPlan plan = getCurrentActiveMembershipPlan(userId);
        return Math.max(0, plan == null || plan.getDailyJdMatchLimit() == null ? 0 : plan.getDailyJdMatchLimit());
    }

    @Override
    public int getVipDailyTemplateLimit(Long userId) {
        MembershipPlan plan = getCurrentActiveMembershipPlan(userId);
        return Math.max(0, plan == null || plan.getDailyTemplateLimit() == null ? 0 : plan.getDailyTemplateLimit());
    }

    @Override
    public int getVipDailyOfferLimit(Long userId) {
        MembershipPlan plan = getCurrentActiveMembershipPlan(userId);
        return Math.max(0, plan == null || plan.getDailyOfferLimit() == null ? 0 : plan.getDailyOfferLimit());
    }

    @Override
    public int getVipCycleLimit(Long userId, String featureType) {
        MembershipPlan plan = getCurrentActiveMembershipPlan(userId);
        if (plan == null) {
            return 0;
        }
        return switch (featureType) {
            case "polish" -> safeInt(plan.getTotalPolishQuota());
            case "jd_match" -> safeInt(plan.getTotalJdMatchQuota());
            case "template" -> safeInt(plan.getTotalTemplateQuota());
            case "offer" -> safeInt(plan.getTotalOfferQuota());
            case "resume" -> safeInt(plan.getTotalResumeQuota());
            case "interview" -> safeInt(plan.getTotalInterviewQuota());
            default -> 0;
        };
    }

    private int safeInt(Integer value) {
        return value == null ? 0 : value;
    }

}
