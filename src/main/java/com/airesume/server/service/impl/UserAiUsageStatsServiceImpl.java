package com.airesume.server.service.impl;

import com.airesume.server.common.constants.UserAiConstants;
import com.airesume.server.common.exception.BusinessException;
import com.airesume.server.dto.admin.CustomAiUsageStatsResponse;
import com.airesume.server.dto.admin.CustomAiUsageTrendActiveUserRow;
import com.airesume.server.dto.admin.CustomAiUsageTrendDayResponse;
import com.airesume.server.dto.admin.CustomAiUsageTrendResponse;
import com.airesume.server.dto.admin.CustomAiUsageTrendTypeStatRow;
import com.airesume.server.dto.admin.CustomAiUsageTypeStatResponse;
import com.airesume.server.dto.admin.CustomAiUserUsageStatResponse;
import com.airesume.server.dto.admin.CustomAiUserUsageTypeStatResponse;
import com.airesume.server.mapper.UserAiUsageDetailMapper;
import com.airesume.server.service.UserAiUsageStatsService;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

/**
 * 用户自定义 AI 用量统计服务实现。
 */
@Service
@RequiredArgsConstructor
public class UserAiUsageStatsServiceImpl implements UserAiUsageStatsService {

    private static final int MAX_PAGE = 10_000;
    private static final int MAX_PAGE_SIZE = 100;
    private static final int DEFAULT_TREND_DAYS = 7;
    private static final int MAX_TREND_DAYS = 90;

    private final UserAiUsageDetailMapper userAiUsageDetailMapper;

    @Override
    public CustomAiUsageStatsResponse getDailyStats(LocalDate date, LocalDate startDate, LocalDate endDate,
                                                    int page, int pageSize) {
        DateRange range = resolveStatsDateRange(date, startDate, endDate);
        // Service 层再次收敛分页边界，防止绕过 Controller 的调用传入异常页码。
        int normalizedPage = Math.min(MAX_PAGE, Math.max(1, page));
        int normalizedPageSize = Math.min(MAX_PAGE_SIZE, Math.max(1, pageSize));
        int offset = (normalizedPage - 1) * normalizedPageSize;

        List<CustomAiUsageTypeStatResponse> typeStats = withLabels(
                userAiUsageDetailMapper.selectTypeStats(range.startDate(), range.endDate()));
        List<CustomAiUserUsageStatResponse> userStats =
                userAiUsageDetailMapper.selectUserStatsPage(range.startDate(), range.endDate(), offset, normalizedPageSize);
        attachUserTypeStats(range, userStats);

        return CustomAiUsageStatsResponse.builder()
                .date(range.startDate().equals(range.endDate()) ? range.startDate() : null)
                .startDate(range.startDate())
                .endDate(range.endDate())
                .configuredUserCount(userAiUsageDetailMapper.countConfiguredUsers())
                .activeUserCount(userAiUsageDetailMapper.countActiveUsers(range.startDate(), range.endDate()))
                .totalCalls(userAiUsageDetailMapper.sumTotalCalls(range.startDate(), range.endDate()))
                .totalUsers(userAiUsageDetailMapper.countUserStats(range.startDate(), range.endDate()))
                .page(normalizedPage)
                .pageSize(normalizedPageSize)
                .typeStats(typeStats)
                .userStats(userStats)
                .build();
    }

    @Override
    public CustomAiUsageTrendResponse getUsageTrends(LocalDate startDate, LocalDate endDate) {
        DateRange range = resolveTrendDateRange(startDate, endDate);
        Map<LocalDate, Map<String, Integer>> typeCountsByDate = buildTrendTypeCounts(range);
        Map<LocalDate, Integer> activeUserCountsByDate = buildTrendActiveUserCounts(range);

        List<CustomAiUsageTrendDayResponse> days = new java.util.ArrayList<>();
        int totalCalls = 0;
        int activeUserCount = 0;
        LocalDate cursor = range.startDate();
        while (!cursor.isAfter(range.endDate())) {
            Map<String, Integer> typeCounts = typeCountsByDate.getOrDefault(cursor, Map.of());
            int dayTotalCalls = typeCounts.values().stream().mapToInt(Integer::intValue).sum();
            int dayActiveUserCount = activeUserCountsByDate.getOrDefault(cursor, 0);
            totalCalls += dayTotalCalls;
            activeUserCount += dayActiveUserCount;
            days.add(CustomAiUsageTrendDayResponse.builder()
                    .date(cursor)
                    .totalCalls(dayTotalCalls)
                    .activeUserCount(dayActiveUserCount)
                    .typeStats(toSortedTypeStats(typeCounts))
                    .build());
            cursor = cursor.plusDays(1);
        }

        return CustomAiUsageTrendResponse.builder()
                .startDate(range.startDate())
                .endDate(range.endDate())
                .totalCalls(totalCalls)
                .activeUserCount(activeUserCount)
                .days(days)
                .build();
    }

    private void attachUserTypeStats(DateRange range, List<CustomAiUserUsageStatResponse> userStats) {
        if (userStats == null || userStats.isEmpty()) {
            return;
        }
        List<Long> userIds = userStats.stream()
                .map(CustomAiUserUsageStatResponse::getUserId)
                .toList();
        // 用户明细和功能分布必须使用同一日期范围，避免表格总调用与功能拆分口径不一致。
        Map<Long, List<CustomAiUsageTypeStatResponse>> statsByUser = userAiUsageDetailMapper
                .selectUserTypeStats(range.startDate(), range.endDate(), userIds)
                .stream()
                .map(this::toTypeStat)
                .collect(Collectors.groupingBy(CustomAiUsageTypeStatResponseWithUser::userId,
                        Collectors.mapping(CustomAiUsageTypeStatResponseWithUser::stat, Collectors.toList())));

        for (CustomAiUserUsageStatResponse userStat : userStats) {
            userStat.setTypeStats(statsByUser.getOrDefault(userStat.getUserId(), List.of()));
        }
    }

    private List<CustomAiUsageTypeStatResponse> withLabels(List<CustomAiUsageTypeStatResponse> stats) {
        if (stats == null) {
            return List.of();
        }
        return stats.stream()
                .map(stat -> CustomAiUsageTypeStatResponse.builder()
                        .usageType(normalizeUsageType(stat.getUsageType()))
                        .usageTypeDesc(resolveUsageTypeDesc(stat.getUsageType()))
                        .callCount(stat.getCallCount() == null ? 0 : stat.getCallCount())
                        .build())
                .toList();
    }

    private Map<LocalDate, Map<String, Integer>> buildTrendTypeCounts(DateRange range) {
        Map<LocalDate, Map<String, Integer>> grouped = new LinkedHashMap<>();
        List<CustomAiUsageTrendTypeStatRow> rows =
                userAiUsageDetailMapper.selectTrendTypeStats(range.startDate(), range.endDate());
        if (rows == null) {
            return grouped;
        }
        for (CustomAiUsageTrendTypeStatRow row : rows) {
            if (row == null || row.getDate() == null) {
                continue;
            }
            String usageType = normalizeUsageType(row.getUsageType());
            int callCount = row.getCallCount() == null ? 0 : row.getCallCount();
            grouped.computeIfAbsent(row.getDate(), ignored -> new LinkedHashMap<>())
                    .merge(usageType, callCount, Integer::sum);
        }
        return grouped;
    }

    private Map<LocalDate, Integer> buildTrendActiveUserCounts(DateRange range) {
        List<CustomAiUsageTrendActiveUserRow> rows =
                userAiUsageDetailMapper.selectTrendActiveUserCounts(range.startDate(), range.endDate());
        if (rows == null) {
            return Map.of();
        }
        return rows.stream()
                .filter(row -> row != null && row.getDate() != null)
                .collect(Collectors.toMap(
                        CustomAiUsageTrendActiveUserRow::getDate,
                        row -> row.getActiveUserCount() == null ? 0 : row.getActiveUserCount(),
                        Integer::sum,
                        LinkedHashMap::new
                ));
    }

    private List<CustomAiUsageTypeStatResponse> toSortedTypeStats(Map<String, Integer> typeCounts) {
        if (typeCounts == null || typeCounts.isEmpty()) {
            return List.of();
        }
        return typeCounts.entrySet().stream()
                .map(entry -> CustomAiUsageTypeStatResponse.builder()
                        .usageType(normalizeUsageType(entry.getKey()))
                        .usageTypeDesc(resolveUsageTypeDesc(entry.getKey()))
                        .callCount(entry.getValue() == null ? 0 : entry.getValue())
                        .build())
                .sorted(Comparator
                        .comparing(CustomAiUsageTypeStatResponse::getCallCount, Comparator.nullsLast(Comparator.reverseOrder()))
                        .thenComparing(CustomAiUsageTypeStatResponse::getUsageType, Comparator.nullsLast(String::compareTo)))
                .toList();
    }

    private DateRange resolveStatsDateRange(LocalDate date, LocalDate startDate, LocalDate endDate) {
        if (date != null) {
            // 旧版 date 参数优先级最高，确保旧前端和外部调用仍按单日统计。
            return new DateRange(date, date);
        }
        LocalDate safeStart = startDate;
        LocalDate safeEnd = endDate;
        if (safeStart == null && safeEnd == null) {
            // usage-stats 的无参兼容语义保持为“今天”，真正的默认近 7 天由新版前端显式传参。
            safeEnd = LocalDate.now();
            safeStart = safeEnd;
        } else if (safeStart == null) {
            safeStart = safeEnd;
        } else if (safeEnd == null) {
            safeEnd = safeStart;
        }
        return validateDateRange(safeStart, safeEnd);
    }

    private DateRange resolveTrendDateRange(LocalDate startDate, LocalDate endDate) {
        LocalDate safeStart = startDate;
        LocalDate safeEnd = endDate;
        if (safeStart == null && safeEnd == null) {
            safeEnd = LocalDate.now();
            safeStart = safeEnd.minusDays(DEFAULT_TREND_DAYS - 1L);
        } else if (safeStart == null) {
            // 只传 endDate 时按单日查询，避免把缺省值误扩为大范围查询。
            safeStart = safeEnd;
        } else if (safeEnd == null) {
            // 只传 startDate 时按单日查询，保持和管理端日期选择交互一致。
            safeEnd = safeStart;
        }
        return validateDateRange(safeStart, safeEnd);
    }

    private DateRange validateDateRange(LocalDate safeStart, LocalDate safeEnd) {
        if (safeStart.isAfter(safeEnd)) {
            throw new BusinessException("startDate 不能大于 endDate");
        }
        long inclusiveDays = ChronoUnit.DAYS.between(safeStart, safeEnd) + 1;
        if (inclusiveDays > MAX_TREND_DAYS) {
            throw new BusinessException("查询范围不能超过 90 天");
        }
        return new DateRange(safeStart, safeEnd);
    }

    private CustomAiUsageTypeStatResponseWithUser toTypeStat(CustomAiUserUsageTypeStatResponse row) {
        CustomAiUsageTypeStatResponse stat = CustomAiUsageTypeStatResponse.builder()
                .usageType(normalizeUsageType(row.getUsageType()))
                .usageTypeDesc(resolveUsageTypeDesc(row.getUsageType()))
                .callCount(row.getCallCount() == null ? 0 : row.getCallCount())
                .build();
        return new CustomAiUsageTypeStatResponseWithUser(row.getUserId(), stat);
    }

    private String normalizeUsageType(String usageType) {
        String normalized = usageType == null ? "" : usageType.trim().toLowerCase();
        if (UserAiConstants.SUPPORTED_USAGE_TYPES.contains(normalized)) {
            return normalized;
        }
        return UserAiConstants.USAGE_TYPE_UNKNOWN;
    }

    private String resolveUsageTypeDesc(String usageType) {
        return UserAiConstants.USAGE_TYPE_LABELS.getOrDefault(
                normalizeUsageType(usageType),
                UserAiConstants.USAGE_TYPE_LABELS.get(UserAiConstants.USAGE_TYPE_UNKNOWN));
    }

    private record CustomAiUsageTypeStatResponseWithUser(
            Long userId,
            CustomAiUsageTypeStatResponse stat
    ) {
    }

    private record DateRange(
            LocalDate startDate,
            LocalDate endDate
    ) {
    }
}
