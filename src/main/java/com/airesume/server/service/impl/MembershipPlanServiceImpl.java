package com.airesume.server.service.impl;

import com.airesume.server.common.constants.MembershipConstants;
import com.airesume.server.entity.MembershipPlan;
import com.airesume.server.mapper.MembershipPlanMapper;
import com.airesume.server.service.MembershipPlanService;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;

@Service
public class MembershipPlanServiceImpl extends ServiceImpl<MembershipPlanMapper, MembershipPlan>
        implements MembershipPlanService {

    @Override
    @Cacheable(value = "config:membershipPlan", key = "'active::' + #planCode", unless = "#result == null")
    public MembershipPlan getActiveByCode(String planCode) {
        LambdaQueryWrapper<MembershipPlan> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(MembershipPlan::getPlanCode, planCode)
                .eq(MembershipPlan::getStatus, MembershipConstants.PLAN_STATUS_ENABLED);
        return getOne(wrapper);
    }

    @Override
    @Cacheable(value = "config:membershipPlan", key = "'code::' + #planCode", unless = "#result == null")
    public MembershipPlan getByPlanCode(String planCode) {
        if (planCode == null || planCode.isBlank()) {
            return null;
        }
        LambdaQueryWrapper<MembershipPlan> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(MembershipPlan::getPlanCode, planCode.trim())
                .last("LIMIT 1");
        return getOne(wrapper, false);
    }

    @Override
    @CacheEvict(value = "config:membershipPlan", allEntries = true)
    public boolean save(MembershipPlan entity) {
        return super.save(entity);
    }

    @Override
    @CacheEvict(value = "config:membershipPlan", allEntries = true)
    public boolean updateById(MembershipPlan entity) {
        return super.updateById(entity);
    }

    @Override
    @CacheEvict(value = "config:membershipPlan", allEntries = true)
    public boolean removeById(java.io.Serializable id) {
        return super.removeById(id);
    }
}
