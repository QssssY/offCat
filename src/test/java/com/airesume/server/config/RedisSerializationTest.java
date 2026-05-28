package com.airesume.server.config;

import com.airesume.server.common.result.Result;
import com.airesume.server.dto.admin.BusinessDistributionResponse;
import com.airesume.server.dto.admin.DashboardOverviewResponse;
import com.airesume.server.dto.growth.GrowthOverviewResponse;
import com.airesume.server.dto.resume.ResumeDiagnosisTaskResponse;
import com.airesume.server.dto.resume.ResumeJobMatchAnalyzeResponse;
import com.airesume.server.dto.resume.ResumePolishAnalyzeResponse;
import com.airesume.server.vo.membership.MembershipPlanVO;
import org.junit.jupiter.api.Test;
import org.springframework.data.redis.serializer.RedisSerializer;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Redis 序列化回归测试。
 * 重点覆盖会员中心、成长中心和任务轮询接口涉及的缓存对象。
 */
class RedisSerializationTest {

    private final RedisSerializer<Object> serializer = new RedisConfig().redisValueSerializer();

    @Test
    void shouldRoundTripMembershipPlanList() {
        List<MembershipPlanVO> expected = List.of(
                MembershipPlanVO.builder()
                        .planCode("vip_month")
                        .planName("Monthly VIP")
                        .description("会员有效期 30 天，有效期内每日 5 次简历诊断、每日 10 次模拟面试")
                        .priceAmount(new BigDecimal("29.90"))
                        .durationDays(30)
                        .resumeQuota(5)
                        .interviewQuota(10)
                        .build()
        );

        Object actual = roundTrip(expected);

        assertThat(actual).isInstanceOf(List.class);
        assertThat(actual).usingRecursiveComparison().isEqualTo(expected);
    }

    @Test
    void shouldRoundTripGrowthOverview() {
        GrowthOverviewResponse expected = GrowthOverviewResponse.builder()
                .summary(GrowthOverviewResponse.SummaryVO.builder()
                        .latestResumeScore(82)
                        .latestInterviewScore(76)
                        .latestJobMatchScore(79)
                        .resumeDiagnosisCount(12)
                        .mockInterviewCount(8)
                        .jobMatchCount(6)
                        .polishCount(3)
                        .build())
                .resumeScoreTrend(List.of(
                        GrowthOverviewResponse.ScoreTrendItem.builder().date("05/01").score(75).build(),
                        GrowthOverviewResponse.ScoreTrendItem.builder().date("05/08").score(82).build()
                ))
                .interviewScoreTrend(List.of(
                        GrowthOverviewResponse.ScoreTrendItem.builder().date("05/02").score(70).build(),
                        GrowthOverviewResponse.ScoreTrendItem.builder().date("05/09").score(76).build()
                ))
                .latestJobMatch(GrowthOverviewResponse.LatestJobMatchVO.builder()
                        .matchScore(79)
                        .matchedKeywords(List.of("Spring Boot", "Redis"))
                        .missingKeywords(List.of("消息队列"))
                        .suggestions(List.of("补充项目中的高并发缓存实践"))
                        .createTime("2026-05-09 00:30:00")
                        .build())
                .latestPolish(GrowthOverviewResponse.LatestPolishVO.builder()
                        .sourceType("JD_MATCH")
                        .modificationNotes(List.of("补充量化结果", "突出 Redis 命中率优化"))
                        .createTime("2026-05-09 00:35:00")
                        .build())
                .latestInterviewFeedback(GrowthOverviewResponse.LatestInterviewFeedbackVO.builder()
                        .jobRole("Java 后端工程师")
                        .interviewMode("mock")
                        .comprehensiveScore(76)
                        .evaluationReport("表达较清晰，但缓存一致性追问回答不够深入。")
                        .jobTargetedFeedback("建议补充 Redis 序列化方案与失效策略。")
                        .createTime("2026-05-09 00:40:00")
                        .build())
                .weaknessSummary(GrowthOverviewResponse.WeaknessSummaryVO.builder()
                        .resumeWeaknesses(List.of("项目指标不够量化"))
                        .jobMatchWeaknesses(List.of("消息队列经验描述较弱"))
                        .interviewWeaknesses(List.of("缓存兼容性说明不完整"))
                        .suggestions(List.of("补充 Redis 缓存迁移方案"))
                        .build())
                .growthConfig(GrowthOverviewResponse.GrowthConfigVO.builder()
                        .encouragementMessages(List.of("本周面试表达有进步，建议继续复盘最近一次反馈。"))
                        .milestones(List.of(GrowthOverviewResponse.MilestoneConfigVO.builder()
                                .configKey("milestone_first_interview")
                                .title("完成第一次模拟面试")
                                .description("开始沉淀面试反馈")
                                .sort(1)
                                .build()))
                        .build())
                .build();

        Object actual = roundTrip(expected);

        assertThat(actual).isInstanceOf(GrowthOverviewResponse.class);
        assertThat(actual).usingRecursiveComparison().isEqualTo(expected);
    }

    @Test
    void shouldRoundTripResumeDiagnosisTaskResponse() {
        ResumeDiagnosisTaskResponse expected = ResumeDiagnosisTaskResponse.builder()
                .taskId("task-001")
                .userId(1001L)
                .fileUrl("/uploads/resume.pdf")
                .status(2)
                .statusDesc("已完成")
                .diagnosisResult("{\"score\":88}")
                .errorMsg(null)
                .resumeText("五年 Java 后端开发经验")
                .parseMode("TEXT")
                .parseMessage("原生文本解析成功")
                .latestJobMatchAnalysis(ResumeJobMatchAnalyzeResponse.builder()
                        .analysisId("match-001")
                        .resumeTaskId("task-001")
                        .matchScore(84)
                        .matchedKeywords(List.of("Java", "Redis"))
                        .missingKeywords(List.of("Kafka"))
                        .suggestions(List.of("补充消息队列项目经历"))
                        .analysisSummary("核心技术匹配度较高")
                        .createTime(LocalDateTime.of(2026, 5, 9, 0, 45))
                        .build())
                .latestPolishResult(ResumePolishAnalyzeResponse.builder()
                        .polishRecordId("polish-001")
                        .resumeTaskId("task-001")
                        .polishedResumeText("优化后的简历内容")
                        .modificationNotes(List.of("强化性能优化成果"))
                        .sourceType("RESUME_DIAGNOSIS")
                        .createTime(LocalDateTime.of(2026, 5, 9, 0, 46))
                        .build())
                .createTime(LocalDateTime.of(2026, 5, 9, 0, 44))
                .updateTime(LocalDateTime.of(2026, 5, 9, 0, 47))
                .build();

        Object actual = roundTrip(expected);

        assertThat(actual).isInstanceOf(ResumeDiagnosisTaskResponse.class);
        assertThat(actual).usingRecursiveComparison().isEqualTo(expected);
    }

    @Test
    void shouldRoundTripResultWrapper() {
        Result<Map<String, Long>> expected = Result.success(Map.of(
                "resumeCountThisMonth", 12L,
                "interviewCountThisMonth", 5L
        ));

        Object actual = roundTrip(expected);

        assertThat(actual).isInstanceOf(Result.class);
        assertThat(actual).usingRecursiveComparison().isEqualTo(expected);
    }

    @Test
    void shouldRoundTripDashboardOverviewResponse() {
        DashboardOverviewResponse expected = DashboardOverviewResponse.builder()
                .totalUserCount(100L)
                .vipUserCount(20L)
                .activePromptCount(8L)
                .activeJobRoleCount(12L)
                .activeAiEngineCount(3L)
                .todayInterviewSessionCount(15L)
                .todayResumeDiagnosisCount(18L)
                .feedbackCount(4L)
                .communityPostCount(6L)
                .resumePolishCount(7L)
                .jdMatchCount(9L)
                .orderCount(5L)
                .orderRevenue(new BigDecimal("199.00"))
                .build();

        Object actual = roundTrip(expected);

        assertThat(actual).isInstanceOf(DashboardOverviewResponse.class);
        assertThat(actual).usingRecursiveComparison().isEqualTo(expected);
    }

    @Test
    void shouldRoundTripBusinessDistributionResponse() {
        BusinessDistributionResponse expected = BusinessDistributionResponse.builder()
                .startDate("2026-05-22")
                .endDate("2026-05-28")
                .interviewCount(30L)
                .resumeCount(40L)
                .resumePolishCount(12L)
                .jdMatchCount(16L)
                .communityPostCount(2L)
                .totalCount(100L)
                .interviewPercent(30.00)
                .resumePercent(40.00)
                .polishPercent(12.00)
                .jdMatchPercent(16.00)
                .communityPercent(2.00)
                .build();

        Object actual = roundTrip(expected);

        assertThat(actual).isInstanceOf(BusinessDistributionResponse.class);
        assertThat(actual).usingRecursiveComparison().isEqualTo(expected);
    }

    private Object roundTrip(Object value) {
        byte[] bytes = serializer.serialize(value);
        assertThat(bytes).isNotNull();
        return serializer.deserialize(bytes);
    }
}
