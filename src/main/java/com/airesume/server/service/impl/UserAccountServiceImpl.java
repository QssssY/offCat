package com.airesume.server.service.impl;

import com.airesume.server.common.exception.BusinessException;
import com.airesume.server.common.result.ResultCode;
import com.airesume.server.dto.user.AccountDeleteRequest;
import com.airesume.server.entity.SysUser;
import com.airesume.server.mapper.SysUserMapper;
import com.airesume.server.mapper.UserNotificationMapper;
import com.airesume.server.mapper.UserOnboardingStateMapper;
import com.airesume.server.mapper.UserQuotaMapper;
import com.airesume.server.service.InterviewService;
import com.airesume.server.service.NotificationService;
import com.airesume.server.service.ResumeDiagnosisTaskService;
import com.airesume.server.service.SysUserService;
import com.airesume.server.service.UserAccountService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

/**
 * 用户账号数据管理服务实现。
 * 负责设置中心账号注销入口的密码校验、业务数据清理和账号匿名化。
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class UserAccountServiceImpl implements UserAccountService {

    private final SysUserService sysUserService;
    private final SysUserMapper sysUserMapper;
    private final PasswordEncoder passwordEncoder;
    private final InterviewService interviewService;
    private final ResumeDiagnosisTaskService resumeDiagnosisTaskService;
    private final NotificationService notificationService;
    private final UserNotificationMapper userNotificationMapper;
    private final UserQuotaMapper userQuotaMapper;
    private final UserOnboardingStateMapper userOnboardingStateMapper;

    /**
     * 获取当前账号安全问题。
     * 该方法只服务已登录账号注销页，不复用忘记密码公共接口，避免公共接口暴露真实问题。
     */
    @Override
    public String getCurrentSecurityQuestion(Long userId) {
        SysUser user = sysUserService.getById(userId);
        if (user == null || Integer.valueOf(1).equals(user.getIsDeleted())) {
            throw new BusinessException(ResultCode.NOT_FOUND);
        }
        if (user.getSecurityQuestion() == null || user.getSecurityQuestion().isBlank()
                || user.getSecurityAnswer() == null || user.getSecurityAnswer().isBlank()) {
            throw new BusinessException("当前账号未设置安全问题，暂不能注销账号");
        }
        return user.getSecurityQuestion();
    }

    /**
     * 注销当前账号。
     * 账号主表只做逻辑删除和敏感字段匿名化，关联业务数据也统一逻辑删除，避免破坏外键。
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    @CacheEvict(value = "auth:userInfo", key = "#userId")
    public void deleteAccount(Long userId, AccountDeleteRequest request) {
        SysUser user = sysUserService.getById(userId);
        if (user == null || Integer.valueOf(1).equals(user.getIsDeleted())) {
            throw new BusinessException(ResultCode.NOT_FOUND);
        }
        if (!request.getOldPassword().equals(request.getConfirmPassword())) {
            throw new BusinessException("两次输入的当前密码不一致");
        }
        if (!passwordEncoder.matches(request.getOldPassword(), user.getPassword())) {
            throw new BusinessException("当前密码不正确");
        }
        if (user.getSecurityQuestion() == null || user.getSecurityQuestion().isBlank()
                || user.getSecurityAnswer() == null || user.getSecurityAnswer().isBlank()) {
            throw new BusinessException("当前账号未设置安全问题，暂不能注销账号");
        }
        if (!passwordEncoder.matches(request.getSecurityAnswer(), user.getSecurityAnswer())) {
            throw new BusinessException("安全问题答案不正确");
        }

        // 注销前先清理业务记录，确保后续旧 token 即使失效前也无法读到历史数据。
        int interviewCount = interviewService.clearHistory(userId);
        int resumeCount = resumeDiagnosisTaskService.clearHistory(userId);
        userNotificationMapper.logicalDeleteByUserId(userId);
        userQuotaMapper.logicalDeleteByUserId(userId);
        userOnboardingStateMapper.logicalDeleteByUserId(userId);

        String deletedUsername = "deleted_" + userId;
        String deletedPassword = passwordEncoder.encode(UUID.randomUUID().toString());
        int affected = sysUserMapper.anonymizeDeletedUser(userId, deletedUsername, "已注销用户", deletedPassword);
        if (affected == 0) {
            throw new BusinessException("账号注销失败，请稍后重试");
        }

        log.info("账号注销完成, userId: {}, interviewCount: {}, resumeCount: {}",
                userId, interviewCount, resumeCount);

        notificationService.unregisterEmitter(userId);
    }
}
