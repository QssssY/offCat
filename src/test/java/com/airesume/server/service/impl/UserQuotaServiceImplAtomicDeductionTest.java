package com.airesume.server.service.impl;

import com.airesume.server.common.constants.QuotaConstants;
import com.airesume.server.common.exception.BusinessException;
import com.airesume.server.entity.MembershipPlan;
import com.airesume.server.entity.UserQuota;
import com.airesume.server.mapper.ResumePolishRecordMapper;
import com.airesume.server.mapper.UserQuotaMapper;
import com.airesume.server.service.QuotaConsumptionLogService;
import com.airesume.server.service.SysUserService;
import com.baomidou.mybatisplus.core.conditions.update.UpdateWrapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class UserQuotaServiceImplAtomicDeductionTest {

    private static final Long NORMAL_USER_ID = 12345L;
    private static final Long VIP_USER_ID = 67890L;

    @Mock
    private UserQuotaMapper userQuotaMapper;

    private TestableUserQuotaService createService(Long userId, boolean vipUser) {
        SysUserService sysUserService = mock(SysUserService.class);
        if (vipUser) {
            MembershipPlan plan = new MembershipPlan();
            plan.setResumeQuota(QuotaConstants.VIP_USER_DAILY_RESUME_LIMIT);
            plan.setInterviewQuota(QuotaConstants.VIP_USER_DAILY_INTERVIEW_LIMIT);
            when(sysUserService.getActiveMembershipPlan(userId)).thenReturn(plan);
        }

        return createService(sysUserService, mock(ResumePolishRecordMapper.class), userId);
    }

    private TestableUserQuotaService createService(SysUserService sysUserService,
                                                   ResumePolishRecordMapper polishRecordMapper,
                                                   Long userId) {
        UserQuota quota = new UserQuota();
        quota.setUserId(userId);
        quota.setLastRefreshDate(LocalDate.now());

        return createService(sysUserService, polishRecordMapper, quota);
    }

    private TestableUserQuotaService createService(SysUserService sysUserService,
                                                   ResumePolishRecordMapper polishRecordMapper,
                                                   UserQuota quota) {
        TestableUserQuotaService service = spy(new TestableUserQuotaService(sysUserService, polishRecordMapper, quota));
        ReflectionTestUtils.setField(service, "baseMapper", userQuotaMapper);
        ReflectionTestUtils.setField(service, "self", service);
        return service;
    }

    private MembershipPlan createFeaturePlan() {
        MembershipPlan plan = new MembershipPlan();
        plan.setDailyPolishLimit(6);
        plan.setTotalPolishQuota(20);
        plan.setDailyJdMatchLimit(7);
        plan.setTotalJdMatchQuota(21);
        plan.setDailyTemplateLimit(8);
        plan.setTotalTemplateQuota(22);
        plan.setDailyOfferLimit(9);
        plan.setTotalOfferQuota(23);
        return plan;
    }

    private MembershipPlan createResumeInterviewPlan() {
        MembershipPlan plan = new MembershipPlan();
        plan.setResumeQuota(13);
        plan.setInterviewQuota(11);
        return plan;
    }

    private SysUserService createLegacyCompatibleVipService(MembershipPlan plan) {
        SysUserService sysUserService = mock(SysUserService.class);
        when(sysUserService.getActiveMembershipPlan(VIP_USER_ID)).thenReturn(plan);

        // 旧实现会分散查询 VIP 身份、每日额度和周期额度；新实现应只读取一次生效套餐。
        lenient().when(sysUserService.isVipUser(VIP_USER_ID)).thenReturn(true);
        lenient().when(sysUserService.getVipDailyPolishLimit(VIP_USER_ID)).thenReturn(plan.getDailyPolishLimit());
        lenient().when(sysUserService.getVipDailyJdMatchLimit(VIP_USER_ID)).thenReturn(plan.getDailyJdMatchLimit());
        lenient().when(sysUserService.getVipDailyTemplateLimit(VIP_USER_ID)).thenReturn(plan.getDailyTemplateLimit());
        lenient().when(sysUserService.getVipDailyOfferLimit(VIP_USER_ID)).thenReturn(plan.getDailyOfferLimit());
        lenient().when(sysUserService.getVipCycleLimit(VIP_USER_ID, "polish")).thenReturn(plan.getTotalPolishQuota());
        lenient().when(sysUserService.getVipCycleLimit(VIP_USER_ID, "jd_match")).thenReturn(plan.getTotalJdMatchQuota());
        lenient().when(sysUserService.getVipCycleLimit(VIP_USER_ID, "template")).thenReturn(plan.getTotalTemplateQuota());
        lenient().when(sysUserService.getVipCycleLimit(VIP_USER_ID, "offer")).thenReturn(plan.getTotalOfferQuota());
        return sysUserService;
    }

    @Test
    @DisplayName("VIP AI润色配额扣减应该只查询一次生效套餐")
    void shouldUseActiveMembershipPlanOnceWhenDeductingPolishQuotaForVipUser() {
        MembershipPlan plan = createFeaturePlan();
        SysUserService sysUserService = createLegacyCompatibleVipService(plan);
        ResumePolishRecordMapper polishRecordMapper = mock(ResumePolishRecordMapper.class);
        when(polishRecordMapper.selectCount(any())).thenReturn(0L);
        TestableUserQuotaService service = createService(sysUserService, polishRecordMapper, VIP_USER_ID);
        when(userQuotaMapper.consumeVipDailyPolishQuotaAtomic(VIP_USER_ID, 6, 20)).thenReturn(1);

        service.checkAndDeductPolishQuota(VIP_USER_ID, 1001L);

        verify(sysUserService).getActiveMembershipPlan(VIP_USER_ID);
        verify(sysUserService, never()).isVipUser(VIP_USER_ID);
        verify(sysUserService, never()).getVipDailyPolishLimit(VIP_USER_ID);
        verify(sysUserService, never()).getVipCycleLimit(VIP_USER_ID, "polish");
        verify(userQuotaMapper).consumeVipDailyPolishQuotaAtomic(VIP_USER_ID, 6, 20);
    }

    @Test
    @DisplayName("VIP JD匹配配额扣减应该只查询一次生效套餐")
    void shouldUseActiveMembershipPlanOnceWhenDeductingJdMatchQuotaForVipUser() {
        MembershipPlan plan = createFeaturePlan();
        SysUserService sysUserService = createLegacyCompatibleVipService(plan);
        TestableUserQuotaService service = createService(sysUserService, mock(ResumePolishRecordMapper.class), VIP_USER_ID);
        when(userQuotaMapper.consumeVipDailyJdMatchQuotaAtomic(VIP_USER_ID, 7, 21)).thenReturn(1);

        service.checkAndDeductJdMatchQuota(VIP_USER_ID);

        verify(sysUserService).getActiveMembershipPlan(VIP_USER_ID);
        verify(sysUserService, never()).isVipUser(VIP_USER_ID);
        verify(sysUserService, never()).getVipDailyJdMatchLimit(VIP_USER_ID);
        verify(sysUserService, never()).getVipCycleLimit(VIP_USER_ID, "jd_match");
        verify(userQuotaMapper).consumeVipDailyJdMatchQuotaAtomic(VIP_USER_ID, 7, 21);
    }

    @Test
    @DisplayName("VIP 模板配额扣减应该只查询一次生效套餐")
    void shouldUseActiveMembershipPlanOnceWhenDeductingTemplateQuotaForVipUser() {
        MembershipPlan plan = createFeaturePlan();
        SysUserService sysUserService = createLegacyCompatibleVipService(plan);
        TestableUserQuotaService service = createService(sysUserService, mock(ResumePolishRecordMapper.class), VIP_USER_ID);
        when(userQuotaMapper.consumeVipDailyTemplateQuotaAtomic(VIP_USER_ID, 8, 22)).thenReturn(1);

        service.checkAndDeductTemplateQuota(VIP_USER_ID);

        verify(sysUserService).getActiveMembershipPlan(VIP_USER_ID);
        verify(sysUserService, never()).isVipUser(VIP_USER_ID);
        verify(sysUserService, never()).getVipDailyTemplateLimit(VIP_USER_ID);
        verify(sysUserService, never()).getVipCycleLimit(VIP_USER_ID, "template");
        verify(userQuotaMapper).consumeVipDailyTemplateQuotaAtomic(VIP_USER_ID, 8, 22);
    }

    @Test
    @DisplayName("VIP Offer配额扣减应该只查询一次生效套餐")
    void shouldUseActiveMembershipPlanOnceWhenDeductingOfferQuotaForVipUser() {
        MembershipPlan plan = createFeaturePlan();
        SysUserService sysUserService = createLegacyCompatibleVipService(plan);
        TestableUserQuotaService service = createService(sysUserService, mock(ResumePolishRecordMapper.class), VIP_USER_ID);
        when(userQuotaMapper.consumeVipDailyOfferQuotaAtomic(VIP_USER_ID, 9, 23)).thenReturn(1);

        service.checkAndDeductOfferQuota(VIP_USER_ID);

        verify(sysUserService).getActiveMembershipPlan(VIP_USER_ID);
        verify(sysUserService, never()).isVipUser(VIP_USER_ID);
        verify(sysUserService, never()).getVipDailyOfferLimit(VIP_USER_ID);
        verify(sysUserService, never()).getVipCycleLimit(VIP_USER_ID, "offer");
        verify(userQuotaMapper).consumeVipDailyOfferQuotaAtomic(VIP_USER_ID, 9, 23);
    }

    @Test
    @DisplayName("VIP 面试额度检查应该只查询一次生效套餐")
    void shouldUseActiveMembershipPlanOnceWhenCheckingInterviewQuotaForVipUser() {
        MembershipPlan plan = createResumeInterviewPlan();
        SysUserService sysUserService = mock(SysUserService.class);
        when(sysUserService.getActiveMembershipPlan(VIP_USER_ID)).thenReturn(plan);
        lenient().when(sysUserService.isVipUser(VIP_USER_ID)).thenReturn(true);
        lenient().when(sysUserService.getVipDailyInterviewLimit(VIP_USER_ID)).thenReturn(plan.getInterviewQuota());

        UserQuota quota = new UserQuota();
        quota.setUserId(VIP_USER_ID);
        quota.setLastRefreshDate(LocalDate.now());
        quota.setDailyInterviewUsed(10);
        quota.setInterviewQuota(0);
        TestableUserQuotaService service = createService(sysUserService, mock(ResumePolishRecordMapper.class), quota);

        boolean result = service.checkInterviewQuota(VIP_USER_ID);

        assertEquals(true, result);
        verify(sysUserService).getActiveMembershipPlan(VIP_USER_ID);
        verify(sysUserService, never()).isVipUser(VIP_USER_ID);
        verify(sysUserService, never()).getVipDailyInterviewLimit(VIP_USER_ID);
    }

    @Test
    @DisplayName("VIP 简历额度检查应该只查询一次生效套餐")
    void shouldUseActiveMembershipPlanOnceWhenCheckingResumeQuotaForVipUser() {
        MembershipPlan plan = createResumeInterviewPlan();
        SysUserService sysUserService = mock(SysUserService.class);
        when(sysUserService.getActiveMembershipPlan(VIP_USER_ID)).thenReturn(plan);
        lenient().when(sysUserService.isVipUser(VIP_USER_ID)).thenReturn(true);
        lenient().when(sysUserService.getVipDailyResumeLimit(VIP_USER_ID)).thenReturn(plan.getResumeQuota());

        UserQuota quota = new UserQuota();
        quota.setUserId(VIP_USER_ID);
        quota.setLastRefreshDate(LocalDate.now());
        quota.setDailyResumeUsed(12);
        quota.setResumeQuota(0);
        TestableUserQuotaService service = createService(sysUserService, mock(ResumePolishRecordMapper.class), quota);

        boolean result = service.checkResumeQuota(VIP_USER_ID);

        assertEquals(true, result);
        verify(sysUserService).getActiveMembershipPlan(VIP_USER_ID);
        verify(sysUserService, never()).isVipUser(VIP_USER_ID);
        verify(sysUserService, never()).getVipDailyResumeLimit(VIP_USER_ID);
    }

    @Test
    @DisplayName("VIP 面试配额扣减应该只查询一次生效套餐")
    void shouldUseActiveMembershipPlanOnceWhenDeductingInterviewQuotaForVipUser() {
        MembershipPlan plan = createResumeInterviewPlan();
        SysUserService sysUserService = mock(SysUserService.class);
        when(sysUserService.getActiveMembershipPlan(VIP_USER_ID)).thenReturn(plan);
        lenient().when(sysUserService.isVipUser(VIP_USER_ID)).thenReturn(true);
        lenient().when(sysUserService.getVipDailyInterviewLimit(VIP_USER_ID)).thenReturn(plan.getInterviewQuota());
        TestableUserQuotaService service = createService(sysUserService, mock(ResumePolishRecordMapper.class), VIP_USER_ID);
        when(userQuotaMapper.consumeVipDailyInterviewQuotaAtomic(VIP_USER_ID, 11)).thenReturn(1);

        service.deductInterviewQuota(VIP_USER_ID);

        verify(sysUserService).getActiveMembershipPlan(VIP_USER_ID);
        verify(sysUserService, never()).isVipUser(VIP_USER_ID);
        verify(sysUserService, never()).getVipDailyInterviewLimit(VIP_USER_ID);
        verify(userQuotaMapper).consumeVipDailyInterviewQuotaAtomic(VIP_USER_ID, 11);
        verify(userQuotaMapper, never()).deductInterviewQuotaAtomic(anyLong());
    }

    @Test
    @DisplayName("VIP 简历配额扣减应该只查询一次生效套餐")
    void shouldUseActiveMembershipPlanOnceWhenDeductingResumeQuotaForVipUser() {
        MembershipPlan plan = createResumeInterviewPlan();
        SysUserService sysUserService = mock(SysUserService.class);
        when(sysUserService.getActiveMembershipPlan(VIP_USER_ID)).thenReturn(plan);
        lenient().when(sysUserService.isVipUser(VIP_USER_ID)).thenReturn(true);
        lenient().when(sysUserService.getVipDailyResumeLimit(VIP_USER_ID)).thenReturn(plan.getResumeQuota());
        TestableUserQuotaService service = createService(sysUserService, mock(ResumePolishRecordMapper.class), VIP_USER_ID);
        when(userQuotaMapper.consumeVipDailyResumeQuotaAtomic(VIP_USER_ID, 13)).thenReturn(1);

        service.deductResumeQuota(VIP_USER_ID);

        verify(sysUserService).getActiveMembershipPlan(VIP_USER_ID);
        verify(sysUserService, never()).isVipUser(VIP_USER_ID);
        verify(sysUserService, never()).getVipDailyResumeLimit(VIP_USER_ID);
        verify(userQuotaMapper).consumeVipDailyResumeQuotaAtomic(VIP_USER_ID, 13);
        verify(userQuotaMapper, never()).deductResumeQuotaAtomic(anyLong());
    }

    @Test
    @DisplayName("VIP 简历退还应该只查询一次生效套餐")
    void shouldUseActiveMembershipPlanOnceWhenRefundingResumeQuotaForVipUser() {
        MembershipPlan plan = createResumeInterviewPlan();
        SysUserService sysUserService = mock(SysUserService.class);
        when(sysUserService.getActiveMembershipPlan(VIP_USER_ID)).thenReturn(plan);
        lenient().when(sysUserService.isVipUser(VIP_USER_ID)).thenReturn(true);
        lenient().when(sysUserService.getVipDailyResumeLimit(VIP_USER_ID)).thenReturn(plan.getResumeQuota());
        TestableUserQuotaService service = createService(sysUserService, mock(ResumePolishRecordMapper.class), VIP_USER_ID);
        when(userQuotaMapper.refundResumeQuotaAtomic(VIP_USER_ID, 13)).thenReturn(1);

        service.refundResumeQuota(VIP_USER_ID);

        verify(sysUserService).getActiveMembershipPlan(VIP_USER_ID);
        verify(sysUserService, never()).isVipUser(VIP_USER_ID);
        verify(sysUserService, never()).getVipDailyResumeLimit(VIP_USER_ID);
        verify(userQuotaMapper).refundResumeQuotaAtomic(VIP_USER_ID, 13);
    }

    @Test
    @DisplayName("resume quota uses atomic SQL for normal users")
    void shouldDeductResumeQuotaAtomicallyForNormalUser() {
        TestableUserQuotaService service = createService(NORMAL_USER_ID, false);
        when(userQuotaMapper.deductResumeQuotaAtomic(NORMAL_USER_ID)).thenReturn(1);

        service.deductResumeQuota(NORMAL_USER_ID);

        verify(userQuotaMapper).deductResumeQuotaAtomic(NORMAL_USER_ID);
        verify(userQuotaMapper, never()).consumeVipDailyResumeQuotaAtomic(anyLong(), anyInt());
    }

    @Test
    @DisplayName("resume quota throws when atomic deduction updates zero rows")
    void shouldThrowWhenResumeAtomicDeductionFails() {
        TestableUserQuotaService service = createService(NORMAL_USER_ID, false);
        when(userQuotaMapper.deductResumeQuotaAtomic(NORMAL_USER_ID)).thenReturn(0);

        assertThrows(BusinessException.class, () -> service.deductResumeQuota(NORMAL_USER_ID));

        verify(userQuotaMapper).deductResumeQuotaAtomic(NORMAL_USER_ID);
    }

    @Test
    @DisplayName("vip resume quota consumes daily quota before total quota")
    void shouldConsumeVipDailyResumeQuotaBeforeTotalQuota() {
        TestableUserQuotaService service = createService(VIP_USER_ID, true);
        when(userQuotaMapper.consumeVipDailyResumeQuotaAtomic(VIP_USER_ID, QuotaConstants.VIP_USER_DAILY_RESUME_LIMIT))
                .thenReturn(1);

        service.deductResumeQuota(VIP_USER_ID);

        verify(userQuotaMapper).consumeVipDailyResumeQuotaAtomic(VIP_USER_ID, QuotaConstants.VIP_USER_DAILY_RESUME_LIMIT);
        verify(userQuotaMapper, never()).deductResumeQuotaAtomic(anyLong());
    }

    @Test
    @DisplayName("vip resume quota falls back to total quota after daily limit is exhausted")
    void shouldFallbackToTotalResumeQuotaWhenVipDailyQuotaIsExhausted() {
        TestableUserQuotaService service = createService(VIP_USER_ID, true);
        when(userQuotaMapper.consumeVipDailyResumeQuotaAtomic(VIP_USER_ID, QuotaConstants.VIP_USER_DAILY_RESUME_LIMIT))
                .thenReturn(0);
        when(userQuotaMapper.deductResumeQuotaAtomic(VIP_USER_ID)).thenReturn(1);

        service.deductResumeQuota(VIP_USER_ID);

        verify(userQuotaMapper).consumeVipDailyResumeQuotaAtomic(VIP_USER_ID, QuotaConstants.VIP_USER_DAILY_RESUME_LIMIT);
        verify(userQuotaMapper).deductResumeQuotaAtomic(VIP_USER_ID);
    }

    @Test
    @DisplayName("vip interview quota consumes daily quota before total quota")
    void shouldConsumeVipDailyInterviewQuotaBeforeTotalQuota() {
        TestableUserQuotaService service = createService(VIP_USER_ID, true);
        when(userQuotaMapper.consumeVipDailyInterviewQuotaAtomic(VIP_USER_ID, QuotaConstants.VIP_USER_DAILY_INTERVIEW_LIMIT))
                .thenReturn(1);

        service.deductInterviewQuota(VIP_USER_ID);

        verify(userQuotaMapper).consumeVipDailyInterviewQuotaAtomic(VIP_USER_ID, QuotaConstants.VIP_USER_DAILY_INTERVIEW_LIMIT);
        verify(userQuotaMapper, never()).deductInterviewQuotaAtomic(anyLong());
    }

    @Test
    @DisplayName("vip interview quota falls back to total quota after daily limit is exhausted")
    void shouldFallbackToTotalInterviewQuotaWhenVipDailyQuotaIsExhausted() {
        TestableUserQuotaService service = createService(VIP_USER_ID, true);
        when(userQuotaMapper.consumeVipDailyInterviewQuotaAtomic(VIP_USER_ID, QuotaConstants.VIP_USER_DAILY_INTERVIEW_LIMIT))
                .thenReturn(0);
        when(userQuotaMapper.deductInterviewQuotaAtomic(VIP_USER_ID)).thenReturn(1);

        service.deductInterviewQuota(VIP_USER_ID);

        verify(userQuotaMapper).consumeVipDailyInterviewQuotaAtomic(VIP_USER_ID, QuotaConstants.VIP_USER_DAILY_INTERVIEW_LIMIT);
        verify(userQuotaMapper).deductInterviewQuotaAtomic(VIP_USER_ID);
    }

    @Test
    @DisplayName("resume refund uses atomic SQL for normal users")
    void shouldRefundResumeQuotaAtomicallyForNormalUser() {
        TestableUserQuotaService service = createService(NORMAL_USER_ID, false);
        when(userQuotaMapper.refundResumeQuotaAtomic(NORMAL_USER_ID, 0)).thenReturn(1);

        service.refundResumeQuota(NORMAL_USER_ID);

        verify(userQuotaMapper).refundResumeQuotaAtomic(NORMAL_USER_ID, 0);
    }

    @Test
    @DisplayName("resume refund uses vip daily limit to decide whether total quota is restored")
    void shouldRefundResumeQuotaAtomicallyForVipUser() {
        TestableUserQuotaService service = createService(VIP_USER_ID, true);
        when(userQuotaMapper.refundResumeQuotaAtomic(VIP_USER_ID, QuotaConstants.VIP_USER_DAILY_RESUME_LIMIT))
                .thenReturn(1);

        service.refundResumeQuota(VIP_USER_ID);

        verify(userQuotaMapper).refundResumeQuotaAtomic(VIP_USER_ID, QuotaConstants.VIP_USER_DAILY_RESUME_LIMIT);
    }

    @Test
    @DisplayName("daily refresh should only update daily fields to avoid overwriting atomic deduction")
    void shouldRefreshDailyQuotaWithoutOverwritingAtomicDeductionFields() {
        SysUserService sysUserService = mock(SysUserService.class);
        UserQuota quota = new UserQuota();
        quota.setUserId(NORMAL_USER_ID);
        quota.setLastRefreshDate(LocalDate.now().minusDays(1));
        quota.setDailyInterviewUsed(3);
        quota.setDailyResumeUsed(2);
        quota.setInterviewQuota(7);
        quota.setResumeQuota(4);

        UserQuotaServiceImpl service = spy(new UserQuotaServiceImpl(
                sysUserService,
                mock(ResumePolishRecordMapper.class),
                mock(QuotaConsumptionLogService.class)));
        ReflectionTestUtils.setField(service, "baseMapper", userQuotaMapper);
        ReflectionTestUtils.setField(service, "entityClass", UserQuota.class);
        doReturn(true).when(service).update(any(UpdateWrapper.class));

        service.refreshDailyQuotaIfNeeded(NORMAL_USER_ID, quota);

        verify(service).update(any(UpdateWrapper.class));
        assertEquals(0, quota.getDailyInterviewUsed());
        assertEquals(0, quota.getDailyResumeUsed());
        assertEquals(7, quota.getInterviewQuota());
        assertEquals(4, quota.getResumeQuota());
        assertEquals(LocalDate.now(), quota.getLastRefreshDate());
    }

    private static class TestableUserQuotaService extends UserQuotaServiceImpl {

        private final UserQuota quota;

        private TestableUserQuotaService(SysUserService sysUserService,
                                         ResumePolishRecordMapper polishRecordMapper,
                                         UserQuota quota) {
            super(sysUserService, polishRecordMapper, mock(QuotaConsumptionLogService.class));
            this.quota = quota;
        }

        @Override
        public UserQuota getByUserId(Long userId) {
            return quota;
        }

        @Override
        public void initUserQuota(Long userId) {
            throw new UnsupportedOperationException("initUserQuota should not be called in this test");
        }

        @Override
        public void refreshDailyQuotaIfNeeded(Long userId, UserQuota userQuota) {
            // Keep tests focused on atomic deduction branch selection.
        }

        @Override
        public boolean updateById(UserQuota entity) {
            return true;
        }
    }
}
