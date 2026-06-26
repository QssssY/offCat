package com.airesume.server.service;

import com.airesume.server.entity.MembershipPlan;
import com.baomidou.mybatisplus.extension.service.IService;

public interface MembershipPlanService extends IService<MembershipPlan> {

    MembershipPlan getActiveByCode(String planCode);

    /**
     * 按套餐编码查询套餐，忽略启用状态。
     *
     * 该方法用于管理端手工修改权益时校验套餐编码是否存在。
     *
     * @param planCode 套餐编码
     * @return 套餐实体，不存在返回 null
     */
    MembershipPlan getByPlanCode(String planCode);
}
