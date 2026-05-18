package com.airesume.server.service.impl;

import com.airesume.server.common.exception.BusinessException;
import com.airesume.server.dto.user.UserSettingsRequest;
import com.airesume.server.dto.user.UserSettingsResponse;
import com.airesume.server.entity.UserSettings;
import com.airesume.server.mapper.UserSettingsMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class UserSettingsServiceImplTest {

    @Mock private UserSettingsMapper userSettingsMapper;

    private UserSettingsServiceImpl service;

    @BeforeEach
    void setUp() {
        service = new UserSettingsServiceImpl();
        ReflectionTestUtils.setField(service, "baseMapper", userSettingsMapper);
    }

    @Test
    void shouldReturnDefaultSettingsWhenUserHasNoRecord() {
        when(userSettingsMapper.selectActiveByUserId(123L)).thenReturn(null);

        UserSettingsResponse response = service.getSettings(123L);

        assertEquals(0, response.getInterviewRetentionDays());
        assertEquals(0, response.getResumeRetentionDays());
    }

    @Test
    void shouldSaveValidUserSettings() {
        ArgumentCaptor<UserSettings> settingsCaptor = ArgumentCaptor.forClass(UserSettings.class);
        UserSettingsRequest request = new UserSettingsRequest();
        request.setInterviewRetentionDays(30);
        request.setResumeRetentionDays(90);
        when(userSettingsMapper.selectActiveByUserId(123L)).thenReturn(null);
        when(userSettingsMapper.insert(any(UserSettings.class))).thenReturn(1);

        UserSettingsResponse response = service.saveSettings(123L, request);

        assertEquals(30, response.getInterviewRetentionDays());
        assertEquals(90, response.getResumeRetentionDays());
        verify(userSettingsMapper).insert(settingsCaptor.capture());
        assertEquals(123L, settingsCaptor.getValue().getUserId());
        assertEquals(30, settingsCaptor.getValue().getInterviewRetentionDays());
        assertEquals(90, settingsCaptor.getValue().getResumeRetentionDays());
        assertEquals(0, settingsCaptor.getValue().getIsDeleted());
    }

    @Test
    void shouldRejectInvalidRetentionDays() {
        UserSettingsRequest request = new UserSettingsRequest();
        request.setInterviewRetentionDays(7);
        request.setResumeRetentionDays(90);

        assertThrows(BusinessException.class, () -> service.saveSettings(123L, request));
    }

    @Test
    void shouldRejectNullRetentionDaysOnSave() {
        UserSettingsRequest request = new UserSettingsRequest();
        request.setInterviewRetentionDays(null);
        request.setResumeRetentionDays(90);

        assertThrows(BusinessException.class, () -> service.saveSettings(123L, request));
    }
}
