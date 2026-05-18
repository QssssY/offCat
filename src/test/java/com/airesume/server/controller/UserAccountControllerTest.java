package com.airesume.server.controller;

import com.airesume.server.common.result.Result;
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

        Result<Void> result = controller.deleteAccount(request, authentication);

        assertEquals(200, result.getCode());
        verify(userAccountService).deleteAccount(123L, request);
    }
}
