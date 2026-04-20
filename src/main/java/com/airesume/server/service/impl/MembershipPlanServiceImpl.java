package com.airesume.server.service.impl;

import com.airesume.server.common.constants.MembershipConstants;
import com.airesume.server.entity.MembershipPlan;
import com.airesume.server.mapper.MembershipPlanMapper;
import com.airesume.server.service.MembershipPlanService;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import org.springframework.stereotype.Service;

@Service
public class MembershipPlanServiceImpl extends ServiceImpl<MembershipPlanMapper, MembershipPlan>
        implements MembershipPlanService {

    @Override
    public MembershipPlan getActiveByCode(String planCode) {
        LambdaQueryWrapper<MembershipPlan> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(MembershipPlan::getPlanCode, planCode)
                .eq(MembershipPlan::getStatus, MembershipConstants.PLAN_STATUS_ENABLED);
        return getOne(wrapper);
    }

    @Override
    public MembershipPlan getByPlanCode(String planCode) {
        if (planCode == null || planCode.isBlank()) {
            return null;
        }
        LambdaQueryWrapper<MembershipPlan> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(MembershipPlan::getPlanCode, planCode.trim())
                .last("LIMIT 1");
        return getOne(wrapper, false);
    }
}
