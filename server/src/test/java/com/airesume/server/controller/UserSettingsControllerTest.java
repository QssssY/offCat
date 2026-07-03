package com.airesume.server.controller;

import com.airesume.server.common.result.Result;
import com.airesume.server.dto.user.UserSettingsRequest;
import com.airesume.server.dto.user.UserSettingsResponse;
import com.airesume.server.service.UserSettingsService;
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
class UserSettingsControllerTest {

    @Mock private UserSettingsService userSettingsService;
    @Mock private Authentication authentication;

    private UserSettingsController controller;

    @BeforeEach
    void setUp() {
        controller = new UserSettingsController(userSettingsService);
        when(authentication.getPrincipal()).thenReturn(123L);
    }

    @Test
    void shouldGetCurrentUserSettings() {
        when(userSettingsService.getSettings(123L)).thenReturn(new UserSettingsResponse(30, 90));

        Result<UserSettingsResponse> result = controller.getSettings(authentication);

        assertEquals(200, result.getCode());
        assertEquals(30, result.getData().getInterviewRetentionDays());
        verify(userSettingsService).getSettings(123L);
    }

    @Test
    void shouldSaveCurrentUserSettings() {
        UserSettingsRequest request = new UserSettingsRequest();
        when(userSettingsService.saveSettings(123L, request)).thenReturn(new UserSettingsResponse(30, 90));

        Result<UserSettingsResponse> result = controller.saveSettings(request, authentication);

        assertEquals(200, result.getCode());
        assertEquals(90, result.getData().getResumeRetentionDays());
        verify(userSettingsService).saveSettings(123L, request);
    }
}
