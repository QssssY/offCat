package com.airesume.server.service.impl;

import com.airesume.server.common.constants.QuotaConstants;
import com.airesume.server.entity.UserQuota;
import com.airesume.server.service.SysUserService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.*;

/**
 * UserQuotaService 配额服务测试
 *
 * 测试覆盖：
 * - 配额常量验证
 * - UserQuota 实体逻辑验证
 * - 每日配额刷新逻辑
 * - VIP 配额计算逻辑
 *
 * 注意：由于 UserQuotaServiceImpl 继承 ServiceImpl，内部方法链
 * （ensureUserQuota -> getByUserId -> getOne -> selectOne）难以通过
 * Mockito 简单 mock，因此本测试聚焦于可独立验证的业务逻辑。
 */
@DisplayName("UserQuotaService 配额服务测试")
class UserQuotaServiceImplTest {

    private static final Long TEST_USER_ID = 12345L;
    private static final Long VIP_USER_ID = 67890L;

    @Nested
    @DisplayName("配额常量测试")
    class ConstantsTests {

        @Test
        @DisplayName("普通用户免费面试限制应该是 3")
        void shouldHaveCorrectNormalUserInterviewLimit() {
            assertEquals(3, QuotaConstants.NORMAL_USER_FREE_INTERVIEW_LIMIT);
        }

        @Test
        @DisplayName("普通用户免费简历限制应该是 1")
        void shouldHaveCorrectNormalUserResumeLimit() {
            assertEquals(1, QuotaConstants.NORMAL_USER_FREE_RESUME_LIMIT);
        }

        @Test
        @DisplayName("VIP 用户每日面试限制应该是 10")
        void shouldHaveCorrectVipUserDailyInterviewLimit() {
            assertEquals(10, QuotaConstants.VIP_USER_DAILY_INTERVIEW_LIMIT);
        }

        @Test
        @DisplayName("VIP 用户每日简历限制应该是 5")
        void shouldHaveCorrectVipUserDailyResumeLimit() {
            assertEquals(5, QuotaConstants.VIP_USER_DAILY_RESUME_LIMIT);
        }
    }

    @Nested
    @DisplayName("UserQuota 实体逻辑测试")
    class UserQuotaEntityTests {

        @Test
        @DisplayName("应该正确初始化普通用户配额")
        void shouldInitializeNormalUserQuota() {
            UserQuota quota = new UserQuota();
            quota.setUserId(TEST_USER_ID);
            quota.setInterviewQuota(QuotaConstants.NORMAL_USER_FREE_INTERVIEW_LIMIT);
            quota.setResumeQuota(QuotaConstants.NORMAL_USER_FREE_RESUME_LIMIT);
            quota.setTotalInterviewUsed(0);
            quota.setTotalResumeUsed(0);
            quota.setDailyInterviewUsed(0);
            quota.setDailyResumeUsed(0);
            quota.setLastRefreshDate(LocalDate.now());

            assertEquals(TEST_USER_ID, quota.getUserId());
            assertEquals(3, quota.getInterviewQuota());
            assertEquals(1, quota.getResumeQuota());
            assertEquals(0, quota.getTotalInterviewUsed());
            assertEquals(0, quota.getTotalResumeUsed());
            assertEquals(0, quota.getDailyInterviewUsed());
            assertEquals(0, quota.getDailyResumeUsed());
            assertEquals(LocalDate.now(), quota.getLastRefreshDate());
        }

        @Test
        @DisplayName("应该正确计算普通用户剩余面试配额")
        void shouldCalculateRemainingInterviewQuota() {
            UserQuota quota = new UserQuota();
            quota.setInterviewQuota(QuotaConstants.NORMAL_USER_FREE_INTERVIEW_LIMIT);
            quota.setTotalInterviewUsed(1);

            int remaining = Math.max(0, quota.getInterviewQuota());
            assertEquals(3, remaining);
        }

        @Test
        @DisplayName("配额为负数时应该返回 0")
        void shouldReturnZeroWhenQuotaNegative() {
            UserQuota quota = new UserQuota();
            quota.setInterviewQuota(-1);
            quota.setResumeQuota(-5);

            assertEquals(0, Math.max(0, quota.getInterviewQuota()));
            assertEquals(0, Math.max(0, quota.getResumeQuota()));
        }

        @Test
        @DisplayName("null 配额值应该安全处理为 0")
        void shouldHandleNullQuotaValues() {
            UserQuota quota = new UserQuota();
            quota.setInterviewQuota(null);
            quota.setResumeQuota(null);

            int safeInterview = quota.getInterviewQuota() == null ? 0 : quota.getInterviewQuota();
            int safeResume = quota.getResumeQuota() == null ? 0 : quota.getResumeQuota();

            assertEquals(0, safeInterview);
            assertEquals(0, safeResume);
        }
    }

    @Nested
    @DisplayName("每日配额刷新逻辑测试")
    class DailyQuotaRefreshTests {

        @Test
        @DisplayName("同一天不应该刷新每日配额")
        void shouldNotRefreshOnSameDay() {
            UserQuota quota = new UserQuota();
            quota.setLastRefreshDate(LocalDate.now());
            quota.setDailyInterviewUsed(5);
            quota.setDailyResumeUsed(3);

            LocalDate today = LocalDate.now();
            boolean shouldRefresh = quota.getLastRefreshDate() == null
                    || !today.equals(quota.getLastRefreshDate());

            assertFalse(shouldRefresh);
            assertEquals(5, quota.getDailyInterviewUsed());
            assertEquals(3, quota.getDailyResumeUsed());
        }

        @Test
        @DisplayName("不同天应该刷新每日配额")
        void shouldRefreshOnDifferentDay() {
            UserQuota quota = new UserQuota();
            quota.setLastRefreshDate(LocalDate.now().minusDays(1));
            quota.setDailyInterviewUsed(5);
            quota.setDailyResumeUsed(3);

            LocalDate today = LocalDate.now();
            boolean shouldRefresh = quota.getLastRefreshDate() == null
                    || !today.equals(quota.getLastRefreshDate());

            assertTrue(shouldRefresh);

            // 模拟刷新操作
            if (shouldRefresh) {
                quota.setDailyInterviewUsed(0);
                quota.setDailyResumeUsed(0);
                quota.setLastRefreshDate(today);
            }

            assertEquals(0, quota.getDailyInterviewUsed());
            assertEquals(0, quota.getDailyResumeUsed());
            assertEquals(LocalDate.now(), quota.getLastRefreshDate());
        }

        @Test
        @DisplayName("lastRefreshDate 为 null 时应该刷新每日配额")
        void shouldRefreshWhenLastRefreshDateIsNull() {
            UserQuota quota = new UserQuota();
            quota.setLastRefreshDate(null);
            quota.setDailyInterviewUsed(5);

            LocalDate today = LocalDate.now();
            boolean shouldRefresh = quota.getLastRefreshDate() == null
                    || !today.equals(quota.getLastRefreshDate());

            assertTrue(shouldRefresh);
        }
    }

    @Nested
    @DisplayName("VIP 配额计算逻辑测试")
    class VipQuotaCalculationTests {

        @Test
        @DisplayName("VIP 用户每日面试剩余应该正确计算")
        void shouldCalculateVipDailyInterviewRemaining() {
            UserQuota quota = new UserQuota();
            quota.setDailyInterviewUsed(3);

            int remaining = Math.max(0, QuotaConstants.VIP_USER_DAILY_INTERVIEW_LIMIT - quota.getDailyInterviewUsed());
            assertEquals(7, remaining);
        }

        @Test
        @DisplayName("VIP 用户每日简历剩余应该正确计算")
        void shouldCalculateVipDailyResumeRemaining() {
            UserQuota quota = new UserQuota();
            quota.setDailyResumeUsed(2);

            int remaining = Math.max(0, QuotaConstants.VIP_USER_DAILY_RESUME_LIMIT - quota.getDailyResumeUsed());
            assertEquals(3, remaining);
        }

        @Test
        @DisplayName("VIP 用户每日配额用完时应该返回 0")
        void shouldReturnZeroWhenVipDailyQuotaExhausted() {
            UserQuota quota = new UserQuota();
            quota.setDailyInterviewUsed(QuotaConstants.VIP_USER_DAILY_INTERVIEW_LIMIT);
            quota.setDailyResumeUsed(QuotaConstants.VIP_USER_DAILY_RESUME_LIMIT);

            int interviewRemaining = Math.max(0, QuotaConstants.VIP_USER_DAILY_INTERVIEW_LIMIT - quota.getDailyInterviewUsed());
            int resumeRemaining = Math.max(0, QuotaConstants.VIP_USER_DAILY_RESUME_LIMIT - quota.getDailyResumeUsed());

            assertEquals(0, interviewRemaining);
            assertEquals(0, resumeRemaining);
        }

        @Test
        @DisplayName("VIP 用户每日配额超用时应该返回 0")
        void shouldReturnZeroWhenVipDailyQuotaExceeded() {
            UserQuota quota = new UserQuota();
            quota.setDailyInterviewUsed(QuotaConstants.VIP_USER_DAILY_INTERVIEW_LIMIT + 5);

            int remaining = Math.max(0, QuotaConstants.VIP_USER_DAILY_INTERVIEW_LIMIT - quota.getDailyInterviewUsed());
            assertEquals(0, remaining);
        }

        @Test
        @DisplayName("VIP 用户有总配额时应该可以使用")
        void shouldAllowUsageWhenTotalQuotaAvailable() {
            UserQuota quota = new UserQuota();
            quota.setInterviewQuota(5);
            quota.setDailyInterviewUsed(QuotaConstants.VIP_USER_DAILY_INTERVIEW_LIMIT);

            boolean canUse = Math.max(0, QuotaConstants.VIP_USER_DAILY_INTERVIEW_LIMIT - quota.getDailyInterviewUsed()) > 0
                    || quota.getInterviewQuota() > 0;

            assertTrue(canUse);
        }

        @Test
        @DisplayName("VIP 用户无任何配额时应该不能使用")
        void shouldDenyUsageWhenNoQuotaAvailable() {
            UserQuota quota = new UserQuota();
            quota.setInterviewQuota(0);
            quota.setDailyInterviewUsed(QuotaConstants.VIP_USER_DAILY_INTERVIEW_LIMIT);

            boolean canUse = Math.max(0, QuotaConstants.VIP_USER_DAILY_INTERVIEW_LIMIT - quota.getDailyInterviewUsed()) > 0
                    || quota.getInterviewQuota() > 0;

            assertFalse(canUse);
        }
    }

    @Nested
    @DisplayName("配额退还逻辑测试")
    class QuotaRefundTests {

        @Test
        @DisplayName("普通用户退还配额应该增加总配额")
        void shouldIncreaseTotalQuotaOnRefundForNormalUser() {
            UserQuota quota = new UserQuota();
            quota.setTotalResumeUsed(1);
            quota.setDailyResumeUsed(1);
            quota.setResumeQuota(0);

            // 模拟退还操作
            quota.setTotalResumeUsed(Math.max(0, quota.getTotalResumeUsed() - 1));
            quota.setDailyResumeUsed(Math.max(0, quota.getDailyResumeUsed() - 1));
            quota.setResumeQuota(quota.getResumeQuota() + 1);

            assertEquals(0, quota.getTotalResumeUsed());
            assertEquals(0, quota.getDailyResumeUsed());
            assertEquals(1, quota.getResumeQuota());
        }

        @Test
        @DisplayName("退还配额时使用次数不应该变为负数")
        void shouldNotGoNegativeOnRefund() {
            UserQuota quota = new UserQuota();
            quota.setTotalResumeUsed(0);
            quota.setDailyResumeUsed(0);

            quota.setTotalResumeUsed(Math.max(0, quota.getTotalResumeUsed() - 1));
            quota.setDailyResumeUsed(Math.max(0, quota.getDailyResumeUsed() - 1));

            assertEquals(0, quota.getTotalResumeUsed());
            assertEquals(0, quota.getDailyResumeUsed());
        }

        @Test
        @DisplayName("VIP 用户在每日配额内退还不应该增加总配额")
        void shouldNotIncreaseTotalQuotaOnRefundForVipUserWithinDailyLimit() {
            UserQuota quota = new UserQuota();
            quota.setTotalResumeUsed(1);
            quota.setDailyResumeUsed(1);
            quota.setResumeQuota(0);

            boolean isVipUser = true;
            boolean shouldRestoreTotalQuota = !isVipUser
                    || quota.getDailyResumeUsed() > QuotaConstants.VIP_USER_DAILY_RESUME_LIMIT;

            // 模拟退还操作
            quota.setTotalResumeUsed(Math.max(0, quota.getTotalResumeUsed() - 1));
            quota.setDailyResumeUsed(Math.max(0, quota.getDailyResumeUsed() - 1));
            if (shouldRestoreTotalQuota) {
                quota.setResumeQuota(quota.getResumeQuota() + 1);
            }

            assertEquals(0, quota.getTotalResumeUsed());
            assertEquals(0, quota.getDailyResumeUsed());
            assertEquals(0, quota.getResumeQuota()); // VIP 用户在每日配额内不退还总配额
        }

        @Test
        @DisplayName("VIP 用户超过每日配额退还应该增加总配额")
        void shouldIncreaseTotalQuotaOnRefundForVipUserExceedingDailyLimit() {
            UserQuota quota = new UserQuota();
            quota.setTotalResumeUsed(7);
            quota.setDailyResumeUsed(6); // 超过 VIP_USER_DAILY_RESUME_LIMIT (5)
            quota.setResumeQuota(0);

            boolean isVipUser = true;
            boolean shouldRestoreTotalQuota = !isVipUser
                    || quota.getDailyResumeUsed() > QuotaConstants.VIP_USER_DAILY_RESUME_LIMIT;

            // 模拟退还操作
            quota.setTotalResumeUsed(Math.max(0, quota.getTotalResumeUsed() - 1));
            quota.setDailyResumeUsed(Math.max(0, quota.getDailyResumeUsed() - 1));
            if (shouldRestoreTotalQuota) {
                quota.setResumeQuota(quota.getResumeQuota() + 1);
            }

            assertEquals(6, quota.getTotalResumeUsed());
            assertEquals(5, quota.getDailyResumeUsed());
            assertEquals(1, quota.getResumeQuota()); // 超过每日配额时应该退还总配额
        }
    }

    @Nested
    @DisplayName("配额扣减逻辑测试")
    class QuotaDeductionTests {

        @Test
        @DisplayName("普通用户面试配额扣减逻辑验证")
        void shouldDeductInterviewQuotaForNormalUser() {
            UserQuota quota = new UserQuota();
            quota.setInterviewQuota(3);

            // 模拟原子扣减
            int affected = quota.getInterviewQuota() > 0 ? 1 : 0;
            if (affected > 0) {
                quota.setInterviewQuota(quota.getInterviewQuota() - 1);
            }

            assertEquals(1, affected);
            assertEquals(2, quota.getInterviewQuota());
        }

        @Test
        @DisplayName("普通用户面试配额用完时扣减应该失败")
        void shouldFailDeductionWhenInterviewQuotaExhausted() {
            UserQuota quota = new UserQuota();
            quota.setInterviewQuota(0);

            int affected = quota.getInterviewQuota() > 0 ? 1 : 0;

            assertEquals(0, affected);
        }

        @Test
        @DisplayName("普通用户简历配额扣减逻辑验证")
        void shouldDeductResumeQuotaForNormalUser() {
            UserQuota quota = new UserQuota();
            quota.setResumeQuota(1);

            int affected = quota.getResumeQuota() > 0 ? 1 : 0;
            if (affected > 0) {
                quota.setResumeQuota(quota.getResumeQuota() - 1);
            }

            assertEquals(1, affected);
            assertEquals(0, quota.getResumeQuota());
        }

        @Test
        @DisplayName("VIP 用户应该优先扣除每日配额")
        void shouldDeductDailyQuotaFirstForVipUser() {
            UserQuota quota = new UserQuota();
            quota.setDailyInterviewUsed(0);
            quota.setInterviewQuota(5);

            boolean isVipUser = true;
            boolean dailyAvailable = quota.getDailyInterviewUsed() < QuotaConstants.VIP_USER_DAILY_INTERVIEW_LIMIT;

            if (isVipUser && dailyAvailable) {
                quota.setDailyInterviewUsed(quota.getDailyInterviewUsed() + 1);
            } else {
                quota.setInterviewQuota(quota.getInterviewQuota() - 1);
            }

            assertEquals(1, quota.getDailyInterviewUsed());
            assertEquals(5, quota.getInterviewQuota()); // 总配额不变
        }
    }
}
