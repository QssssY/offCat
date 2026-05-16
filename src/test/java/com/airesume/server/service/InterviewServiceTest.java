package com.airesume.server.service;

import com.airesume.server.entity.InterviewChatLog;
import com.airesume.server.entity.InterviewSession;
import com.airesume.server.mock.MockInterviewService;
import com.airesume.server.repository.InterviewMessageRepository;
import com.airesume.server.repository.InterviewSessionRepository;
import com.airesume.server.dto.interview.InterviewEvaluationReport;
import com.airesume.server.dto.interview.InterviewHistoryResponse;
import com.airesume.server.dto.interview.InterviewSessionResponse;
import com.airesume.server.dto.interview.SendMessageRequest;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.stubbing.Answer;
import org.reactivestreams.Publisher;
import org.reactivestreams.Subscription;
import org.springframework.data.domain.PageImpl;
import org.springframework.transaction.support.TransactionCallbackWithoutResult;
import org.springframework.transaction.support.TransactionTemplate;
import org.springframework.web.servlet.mvc.method.annotation.ResponseBodyEmitter;

import java.lang.reflect.Method;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.Executor;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.isA;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class InterviewServiceTest {

    @Mock private InterviewSessionRepository interviewSessionRepository;
    @Mock private InterviewMessageService interviewMessageService;
    @Mock private MockInterviewService mockInterviewService;
    @Mock private InterviewMessageRepository interviewMessageRepository;
    @Mock private InterviewAiService interviewAiService;
    @Mock private SysJobRoleService sysJobRoleService;
    @Mock private TransactionTemplate transactionTemplate;
    @Mock private UserQuotaService userQuotaService;
    @Mock private MockInterviewJobTargetService mockInterviewJobTargetService;
    @Mock private NotificationService notificationService;
    @Mock private Executor aiAsyncExecutor;
    @Mock private Subscription subscription;

    private final ObjectMapper objectMapper = new ObjectMapper();
    private InterviewService interviewService;

    @Captor
    private ArgumentCaptor<InterviewChatLog> chatLogCaptor;
    @Captor
    private ArgumentCaptor<Runnable> timeoutCaptor;

    @BeforeEach
    void setUp() {
        interviewService = new InterviewService(
                interviewSessionRepository, interviewMessageService, mockInterviewService,
                interviewMessageRepository, interviewAiService, objectMapper, sysJobRoleService,
                transactionTemplate, userQuotaService, mockInterviewJobTargetService,
                notificationService, aiAsyncExecutor);
    }

    @Test
    void subscribeAndWriteStreamShouldPersistBeforeDone() throws Exception {
        String sessionId = "test-session-id";
        ResponseBodyEmitter emitter = mock(ResponseBodyEmitter.class);
        StringBuilder fullReply = new StringBuilder();

        Publisher<String> publisher = subscriber -> {
            subscriber.onSubscribe(subscription);
            subscriber.onNext("Hello, ");
            subscriber.onNext("this is AI response");
            subscriber.onComplete();
        };

        doAnswer((Answer<Void>) invocation -> {
            Object arg = invocation.getArgument(0);
            if (arg instanceof TransactionCallbackWithoutResult callback) {
                callback.doInTransaction(null);
            } else if (arg instanceof java.util.function.Consumer) {
                ((java.util.function.Consumer<Object>) arg).accept(null);
            }
            return null;
        }).when(transactionTemplate).executeWithoutResult(Mockito.any());

        interviewService.subscribeAndWriteStream(sessionId, emitter, publisher, fullReply);

        verify(interviewMessageRepository).save(chatLogCaptor.capture());
        InterviewChatLog savedMessage = chatLogCaptor.getValue();
        assertEquals(sessionId, savedMessage.getSessionId());
        assertEquals("assistant", savedMessage.getMessageRole());
        assertEquals("Hello, this is AI response", savedMessage.getContent());
        assertEquals("Hello, this is AI response", savedMessage.getContent());

        verify(emitter, atLeastOnce()).send(anyString());
        verify(emitter).complete();
    }

    @Test
    void subscribeAndWriteStreamShouldSendErrorOnPersistFailure() throws Exception {
        String sessionId = "test-session-id";
        ResponseBodyEmitter emitter = mock(ResponseBodyEmitter.class);
        StringBuilder fullReply = new StringBuilder("AI response content");

        Publisher<String> publisher = subscriber -> {
            subscriber.onSubscribe(subscription);
            subscriber.onNext("AI ");
            subscriber.onNext("response content");
            subscriber.onComplete();
        };

        doThrow(new RuntimeException("DB connection lost"))
                .when(transactionTemplate).executeWithoutResult(Mockito.any());

        interviewService.subscribeAndWriteStream(sessionId, emitter, publisher, fullReply);

        verify(emitter, atLeastOnce()).send(anyString());
        verify(emitter).completeWithError(any(RuntimeException.class));
    }

    @Test
    void subscribeAndWriteStreamShouldHandleEmptyReply() throws Exception {
        String sessionId = "test-session-id";
        ResponseBodyEmitter emitter = mock(ResponseBodyEmitter.class);
        StringBuilder fullReply = new StringBuilder();

        Publisher<String> publisher = subscriber -> {
            subscriber.onSubscribe(subscription);
            subscriber.onComplete();
        };

        interviewService.subscribeAndWriteStream(sessionId, emitter, publisher, fullReply);

        verify(interviewMessageRepository, never()).save(any());
        verify(emitter, atLeastOnce()).send(anyString());
        verify(emitter).complete();
    }

    @Test
    void subscribeAndWriteStreamShouldCancelSubscriptionOnTimeout() throws Exception {
        String sessionId = "test-session-id";
        ResponseBodyEmitter emitter = mock(ResponseBodyEmitter.class);
        StringBuilder fullReply = new StringBuilder();

        Publisher<String> publisher = subscriber -> subscriber.onSubscribe(subscription);

        interviewService.subscribeAndWriteStream(sessionId, emitter, publisher, fullReply);

        verify(emitter).onTimeout(timeoutCaptor.capture());
        timeoutCaptor.getValue().run();

        verify(subscription).cancel();
        verify(emitter).complete();
        verify(interviewMessageRepository, never()).save(any());
    }

    @Test
    void generateAndPersistEvaluationReportShouldSkipWhenReportAlreadyWritten() throws Exception {
        String sessionId = "session-1";
        InterviewSession session = new InterviewSession();
        session.setSessionId(sessionId);
        session.setUserId(123L);
        session.setStatus(1);
        session.setJobRole("Java工程师");
        session.setDifficulty(2);
        session.setInterviewMode("normal");
        session.setCreateTime(LocalDateTime.now());
        session.setUpdateTime(LocalDateTime.now());

        InterviewEvaluationReport report = InterviewEvaluationReport.builder()
                .overallScore(88)
                .summary("summary")
                .build();

        when(interviewSessionRepository.findBySessionId(sessionId)).thenReturn(Optional.of(session));
        when(interviewMessageService.getMessageList(sessionId)).thenReturn(List.of());
        when(interviewAiService.generateEvaluationReport(eq(sessionId), anyList(), anyString(), any(), any(), anyString(), any()))
                .thenReturn(report);
        doAnswer((Answer<Void>) invocation -> {
            Object arg = invocation.getArgument(0);
            if (arg instanceof TransactionCallbackWithoutResult callback) {
                callback.doInTransaction(null);
            } else if (arg instanceof java.util.function.Consumer) {
                ((java.util.function.Consumer<Object>) arg).accept(null);
            }
            return null;
        }).when(transactionTemplate).executeWithoutResult(Mockito.any());
        Method method = InterviewService.class.getDeclaredMethod("generateAndPersistEvaluationReport", String.class);
        method.setAccessible(true);
        method.invoke(interviewService, sessionId);

        verify(notificationService, never()).createNotification(anyLong(), anyString(), anyString(), anyString(), anyString(), anyString());
    }

    @Test
    void buildFallbackEvaluationReportShouldIncludeDeepAnalysisFields() throws Exception {
        String sessionId = "session-fallback";
        InterviewSession session = new InterviewSession();
        session.setSessionId(sessionId);
        session.setUserId(123L);
        session.setStatus(1);
        session.setJobRole("Java工程师");
        session.setDifficulty(2);
        session.setInterviewMode("normal");
        session.setCreateTime(LocalDateTime.now());
        session.setUpdateTime(LocalDateTime.now());

        when(mockInterviewService.generateMockScore(sessionId)).thenReturn(66);

        Method method = InterviewService.class.getDeclaredMethod(
                "buildFallbackEvaluationReport", InterviewSession.class, com.airesume.server.dto.interview.InterviewJobTargetContext.class);
        method.setAccessible(true);
        InterviewEvaluationReport report = (InterviewEvaluationReport) method.invoke(interviewService, session, null);

        assertEquals(3, report.getImmediateActions().size());
        assertFalse(report.getFollowUpLossPoints().isEmpty());
        assertFalse(report.getCommonLossPatterns().isEmpty());
        assertNotNull(report.getProjectExpression());
        assertNotNull(report.getTechnicalDepth());
    }

    @Test
    void shouldBuildReplayRoundsFromChatLogs() {
        String sessionId = "session-replay";
        Long userId = 123L;
        InterviewSession session = buildInProgressSession(sessionId, userId);
        session.setStatus(1);

        List<InterviewChatLog> chatLogs = List.of(
                buildChatLog(1L, sessionId, "assistant", "请先介绍一下自己", 0),
                buildChatLog(2L, sessionId, "user", "我做过订单系统", 1),
                buildChatLog(3L, sessionId, "assistant", "回答有方向。订单系统里你负责哪块？", 2),
                buildChatLog(4L, sessionId, "user", "我负责支付模块", 3),
                buildChatLog(5L, sessionId, "assistant", "支付模块可以继续补充稳定性设计。", 4)
        );

        when(interviewSessionRepository.findBySessionIdAndUserId(sessionId, userId)).thenReturn(Optional.of(session));
        when(interviewMessageService.getMessageList(sessionId)).thenReturn(chatLogs);

        InterviewSessionResponse response = interviewService.getSessionDetail(userId, sessionId);

        assertEquals(2, response.getReplayRounds().size());
        assertEquals("请先介绍一下自己", response.getReplayRounds().get(0).getQuestionContent());
        assertEquals("我做过订单系统", response.getReplayRounds().get(0).getAnswerContent());
        assertEquals("回答有方向。订单系统里你负责哪块？", response.getReplayRounds().get(0).getFeedbackContent());
        assertEquals("回答有方向。订单系统里你负责哪块？", response.getReplayRounds().get(1).getQuestionContent());
        assertEquals("支付模块可以继续补充稳定性设计。", response.getReplayRounds().get(1).getFeedbackContent());
    }

    @Test
    void shouldPassImmediateFeedbackModeWhenSendingMessage() {
        String sessionId = "session-feedback";
        Long userId = 123L;
        InterviewSession session = buildInProgressSession(sessionId, userId);
        session.setFeedbackMode("immediate");
        SendMessageRequest request = new SendMessageRequest();
        request.setContent("我负责支付模块的幂等和回调处理");

        when(interviewSessionRepository.findBySessionIdAndUserId(sessionId, userId)).thenReturn(Optional.of(session));
        when(interviewMessageService.getMessageList(sessionId)).thenReturn(List.of(
                buildChatLog(1L, sessionId, "assistant", "你负责过什么项目？", 0)
        ));
        when(interviewAiService.generateReply(
                eq(sessionId),
                anyList(),
                eq(request.getContent()),
                eq(session.getJobRoleCode()),
                eq(session.getDifficulty()),
                any(),
                eq("immediate"),
                eq("normal")
        )).thenReturn("请继续说明幂等方案。\n\n<FEEDBACK>\n本题反馈：回答方向清晰，但还需要补充具体处理细节。\n</FEEDBACK>");

        interviewService.sendMessage(userId, sessionId, request);

        verify(interviewAiService).generateReply(
                eq(sessionId),
                anyList(),
                eq(request.getContent()),
                eq(session.getJobRoleCode()),
                eq(session.getDifficulty()),
                any(),
                eq("immediate"),
                eq("normal")
        );
        verify(interviewMessageService).saveMessage(session, "user", request.getContent());
        verify(interviewMessageService).saveMessage(session, "assistant", "请继续说明幂等方案。\n\n<FEEDBACK>\n本题反馈：回答方向清晰，但还需要补充具体处理细节。\n</FEEDBACK>");
    }

    @Test
    void shouldReturnNormalizedFeedbackModeInSessionDetail() {
        String sessionId = "session-feedback-detail";
        Long userId = 123L;
        InterviewSession session = buildInProgressSession(sessionId, userId);
        session.setFeedbackMode("immediate");

        when(interviewSessionRepository.findBySessionIdAndUserId(sessionId, userId)).thenReturn(Optional.of(session));
        when(interviewMessageService.getMessageList(sessionId)).thenReturn(List.of());

        InterviewSessionResponse response = interviewService.getSessionDetail(userId, sessionId);

        assertEquals("immediate", response.getFeedbackMode());
    }

    @Test
    void shouldReturnFeedbackModeInHistory() {
        String sessionId = "session-feedback-history";
        Long userId = 123L;
        InterviewSession session = buildInProgressSession(sessionId, userId);
        session.setFeedbackMode("immediate");

        when(interviewSessionRepository.findByUserId(eq(userId), any())).thenReturn(new PageImpl<>(List.of(session)));
        when(interviewMessageService.getMessageCountMap(any())).thenReturn(java.util.Map.of(sessionId, 2));
        when(mockInterviewJobTargetService.getSessionContextSummaryMap(eq(userId), any())).thenReturn(java.util.Map.of());

        List<InterviewHistoryResponse> history = interviewService.getHistory(userId, 1, 5).getList();

        assertEquals(1, history.size());
        assertEquals("immediate", history.get(0).getFeedbackMode());
    }

    @Test
    void shouldKeepPersonaModeWhenJobTargetedContextExists() {
        String sessionId = "session-persona";
        Long userId = 123L;
        InterviewSession session = buildInProgressSession(sessionId, userId);
        session.setInterviewMode("tech_leader");
        com.airesume.server.dto.interview.InterviewJobTargetContext context =
                com.airesume.server.dto.interview.InterviewJobTargetContext.builder()
                        .jobTargeted(true)
                        .build();

        when(interviewSessionRepository.findBySessionIdAndUserId(sessionId, userId)).thenReturn(Optional.of(session));
        when(interviewMessageService.getMessageList(sessionId)).thenReturn(List.of());
        when(mockInterviewJobTargetService.getSessionContext(userId, sessionId)).thenReturn(context);

        InterviewSessionResponse response = interviewService.getSessionDetail(userId, sessionId);

        assertEquals("tech_leader", response.getInterviewMode());
        assertEquals("技术 Leader 面", response.getInterviewModeDesc());
        assertTrue(response.getJobTargeted());
    }

    @Test
    void shouldResolveHrPersonaInSessionDetail() {
        String sessionId = "session-hr";
        Long userId = 123L;
        InterviewSession session = buildInProgressSession(sessionId, userId);
        session.setInterviewMode("big_company_hr");

        when(interviewSessionRepository.findBySessionIdAndUserId(sessionId, userId)).thenReturn(Optional.of(session));
        when(interviewMessageService.getMessageList(sessionId)).thenReturn(List.of());

        InterviewSessionResponse response = interviewService.getSessionDetail(userId, sessionId);

        assertEquals("big_company_hr", response.getInterviewMode());
        assertEquals("大厂 HR 面", response.getInterviewModeDesc());
    }

    @Test
    void shouldResolveForeignPersonaInSessionDetail() {
        String sessionId = "session-foreign";
        Long userId = 123L;
        InterviewSession session = buildInProgressSession(sessionId, userId);
        session.setInterviewMode("foreign_interviewer");

        when(interviewSessionRepository.findBySessionIdAndUserId(sessionId, userId)).thenReturn(Optional.of(session));
        when(interviewMessageService.getMessageList(sessionId)).thenReturn(List.of());

        InterviewSessionResponse response = interviewService.getSessionDetail(userId, sessionId);

        assertEquals("foreign_interviewer", response.getInterviewMode());
        assertEquals("外企面试官", response.getInterviewModeDesc());
    }

    @Test
    void shouldDefaultToNormalPersonaWhenModeIsNull() {
        String sessionId = "session-null-mode";
        Long userId = 123L;
        InterviewSession session = buildInProgressSession(sessionId, userId);
        session.setInterviewMode(null);

        when(interviewSessionRepository.findBySessionIdAndUserId(sessionId, userId)).thenReturn(Optional.of(session));
        when(interviewMessageService.getMessageList(sessionId)).thenReturn(List.of());

        InterviewSessionResponse response = interviewService.getSessionDetail(userId, sessionId);

        assertEquals("normal", response.getInterviewMode());
        assertEquals("普通面试", response.getInterviewModeDesc());
    }

    private InterviewSession buildInProgressSession(String sessionId, Long userId) {
        InterviewSession session = new InterviewSession();
        session.setId(1L);
        session.setSessionId(sessionId);
        session.setUserId(userId);
        session.setStatus(0);
        session.setJobRole("Java工程师");
        session.setJobRoleCode("java_backend");
        session.setDifficulty(2);
        session.setInterviewMode("normal");
        session.setOpeningGenerated(1);
        session.setCreateTime(LocalDateTime.now());
        session.setUpdateTime(LocalDateTime.now());
        return session;
    }

    private InterviewChatLog buildChatLog(Long id, String sessionId, String role, String content, int secondOffset) {
        InterviewChatLog log = new InterviewChatLog();
        log.setId(id);
        log.setSessionId(sessionId);
        log.setMessageRole(role);
        log.setContent(content);
        log.setCreateTime(LocalDateTime.of(2026, 5, 16, 10, 0).plusSeconds(secondOffset));
        log.setUpdateTime(log.getCreateTime());
        log.setIsDeleted(0);
        return log;
    }
}
