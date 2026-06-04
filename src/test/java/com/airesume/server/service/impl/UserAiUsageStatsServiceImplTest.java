package com.airesume.server.service.impl;

import com.airesume.server.common.constants.UserAiConstants;
import com.airesume.server.common.exception.BusinessException;
import com.airesume.server.dto.admin.CustomAiUsageTrendActiveUserRow;
import com.airesume.server.dto.admin.CustomAiUsageTrendResponse;
import com.airesume.server.dto.admin.CustomAiUsageTrendTypeStatRow;
import com.airesume.server.dto.admin.CustomAiUsageStatsResponse;
import com.airesume.server.dto.admin.CustomAiUsageTypeStatResponse;
import com.airesume.server.dto.admin.CustomAiUserUsageStatResponse;
import com.airesume.server.dto.admin.CustomAiUserUsageTypeStatResponse;
import com.airesume.server.mapper.UserAiUsageDetailMapper;
import java.time.LocalDate;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class UserAiUsageStatsServiceImplTest {

    private UserAiUsageDetailMapper mapper;
    private UserAiUsageStatsServiceImpl service;

    @BeforeEach
    void setUp() {
        mapper = mock(UserAiUsageDetailMapper.class);
        service = new UserAiUsageStatsServiceImpl(mapper);
    }

    @Test
    void shouldBuildRangeStatsWithTypeBreakdownAndPagedUserDetails() {
        LocalDate startDate = LocalDate.of(2026, 6, 1);
        LocalDate endDate = LocalDate.of(2026, 6, 3);
        when(mapper.countConfiguredUsers()).thenReturn(3);
        when(mapper.countActiveUsers(startDate, endDate)).thenReturn(2);
        when(mapper.sumTotalCalls(startDate, endDate)).thenReturn(9);
        when(mapper.selectTypeStats(startDate, endDate)).thenReturn(List.of(
                typeStat(UserAiConstants.USAGE_TYPE_RESUME_DIAGNOSIS, 4),
                typeStat(UserAiConstants.USAGE_TYPE_INTERVIEW_MESSAGE, 5)
        ));
        when(mapper.countUserStats(startDate, endDate)).thenReturn(1L);
        when(mapper.selectUserStatsPage(startDate, endDate, 0, 20)).thenReturn(List.of(
                userStat(10L, "alice", "Alice", 9)
        ));
        when(mapper.selectUserTypeStats(startDate, endDate, List.of(10L))).thenReturn(List.of(
                userTypeStat(10L, UserAiConstants.USAGE_TYPE_RESUME_DIAGNOSIS, 4),
                userTypeStat(10L, UserAiConstants.USAGE_TYPE_INTERVIEW_MESSAGE, 5)
        ));

        CustomAiUsageStatsResponse response = service.getDailyStats(null, startDate, endDate, 1, 20);

        assertEquals(startDate, response.getStartDate());
        assertEquals(endDate, response.getEndDate());
        assertEquals(3, response.getConfiguredUserCount());
        assertEquals(2, response.getActiveUserCount());
        assertEquals(9, response.getTotalCalls());
        assertEquals(1, response.getTotalUsers());
        assertEquals("简历诊断", response.getTypeStats().get(0).getUsageTypeDesc());
        assertEquals(9, response.getUserStats().get(0).getTotalCalls());
        assertEquals(2, response.getUserStats().get(0).getTypeStats().size());
        verify(mapper).selectUserStatsPage(startDate, endDate, 0, 20);
    }

    @Test
    void shouldTreatDateParamAsSingleDayStatsForBackwardCompatibility() {
        LocalDate date = LocalDate.of(2026, 6, 3);
        when(mapper.selectTypeStats(date, date)).thenReturn(List.of());
        when(mapper.selectUserStatsPage(date, date, 999_900, 100)).thenReturn(List.of());

        CustomAiUsageStatsResponse response =
                service.getDailyStats(date, LocalDate.of(2026, 6, 1), LocalDate.of(2026, 6, 7),
                        Integer.MAX_VALUE, Integer.MAX_VALUE);

        assertEquals(date, response.getDate());
        assertEquals(date, response.getStartDate());
        assertEquals(date, response.getEndDate());
        assertEquals(10_000, response.getPage());
        assertEquals(100, response.getPageSize());
        verify(mapper).selectUserStatsPage(date, date, 999_900, 100);
    }

    @Test
    void shouldRejectInvalidStatsDateRange() {
        LocalDate startDate = LocalDate.of(2026, 6, 4);
        LocalDate endDate = LocalDate.of(2026, 6, 3);

        assertThrows(BusinessException.class, () -> service.getDailyStats(null, startDate, endDate, 1, 20));
    }

    @Test
    void shouldRejectStatsDateRangeOverNinetyDays() {
        LocalDate startDate = LocalDate.of(2026, 1, 1);
        LocalDate endDate = LocalDate.of(2026, 4, 1);

        assertThrows(BusinessException.class, () -> service.getDailyStats(null, startDate, endDate, 1, 20));
    }

    @Test
    void shouldBuildDefaultSevenDayTrendsWithMissingDatesAndNormalizedUsageTypes() {
        LocalDate endDate = LocalDate.now();
        LocalDate startDate = endDate.minusDays(6);
        LocalDate activeDate = startDate.plusDays(1);
        when(mapper.selectTrendTypeStats(startDate, endDate)).thenReturn(List.of(
                trendTypeRow(activeDate, UserAiConstants.USAGE_TYPE_RESUME_DIAGNOSIS, 4),
                trendTypeRow(activeDate, "legacy_type", 2),
                trendTypeRow(activeDate, UserAiConstants.USAGE_TYPE_UNKNOWN, 3)
        ));
        when(mapper.selectTrendActiveUserCounts(startDate, endDate)).thenReturn(List.of(
                trendActiveUserRow(activeDate, 2)
        ));

        CustomAiUsageTrendResponse response = service.getUsageTrends(null, null);

        assertEquals(startDate, response.getStartDate());
        assertEquals(endDate, response.getEndDate());
        assertEquals(7, response.getDays().size());
        assertEquals(9, response.getTotalCalls());
        assertEquals(2, response.getActiveUserCount());
        assertEquals(0, response.getDays().get(0).getTotalCalls());
        assertEquals(activeDate, response.getDays().get(1).getDate());
        assertEquals(9, response.getDays().get(1).getTotalCalls());
        assertEquals(2, response.getDays().get(1).getActiveUserCount());
        assertEquals(2, response.getDays().get(1).getTypeStats().size());
        CustomAiUsageTypeStatResponse unknownStat = response.getDays().get(1).getTypeStats().stream()
                .filter(item -> "unknown".equals(item.getUsageType()))
                .findFirst()
                .orElseThrow();
        assertEquals("未分类", unknownStat.getUsageTypeDesc());
        assertEquals(5, unknownStat.getCallCount());
        verify(mapper).selectTrendTypeStats(startDate, endDate);
        verify(mapper).selectTrendActiveUserCounts(startDate, endDate);
    }

    @Test
    void shouldTreatSingleProvidedTrendDateAsSingleDayQuery() {
        LocalDate date = LocalDate.of(2026, 6, 3);
        when(mapper.selectTrendTypeStats(date, date)).thenReturn(List.of(
                trendTypeRow(date, UserAiConstants.USAGE_TYPE_INTERVIEW_MESSAGE, 6)
        ));
        when(mapper.selectTrendActiveUserCounts(date, date)).thenReturn(List.of(
                trendActiveUserRow(date, 1)
        ));

        CustomAiUsageTrendResponse response = service.getUsageTrends(date, null);

        assertEquals(date, response.getStartDate());
        assertEquals(date, response.getEndDate());
        assertEquals(1, response.getDays().size());
        assertEquals(6, response.getTotalCalls());
        assertEquals(1, response.getActiveUserCount());
    }

    @Test
    void shouldRejectInvalidTrendDateRange() {
        LocalDate startDate = LocalDate.of(2026, 6, 4);
        LocalDate endDate = LocalDate.of(2026, 6, 3);

        assertThrows(BusinessException.class, () -> service.getUsageTrends(startDate, endDate));
    }

    @Test
    void shouldRejectTrendDateRangeOverNinetyDays() {
        LocalDate startDate = LocalDate.of(2026, 1, 1);
        LocalDate endDate = LocalDate.of(2026, 4, 1);

        assertThrows(BusinessException.class, () -> service.getUsageTrends(startDate, endDate));
    }

    private CustomAiUsageTypeStatResponse typeStat(String usageType, int count) {
        CustomAiUsageTypeStatResponse response = new CustomAiUsageTypeStatResponse();
        response.setUsageType(usageType);
        response.setCallCount(count);
        return response;
    }

    private CustomAiUserUsageStatResponse userStat(Long userId, String username, String nickname, int totalCalls) {
        CustomAiUserUsageStatResponse response = new CustomAiUserUsageStatResponse();
        response.setUserId(userId);
        response.setUsername(username);
        response.setNickname(nickname);
        response.setTotalCalls(totalCalls);
        return response;
    }

    private CustomAiUserUsageTypeStatResponse userTypeStat(Long userId, String usageType, int count) {
        CustomAiUserUsageTypeStatResponse response = new CustomAiUserUsageTypeStatResponse();
        response.setUserId(userId);
        response.setUsageType(usageType);
        response.setCallCount(count);
        return response;
    }

    private CustomAiUsageTrendTypeStatRow trendTypeRow(LocalDate date, String usageType, int count) {
        CustomAiUsageTrendTypeStatRow response = new CustomAiUsageTrendTypeStatRow();
        response.setDate(date);
        response.setUsageType(usageType);
        response.setCallCount(count);
        return response;
    }

    private CustomAiUsageTrendActiveUserRow trendActiveUserRow(LocalDate date, int activeUserCount) {
        CustomAiUsageTrendActiveUserRow response = new CustomAiUsageTrendActiveUserRow();
        response.setDate(date);
        response.setActiveUserCount(activeUserCount);
        return response;
    }
}
