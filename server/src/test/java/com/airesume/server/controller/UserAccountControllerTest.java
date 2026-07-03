package com.airesume.server.controller;

import com.airesume.server.common.result.Result;
import com.airesume.server.dto.auth.SecurityQuestionResponse;
import com.airesume.server.dto.user.AccountDeleteRequest;
import com.airesume.server.service.UserAccountService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.Authentication;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class UserAccountControllerTest {

    @Mock private UserAccountService userAccountService;
    @Mock private Authentication authentication;

    private UserAccountController controller;

    @BeforeEach
    void setUp() {
        controller = new UserAccountController(userAccountService);
        when(authentication.getPrincipal()).thenReturn(123L);
    }

    @Test
    void shouldDeleteCurrentUserAccount() {
        AccountDeleteRequest request = new AccountDeleteRequest();
        request.setOldPassword("password");
        request.setConfirmPassword("password");
        request.setSecurityAnswer("answer");

        Result<Void> result = controller.deleteAccount(request, authentication);

        assertEquals(200, result.getCode());
        verify(userAccountService).deleteAccount(123L, request);
    }

    @Test
    void shouldGetCurrentUserSecurityQuestion() {
        when(userAccountService.getCurrentSecurityQuestion(123L)).thenReturn("你的出生城市是哪里？");

        Result<SecurityQuestionResponse> result = controller.getCurrentSecurityQuestion(authentication);

        assertEquals(200, result.getCode());
        assertEquals("你的出生城市是哪里？", result.getData().getSecurityQuestion());
        verify(userAccountService).getCurrentSecurityQuestion(123L);
    }
}
