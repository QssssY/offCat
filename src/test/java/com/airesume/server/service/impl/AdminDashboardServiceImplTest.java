package com.airesume.server.service.impl;

import com.airesume.server.dto.admin.DashboardTrendResponse;
import com.airesume.server.dto.admin.MonitorOverviewResponse;
import com.airesume.server.entity.ResumePolishRecord;
import com.airesume.server.mapper.CommunityCommentMapper;
import com.airesume.server.mapper.CommunityPostMapper;
import com.airesume.server.mapper.InterviewSessionMapper;
import com.airesume.server.mapper.MembershipOrderMapper;
import com.airesume.server.mapper.ResumeDiagnosisTaskMapper;
import com.airesume.server.service.InterviewSessionService;
import com.airesume.server.service.MembershipOrderService;
import com.airesume.server.service.ResumeDiagnosisTaskService;
import com.airesume.server.service.ResumeJobMatchService;
import com.airesume.server.service.ResumePolishService;
import com.airesume.server.service.SysAiEngineConfigService;
import com.airesume.server.service.SysJobRoleService;
import com.airesume.server.service.SysPromptService;
import com.airesume.server.service.SysUserService;
import com.airesume.server.service.UserFeedbackService;
import com.baomidou.mybatisplus.core.MybatisConfiguration;
import com.baomidou.mybatisplus.core.metadata.TableInfoHelper;
import org.apache.ibatis.builder.MapperBuilderAssistant;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.cache.annotation.Cacheable;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class AdminDashboardServiceImplTest {

    @BeforeEach
    void setUp() {
        TableInfoHelper.initTableInfo(
                new MapperBuilderAssistant(new MybatisConfiguration(), ""),
                ResumePolishRecord.class);
    }

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

    @Test
    void shouldEnableSynchronizedCacheLoadingForDashboardCacheMisses() throws NoSuchMethodException {
        assertDashboardMethodUsesSyncCache("getDashboardOverview", LocalDate.class, LocalDate.class);
        assertDashboardMethodUsesSyncCache("getDashboardTrends", LocalDate.class, LocalDate.class);
        assertDashboardMethodUsesSyncCache("getHotJobRoles", LocalDate.class, LocalDate.class, Integer.class);
        assertDashboardMethodUsesSyncCache("getBusinessDistribution", LocalDate.class, LocalDate.class);
        assertDashboardMethodUsesSyncCache("getMonitorOverview");
    }

    @Test
    void shouldReturnExpandedBusinessMonitorOverviewCounts() {
        InterviewSessionService interviewSessionService = mock(InterviewSessionService.class);
        ResumeDiagnosisTaskService resumeDiagnosisTaskService = mock(ResumeDiagnosisTaskService.class);
        UserFeedbackService userFeedbackService = mock(UserFeedbackService.class);
        ResumePolishService resumePolishService = mock(ResumePolishService.class);
        ResumeJobMatchService resumeJobMatchService = mock(ResumeJobMatchService.class);
        MembershipOrderService membershipOrderService = mock(MembershipOrderService.class);
        CommunityPostMapper communityPostMapper = mock(CommunityPostMapper.class);
        CommunityCommentMapper communityCommentMapper = mock(CommunityCommentMapper.class);
        AdminDashboardServiceImpl service = buildService(
                interviewSessionService,
                resumeDiagnosisTaskService,
                mock(InterviewSessionMapper.class),
                mock(ResumeDiagnosisTaskMapper.class),
                userFeedbackService,
                communityPostMapper,
                communityCommentMapper,
                resumePolishService,
                resumeJobMatchService,
                membershipOrderService
        );

        when(resumeDiagnosisTaskService.count(any())).thenReturn(1L, 2L, 3L, 4L, 5L);
        when(interviewSessionService.count(any())).thenReturn(6L, 7L);
        when(userFeedbackService.count(any())).thenReturn(8L, 9L, 10L);
        when(resumePolishService.count(any())).thenReturn(11L);
        when(resumeJobMatchService.count(any())).thenReturn(12L);
        when(membershipOrderService.count(any())).thenReturn(13L);
        when(communityPostMapper.selectCount(any())).thenReturn(14L, 15L);
        when(communityCommentMapper.selectCount(any())).thenReturn(16L);

        MonitorOverviewResponse response = service.getMonitorOverview();

        assertEquals(1L, response.getPendingResumeTaskCount());
        assertEquals(2L, response.getProcessingResumeTaskCount());
        assertEquals(3L, response.getFailedResumeTaskCount());
        assertEquals(4L, response.getCompletedResumeTaskCount());
        assertEquals(6L, response.getActiveInterviewSessionCount());
        assertEquals(7L, response.getTodayInterviewSessionCount());
        assertEquals(5L, response.getTodayResumeDiagnosisCount());
        assertEquals(11L, response.getTodayResumePolishCount());
        assertEquals(12L, response.getTodayJobMatchCount());
        assertEquals(14L, response.getTodayCommunityPostCount());
        assertEquals(8L, response.getPendingFeedbackCount());
        assertEquals(9L, response.getProcessingFeedbackCount());
        assertEquals(10L, response.getTodayFeedbackCount());
        assertEquals(15L, response.getPendingCommunityPostCount());
        assertEquals(16L, response.getPendingCommunityCommentCount());
        assertEquals(31L, response.getPendingCommunityReviewCount());
        assertEquals(13L, response.getTodayOrderCount());
    }

    @Test
    void shouldUseTodayHalfOpenRangeForBusinessMonitorCounts() {
        LocalDateTime todayStart = LocalDate.now().atStartOfDay();
        LocalDateTime tomorrowStart = LocalDate.now().plusDays(1).atStartOfDay();
        ResumePolishService resumePolishService = mock(ResumePolishService.class);
        AdminDashboardServiceImpl service = buildService(
                mock(InterviewSessionService.class),
                mock(ResumeDiagnosisTaskService.class),
                mock(InterviewSessionMapper.class),
                mock(ResumeDiagnosisTaskMapper.class),
                mock(UserFeedbackService.class),
                mock(CommunityPostMapper.class),
                mock(CommunityCommentMapper.class),
                resumePolishService,
                mock(ResumeJobMatchService.class),
                mock(MembershipOrderService.class)
        );
        when(resumePolishService.count(any())).thenAnswer(invocation -> {
            Object wrapper = invocation.getArgument(0);
            com.baomidou.mybatisplus.core.conditions.AbstractWrapper<?, ?, ?> queryWrapper =
                    (com.baomidou.mybatisplus.core.conditions.AbstractWrapper<?, ?, ?>) wrapper;
            queryWrapper.getSqlSegment();
            Map<String, Object> params = queryWrapper.getParamNameValuePairs();
            assertTrue(params.containsValue(todayStart), "今日统计必须包含当天开始时间, params=" + params);
            assertTrue(params.containsValue(tomorrowStart), "今日统计必须包含次日开始时间作为开区间上界, params=" + params);
            return 0L;
        });

        service.getMonitorOverview();

        verify(resumePolishService).count(any());
    }

    private void assertDashboardMethodUsesSyncCache(String methodName, Class<?>... parameterTypes) throws NoSuchMethodException {
        Cacheable cacheable = AdminDashboardServiceImpl.class
                .getMethod(methodName, parameterTypes)
                .getAnnotation(Cacheable.class);
        org.junit.jupiter.api.Assertions.assertNotNull(cacheable, methodName + " should use @Cacheable");
        org.junit.jupiter.api.Assertions.assertTrue(cacheable.sync(), methodName + " should use sync cache loading");
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
        return buildService(
                interviewSessionService,
                resumeDiagnosisTaskService,
                interviewSessionMapper,
                resumeDiagnosisTaskMapper,
                mock(UserFeedbackService.class),
                mock(CommunityPostMapper.class),
                mock(CommunityCommentMapper.class),
                mock(ResumePolishService.class),
                mock(ResumeJobMatchService.class),
                mock(MembershipOrderService.class)
        );
    }

    private AdminDashboardServiceImpl buildService(InterviewSessionService interviewSessionService,
                                                   ResumeDiagnosisTaskService resumeDiagnosisTaskService,
                                                   InterviewSessionMapper interviewSessionMapper,
                                                   ResumeDiagnosisTaskMapper resumeDiagnosisTaskMapper,
                                                   UserFeedbackService userFeedbackService,
                                                   CommunityPostMapper communityPostMapper,
                                                   CommunityCommentMapper communityCommentMapper,
                                                   ResumePolishService resumePolishService,
                                                   ResumeJobMatchService resumeJobMatchService,
                                                   MembershipOrderService membershipOrderService) {
        return new AdminDashboardServiceImpl(
                mock(SysUserService.class),
                mock(SysPromptService.class),
                mock(SysJobRoleService.class),
                mock(SysAiEngineConfigService.class),
                interviewSessionService,
                resumeDiagnosisTaskService,
                interviewSessionMapper,
                resumeDiagnosisTaskMapper,
                userFeedbackService,
                communityPostMapper,
                communityCommentMapper,
                resumePolishService,
                resumeJobMatchService,
                membershipOrderService,
                mock(MembershipOrderMapper.class),
                Runnable::run
        );
    }
}
