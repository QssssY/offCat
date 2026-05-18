package com.airesume.server.service.impl;

import com.airesume.server.common.exception.BusinessException;
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
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class UserAccountServiceImplTest {

    @Mock private SysUserService sysUserService;
    @Mock private SysUserMapper sysUserMapper;
    @Mock private PasswordEncoder passwordEncoder;
    @Mock private InterviewService interviewService;
    @Mock private ResumeDiagnosisTaskService resumeDiagnosisTaskService;
    @Mock private NotificationService notificationService;
    @Mock private UserNotificationMapper userNotificationMapper;
    @Mock private UserQuotaMapper userQuotaMapper;
    @Mock private UserOnboardingStateMapper userOnboardingStateMapper;

    private UserAccountServiceImpl service;

    @BeforeEach
    void setUp() {
        service = new UserAccountServiceImpl(
                sysUserService,
                sysUserMapper,
                passwordEncoder,
                interviewService,
                resumeDiagnosisTaskService,
                notificationService,
                userNotificationMapper,
                userQuotaMapper,
                userOnboardingStateMapper);
    }

    @Test
    void shouldGetCurrentSecurityQuestionForAccountDeletion() {
        Long userId = 123L;
        SysUser user = new SysUser();
        user.setId(userId);
        user.setSecurityQuestion("你的出生城市是哪里？");
        user.setSecurityAnswer("encoded-answer");
        user.setIsDeleted(0);

        when(sysUserService.getById(userId)).thenReturn(user);

        String question = service.getCurrentSecurityQuestion(userId);

        verify(sysUserService).getById(userId);
        org.junit.jupiter.api.Assertions.assertEquals("你的出生城市是哪里？", question);
    }

    @Test
    void shouldRejectSecurityQuestionLookupWhenNotConfigured() {
        Long userId = 123L;
        SysUser user = new SysUser();
        user.setId(userId);
        user.setIsDeleted(0);

        when(sysUserService.getById(userId)).thenReturn(user);

        assertThrows(BusinessException.class, () -> service.getCurrentSecurityQuestion(userId));
    }

    @Test
    void shouldRejectWrongPasswordWhenDeletingAccount() {
        Long userId = 123L;
        AccountDeleteRequest request = new AccountDeleteRequest();
        request.setOldPassword("wrong");
        request.setConfirmPassword("wrong");
        request.setSecurityAnswer("answer");
        SysUser user = new SysUser();
        user.setId(userId);
        user.setPassword("encoded");
        user.setSecurityQuestion("question");
        user.setSecurityAnswer("encoded-answer");
        user.setStatus(1);
        user.setIsDeleted(0);

        when(sysUserService.getById(userId)).thenReturn(user);
        when(passwordEncoder.matches("wrong", "encoded")).thenReturn(false);

        assertThrows(BusinessException.class, () -> service.deleteAccount(userId, request));

        verify(interviewService, never()).clearHistory(anyLong());
        verify(sysUserMapper, never()).anonymizeDeletedUser(anyLong(), anyString(), anyString(), anyString());
    }

    @Test
    void shouldRejectWhenConfirmPasswordDoesNotMatchWhenDeletingAccount() {
        Long userId = 123L;
        AccountDeleteRequest request = new AccountDeleteRequest();
        request.setOldPassword("password");
        request.setConfirmPassword("different");
        request.setSecurityAnswer("answer");
        SysUser user = new SysUser();
        user.setId(userId);
        user.setPassword("encoded");
        user.setSecurityQuestion("question");
        user.setSecurityAnswer("encoded-answer");
        user.setStatus(1);
        user.setIsDeleted(0);

        when(sysUserService.getById(userId)).thenReturn(user);

        assertThrows(BusinessException.class, () -> service.deleteAccount(userId, request));

        verify(passwordEncoder, never()).matches(anyString(), anyString());
        verify(interviewService, never()).clearHistory(anyLong());
    }

    @Test
    void shouldRejectWrongSecurityAnswerWhenDeletingAccount() {
        Long userId = 123L;
        AccountDeleteRequest request = new AccountDeleteRequest();
        request.setOldPassword("password");
        request.setConfirmPassword("password");
        request.setSecurityAnswer("wrong-answer");
        SysUser user = new SysUser();
        user.setId(userId);
        user.setPassword("encoded");
        user.setSecurityQuestion("question");
        user.setSecurityAnswer("encoded-answer");
        user.setStatus(1);
        user.setIsDeleted(0);

        when(sysUserService.getById(userId)).thenReturn(user);
        when(passwordEncoder.matches("password", "encoded")).thenReturn(true);
        when(passwordEncoder.matches("wrong-answer", "encoded-answer")).thenReturn(false);

        assertThrows(BusinessException.class, () -> service.deleteAccount(userId, request));

        verify(interviewService, never()).clearHistory(anyLong());
        verify(sysUserMapper, never()).anonymizeDeletedUser(anyLong(), anyString(), anyString(), anyString());
    }

    @Test
    void shouldDeleteAccountAndRelatedData() {
        Long userId = 123L;
        AccountDeleteRequest request = new AccountDeleteRequest();
        request.setOldPassword("password");
        request.setConfirmPassword("password");
        request.setSecurityAnswer("answer");
        SysUser user = new SysUser();
        user.setId(userId);
        user.setPassword("encoded");
        user.setSecurityQuestion("question");
        user.setSecurityAnswer("encoded-answer");
        user.setStatus(1);
        user.setIsDeleted(0);

        when(sysUserService.getById(userId)).thenReturn(user);
        when(passwordEncoder.matches("password", "encoded")).thenReturn(true);
        when(passwordEncoder.matches("answer", "encoded-answer")).thenReturn(true);
        when(passwordEncoder.encode(anyString())).thenReturn("deleted-password");
        when(interviewService.clearHistory(userId)).thenReturn(2);
        when(resumeDiagnosisTaskService.clearHistory(userId)).thenReturn(3);
        when(sysUserMapper.anonymizeDeletedUser(eq(userId), eq("deleted_123"), eq("已注销用户"), eq("deleted-password")))
                .thenReturn(1);

        service.deleteAccount(userId, request);

        verify(interviewService).clearHistory(userId);
        verify(resumeDiagnosisTaskService).clearHistory(userId);
        verify(userNotificationMapper).logicalDeleteByUserId(userId);
        verify(userQuotaMapper).logicalDeleteByUserId(userId);
        verify(userOnboardingStateMapper).logicalDeleteByUserId(userId);
        verify(sysUserMapper).anonymizeDeletedUser(eq(userId), eq("deleted_123"), eq("已注销用户"), eq("deleted-password"));
    }
}
