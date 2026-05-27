package com.airesume.server.service.impl;

import com.airesume.server.dto.admin.DashboardTrendResponse;
import com.airesume.server.mapper.InterviewSessionMapper;
import com.airesume.server.mapper.ResumeDiagnosisTaskMapper;
import com.airesume.server.service.InterviewSessionService;
import com.airesume.server.service.ResumeDiagnosisTaskService;
import com.airesume.server.service.SysAiEngineConfigService;
import com.airesume.server.service.SysJobRoleService;
import com.airesume.server.service.SysPromptService;
import com.airesume.server.service.SysUserService;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class AdminDashboardServiceImplTest {

    @Test
    void shouldBuildSevenDayTrendsFromAggregatedRows() {
        InterviewSessionMapper interviewSessionMapper = mock(InterviewSessionMapper.class);
        ResumeDiagnosisTaskMapper resumeDiagnosisTaskMapper = mock(ResumeDiagnosisTaskMapper.class);
        AdminDashboardServiceImpl service = buildService(interviewSessionMapper, resumeDiagnosisTaskMapper);

        LocalDate startDate = LocalDate.of(2026, 5, 1);
        LocalDate endDate = LocalDate.of(2026, 5, 7);
        LocalDateTime start = startDate.atStartOfDay();
        LocalDateTime endExclusive = endDate.plusDays(1).atStartOfDay();
        when(interviewSessionMapper.countByCreateDate(start, endExclusive)).thenReturn(List.of(
                Map.of("statDate", startDate, "totalCount", 2L),
                Map.of("statDate", LocalDate.of(2026, 5, 3), "totalCount", 5L)
        ));
        when(resumeDiagnosisTaskMapper.countByCreateDate(start, endExclusive)).thenReturn(List.of(
                Map.of("statDate", startDate, "totalCount", 1L),
                Map.of("statDate", endDate, "totalCount", 4L)
        ));

        List<DashboardTrendResponse> trends = service.getDashboardTrends(startDate, endDate);

        assertEquals(7, trends.size());
        assertEquals(startDate, trends.get(0).getDate());
        assertEquals(2L, trends.get(0).getInterviewSessionCount());
        assertEquals(1L, trends.get(0).getResumeDiagnosisCount());
        assertEquals(LocalDate.of(2026, 5, 2), trends.get(1).getDate());
        assertEquals(0L, trends.get(1).getInterviewSessionCount());
        assertEquals(0L, trends.get(1).getResumeDiagnosisCount());
        assertEquals(5L, trends.get(2).getInterviewSessionCount());
        assertEquals(4L, trends.get(6).getResumeDiagnosisCount());
        verify(interviewSessionMapper).countByCreateDate(start, endExclusive);
        verify(resumeDiagnosisTaskMapper).countByCreateDate(start, endExclusive);
    }

    @Test
    void shouldNotUsePerDayCountQueriesWhenBuildingTrends() {
        InterviewSessionMapper interviewSessionMapper = mock(InterviewSessionMapper.class);
        ResumeDiagnosisTaskMapper resumeDiagnosisTaskMapper = mock(ResumeDiagnosisTaskMapper.class);
        InterviewSessionService interviewSessionService = mock(InterviewSessionService.class);
        ResumeDiagnosisTaskService resumeDiagnosisTaskService = mock(ResumeDiagnosisTaskService.class);
        AdminDashboardServiceImpl service = buildService(
                interviewSessionService,
                resumeDiagnosisTaskService,
                interviewSessionMapper,
                resumeDiagnosisTaskMapper
        );

        LocalDate startDate = LocalDate.of(2026, 5, 1);
        LocalDate endDate = LocalDate.of(2026, 5, 7);
        when(interviewSessionMapper.countByCreateDate(startDate.atStartOfDay(), endDate.plusDays(1).atStartOfDay()))
                .thenReturn(List.of());
        when(resumeDiagnosisTaskMapper.countByCreateDate(startDate.atStartOfDay(), endDate.plusDays(1).atStartOfDay()))
                .thenReturn(List.of());

        service.getDashboardTrends(startDate, endDate);

        verify(interviewSessionService, never()).count();
        verify(resumeDiagnosisTaskService, never()).count();
    }

    private AdminDashboardServiceImpl buildService(InterviewSessionMapper interviewSessionMapper,
                                                   ResumeDiagnosisTaskMapper resumeDiagnosisTaskMapper) {
        return buildService(
                mock(InterviewSessionService.class),
                mock(ResumeDiagnosisTaskService.class),
                interviewSessionMapper,
                resumeDiagnosisTaskMapper
        );
    }

    private AdminDashboardServiceImpl buildService(InterviewSessionService interviewSessionService,
                                                   ResumeDiagnosisTaskService resumeDiagnosisTaskService,
                                                   InterviewSessionMapper interviewSessionMapper,
                                                   ResumeDiagnosisTaskMapper resumeDiagnosisTaskMapper) {
        return new AdminDashboardServiceImpl(
                mock(SysUserService.class),
                mock(SysPromptService.class),
                mock(SysJobRoleService.class),
                mock(SysAiEngineConfigService.class),
                interviewSessionService,
                resumeDiagnosisTaskService,
                interviewSessionMapper,
                resumeDiagnosisTaskMapper
        );
    }
}
