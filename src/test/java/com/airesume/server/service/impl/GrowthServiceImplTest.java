package com.airesume.server.service.impl;

import com.airesume.server.dto.growth.GrowthOverviewResponse;
import com.airesume.server.dto.growth.InterviewRadarResponse;
import com.airesume.server.entity.InterviewDimensionScore;
import com.airesume.server.entity.InterviewSession;
import com.airesume.server.entity.ResumeDiagnosisTask;
import com.airesume.server.entity.ResumeJobMatchRecord;
import com.airesume.server.entity.ResumePolishRecord;
import com.airesume.server.entity.SysGrowthConfig;
import com.airesume.server.mapper.InterviewDimensionScoreMapper;
import com.airesume.server.mapper.InterviewSessionMapper;
import com.airesume.server.mapper.MockInterviewJobTargetRecordMapper;
import com.airesume.server.mapper.ResumeDiagnosisTaskMapper;
import com.airesume.server.mapper.ResumeJobMatchRecordMapper;
import com.airesume.server.mapper.ResumePolishRecordMapper;
import com.airesume.server.service.SysGrowthConfigService;
import com.baomidou.mybatisplus.core.conditions.Wrapper;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class GrowthServiceImplTest {

    @Mock private ResumeDiagnosisTaskMapper resumeDiagnosisTaskMapper;
    @Mock private ResumeJobMatchRecordMapper resumeJobMatchRecordMapper;
    @Mock private ResumePolishRecordMapper resumePolishRecordMapper;
    @Mock private InterviewSessionMapper interviewSessionMapper;
    @Mock private MockInterviewJobTargetRecordMapper mockInterviewJobTargetRecordMapper;
    @Mock private InterviewDimensionScoreMapper dimensionScoreMapper;
    @Mock private SysGrowthConfigService sysGrowthConfigService;

    private GrowthServiceImpl growthService;

    @BeforeEach
    void setUp() {
        growthService = new GrowthServiceImpl(
                resumeDiagnosisTaskMapper,
                resumeJobMatchRecordMapper,
                resumePolishRecordMapper,
                interviewSessionMapper,
                mockInterviewJobTargetRecordMapper,
                dimensionScoreMapper,
                sysGrowthConfigService,
                new ObjectMapper());
    }

    @Test
    void shouldUseEvaluationReportSessionsForInterviewRadarAndKeepCandidateCountWhenNoScores() {
        Long userId = 123L;
        InterviewSession session = buildEndedSessionWithReport("session-a", userId);
        when(interviewSessionMapper.selectList(any()))
                .thenReturn(List.of(session));
        when(dimensionScoreMapper.selectList(any())).thenReturn(List.of());

        InterviewRadarResponse response = growthService.getInterviewRadar(userId);

        assertNull(response.getLatestRadar());
        assertEquals(1, response.getSessionCount());
        assertEquals(List.of(), response.getDimensionTrends());
        assertEquals(List.of(), response.getBlindSpotTips());
        verify(interviewSessionMapper).selectList(any());
        verify(dimensionScoreMapper).selectList(any());
        verify(dimensionScoreMapper, never()).selectCount(any());
        verify(dimensionScoreMapper, never()).insert(any(InterviewDimensionScore.class));
    }

    @Test
    void shouldBuildInterviewRadarWithScoresTrendsAndBlindSpotTips() {
        Long userId = 123L;
        InterviewSession latest = buildEndedSessionWithReport("session-new", userId);
        latest.setCreateTime(LocalDateTime.of(2026, 5, 21, 10, 0));
        InterviewSession previous = buildEndedSessionWithReport("session-old", userId);
        previous.setCreateTime(LocalDateTime.of(2026, 5, 20, 10, 0));
        when(interviewSessionMapper.selectList(any()))
                .thenReturn(List.of(latest, previous));
        when(dimensionScoreMapper.selectList(any())).thenReturn(List.of(
                buildDimensionScore("session-new", "technicalDepth", 55),
                buildDimensionScore("session-old", "technicalDepth", 50),
                buildDimensionScore("session-new", "communication", 82),
                buildDimensionScore("session-old", "communication", 88)
        ));

        InterviewRadarResponse response = growthService.getInterviewRadar(userId);

        assertEquals(2, response.getSessionCount());
        assertNotNull(response.getLatestRadar());
        assertEquals("session-new", response.getLatestRadar().getSessionId());
        assertEquals(55, response.getLatestRadar().getDimensions().get("technicalDepth").getScore());
        assertEquals(2, response.getDimensionTrends().stream()
                .filter(t -> "technicalDepth".equals(t.getDimensionKey()))
                .findFirst()
                .orElseThrow()
                .getPoints()
                .size());
        assertFalse(response.getBlindSpotTips().isEmpty());
        assertEquals("technicalDepth", response.getBlindSpotTips().get(0).getDimensionKey());
        verify(dimensionScoreMapper, never()).insert(any(InterviewDimensionScore.class));
    }

    @Test
    void shouldBuildGrowthOverviewSummaryAndTrends() {
        Long userId = 123L;
        ResumeDiagnosisTask resumeTask = new ResumeDiagnosisTask();
        resumeTask.setUserId(userId);
        resumeTask.setStatus(2);
        resumeTask.setDiagnosisResult("""
                {"overallEvaluation":{"totalScore":76}}
                """);
        resumeTask.setCreateTime(LocalDateTime.of(2026, 5, 20, 9, 0));
        InterviewSession interviewSession = buildEndedSessionWithReport("session-score", userId);
        interviewSession.setComprehensiveScore(81);
        interviewSession.setJobRole("Java工程师");
        interviewSession.setCreateTime(LocalDateTime.of(2026, 5, 21, 9, 0));
        when(resumeDiagnosisTaskMapper.selectList(any())).thenReturn(List.of(resumeTask));
        when(interviewSessionMapper.selectList(any())).thenReturn(List.of(interviewSession));
        when(resumeDiagnosisTaskMapper.selectCount(any())).thenReturn(1L);
        when(interviewSessionMapper.selectCount(any())).thenReturn(1L);
        when(resumeJobMatchRecordMapper.selectCount(any())).thenReturn(0L);
        when(resumePolishRecordMapper.selectCount(any())).thenReturn(0L);
        when(sysGrowthConfigService.getByGroup("encouragement")).thenReturn(List.of(
                buildGrowthConfig("encourage_resume_80", "你的简历质量已经超过 80 分，适合继续打磨岗位匹配。", "encouragement", "简历高分鼓励", 1)
        ));
        when(sysGrowthConfigService.getByGroup("milestone")).thenReturn(List.of(
                buildGrowthConfig("milestone_first_interview", "完成第一次模拟面试", "milestone", "开始沉淀面试反馈", 1)
        ));

        GrowthOverviewResponse response = growthService.getGrowthOverview(userId);

        assertEquals(76, response.getSummary().getLatestResumeScore());
        assertEquals(81, response.getSummary().getLatestInterviewScore());
        assertEquals(1, response.getSummary().getResumeDiagnosisCount());
        assertEquals(1, response.getSummary().getMockInterviewCount());
        assertEquals(1, response.getResumeScoreTrend().size());
        assertEquals(1, response.getInterviewScoreTrend().size());
        assertNotNull(response.getLatestInterviewFeedback());
        assertEquals(List.of("你的简历质量已经超过 80 分，适合继续打磨岗位匹配。"),
                response.getGrowthConfig().getEncouragementMessages());
        assertEquals("milestone_first_interview",
                response.getGrowthConfig().getMilestones().get(0).getConfigKey());
        assertEquals("完成第一次模拟面试",
                response.getGrowthConfig().getMilestones().get(0).getTitle());
    }

    @Test
    void shouldLimitGrowthOverviewQueriesToFieldsNeededByResponse() {
        Long userId = 123L;
        ResumeDiagnosisTask resumeTask = new ResumeDiagnosisTask();
        resumeTask.setDiagnosisResult("{\"overallEvaluation\":{\"totalScore\":76}} ");
        resumeTask.setCreateTime(LocalDateTime.of(2026, 5, 20, 9, 0));
        ResumeJobMatchRecord jobMatchRecord = new ResumeJobMatchRecord();
        jobMatchRecord.setMatchScore(82);
        jobMatchRecord.setAnalysisResult("{\"matchedKeywords\":[\"Java\"],\"missingKeywords\":[],\"suggestions\":[]}");
        jobMatchRecord.setCreateTime(LocalDateTime.of(2026, 5, 21, 9, 0));
        ResumePolishRecord polishRecord = new ResumePolishRecord();
        polishRecord.setSourceType("diagnosis");
        polishRecord.setModificationNotes("[\"优化项目描述\"]");
        polishRecord.setCreateTime(LocalDateTime.of(2026, 5, 22, 9, 0));

        final String[] resumeSelect = new String[1];
        final String[] jobMatchSelect = new String[1];
        final String[] polishSelect = new String[1];
        doAnswer(invocation -> {
            resumeSelect[0] = invocation.<Wrapper<ResumeDiagnosisTask>>getArgument(0).getSqlSelect();
            return List.of(resumeTask);
        }).when(resumeDiagnosisTaskMapper).selectList(any());
        doAnswer(invocation -> {
            jobMatchSelect[0] = invocation.<Wrapper<ResumeJobMatchRecord>>getArgument(0).getSqlSelect();
            return jobMatchRecord;
        }).when(resumeJobMatchRecordMapper).selectOne(any());
        doAnswer(invocation -> {
            polishSelect[0] = invocation.<Wrapper<ResumePolishRecord>>getArgument(0).getSqlSelect();
            return polishRecord;
        }).when(resumePolishRecordMapper).selectOne(any());
        when(interviewSessionMapper.selectList(any())).thenReturn(List.of());
        when(resumeDiagnosisTaskMapper.selectCount(any())).thenReturn(1L);
        when(interviewSessionMapper.selectCount(any())).thenReturn(0L);
        when(resumeJobMatchRecordMapper.selectCount(any())).thenReturn(1L);
        when(resumePolishRecordMapper.selectCount(any())).thenReturn(1L);
        when(sysGrowthConfigService.getByGroup("encouragement")).thenReturn(List.of());
        when(sysGrowthConfigService.getByGroup("milestone")).thenReturn(List.of());

        GrowthOverviewResponse response = growthService.getGrowthOverview(userId);

        assertEquals(76, response.getSummary().getLatestResumeScore());
        assertEquals(82, response.getSummary().getLatestJobMatchScore());
        assertNotNull(response.getLatestPolish());
        assertTrue(resumeSelect[0].contains("diagnosis_result"));
        assertTrue(resumeSelect[0].contains("create_time"));
        assertFalse(resumeSelect[0].contains("resume_text"));
        assertTrue(jobMatchSelect[0].contains("match_score"));
        assertTrue(jobMatchSelect[0].contains("analysis_result"));
        assertFalse(jobMatchSelect[0].contains("resume_text"));
        assertFalse(jobMatchSelect[0].contains("jd_text"));
        assertTrue(polishSelect[0].contains("source_type"));
        assertTrue(polishSelect[0].contains("modification_notes"));
        assertFalse(polishSelect[0].contains("polished_resume_text"));
        assertFalse(polishSelect[0].contains("document_json"));
    }

    private InterviewSession buildEndedSessionWithReport(String sessionId, Long userId) {
        InterviewSession session = new InterviewSession();
        session.setId(1L);
        session.setSessionId(sessionId);
        session.setUserId(userId);
        session.setStatus(1);
        session.setEvaluationReport("""
                {"technicalDepth":{"score":80,"comment":"ok"}}
                """);
        session.setCreateTime(LocalDateTime.of(2026, 5, 20, 10, 0));
        session.setIsDeleted(0);
        return session;
    }

    private InterviewDimensionScore buildDimensionScore(String sessionId, String dimensionKey, Integer score) {
        InterviewDimensionScore dimensionScore = new InterviewDimensionScore();
        dimensionScore.setUserId(123L);
        dimensionScore.setSessionId(sessionId);
        dimensionScore.setDimensionKey(dimensionKey);
        dimensionScore.setScore(score);
        dimensionScore.setComment("ok");
        dimensionScore.setStrengths("[\"clear\"]");
        dimensionScore.setWeaknesses("[\"detail\"]");
        dimensionScore.setCreateTime(LocalDateTime.now());
        dimensionScore.setIsDeleted(0);
        return dimensionScore;
    }

    private SysGrowthConfig buildGrowthConfig(String key,
                                              String value,
                                              String groupName,
                                              String description,
                                              Integer sort) {
        SysGrowthConfig config = new SysGrowthConfig();
        config.setConfigKey(key);
        config.setConfigValue(value);
        config.setGroupName(groupName);
        config.setDescription(description);
        config.setSort(sort);
        return config;
    }
}
