package com.airesume.server.service.impl;

import com.airesume.server.common.constants.UserRoleConstants;
import com.airesume.server.common.exception.BusinessException;
import com.airesume.server.dto.admin.UserRightsResponse;
import com.airesume.server.dto.admin.UserRightsUpdateRequest;
import com.airesume.server.entity.MembershipPlan;
import com.airesume.server.entity.SysUser;
import com.airesume.server.entity.UserQuota;
import com.airesume.server.entity.UserRightsChangeLog;
import com.airesume.server.service.AdminUserRightsService;
import com.airesume.server.service.MembershipPlanService;
import com.airesume.server.service.SysUserService;
import com.airesume.server.service.UserQuotaService;
import com.airesume.server.service.UserRightsChangeLogService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Set;

/**
 * 管理端用户权益聚合与手工调整服务实现。
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AdminUserRightsServiceImpl implements AdminUserRightsService {

    private static final Set<Integer> SUPPORTED_ROLES = Set.of(
            UserRoleConstants.ROLE_NORMAL,
            UserRoleConstants.ROLE_VIP,
            UserRoleConstants.ROLE_ADMIN
    );

    private final SysUserService sysUserService;
    private final UserQuotaService userQuotaService;
    private final MembershipPlanService membershipPlanService;
    private final UserRightsChangeLogService userRightsChangeLogService;

    @Override
    public UserRightsResponse getUserRights(Long userId) {
        SysUser user = getExistingUser(userId);

        // 复用与用户侧 /me 接口一致的额度计算语义。
        // 同时确保管理员读取权益详情前，缺失的额度记录会被初始化。
        int resumeQuota = userQuotaService.getRemainingResumeQuota(userId);
        int interviewQuota = userQuotaService.getRemainingInterviewQuota(userId);
        // 会员有效性要统一复用服务层判断，避免列表页和详情页判定口径不一致。
        boolean vipActive = sysUserService.isVipUser(userId);

        UserQuota userQuota = userQuotaService.getByUserId(userId);
        if (userQuota == null) {
            throw new BusinessException("用户额度记录不存在");
        }

        return UserRightsResponse.builder()
                .userId(user.getId())
                .username(user.getUsername())
                .role(user.getRole())
                .roleDesc(getRoleDesc(user.getRole(), vipActive))
                .membershipPlanCode(user.getMembershipPlanCode())
                .vipExpireTime(user.getVipExpireTime())
                .isVipActive(vipActive)
                .resumeQuota(resumeQuota)
                .interviewQuota(interviewQuota)
                .dailyResumeUsed(safeValue(userQuota.getDailyResumeUsed()))
                .dailyInterviewUsed(safeValue(userQuota.getDailyInterviewUsed()))
                .totalResumeUsed(safeValue(userQuota.getTotalResumeUsed()))
                .totalInterviewUsed(safeValue(userQuota.getTotalInterviewUsed()))
                .lastRefreshDate(userQuota.getLastRefreshDate())
                .build();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void updateUserRights(Long operatorUserId, Long userId, UserRightsUpdateRequest request) {
        if (request == null) {
            throw new BusinessException("用户权益修改请求不能为空");
        }
        if (request.getRole() == null && request.getMembershipPlanCode() == null
                && request.getVipExpireTime() == null) {
            throw new BusinessException("至少提供一个可修改字段");
        }

        SysUser user = getExistingUser(userId);

        Integer beforeRole = user.getRole();
        String beforePlanCode = user.getMembershipPlanCode();
        LocalDateTime beforeVipExpireTime = user.getVipExpireTime();

        Integer finalRole = request.getRole() != null ? request.getRole() : user.getRole();
        validateRole(finalRole);

        boolean membershipPlanProvided = request.getMembershipPlanCode() != null;
        String finalPlanCode = membershipPlanProvided ? trimToNull(request.getMembershipPlanCode()) : user.getMembershipPlanCode();
        LocalDateTime finalVipExpireTime = request.getVipExpireTime() != null ? request.getVipExpireTime() : user.getVipExpireTime();

        if (finalRole != null && finalRole != UserRoleConstants.ROLE_VIP) {
            // 非 VIP 角色不应保留会员身份字段。
            // 这样可保证管理员手工降级后的后端状态一致。
            finalPlanCode = null;
            finalVipExpireTime = null;
        } else if (finalPlanCode != null) {
            // 当最终角色为 VIP 时，membershipPlanCode 必须对应有效套餐配置。
            MembershipPlan plan = membershipPlanService.getByPlanCode(finalPlanCode);
            if (plan == null) {
                throw new BusinessException("会员套餐不存在");
            }
        }

        boolean changed = !sameValue(beforeRole, finalRole)
                || !sameValue(beforePlanCode, finalPlanCode)
                || !sameValue(beforeVipExpireTime, finalVipExpireTime);

        if (!changed) {
            log.info("Admin update user rights skipped because nothing changed, operatorUserId: {}, userId: {}",
                    operatorUserId, userId);
            return;
        }

        user.setRole(finalRole);
        user.setMembershipPlanCode(finalPlanCode);
        user.setVipExpireTime(finalVipExpireTime);
        sysUserService.updateById(user);

        UserRightsChangeLog changeLog = new UserRightsChangeLog();
        changeLog.setUserId(userId);
        changeLog.setOperatorUserId(operatorUserId);
        changeLog.setBeforeRole(beforeRole);
        changeLog.setAfterRole(finalRole);
        changeLog.setBeforeMembershipPlanCode(beforePlanCode);
        changeLog.setAfterMembershipPlanCode(finalPlanCode);
        changeLog.setBeforeVipExpireTime(beforeVipExpireTime);
        changeLog.setAfterVipExpireTime(finalVipExpireTime);
        changeLog.setRemark(buildChangeRemark(request.getRemark()));
        userRightsChangeLogService.save(changeLog);

        log.info("Admin updated user rights, operatorUserId: {}, userId: {}, beforeRole: {}, afterRole: {}",
                operatorUserId, userId, beforeRole, finalRole);
    }

    /**
     * 生成最终日志备注。
     *
     * 即使管理员未填写自定义原因，也要保证日志可读。
     */
    private String buildChangeRemark(String remark) {
        String normalized = trimToNull(remark);
        return normalized != null ? normalized : "Admin manual rights update";
    }

    /**
     * 校验角色是否在系统支持范围内。
     */
    private void validateRole(Integer role) {
        if (role == null || !SUPPORTED_ROLES.contains(role)) {
            throw new BusinessException("role 只支持 0、1、9");
        }
    }

    private SysUser getExistingUser(Long userId) {
        SysUser user = sysUserService.getById(userId);
        if (user == null) {
            throw new BusinessException("用户不存在");
        }
        return user;
    }

    private String getRoleDesc(Integer role, boolean vipActive) {
        return switch (role) {
            case UserRoleConstants.ROLE_NORMAL -> "普通用户";
            // 会员角色但已过期时，管理端应明确显示已过期，避免误导管理员判断当前权益。
            case UserRoleConstants.ROLE_VIP -> vipActive ? "会员用户" : "普通用户（会员已过期）";
            case UserRoleConstants.ROLE_ADMIN -> "管理员";
            default -> "未知";
        };
    }

    private boolean sameValue(Object left, Object right) {
        return left == null ? right == null : left.equals(right);
    }

    private int safeValue(Integer value) {
        return value == null ? 0 : value;
    }

    private String trimToNull(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }
}
