package com.airesume.server.service;

import com.airesume.server.dto.membership.MembershipUpgradeRequest;
import com.airesume.server.vo.membership.MembershipPlanVO;
import com.airesume.server.vo.membership.MembershipUpgradeVO;

import java.util.List;

public interface MembershipService {

    List<MembershipPlanVO> listPlans();

    MembershipUpgradeVO mockUpgrade(Long userId, MembershipUpgradeRequest request);
}
