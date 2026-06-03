package com.airesume.server.controller;

import com.airesume.server.common.constants.UserRoleConstants;
import com.airesume.server.common.exception.BusinessException;
import com.airesume.server.dto.admin.CustomAiUsageTrendDayResponse;
import com.airesume.server.dto.admin.CustomAiUsageTrendResponse;
import com.airesume.server.dto.admin.CustomAiUsageStatsResponse;
import com.airesume.server.dto.admin.CustomAiUsageTypeStatResponse;
import com.airesume.server.entity.SysUser;
import com.airesume.server.service.SysUserService;
import com.airesume.server.service.UserAiUsageStatsService;
import java.time.LocalDate;
import java.util.Arrays;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.core.Authentication;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class AdminCustomAiStatsControllerTest {

    private UserAiUsageStatsService statsService;
    private SysUserService sysUserService;
    private Authentication authentication;
    private AdminCustomAiStatsController controller;

    @BeforeEach
    void setUp() {
        statsService = mock(UserAiUsageStatsService.class);
        sysUserService = mock(SysUserService.class);
        authentication = mock(Authentication.class);
        controller = new AdminCustomAiStatsController(statsService, sysUserService);
        when(authentication.getPrincipal()).thenReturn(1L);
    }

    @Test
    void shouldRequireAdminAndNormalizePageBoundsWhenQueryingStats() {
        SysUser admin = new SysUser();
        admin.setId(1L);
        admin.setRole(UserRoleConstants.ROLE_ADMIN);
        when(sysUserService.getById(1L)).thenReturn(admin);

        LocalDate date = LocalDate.of(2026, 6, 3);
        CustomAiUsageStatsResponse response = new CustomAiUsageStatsResponse();
        response.setDate(date);
        when(statsService.getDailyStats(date, 1, 100)).thenReturn(response);

        var result = controller.getCustomAiUsageStats(date, 0, 500, authentication);

        assertEquals(date, result.getData().getDate());
        verify(statsService).getDailyStats(date, 1, 100);
    }

    @Test
    void shouldClampHugePageWhenQueryingStats() {
        SysUser admin = new SysUser();
        admin.setId(1L);
        admin.setRole(UserRoleConstants.ROLE_ADMIN);
        when(sysUserService.getById(1L)).thenReturn(admin);

        LocalDate date = LocalDate.of(2026, 6, 3);
        CustomAiUsageStatsResponse response = new CustomAiUsageStatsResponse();
        response.setDate(date);
        when(statsService.getDailyStats(date, 10_000, 100)).thenReturn(response);

        var result = controller.getCustomAiUsageStats(date, Integer.MAX_VALUE, Integer.MAX_VALUE, authentication);

        assertEquals(date, result.getData().getDate());
        verify(statsService).getDailyStats(date, 10_000, 100);
    }

    @Test
    void shouldRejectNonAdminStatsRequest() {
        SysUser normalUser = new SysUser();
        normalUser.setId(1L);
        normalUser.setRole(UserRoleConstants.ROLE_NORMAL);
        when(sysUserService.getById(1L)).thenReturn(normalUser);

        assertThrows(BusinessException.class,
                () -> controller.getCustomAiUsageStats(LocalDate.now(), 1, 20, authentication));
    }

    @Test
    void shouldRequireAdminAndDelegateTrendQuery() {
        SysUser admin = new SysUser();
        admin.setId(1L);
        admin.setRole(UserRoleConstants.ROLE_ADMIN);
        when(sysUserService.getById(1L)).thenReturn(admin);

        LocalDate startDate = LocalDate.of(2026, 6, 1);
        LocalDate endDate = LocalDate.of(2026, 6, 7);
        CustomAiUsageTrendResponse response = CustomAiUsageTrendResponse.builder()
                .startDate(startDate)
                .endDate(endDate)
                .totalCalls(3)
                .activeUserCount(1)
                .days(List.of(CustomAiUsageTrendDayResponse.builder()
                        .date(startDate)
                        .totalCalls(3)
                        .activeUserCount(1)
                        .typeStats(List.of(CustomAiUsageTypeStatResponse.builder()
                                .usageType("resume_diagnosis")
                                .usageTypeDesc("简历诊断")
                                .callCount(3)
                                .build()))
                        .build()))
                .build();
        when(statsService.getUsageTrends(startDate, endDate)).thenReturn(response);

        var result = controller.getCustomAiUsageTrends(startDate, endDate, authentication);

        assertEquals(startDate, result.getData().getStartDate());
        assertEquals(3, result.getData().getTotalCalls());
        verify(statsService).getUsageTrends(startDate, endDate);
    }

    @Test
    void shouldRejectNonAdminTrendRequest() {
        SysUser normalUser = new SysUser();
        normalUser.setId(1L);
        normalUser.setRole(UserRoleConstants.ROLE_NORMAL);
        when(sysUserService.getById(1L)).thenReturn(normalUser);

        assertThrows(BusinessException.class,
                () -> controller.getCustomAiUsageTrends(LocalDate.now(), LocalDate.now(), authentication));
    }

    @Test
    void shouldNotExposeProviderSensitiveFieldsInTrendResponseShape() {
        List<String> sensitiveFields = List.of("apiKey", "baseUrl", "model", "modelName", "providerName");

        List<String> responseFields = Arrays.stream(CustomAiUsageTrendResponse.class.getDeclaredFields())
                .map(field -> field.getName())
                .toList();
        List<String> dayFields = Arrays.stream(CustomAiUsageTrendDayResponse.class.getDeclaredFields())
                .map(field -> field.getName())
                .toList();
        List<String> typeFields = Arrays.stream(CustomAiUsageTypeStatResponse.class.getDeclaredFields())
                .map(field -> field.getName())
                .toList();

        sensitiveFields.forEach(fieldName -> {
            org.junit.jupiter.api.Assertions.assertFalse(responseFields.contains(fieldName));
            org.junit.jupiter.api.Assertions.assertFalse(dayFields.contains(fieldName));
            org.junit.jupiter.api.Assertions.assertFalse(typeFields.contains(fieldName));
        });
    }
}
