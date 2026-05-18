package com.airesume.server.service.impl;

import com.airesume.server.common.constants.QuotaConstants;
import com.airesume.server.common.exception.BusinessException;
import com.airesume.server.entity.UserQuota;
import com.airesume.server.mapper.UserQuotaMapper;
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
        when(sysUserService.isVipUser(userId)).thenReturn(vipUser);

        UserQuota quota = new UserQuota();
        quota.setUserId(userId);
        quota.setLastRefreshDate(LocalDate.now());

        TestableUserQuotaService service = spy(new TestableUserQuotaService(sysUserService, quota));
        ReflectionTestUtils.setField(service, "baseMapper", userQuotaMapper);
        return service;
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

        UserQuotaServiceImpl service = spy(new UserQuotaServiceImpl(sysUserService));
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

        private TestableUserQuotaService(SysUserService sysUserService, UserQuota quota) {
            super(sysUserService);
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
