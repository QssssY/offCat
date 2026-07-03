package com.airesume.server.service.impl;

import com.airesume.server.common.constants.UserRoleConstants;
import com.airesume.server.dto.admin.UserRightsResponse;
import com.airesume.server.entity.SysUser;
import com.airesume.server.entity.UserQuota;
import com.airesume.server.service.MembershipPlanService;
import com.airesume.server.service.SysUserService;
import com.airesume.server.service.UserQuotaService;
import com.airesume.server.service.UserRightsChangeLogService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AdminUserRightsServiceImplTest {

    @Mock private SysUserService sysUserService;
    @Mock private UserQuotaService userQuotaService;
    @Mock private MembershipPlanService membershipPlanService;
    @Mock private UserRightsChangeLogService userRightsChangeLogService;

    private AdminUserRightsServiceImpl service;

    @BeforeEach
    void setUp() {
        service = new AdminUserRightsServiceImpl(
                sysUserService,
                userQuotaService,
                membershipPlanService,
                userRightsChangeLogService);
    }

    @Test
    void shouldBuildUserRightsWithSingleQuotaLookupAndUserSnapshotVipState() {
        SysUser user = new SysUser();
        user.setId(99L);
        user.setUsername("vip-user");
        user.setNickname("会员用户");
        user.setRole(UserRoleConstants.ROLE_VIP);
        user.setMembershipPlanCode("vip_pro");
        user.setVipExpireTime(LocalDateTime.now().plusDays(2));

        UserQuota quota = new UserQuota();
        quota.setUserId(99L);
        quota.setResumeQuota(5);
        quota.setInterviewQuota(6);
        quota.setDailyResumeUsed(1);
        quota.setDailyInterviewUsed(2);
        quota.setTotalResumeUsed(3);
        quota.setTotalInterviewUsed(4);
        quota.setLastRefreshDate(LocalDate.now());

        when(sysUserService.getById(99L)).thenReturn(user);
        when(userQuotaService.getByUserId(99L)).thenReturn(quota);

        UserRightsResponse response = service.getUserRights(99L);

        assertEquals(5, response.getResumeQuota());
        assertEquals(6, response.getInterviewQuota());
        assertTrue(response.getIsVipActive());
        assertEquals("会员用户", response.getRoleDesc());
        verify(userQuotaService).getByUserId(99L);
        verify(userQuotaService).refreshDailyQuotaIfNeeded(99L, quota);
        verify(userQuotaService, never()).getRemainingResumeQuota(99L);
        verify(userQuotaService, never()).getRemainingInterviewQuota(99L);
        verify(sysUserService, never()).isVipUser(99L);
    }
}
