package com.airesume.server.service.impl;

import com.airesume.server.common.constants.MembershipConstants;
import com.airesume.server.dto.membership.MembershipUpgradeRequest;
import com.airesume.server.entity.MembershipOrder;
import com.airesume.server.entity.MembershipPlan;
import com.airesume.server.entity.SysUser;
import com.airesume.server.service.MembershipOrderService;
import com.airesume.server.service.MembershipPlanService;
import com.airesume.server.service.SysUserService;
import com.airesume.server.service.UserQuotaService;
import com.airesume.server.vo.membership.MembershipPlanVO;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class MembershipServiceImplTest {

    @Mock private MembershipPlanService membershipPlanService;
    @Mock private MembershipOrderService membershipOrderService;
    @Mock private SysUserService sysUserService;
    @Mock private UserQuotaService userQuotaService;

    private MembershipServiceImpl membershipService;

    @BeforeEach
    void setUp() {
        membershipService = new MembershipServiceImpl(
                membershipPlanService,
                membershipOrderService,
                sysUserService,
                userQuotaService);
    }

    @Test
    void listPlansShouldUseAdminConfiguredDescriptionAndQuotasAndLimitSixOnlinePlans() {
        List<MembershipPlan> configuredPlans = List.of(
                plan(1L, "vip_a", "A Plan", "A custom intro", 12, 18),
                plan(2L, "vip_b", "B Plan", "B custom intro", 20, 30),
                plan(3L, "vip_c", "C Plan", "C custom intro", 21, 31),
                plan(4L, "vip_d", "D Plan", "D custom intro", 22, 32),
                plan(5L, "vip_e", "E Plan", "E custom intro", 23, 33),
                plan(6L, "vip_f", "F Plan", "F custom intro", 24, 34),
                plan(7L, "vip_g", "G Plan", "G custom intro", 25, 35)
        );
        when(membershipPlanService.list(any(LambdaQueryWrapper.class))).thenReturn(configuredPlans);

        List<MembershipPlanVO> result = membershipService.listPlans();

        assertEquals(6, result.size());
        assertEquals("A custom intro", result.get(0).getDescription());
        assertEquals(12, result.get(0).getResumeQuota());
        assertEquals(18, result.get(0).getInterviewQuota());
        assertEquals("vip_f", result.get(5).getPlanCode());
        assertTrue(result.stream().noneMatch(plan -> "vip_g".equals(plan.getPlanCode())));
    }

    @Test
    void mockUpgradeShouldSnapshotAdminConfiguredPlanQuotasInOrder() {
        MembershipPlan plan = plan(10L, "vip_pro", "Pro Plan", "Pro intro", 16, 28);
        SysUser user = new SysUser();
        user.setId(99L);
        user.setRole(0);
        user.setStatus(MembershipConstants.PLAN_STATUS_ENABLED);
        user.setVipExpireTime(LocalDateTime.now().minusDays(1));
        MembershipUpgradeRequest request = new MembershipUpgradeRequest();
        request.setPlanCode("vip_pro");

        when(membershipPlanService.getActiveByCode("vip_pro")).thenReturn(plan);
        when(sysUserService.getById(99L)).thenReturn(user);
        when(userQuotaService.getRemainingResumeQuota(99L)).thenReturn(16);
        when(userQuotaService.getRemainingInterviewQuota(99L)).thenReturn(28);

        membershipService.mockUpgrade(99L, request);

        ArgumentCaptor<MembershipOrder> captor = ArgumentCaptor.forClass(MembershipOrder.class);
        verify(membershipOrderService).save(captor.capture());
        assertEquals(16, captor.getValue().getGrantedResumeQuota());
        assertEquals(28, captor.getValue().getGrantedInterviewQuota());
    }

    @Test
    void mockUpgradeShouldNotResetCycleQuotaWhenRenewingSamePlan() {
        MembershipPlan plan = plan(10L, "vip_pro", "Pro Plan", "Pro intro", 16, 28);
        SysUser user = new SysUser();
        user.setId(99L);
        user.setRole(1);
        user.setStatus(MembershipConstants.PLAN_STATUS_ENABLED);
        user.setMembershipPlanCode("vip_pro");
        user.setVipExpireTime(LocalDateTime.now().plusDays(3));
        MembershipUpgradeRequest request = new MembershipUpgradeRequest();
        request.setPlanCode("vip_pro");

        when(membershipPlanService.getActiveByCode("vip_pro")).thenReturn(plan);
        when(membershipPlanService.getByPlanCode("vip_pro")).thenReturn(plan);
        when(sysUserService.getById(99L)).thenReturn(user);
        when(userQuotaService.getRemainingResumeQuota(99L)).thenReturn(16);
        when(userQuotaService.getRemainingInterviewQuota(99L)).thenReturn(28);

        membershipService.mockUpgrade(99L, request);

        verify(userQuotaService, never()).resetCycleQuota(99L);
    }

    private static MembershipPlan plan(Long id,
                                       String planCode,
                                       String planName,
                                       String description,
                                       Integer resumeQuota,
                                       Integer interviewQuota) {
        MembershipPlan plan = new MembershipPlan();
        plan.setId(id);
        plan.setPlanCode(planCode);
        plan.setPlanName(planName);
        plan.setDescription(description);
        plan.setPriceAmount(BigDecimal.valueOf(29.9));
        plan.setDurationDays(30);
        plan.setResumeQuota(resumeQuota);
        plan.setInterviewQuota(interviewQuota);
        plan.setStatus(MembershipConstants.PLAN_STATUS_ENABLED);
        plan.setSort(id.intValue());
        return plan;
    }
}
