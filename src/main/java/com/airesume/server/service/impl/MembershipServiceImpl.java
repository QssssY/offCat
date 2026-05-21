package com.airesume.server.service.impl;

import com.airesume.server.common.constants.MembershipConstants;
import com.airesume.server.common.constants.QuotaConstants;
import com.airesume.server.common.constants.UserRoleConstants;
import com.airesume.server.common.exception.BusinessException;
import com.airesume.server.common.result.ResultCode;
import com.airesume.server.dto.membership.MembershipUpgradeRequest;
import com.airesume.server.entity.MembershipOrder;
import com.airesume.server.entity.MembershipPlan;
import com.airesume.server.entity.SysUser;
import com.airesume.server.service.MembershipOrderService;
import com.airesume.server.service.MembershipPlanService;
import com.airesume.server.service.MembershipService;
import com.airesume.server.service.SysUserService;
import com.airesume.server.service.UserQuotaService;
import com.airesume.server.vo.membership.MembershipPlanVO;
import com.airesume.server.vo.membership.MembershipUpgradeVO;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;

@Slf4j
@Service
@RequiredArgsConstructor
public class MembershipServiceImpl implements MembershipService {

    private static final DateTimeFormatter ORDER_NO_TIME_FORMATTER =
            DateTimeFormatter.ofPattern("yyyyMMddHHmmssSSS");

    private final MembershipPlanService membershipPlanService;
    private final MembershipOrderService membershipOrderService;
    private final SysUserService sysUserService;
    private final UserQuotaService userQuotaService;

    @Override
    @Cacheable(value = "config:membershipPlans", key = "'all'")
    public List<MembershipPlanVO> listPlans() {
        LambdaQueryWrapper<MembershipPlan> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(MembershipPlan::getStatus, MembershipConstants.PLAN_STATUS_ENABLED)
                .orderByAsc(MembershipPlan::getSort)
                .orderByAsc(MembershipPlan::getId);

        return membershipPlanService.list(wrapper).stream()
                .map(this::buildPlanVO)
                .toList();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    @CacheEvict(value = "auth:userInfo", key = "#userId")
    public MembershipUpgradeVO mockUpgrade(Long userId, MembershipUpgradeRequest request) {
        if (userId == null) {
            throw new BusinessException(ResultCode.MEMBERSHIP_USER_NOT_LOGGED_IN);
        }

        MembershipPlan plan = membershipPlanService.getActiveByCode(request.getPlanCode());
        if (plan == null) {
            throw new BusinessException(ResultCode.MEMBERSHIP_PLAN_NOT_FOUND);
        }

        SysUser user = sysUserService.getById(userId);
        if (user == null) {
            throw new BusinessException(ResultCode.MEMBERSHIP_USER_NOT_FOUND);
        }
        if (user.getStatus() != null && user.getStatus() == 0) {
            throw new BusinessException(ResultCode.MEMBERSHIP_ACCOUNT_DISABLED);
        }

        LocalDateTime now = LocalDateTime.now();
        LocalDateTime expireTimeBefore = user.getVipExpireTime();
        LocalDateTime baseExpireTime = expireTimeBefore != null && expireTimeBefore.isAfter(now)
                ? expireTimeBefore
                : now;
        LocalDateTime expireTimeAfter = baseExpireTime.plusDays(plan.getDurationDays());

        MembershipOrder order = buildMembershipOrder(userId, plan, expireTimeBefore, expireTimeAfter, now);
        membershipOrderService.save(order);

        // The old implementation added a purchased total quota after upgrade.
        // That does not match the business rule anymore.
        // Renewing or upgrading now only changes VIP validity and membership identity.
        // Keeping the same API here is intentional: current-plan renewal should call
        // the same upgrade endpoint so vipExpireTime can continue to extend forward.
        user.setRole(UserRoleConstants.ROLE_VIP);
        user.setMembershipPlanCode(plan.getPlanCode());
        user.setVipExpireTime(expireTimeAfter);
        sysUserService.updateById(user);

        int resumeQuota = userQuotaService.getRemainingResumeQuota(userId);
        int interviewQuota = userQuotaService.getRemainingInterviewQuota(userId);

        log.info("Membership upgraded successfully, userId: {}, planCode: {}, orderNo: {}",
                userId, plan.getPlanCode(), order.getOrderNo());

        return MembershipUpgradeVO.builder()
                .orderNo(order.getOrderNo())
                .orderStatus(order.getOrderStatus())
                .payChannel(order.getPayChannel())
                .planCode(plan.getPlanCode())
                .planName(plan.getPlanName())
                .role(user.getRole())
                .membershipPlanCode(user.getMembershipPlanCode())
                .vipExpireTime(user.getVipExpireTime())
                .resumeQuota(resumeQuota)
                .interviewQuota(interviewQuota)
                .build();
    }

    private MembershipPlanVO buildPlanVO(MembershipPlan plan) {
        return MembershipPlanVO.builder()
                .planCode(plan.getPlanCode())
                .planName(plan.getPlanName())
                // The old plan description often described "gifted total counts" such as 35 or 150.
                // That does not match the current business rule, so the API now returns a daily-limit description.
                .description(buildPlanDescription(plan.getDurationDays()))
                .priceAmount(plan.getPriceAmount())
                .durationDays(plan.getDurationDays())
                // The old implementation exposed cumulative package counts.
                // The current business rule should expose the VIP daily limit instead.
                .resumeQuota(QuotaConstants.VIP_USER_DAILY_RESUME_LIMIT)
                .interviewQuota(QuotaConstants.VIP_USER_DAILY_INTERVIEW_LIMIT)
                .build();
    }

    private MembershipOrder buildMembershipOrder(Long userId,
                                                 MembershipPlan plan,
                                                 LocalDateTime expireTimeBefore,
                                                 LocalDateTime expireTimeAfter,
                                                 LocalDateTime paidAt) {
        MembershipOrder order = new MembershipOrder();
        order.setOrderNo(buildOrderNo(userId));
        order.setUserId(userId);
        order.setPlanId(plan.getId());
        order.setPlanCode(plan.getPlanCode());
        order.setPlanName(plan.getPlanName());
        order.setOrderStatus(MembershipConstants.ORDER_STATUS_PAID);
        order.setPayChannel(MembershipConstants.PAY_CHANNEL_MOCK);
        order.setOrderAmount(plan.getPriceAmount());
        order.setDurationDays(plan.getDurationDays());

        // These fields are legacy schema fields from the old "granted total count" design.
        // For compatibility we keep writing them, but the value now snapshots the VIP daily limit.
        order.setGrantedResumeQuota(QuotaConstants.VIP_USER_DAILY_RESUME_LIMIT);
        order.setGrantedInterviewQuota(QuotaConstants.VIP_USER_DAILY_INTERVIEW_LIMIT);

        order.setExpireTimeBefore(expireTimeBefore);
        order.setExpireTimeAfter(expireTimeAfter);
        order.setPaidAt(paidAt);
        return order;
    }

    private String buildPlanDescription(Integer durationDays) {
        return String.format("会员有效期 %d 天，有效期内每日 %d 次简历诊断、每日 %d 次模拟面试",
                durationDays,
                QuotaConstants.VIP_USER_DAILY_RESUME_LIMIT,
                QuotaConstants.VIP_USER_DAILY_INTERVIEW_LIMIT);
    }

    private String buildOrderNo(Long userId) {
        return "MOCK" + ORDER_NO_TIME_FORMATTER.format(LocalDateTime.now())
                + userId
                + ThreadLocalRandom.current().nextInt(100, 1000);
    }
}
