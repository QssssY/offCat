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
import com.airesume.server.entity.*;
import com.airesume.server.mapper.InterviewSessionMapper;
import com.airesume.server.mapper.MembershipOrderMapper;
import com.airesume.server.mapper.ResumeDiagnosisTaskMapper;
import com.airesume.server.mapper.CommunityPostMapper;
import org.springframework.cache.annotation.Cacheable;
import com.airesume.server.service.*;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.sql.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;

import org.springframework.beans.factory.annotation.Qualifier;

/**
 * 应用层看板与监控实现。
 *
 * 该实现只聚合业务表数据，刻意不依赖中间件深度指标，
 * 以便在 Redis/RabbitMQ 监控尚未接入时也可稳定运行。
 */
@Service
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
    private final InterviewSessionMapper interviewSessionMapper;
    private final ResumeDiagnosisTaskMapper resumeDiagnosisTaskMapper;
    private final UserFeedbackService userFeedbackService;
    private final CommunityPostMapper communityPostMapper;
    private final ResumePolishService resumePolishService;
    private final ResumeJobMatchService resumeJobMatchService;
    private final MembershipOrderService membershipOrderService;
    private final MembershipOrderMapper membershipOrderMapper;
    private final Executor dashboardExecutor;

    public AdminDashboardServiceImpl(
            SysUserService sysUserService,
            SysPromptService sysPromptService,
            SysJobRoleService sysJobRoleService,
            SysAiEngineConfigService sysAiEngineConfigService,
            InterviewSessionService interviewSessionService,
            ResumeDiagnosisTaskService resumeDiagnosisTaskService,
            InterviewSessionMapper interviewSessionMapper,
            ResumeDiagnosisTaskMapper resumeDiagnosisTaskMapper,
            UserFeedbackService userFeedbackService,
            CommunityPostMapper communityPostMapper,
            ResumePolishService resumePolishService,
            ResumeJobMatchService resumeJobMatchService,
            MembershipOrderService membershipOrderService,
            MembershipOrderMapper membershipOrderMapper,
            @Qualifier("dashboardExecutor") Executor dashboardExecutor) {
        this.sysUserService = sysUserService;
        this.sysPromptService = sysPromptService;
        this.sysJobRoleService = sysJobRoleService;
        this.sysAiEngineConfigService = sysAiEngineConfigService;
        this.interviewSessionService = interviewSessionService;
        this.resumeDiagnosisTaskService = resumeDiagnosisTaskService;
        this.interviewSessionMapper = interviewSessionMapper;
        this.resumeDiagnosisTaskMapper = resumeDiagnosisTaskMapper;
        this.userFeedbackService = userFeedbackService;
        this.communityPostMapper = communityPostMapper;
        this.resumePolishService = resumePolishService;
        this.resumeJobMatchService = resumeJobMatchService;
        this.membershipOrderService = membershipOrderService;
        this.membershipOrderMapper = membershipOrderMapper;
        this.dashboardExecutor = dashboardExecutor;
    }

    @Override
    @Cacheable(value = "admin:dashboardOverview", key = "#startDate + ':' + #endDate", sync = true)
    public DashboardOverviewResponse getDashboardOverview(LocalDate startDate, LocalDate endDate) {
        DateRange range = resolveDateRange(startDate, endDate, DateRangeDefault.TODAY);

        // 全局配置统计（无日期范围，变化频率低）
        CompletableFuture<Long> totalUserFuture = supplyAsync(() -> sysUserService.count());
        CompletableFuture<Long> vipUserFuture = supplyAsync(() -> sysUserService.count(
                new LambdaQueryWrapper<SysUser>().eq(SysUser::getRole, UserRoleConstants.ROLE_VIP)));
        CompletableFuture<Long> activePromptFuture = supplyAsync(() -> sysPromptService.count(
                new LambdaQueryWrapper<SysPrompt>().eq(SysPrompt::getIsActive, PromptConstants.ACTIVE)));
        CompletableFuture<Long> activeJobRoleFuture = supplyAsync(() -> sysJobRoleService.count(
                new LambdaQueryWrapper<SysJobRole>().eq(SysJobRole::getIsActive, PromptConstants.ACTIVE)));
        CompletableFuture<Long> activeAiEngineFuture = supplyAsync(() -> sysAiEngineConfigService.count(
                new LambdaQueryWrapper<SysAiEngineConfig>().eq(SysAiEngineConfig::getIsActive, AiEngineConstants.ACTIVE)));

        // 日期范围内的业务统计
        CompletableFuture<Long> interviewFuture = supplyAsync(() -> countInterviewSessionsBetween(range.startDateTime(), range.endExclusiveDateTime()));
        CompletableFuture<Long> resumeFuture = supplyAsync(() -> countResumeTasksBetween(range.startDateTime(), range.endExclusiveDateTime()));
        CompletableFuture<Long> feedbackFuture = supplyAsync(() -> countFeedbackBetween(range.startDateTime(), range.endExclusiveDateTime()));
        CompletableFuture<Long> communityFuture = supplyAsync(() -> countCommunityPostsBetween(range.startDateTime(), range.endExclusiveDateTime()));
        CompletableFuture<Long> polishFuture = supplyAsync(() -> countPolishBetween(range.startDateTime(), range.endExclusiveDateTime()));
        CompletableFuture<Long> jdMatchFuture = supplyAsync(() -> countJdMatchBetween(range.startDateTime(), range.endExclusiveDateTime()));
        CompletableFuture<Long> orderCountFuture = supplyAsync(() -> countOrdersBetween(range.startDateTime(), range.endExclusiveDateTime()));
        CompletableFuture<BigDecimal> orderRevenueFuture = supplyAsync(() -> sumPaidOrderRevenue(range.startDateTime(), range.endExclusiveDateTime()));

        // 等待所有查询完成
        CompletableFuture.allOf(
                totalUserFuture, vipUserFuture, activePromptFuture, activeJobRoleFuture, activeAiEngineFuture,
                interviewFuture, resumeFuture, feedbackFuture, communityFuture, polishFuture,
                jdMatchFuture, orderCountFuture, orderRevenueFuture
        ).join();

        return DashboardOverviewResponse.builder()
                .totalUserCount(totalUserFuture.join())
                .vipUserCount(vipUserFuture.join())
                .activePromptCount(activePromptFuture.join())
                .activeJobRoleCount(activeJobRoleFuture.join())
                .activeAiEngineCount(activeAiEngineFuture.join())
                .todayInterviewSessionCount(interviewFuture.join())
                .todayResumeDiagnosisCount(resumeFuture.join())
                .feedbackCount(feedbackFuture.join())
                .communityPostCount(communityFuture.join())
                .resumePolishCount(polishFuture.join())
                .jdMatchCount(jdMatchFuture.join())
                .orderCount(orderCountFuture.join())
                .orderRevenue(orderRevenueFuture.join())
                .build();
    }

    @Override
    @Cacheable(value = "admin:dashboardTrends", key = "#startDate + ':' + #endDate", sync = true)
    public List<DashboardTrendResponse> getDashboardTrends(LocalDate startDate, LocalDate endDate) {
        DateRange range = resolveDateRange(startDate, endDate, DateRangeDefault.LAST_7_DAYS);
        Map<LocalDate, Long> interviewCountByDate = toCountByDateMap(
                interviewSessionMapper.countByCreateDate(range.startDateTime(), range.endExclusiveDateTime()));
        Map<LocalDate, Long> resumeCountByDate = toCountByDateMap(
                resumeDiagnosisTaskMapper.countByCreateDate(range.startDateTime(), range.endExclusiveDateTime()));

        // 订单趋势：按日聚合数量和收入
        List<Map<String, Object>> orderRows = membershipOrderMapper.countByCreateDate(
                range.startDateTime(), range.endExclusiveDateTime());
        Map<LocalDate, long[]> orderCountByDate = toOrderCountByDateMap(orderRows);

        List<DashboardTrendResponse> trends = new ArrayList<>();

        // 按日期升序返回，前端图表可直接消费。
        LocalDate cursor = range.startDate();
        while (!cursor.isAfter(range.endDate())) {
            long[] orderStats = orderCountByDate.getOrDefault(cursor, new long[]{0, 0});
            trends.add(DashboardTrendResponse.builder()
                    .date(cursor)
                    // 面试趋势口径使用 interview_session.create_time 聚合结果，缺失日期补 0。
                    .interviewSessionCount(interviewCountByDate.getOrDefault(cursor, 0L))
                    // 简历趋势口径使用 resume_diagnosis_task.create_time 聚合结果，缺失日期补 0。
                    .resumeDiagnosisCount(resumeCountByDate.getOrDefault(cursor, 0L))
                    .orderCount(orderStats[0])
                    .orderRevenue(BigDecimal.valueOf(orderStats[1]))
                    .build());
            cursor = cursor.plusDays(1);
        }

        return trends;
    }

    @Override
    @Cacheable(value = "admin:hotJobRoles", key = "#startDate + ':' + #endDate + ':' + #limit", sync = true)
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
    @Cacheable(value = "admin:dashboardDistribution", key = "#startDate + ':' + #endDate", sync = true)
    public BusinessDistributionResponse getBusinessDistribution(LocalDate startDate, LocalDate endDate) {
        DateRange range = resolveDateRange(startDate, endDate, DateRangeDefault.LAST_7_DAYS);

        // 并行查询所有业务维度计数，复用 overview 提取的独立 count 方法
        CompletableFuture<Long> interviewFuture = supplyAsync(() -> countInterviewSessionsBetween(range.startDateTime(), range.endExclusiveDateTime()));
        CompletableFuture<Long> resumeFuture = supplyAsync(() -> countResumeTasksBetween(range.startDateTime(), range.endExclusiveDateTime()));
        CompletableFuture<Long> polishFuture = supplyAsync(() -> countPolishBetween(range.startDateTime(), range.endExclusiveDateTime()));
        CompletableFuture<Long> jdMatchFuture = supplyAsync(() -> countJdMatchBetween(range.startDateTime(), range.endExclusiveDateTime()));
        CompletableFuture<Long> communityFuture = supplyAsync(() -> countCommunityPostsBetween(range.startDateTime(), range.endExclusiveDateTime()));
        CompletableFuture<Long> orderFuture = supplyAsync(() -> countOrdersBetween(range.startDateTime(), range.endExclusiveDateTime()));

        CompletableFuture.allOf(interviewFuture, resumeFuture, polishFuture, jdMatchFuture, communityFuture, orderFuture).join();

        long interviewCount = interviewFuture.join();
        long resumeCount = resumeFuture.join();
        long polishCount = polishFuture.join();
        long jdMatchCount = jdMatchFuture.join();
        long communityCount = communityFuture.join();
        long orderCount = orderFuture.join();
        long totalCount = interviewCount + resumeCount + polishCount + jdMatchCount + communityCount + orderCount;

        return BusinessDistributionResponse.builder()
                .startDate(range.startDate().toString())
                .endDate(range.endDate().toString())
                .interviewCount(interviewCount)
                .resumeCount(resumeCount)
                .resumePolishCount(polishCount)
                .jdMatchCount(jdMatchCount)
                .communityPostCount(communityCount)
                .totalCount(totalCount)
                .interviewPercent(calculatePercent(interviewCount, totalCount))
                .resumePercent(calculatePercent(resumeCount, totalCount))
                .polishPercent(calculatePercent(polishCount, totalCount))
                .jdMatchPercent(calculatePercent(jdMatchCount, totalCount))
                .communityPercent(calculatePercent(communityCount, totalCount))
                .build();
    }

    @Override
    @Cacheable(value = "admin:monitorOverview", sync = true)
    public MonitorOverviewResponse getMonitorOverview() {
        LocalDateTime todayStart = LocalDate.now().atStartOfDay();
        LocalDateTime tomorrowStart = LocalDate.now().plusDays(1).atStartOfDay();

        // 并行执行 6 个统计查询，复用看板线程池
        CompletableFuture<Long> pendingFuture = supplyAsync(() -> resumeDiagnosisTaskService.count(
                new LambdaQueryWrapper<ResumeDiagnosisTask>()
                        .eq(ResumeDiagnosisTask::getStatus, ResumeDiagnosisConstants.STATUS_PENDING)));
        CompletableFuture<Long> processingFuture = supplyAsync(() -> resumeDiagnosisTaskService.count(
                new LambdaQueryWrapper<ResumeDiagnosisTask>()
                        .eq(ResumeDiagnosisTask::getStatus, ResumeDiagnosisConstants.STATUS_PROCESSING)));
        CompletableFuture<Long> failedFuture = supplyAsync(() -> resumeDiagnosisTaskService.count(
                new LambdaQueryWrapper<ResumeDiagnosisTask>()
                        .eq(ResumeDiagnosisTask::getStatus, ResumeDiagnosisConstants.STATUS_FAILED)));
        CompletableFuture<Long> activeInterviewFuture = supplyAsync(() -> interviewSessionService.count(
                new LambdaQueryWrapper<InterviewSession>()
                        .eq(InterviewSession::getStatus, InterviewConstants.STATUS_IN_PROGRESS)));
        CompletableFuture<Long> todayInterviewFuture = supplyAsync(() -> countInterviewSessionsBetween(todayStart, tomorrowStart));
        CompletableFuture<Long> todayResumeFuture = supplyAsync(() -> countResumeTasksBetween(todayStart, tomorrowStart));

        CompletableFuture.allOf(pendingFuture, processingFuture, failedFuture,
                activeInterviewFuture, todayInterviewFuture, todayResumeFuture).join();

        return MonitorOverviewResponse.builder()
                .pendingResumeTaskCount(pendingFuture.join())
                .processingResumeTaskCount(processingFuture.join())
                .failedResumeTaskCount(failedFuture.join())
                .activeInterviewSessionCount(activeInterviewFuture.join())
                .todayInterviewSessionCount(todayInterviewFuture.join())
                .todayResumeDiagnosisCount(todayResumeFuture.join())
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

    /** 统计区间内用户反馈数。 */
    private long countFeedbackBetween(LocalDateTime start, LocalDateTime end) {
        return userFeedbackService.count(new LambdaQueryWrapper<UserFeedback>()
                .ge(UserFeedback::getCreateTime, start).lt(UserFeedback::getCreateTime, end));
    }

    /** 统计区间内社区帖子数。 */
    private long countCommunityPostsBetween(LocalDateTime start, LocalDateTime end) {
        return communityPostMapper.selectCount(new LambdaQueryWrapper<CommunityPost>()
                .ge(CommunityPost::getCreateTime, start).lt(CommunityPost::getCreateTime, end));
    }

    /** 统计区间内简历润色数。 */
    private long countPolishBetween(LocalDateTime start, LocalDateTime end) {
        return resumePolishService.count(new LambdaQueryWrapper<ResumePolishRecord>()
                .ge(ResumePolishRecord::getCreateTime, start).lt(ResumePolishRecord::getCreateTime, end));
    }

    /** 统计区间内JD匹配分析数。 */
    private long countJdMatchBetween(LocalDateTime start, LocalDateTime end) {
        return resumeJobMatchService.count(new LambdaQueryWrapper<ResumeJobMatchRecord>()
                .ge(ResumeJobMatchRecord::getCreateTime, start).lt(ResumeJobMatchRecord::getCreateTime, end));
    }

    /** 统计区间内订单数。 */
    private long countOrdersBetween(LocalDateTime start, LocalDateTime end) {
        return membershipOrderService.count(new LambdaQueryWrapper<MembershipOrder>()
                .ge(MembershipOrder::getCreateTime, start).lt(MembershipOrder::getCreateTime, end));
    }

    /** CompletableFuture 包装，使用看板专用线程池。 */
    private <T> CompletableFuture<T> supplyAsync(java.util.function.Supplier<T> supplier) {
        return CompletableFuture.supplyAsync(supplier, dashboardExecutor);
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

    /**
     * 将 Mapper 聚合行转换为日期到数量的映射，兼容 JDBC Date、LocalDate 和字符串日期。
     */
    private Map<LocalDate, Long> toCountByDateMap(List<Map<String, Object>> rows) {
        Map<LocalDate, Long> countByDate = new HashMap<>();
        if (rows == null || rows.isEmpty()) {
            return countByDate;
        }
        for (Map<String, Object> row : rows) {
            LocalDate statDate = toLocalDateValue(row.get("statDate"));
            countByDate.put(statDate, toLongValue(row.get("totalCount")));
        }
        return countByDate;
    }

    /**
     * 将订单聚合行转换为日期到 [数量, 收入(分)] 的映射。
     */
    private Map<LocalDate, long[]> toOrderCountByDateMap(List<Map<String, Object>> rows) {
        Map<LocalDate, long[]> map = new HashMap<>();
        if (rows == null || rows.isEmpty()) {
            return map;
        }
        for (Map<String, Object> row : rows) {
            LocalDate statDate = toLocalDateValue(row.get("statDate"));
            long count = toLongValue(row.get("totalCount"));
            // 收入保留2位小数转为 long（乘以100），避免浮点精度问题
            Object revenueObj = row.get("totalRevenue");
            long revenueCents = toBigDecimalValue(revenueObj).multiply(BigDecimal.valueOf(100)).longValue();
            map.put(statDate, new long[]{count, revenueCents});
        }
        return map;
    }

    /**
     * 通过 SQL 聚合统计指定时间范围内已支付订单的收入总额，避免全量加载实体。
     */
    private BigDecimal sumPaidOrderRevenue(LocalDateTime start, LocalDateTime end) {
        BigDecimal result = membershipOrderMapper.sumPaidRevenue(start, end);
        return result != null ? result : BigDecimal.ZERO;
    }

    private BigDecimal toBigDecimalValue(Object value) {
        if (value instanceof BigDecimal bd) {
            return bd;
        }
        if (value instanceof Number number) {
            return BigDecimal.valueOf(number.doubleValue());
        }
        if (value == null) {
            return BigDecimal.ZERO;
        }
        try {
            return new BigDecimal(String.valueOf(value));
        } catch (NumberFormatException e) {
            return BigDecimal.ZERO;
        }
    }

    private LocalDate toLocalDateValue(Object value) {
        if (value instanceof LocalDate localDate) {
            return localDate;
        }
        if (value instanceof Date sqlDate) {
            return sqlDate.toLocalDate();
        }
        return LocalDate.parse(String.valueOf(value));
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
