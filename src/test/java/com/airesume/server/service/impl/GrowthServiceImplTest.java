package com.airesume.server.service.impl;

import com.airesume.server.dto.growth.GrowthOverviewResponse;
import com.airesume.server.dto.growth.InterviewRadarResponse;
import com.airesume.server.entity.InterviewDimensionScore;
import com.airesume.server.entity.InterviewSession;
import com.airesume.server.entity.ResumeDiagnosisTask;
import com.airesume.server.mapper.InterviewDimensionScoreMapper;
import com.airesume.server.mapper.MockInterviewJobTargetRecordMapper;
import com.airesume.server.mapper.ResumeDiagnosisTaskMapper;
import com.airesume.server.mapper.ResumeJobMatchRecordMapper;
import com.airesume.server.mapper.ResumePolishRecordMapper;
import com.airesume.server.repository.InterviewSessionRepository;
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
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class GrowthServiceImplTest {

    @Mock private ResumeDiagnosisTaskMapper resumeDiagnosisTaskMapper;
    @Mock private ResumeJobMatchRecordMapper resumeJobMatchRecordMapper;
    @Mock private ResumePolishRecordMapper resumePolishRecordMapper;
    @Mock private InterviewSessionRepository interviewSessionRepository;
    @Mock private MockInterviewJobTargetRecordMapper mockInterviewJobTargetRecordMapper;
    @Mock private InterviewDimensionScoreMapper dimensionScoreMapper;

    private GrowthServiceImpl growthService;

    @BeforeEach
    void setUp() {
        growthService = new GrowthServiceImpl(
                resumeDiagnosisTaskMapper,
                resumeJobMatchRecordMapper,
                resumePolishRecordMapper,
                interviewSessionRepository,
                mockInterviewJobTargetRecordMapper,
                dimensionScoreMapper,
                new ObjectMapper());
    }

    @Test
    void shouldUseEvaluationReportSessionsForInterviewRadarAndKeepCandidateCountWhenNoScores() {
        Long userId = 123L;
        InterviewSession session = buildEndedSessionWithReport("session-a", userId);
        when(interviewSessionRepository.findRecentEndedSessionsWithEvaluationReport(eq(userId), eq(1), eq(0), any()))
                .thenReturn(List.of(session));
        when(dimensionScoreMapper.selectList(any())).thenReturn(List.of());

        InterviewRadarResponse response = growthService.getInterviewRadar(userId);

        assertNull(response.getLatestRadar());
        assertEquals(1, response.getSessionCount());
        assertEquals(List.of(), response.getDimensionTrends());
        assertEquals(List.of(), response.getBlindSpotTips());
        verify(interviewSessionRepository).findRecentEndedSessionsWithEvaluationReport(eq(userId), eq(1), eq(0), any());
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
        when(interviewSessionRepository.findRecentEndedSessionsWithEvaluationReport(eq(userId), eq(1), eq(0), any()))
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
        when(interviewSessionRepository.findTop10ByUserIdAndStatusAndComprehensiveScoreIsNotNullAndIsDeletedOrderByCreateTimeDesc(
                userId, 1, 0)).thenReturn(List.of(interviewSession));
        when(resumeDiagnosisTaskMapper.selectCount(any())).thenReturn(1L);
        when(interviewSessionRepository.countByUserIdAndStatus(userId, 1)).thenReturn(1L);
        when(resumeJobMatchRecordMapper.selectCount(any())).thenReturn(0L);
        when(resumePolishRecordMapper.selectCount(any())).thenReturn(0L);

        GrowthOverviewResponse response = growthService.getGrowthOverview(userId);

        assertEquals(76, response.getSummary().getLatestResumeScore());
        assertEquals(81, response.getSummary().getLatestInterviewScore());
        assertEquals(1, response.getSummary().getResumeDiagnosisCount());
        assertEquals(1, response.getSummary().getMockInterviewCount());
        assertEquals(1, response.getResumeScoreTrend().size());
        assertEquals(1, response.getInterviewScoreTrend().size());
        assertNotNull(response.getLatestInterviewFeedback());
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
}
