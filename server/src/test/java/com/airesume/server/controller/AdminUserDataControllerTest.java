package com.airesume.server.controller;

import com.airesume.server.common.result.Result;
import com.airesume.server.entity.InterviewSession;
import com.airesume.server.entity.ResumeDiagnosisTask;
import com.airesume.server.mapper.InterviewSessionMapper;
import com.airesume.server.mapper.ResumeDiagnosisTaskMapper;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.Authentication;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AdminUserDataControllerTest {

    private static final int CODE_SUCCESS = 200;

    @Mock private InterviewSessionMapper interviewSessionMapper;
    @Mock private ResumeDiagnosisTaskMapper resumeDiagnosisTaskMapper;
    @Mock private Authentication authentication;

    private AdminUserDataController controller;

    @BeforeEach
    void setUp() {
        controller = new AdminUserDataController(interviewSessionMapper, resumeDiagnosisTaskMapper);
        lenient().when(authentication.getPrincipal()).thenReturn(1L);
    }

    @Test
    void getUserInterviewsShouldReturnPagedResults() {
        InterviewSession session = new InterviewSession();
        session.setSessionId("session-1");
        session.setJobRole("Java工程师");
        session.setDifficulty(2);
        session.setStatus(1);
        session.setInterviewMode("tech_leader");
        session.setComprehensiveScore(85);
        session.setCreateTime(LocalDateTime.now());

        Page<InterviewSession> pageResult = new Page<>(1, 20, 1);
        pageResult.setRecords(List.of(session));

        when(interviewSessionMapper.selectPage(any(Page.class), any(LambdaQueryWrapper.class))).thenReturn(pageResult);

        Result<Map<String, Object>> result = controller.getUserInterviews(10L, 1, 20, authentication);
        assertEquals(CODE_SUCCESS, result.getCode());
        Map<String, Object> data = result.getData();
        List<Map<String, Object>> records = (List<Map<String, Object>>) data.get("records");
        assertEquals(1, records.size());
        assertEquals(1, data.get("total"));
    }

    @Test
    void getUserResumeTasksShouldReturnPagedResults() {
        ResumeDiagnosisTask task = new ResumeDiagnosisTask();
        task.setId(100L);
        task.setStatus(2);
        task.setErrorMsg(null);
        task.setCreateTime(LocalDateTime.now());

        Page<ResumeDiagnosisTask> pageResult = new Page<>(1, 20, 1);
        pageResult.setRecords(List.of(task));

        when(resumeDiagnosisTaskMapper.selectPage(any(Page.class), any(LambdaQueryWrapper.class))).thenReturn(pageResult);

        Result<Map<String, Object>> result = controller.getUserResumeTasks(10L, 1, 20, authentication);
        assertEquals(CODE_SUCCESS, result.getCode());
        Map<String, Object> data = result.getData();
        List<Map<String, Object>> records = (List<Map<String, Object>>) data.get("records");
        assertEquals(1, records.size());
    }

    @Test
    void getUserInterviewsShouldReturnEmptyForNoData() {
        Page<InterviewSession> pageResult = new Page<>(1, 20, 0);
        pageResult.setRecords(List.of());

        when(interviewSessionMapper.selectPage(any(Page.class), any(LambdaQueryWrapper.class))).thenReturn(pageResult);

        Result<Map<String, Object>> result = controller.getUserInterviews(999L, 1, 20, authentication);
        assertEquals(CODE_SUCCESS, result.getCode());
        assertEquals(0, ((List<?>) result.getData().get("records")).size());
    }

    @Test
    void getUserResumeTasksShouldReturnEmptyForNoData() {
        Page<ResumeDiagnosisTask> pageResult = new Page<>(1, 20, 0);
        pageResult.setRecords(List.of());

        when(resumeDiagnosisTaskMapper.selectPage(any(Page.class), any(LambdaQueryWrapper.class))).thenReturn(pageResult);

        Result<Map<String, Object>> result = controller.getUserResumeTasks(999L, 1, 20, authentication);
        assertEquals(CODE_SUCCESS, result.getCode());
        assertEquals(0, ((List<?>) result.getData().get("records")).size());
    }
}
