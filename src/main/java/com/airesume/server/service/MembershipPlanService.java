package com.airesume.server.service;

import com.airesume.server.entity.MembershipPlan;
import com.baomidou.mybatisplus.extension.service.IService;

public interface MembershipPlanService extends IService<MembershipPlan> {

    MembershipPlan getActiveByCode(String planCode);
}
