package com.airesume.server.service.impl;

import com.airesume.server.common.constants.AiEngineConstants;
import com.airesume.server.common.constants.InterviewConstants;
import com.airesume.server.common.constants.PromptConstants;
import com.airesume.server.common.constants.ResumeDiagnosisConstants;
import com.airesume.server.common.constants.UserRoleConstants;
import com.airesume.server.common.exception.BusinessException;
import com.airesume.server.dto.admin.BusinessDistributionResponse;
import com.airesume.server.dto.admin.DashboardOverviewResponse;
import com.airesume.server.dto.admin.DashboardTrendResponse;
import com.airesume.server.dto.admin.HotJobRoleResponse;
import com.airesume.server.dto.admin.MonitorOverviewResponse;
import com.airesume.server.entity.InterviewSession;
import com.airesume.server.entity.ResumeDiagnosisTask;
import org.springframework.cache.annotation.Cacheable;
import com.airesume.server.entity.SysAiEngineConfig;
import com.airesume.server.entity.SysJobRole;
import com.airesume.server.entity.SysPrompt;
import com.airesume.server.entity.SysUser;
import com.airesume.server.service.AdminDashboardService;
import com.airesume.server.service.InterviewSessionService;
import com.airesume.server.service.ResumeDiagnosisTaskService;
import com.airesume.server.service.SysAiEngineConfigService;
import com.airesume.server.service.SysJobRoleService;
import com.airesume.server.service.SysPromptService;
import com.airesume.server.service.SysUserService;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * 应用层看板与监控实现。
 *
 * 该实现只聚合业务表数据，刻意不依赖中间件深度指标，
 * 以便在 Redis/RabbitMQ 监控尚未接入时也可稳定运行。
 */
@Service
@RequiredArgsConstructor
public class AdminDashboardServiceImpl implements AdminDashboardService {

    private static final int DEFAULT_TREND_DAYS = 7;
    private static final int DEFAULT_HOT_ROLE_LIMIT = 10;
    private static final int MAX_QUERY_DAYS = 90;

    private final SysUserService sysUserService;
    private final SysPromptService sysPromptService;
    private final SysJobRoleService sysJobRoleService;
    private final SysAiEngineConfigService sysAiEngineConfigService;
    private final InterviewSessionService interviewSessionService;
    private final ResumeDiagnosisTaskService resumeDiagnosisTaskService;

    @Override
    public DashboardOverviewResponse getDashboardOverview(LocalDate startDate, LocalDate endDate) {
        // 总览接口保留全局配置与用户总量统计，业务流量统计按查询日期范围计算。
        DateRange range = resolveDateRange(startDate, endDate, DateRangeDefault.TODAY);

        long totalUserCount = sysUserService.count();
        long vipUserCount = sysUserService.count(
                new LambdaQueryWrapper<SysUser>().eq(SysUser::getRole, UserRoleConstants.ROLE_VIP)
        );
        long activePromptCount = sysPromptService.count(
                new LambdaQueryWrapper<SysPrompt>().eq(SysPrompt::getIsActive, PromptConstants.ACTIVE)
        );
        long activeJobRoleCount = sysJobRoleService.count(
                new LambdaQueryWrapper<SysJobRole>().eq(SysJobRole::getIsActive, PromptConstants.ACTIVE)
        );
        long activeAiEngineCount = sysAiEngineConfigService.count(
                new LambdaQueryWrapper<SysAiEngineConfig>().eq(SysAiEngineConfig::getIsActive, AiEngineConstants.ACTIVE)
        );

        long interviewSessionCount = countInterviewSessionsBetween(range.startDateTime(), range.endExclusiveDateTime());
        long resumeDiagnosisCount = countResumeTasksBetween(range.startDateTime(), range.endExclusiveDateTime());

        return DashboardOverviewResponse.builder()
                .totalUserCount(totalUserCount)
                .vipUserCount(vipUserCount)
                .activePromptCount(activePromptCount)
                .activeJobRoleCount(activeJobRoleCount)
                .activeAiEngineCount(activeAiEngineCount)
                // 字段名保持向后兼容，避免影响既有前端契约。
                .todayInterviewSessionCount(interviewSessionCount)
                .todayResumeDiagnosisCount(resumeDiagnosisCount)
                .build();
    }

    @Override
    @Cacheable(value = "admin:dashboardTrends", key = "#startDate + ':' + #endDate", unless = "#result == null || #result.isEmpty()")
    public List<DashboardTrendResponse> getDashboardTrends(LocalDate startDate, LocalDate endDate) {
        DateRange range = resolveDateRange(startDate, endDate, DateRangeDefault.LAST_7_DAYS);
        List<DashboardTrendResponse> trends = new ArrayList<>();

        // 按日期升序返回，前端图表可直接消费。
        LocalDate cursor = range.startDate();
        while (!cursor.isAfter(range.endDate())) {
            LocalDateTime dayStart = cursor.atStartOfDay();
            LocalDateTime dayEnd = cursor.plusDays(1).atStartOfDay();

            trends.add(DashboardTrendResponse.builder()
                    .date(cursor)
                    // 面试趋势口径使用 interview_session.create_time。
                    .interviewSessionCount(countInterviewSessionsBetween(dayStart, dayEnd))
                    // 简历趋势口径使用 resume_diagnosis_task.create_time。
                    .resumeDiagnosisCount(countResumeTasksBetween(dayStart, dayEnd))
                    .build());
            cursor = cursor.plusDays(1);
        }

        return trends;
    }

    @Override
    public List<HotJobRoleResponse> getHotJobRoles(LocalDate startDate, LocalDate endDate, Integer limit) {
        DateRange range = resolveDateRange(startDate, endDate, DateRangeDefault.LAST_7_DAYS);
        int safeLimit = normalizeHotRoleLimit(limit);

        QueryWrapper<InterviewSession> wrapper = new QueryWrapper<>();
        wrapper.select("job_role AS jobRole", "COUNT(*) AS sessionCount")
                .isNotNull("job_role")
                .ne("job_role", "")
                // 热门排行沿用面试 create_time 统计口径。
                .ge("create_time", range.startDateTime())
                .lt("create_time", range.endExclusiveDateTime())
                .groupBy("job_role")
                .orderByDesc("sessionCount")
                .last("LIMIT " + safeLimit);

        List<Map<String, Object>> rows = interviewSessionService.listMaps(wrapper);
        List<HotJobRoleResponse> ranking = new ArrayList<>(rows.size());

        for (Map<String, Object> row : rows) {
            ranking.add(HotJobRoleResponse.builder()
                    .jobRole(toStringValue(row.get("jobRole")))
                    .sessionCount(toLongValue(row.get("sessionCount")))
                    .build());
        }
        return ranking;
    }

    @Override
    public BusinessDistributionResponse getBusinessDistribution(LocalDate startDate, LocalDate endDate) {
        DateRange range = resolveDateRange(startDate, endDate, DateRangeDefault.LAST_7_DAYS);

        long interviewCount = countInterviewSessionsBetween(range.startDateTime(), range.endExclusiveDateTime());
        long resumeCount = countResumeTasksBetween(range.startDateTime(), range.endExclusiveDateTime());
        long totalCount = interviewCount + resumeCount;

        return BusinessDistributionResponse.builder()
                .startDate(range.startDate().toString())
                .endDate(range.endDate().toString())
                .interviewCount(interviewCount)
                .resumeCount(resumeCount)
                .totalCount(totalCount)
                .interviewPercent(calculatePercent(interviewCount, totalCount))
                .resumePercent(calculatePercent(resumeCount, totalCount))
                .build();
    }

    @Override
    public MonitorOverviewResponse getMonitorOverview() {
        LocalDateTime todayStart = LocalDate.now().atStartOfDay();
        LocalDateTime tomorrowStart = LocalDate.now().plusDays(1).atStartOfDay();

        long pendingResumeTaskCount = resumeDiagnosisTaskService.count(
                new LambdaQueryWrapper<ResumeDiagnosisTask>()
                        .eq(ResumeDiagnosisTask::getStatus, ResumeDiagnosisConstants.STATUS_PENDING)
        );
        long processingResumeTaskCount = resumeDiagnosisTaskService.count(
                new LambdaQueryWrapper<ResumeDiagnosisTask>()
                        .eq(ResumeDiagnosisTask::getStatus, ResumeDiagnosisConstants.STATUS_PROCESSING)
        );
        long failedResumeTaskCount = resumeDiagnosisTaskService.count(
                new LambdaQueryWrapper<ResumeDiagnosisTask>()
                        .eq(ResumeDiagnosisTask::getStatus, ResumeDiagnosisConstants.STATUS_FAILED)
        );
        long activeInterviewSessionCount = interviewSessionService.count(
                new LambdaQueryWrapper<InterviewSession>()
                        .eq(InterviewSession::getStatus, InterviewConstants.STATUS_IN_PROGRESS)
        );

        return MonitorOverviewResponse.builder()
                .pendingResumeTaskCount(pendingResumeTaskCount)
                .processingResumeTaskCount(processingResumeTaskCount)
                .failedResumeTaskCount(failedResumeTaskCount)
                .activeInterviewSessionCount(activeInterviewSessionCount)
                .todayInterviewSessionCount(countInterviewSessionsBetween(todayStart, tomorrowStart))
                .todayResumeDiagnosisCount(countResumeTasksBetween(todayStart, tomorrowStart))
                .build();
    }

    /**
     * 统计 [start, end) 区间内的面试会话数。
     */
    private long countInterviewSessionsBetween(LocalDateTime start, LocalDateTime end) {
        return interviewSessionService.count(new LambdaQueryWrapper<InterviewSession>()
                .ge(InterviewSession::getCreateTime, start)
                .lt(InterviewSession::getCreateTime, end));
    }

    /**
     * 统计 [start, end) 区间内的简历诊断任务数。
     */
    private long countResumeTasksBetween(LocalDateTime start, LocalDateTime end) {
        return resumeDiagnosisTaskService.count(new LambdaQueryWrapper<ResumeDiagnosisTask>()
                .ge(ResumeDiagnosisTask::getCreateTime, start)
                .lt(ResumeDiagnosisTask::getCreateTime, end));
    }

    /**
     * 构建安全日期范围并执行默认值与校验：
     * 1. startDate 不能大于 endDate
     * 2. 查询范围不能超过 MAX_QUERY_DAYS
     */
    private DateRange resolveDateRange(LocalDate startDate, LocalDate endDate, DateRangeDefault defaultType) {
        LocalDate safeStart = startDate;
        LocalDate safeEnd = endDate;

        if (safeStart == null && safeEnd == null) {
            if (defaultType == DateRangeDefault.TODAY) {
                safeStart = LocalDate.now();
                safeEnd = LocalDate.now();
            } else {
                safeEnd = LocalDate.now();
                safeStart = safeEnd.minusDays(DEFAULT_TREND_DAYS - 1L);
            }
        } else if (safeStart == null) {
            // 仅传 endDate 时，回退为单日查询。
            safeStart = safeEnd;
        } else if (safeEnd == null) {
            // 仅传 startDate 时，回退为单日查询。
            safeEnd = safeStart;
        }

        if (safeStart.isAfter(safeEnd)) {
            throw new BusinessException("startDate 不能大于 endDate");
        }

        long rangeDays = ChronoUnit.DAYS.between(safeStart, safeEnd) + 1;
        if (rangeDays > MAX_QUERY_DAYS) {
            throw new BusinessException("查询范围不能超过 90 天");
        }

        return new DateRange(
                safeStart,
                safeEnd,
                safeStart.atStartOfDay(),
                safeEnd.plusDays(1).atStartOfDay()
        );
    }

    /**
     * 归一化热门岗位 limit，避免超大扫描并保持查询稳定。
     */
    private int normalizeHotRoleLimit(Integer limit) {
        if (limit == null) {
            return DEFAULT_HOT_ROLE_LIMIT;
        }
        if (limit <= 0) {
            throw new BusinessException("limit 必须大于 0");
        }
        return Math.min(limit, 50);
    }

    private Double calculatePercent(long count, long total) {
        if (total <= 0) {
            return 0D;
        }
        return BigDecimal.valueOf((double) count * 100D / (double) total)
                .setScale(2, RoundingMode.HALF_UP)
                .doubleValue();
    }

    private Long toLongValue(Object value) {
        if (value instanceof Number number) {
            return number.longValue();
        }
        return value == null ? 0L : Long.parseLong(String.valueOf(value));
    }

    private String toStringValue(Object value) {
        return value == null ? "" : String.valueOf(value);
    }

    private enum DateRangeDefault {
        TODAY,
        LAST_7_DAYS
    }

    /**
     * 供所有看板查询复用的不可变归一化日期范围对象。
     */
    private record DateRange(LocalDate startDate,
                             LocalDate endDate,
                             LocalDateTime startDateTime,
                             LocalDateTime endExclusiveDateTime) {
    }
}
