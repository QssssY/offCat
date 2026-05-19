package com.airesume.server.service.impl;

import com.airesume.server.dto.interview.InterviewEvaluationReport;
import com.airesume.server.dto.interview.InterviewJobTargetContext;
import com.airesume.server.mock.MockInterviewService;
import com.airesume.server.service.InterviewAiService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.mock;

class MockInterviewAiServiceImplTest {

    @Test
    void shouldGenerateDeepAnalysisFieldsForNormalReport() {
        MockInterviewAiServiceImpl service = new MockInterviewAiServiceImpl(mock(MockInterviewService.class), new ObjectMapper());
        List<InterviewAiService.ChatMessageItem> history = List.of(
                new InterviewAiService.ChatMessageItem("assistant", "请介绍一次你做过的性能优化。"),
                new InterviewAiService.ChatMessageItem("user", "我优化过接口，把慢查询处理了一下。"),
                new InterviewAiService.ChatMessageItem("assistant", "具体怎么定位慢查询？"),
                new InterviewAiService.ChatMessageItem("user", "主要看日志和数据库执行时间。")
        );

        InterviewEvaluationReport report = service.generateEvaluationReport(
                "session-1", history, "Java工程师", "java", 2, "normal", null);

        assertNotNull(report.getProjectExpression());
        assertFalse(report.getRoundReviews().isEmpty());
        assertEquals(3, report.getImmediateActions().size());
        assertFalse(report.getFollowUpLossPoints().isEmpty());
        assertFalse(report.getCommonLossPatterns().isEmpty());
    }

    @Test
    void shouldGenerateTargetedActionWhenMissingKeywordExists() {
        MockInterviewAiServiceImpl service = new MockInterviewAiServiceImpl(mock(MockInterviewService.class), new ObjectMapper());
        InterviewJobTargetContext context = InterviewJobTargetContext.builder()
                .jobTargeted(true)
                .missingKeywords(List.of("高并发"))
                .build();

        InterviewEvaluationReport report = service.generateEvaluationReport(
                "session-2",
                List.of(new InterviewAiService.ChatMessageItem("assistant", "讲讲你的项目。"),
                        new InterviewAiService.ChatMessageItem("user", "我做过订单系统。")),
                "后端工程师",
                "backend",
                2,
                "normal",
                context);

        assertTrue(report.getImmediateActions().stream().anyMatch(item -> item.contains("高并发")));
        assertTrue(report.getProjectExpression().getWeaknesses().stream().anyMatch(item -> item.contains("目标岗位")));
    }

    @Test
    void shouldKeepDeepAnalysisFieldsStableWhenHistoryIsEmpty() {
        MockInterviewAiServiceImpl service = new MockInterviewAiServiceImpl(mock(MockInterviewService.class), new ObjectMapper());

        InterviewEvaluationReport report = service.generateEvaluationReport(
                "session-3", List.of(), "产品经理", "pm", 1, "stress", null);

        assertTrue(report.getRoundReviews().isEmpty());
        assertEquals(3, report.getImmediateActions().size());
        assertNotNull(report.getProjectExpression().getScore());
    }

    @Test
    void shouldAppendStructuredImmediateFeedbackForMockReply() {
        MockInterviewService mockInterviewService = mock(MockInterviewService.class);
        when(mockInterviewService.generateMockReply("session-4", "我的回答", 0, null))
                .thenReturn("请继续讲一个具体项目。");
        MockInterviewAiServiceImpl service = new MockInterviewAiServiceImpl(mockInterviewService, new ObjectMapper());

        String reply = service.generateReply("session-4", List.of(), "我的回答", "java", 2, null, "immediate", "normal", 0);

        assertTrue(reply.startsWith("请继续讲一个具体项目。"));
        assertTrue(reply.contains("<FEEDBACK>"));
        assertTrue(reply.contains("</FEEDBACK>"));
        assertTrue(reply.indexOf("请继续讲一个具体项目。") < reply.indexOf("<FEEDBACK>"));
        assertTrue(reply.contains("请继续讲一个具体项目。"));
    }

    @Test
    void shouldAppendTechLeaderPersonaForMockReply() {
        MockInterviewService mockInterviewService = mock(MockInterviewService.class);
        when(mockInterviewService.generateMockReply("session-5", "我的回答", 0, null))
                .thenReturn("请继续讲一个具体项目。");
        MockInterviewAiServiceImpl service = new MockInterviewAiServiceImpl(mockInterviewService, new ObjectMapper());

        String reply = service.generateReply("session-5", List.of(), "我的回答", "java", 2, null, "after_interview", "tech_leader", 0);

        assertTrue(reply.startsWith("从技术 Leader 角度"));
        assertTrue(reply.contains("请继续讲一个具体项目。"));
    }
}
