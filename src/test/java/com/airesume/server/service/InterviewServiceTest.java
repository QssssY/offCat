package com.airesume.server.service;

import com.airesume.server.entity.InterviewChatLog;
import com.airesume.server.entity.InterviewSession;
import com.airesume.server.common.constants.UserAiConstants;
import com.airesume.server.common.exception.BusinessException;
import com.airesume.server.common.result.ResultCode;
import com.airesume.server.dto.ai.ResolvedAiConfig;
import com.airesume.server.mock.MockInterviewService;
import com.airesume.server.dto.interview.CreateSessionRequest;
import com.airesume.server.dto.interview.InterviewEvaluationReport;
import com.airesume.server.dto.interview.InterviewHistoryResponse;
import com.airesume.server.dto.interview.InterviewJobTargetContext;
import com.airesume.server.dto.interview.InterviewSessionResponse;
import com.airesume.server.dto.interview.SendMessageRequest;
import com.airesume.server.dto.interview.SendMessageResponse;
import com.airesume.server.mapper.InterviewChatLogMapper;
import com.airesume.server.mapper.InterviewDimensionScoreMapper;
import com.airesume.server.mapper.InterviewSessionMapper;
import com.airesume.server.mapper.CommunityPostMapper;
import com.baomidou.mybatisplus.core.conditions.Wrapper;
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
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.transaction.support.TransactionCallbackWithoutResult;
import org.springframework.transaction.support.TransactionTemplate;
import org.springframework.web.servlet.mvc.method.annotation.ResponseBodyEmitter;

import java.lang.reflect.Method;
import java.time.LocalDateTime;
import java.util.List;
import java.util.concurrent.Executor;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.isA;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class InterviewServiceTest {

    @Mock private InterviewSessionMapper interviewSessionMapper;
    @Mock private InterviewMessageService interviewMessageService;
    @Mock private MockInterviewService mockInterviewService;
    @Mock private InterviewChatLogMapper interviewChatLogMapper;
    @Mock private InterviewAiService interviewAiService;
    @Mock private SysJobRoleService sysJobRoleService;
    @Mock private TransactionTemplate transactionTemplate;
    @Mock private UserQuotaService userQuotaService;
    @Mock private MockInterviewJobTargetService mockInterviewJobTargetService;
    @Mock private NotificationService notificationService;
    @Mock private InterviewDimensionScoreMapper dimensionScoreMapper;
    @Mock private CommunityPostMapper communityPostMapper;
    @Mock private InterviewDimensionScoreService dimensionScoreService;
    @Mock private Executor aiAsyncExecutor;
    @Mock private CacheManager cacheManager;
    @Mock private Cache interviewRadarCache;
    @Mock private Cache growthOverviewCache;
    @Mock private Subscription subscription;
    @Mock private UserAiConfigResolver userAiConfigResolver;
    @Mock private UserAiUsageLimitService userAiUsageLimitService;

    private final ObjectMapper objectMapper = new ObjectMapper();
    private InterviewService interviewService;

    @Captor
    private ArgumentCaptor<InterviewChatLog> chatLogCaptor;
    @Captor
    private ArgumentCaptor<Runnable> timeoutCaptor;

    @BeforeEach
    void setUp() {
        interviewService = new InterviewService(
                interviewSessionMapper, interviewMessageService, mockInterviewService,
                interviewChatLogMapper, interviewAiService, objectMapper, sysJobRoleService,
                transactionTemplate, userQuotaService, mockInterviewJobTargetService,
                notificationService, dimensionScoreMapper, communityPostMapper, dimensionScoreService, aiAsyncExecutor,
                userAiConfigResolver, userAiUsageLimitService);
        ReflectionTestUtils.setField(interviewService, "cacheManager", cacheManager);
        lenient().when(cacheManager.getCache("user:interviewRadar")).thenReturn(interviewRadarCache);
        lenient().when(cacheManager.getCache("user:growthOverview")).thenReturn(growthOverviewCache);
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

        interviewService.subscribeAndWriteStream(
                sessionId, emitter, publisher, fullReply,
                new AtomicBoolean(false), new AtomicBoolean(false), new AtomicReference<>());

        verify(interviewChatLogMapper).insert(chatLogCaptor.capture());
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

        interviewService.subscribeAndWriteStream(
                sessionId, emitter, publisher, fullReply,
                new AtomicBoolean(false), new AtomicBoolean(false), new AtomicReference<>());

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

        interviewService.subscribeAndWriteStream(
                sessionId, emitter, publisher, fullReply,
                new AtomicBoolean(false), new AtomicBoolean(false), new AtomicReference<>());

        verify(interviewChatLogMapper, never()).insert(any(InterviewChatLog.class));
        verify(emitter, atLeastOnce()).send(anyString());
        verify(emitter).complete();
    }

    @Test
    void subscribeAndWriteStreamShouldRollbackAndExposeCustomAiErrorCode() throws Exception {
        String sessionId = "test-session-id";
        ResponseBodyEmitter emitter = mock(ResponseBodyEmitter.class);
        StringBuilder fullReply = new StringBuilder();
        Runnable upstreamErrorCallback = mock(Runnable.class);

        Publisher<String> publisher = subscriber -> {
            subscriber.onSubscribe(subscription);
            subscriber.onError(new BusinessException(ResultCode.CUSTOM_AI_CALL_FAILED, "自定义AI调用失败"));
        };

        interviewService.subscribeAndWriteStream(
                sessionId, emitter, publisher, fullReply,
                new AtomicBoolean(false), new AtomicBoolean(false), new AtomicReference<>(), upstreamErrorCallback);

        ArgumentCaptor<String> eventCaptor = ArgumentCaptor.forClass(String.class);
        verify(upstreamErrorCallback).run();
        verify(emitter).send(eventCaptor.capture());
        assertTrue(eventCaptor.getValue().contains("\"code\":4090"));
        assertTrue(eventCaptor.getValue().contains("自定义AI调用失败"));
        verify(emitter).completeWithError(any(BusinessException.class));
    }

    @Test
    void subscribeAndWriteStreamShouldCancelSubscriptionOnTimeout() throws Exception {
        String sessionId = "test-session-id";
        ResponseBodyEmitter emitter = mock(ResponseBodyEmitter.class);
        StringBuilder fullReply = new StringBuilder();
        AtomicBoolean streamClosed = new AtomicBoolean(false);
        AtomicBoolean done = new AtomicBoolean(false);
        AtomicReference<Subscription> subscriptionRef = new AtomicReference<>();

        Publisher<String> publisher = subscriber -> subscriber.onSubscribe(subscription);

        // 控制层职责：先 attach 生命周期回调（独占注册 onTimeout/onCompletion/onError），
        // 服务层 subscribeAndWriteStream 在数据流上消费同一份 streamClosed。
        interviewService.attachStreamLifecycleCallbacks(sessionId, emitter, streamClosed, done, subscriptionRef);
        interviewService.subscribeAndWriteStream(sessionId, emitter, publisher, fullReply, streamClosed, done, subscriptionRef);

        verify(emitter).onTimeout(timeoutCaptor.capture());
        timeoutCaptor.getValue().run();

        // 触发 onTimeout 后 streamClosed 应翻转为 true，立即完成 emitter 并取消上游订阅。
        verify(emitter).complete();
        verify(subscription).cancel();
        verify(interviewChatLogMapper, never()).insert(any(InterviewChatLog.class));
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

        when(interviewSessionMapper.selectOne(any())).thenReturn(session);
        when(interviewMessageService.getMessageList(sessionId)).thenReturn(List.of());
        when(interviewAiService.generateEvaluationReport(
                eq(sessionId),
                anyList(),
                anyString(),
                any(),
                any(),
                anyString(),
                any(),
                eq(session.getUserId()),
                eq(true)))
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
    void shouldGenerateEvaluationReportWithUserCustomAiContextAndUsageCount() throws Exception {
        String sessionId = "session-custom-report";
        Long userId = 123L;
        InterviewSession session = buildEndedSession(sessionId, userId, null);
        session.setAiBillingSource(UserAiConstants.BILLING_SOURCE_USER_CUSTOM);

        InterviewEvaluationReport report = InterviewEvaluationReport.builder()
                .overallScore(88)
                .summary("用户自定义 AI 生成的报告")
                .build();

        when(interviewSessionMapper.selectOne(any())).thenReturn(session);
        when(interviewMessageService.getMessageList(sessionId)).thenReturn(List.of(
                buildChatLog(1L, sessionId, "assistant", "请介绍项目。", 0),
                buildChatLog(2L, sessionId, "user", "我负责订单链路。", 1)
        ));
        when(interviewAiService.generateEvaluationReport(
                eq(sessionId),
                anyList(),
                eq(session.getJobRole()),
                eq(session.getJobRoleCode()),
                eq(session.getDifficulty()),
                eq(session.getInterviewMode()),
                any(),
                eq(userId),
                eq(false)
        )).thenReturn(report);
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

        verify(userAiUsageLimitService).checkAndIncrement(userId, UserAiConstants.USAGE_TYPE_INTERVIEW_REPORT);
        verify(userAiUsageLimitService, never()).rollback(anyLong());
        verify(interviewAiService).generateEvaluationReport(
                eq(sessionId),
                anyList(),
                eq(session.getJobRole()),
                eq(session.getJobRoleCode()),
                eq(session.getDifficulty()),
                eq(session.getInterviewMode()),
                any(),
                eq(userId),
                eq(false)
        );
        verify(interviewAiService, never()).generateEvaluationReport(
                eq(sessionId),
                anyList(),
                anyString(),
                any(),
                any(),
                anyString(),
                any()
        );
    }

    @Test
    void shouldRollbackCustomAiUsageWhenEvaluationReportGenerationFails() throws Exception {
        String sessionId = "session-custom-report-fail";
        Long userId = 123L;
        InterviewSession session = buildEndedSession(sessionId, userId, null);
        session.setAiBillingSource(UserAiConstants.BILLING_SOURCE_USER_CUSTOM);

        when(interviewSessionMapper.selectOne(any())).thenReturn(session);
        when(interviewMessageService.getMessageList(sessionId)).thenReturn(List.of(
                buildChatLog(1L, sessionId, "assistant", "请介绍项目。", 0),
                buildChatLog(2L, sessionId, "user", "我负责订单链路。", 1)
        ));
        when(interviewAiService.generateEvaluationReport(
                eq(sessionId),
                anyList(),
                eq(session.getJobRole()),
                eq(session.getJobRoleCode()),
                eq(session.getDifficulty()),
                eq(session.getInterviewMode()),
                any(),
                eq(userId),
                eq(false)
        )).thenThrow(new BusinessException(ResultCode.CUSTOM_AI_CALL_FAILED, "自定义 AI 失败"));
        when(mockInterviewService.generateMockScore(sessionId)).thenReturn(60);
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

        verify(userAiUsageLimitService).checkAndIncrement(userId, UserAiConstants.USAGE_TYPE_INTERVIEW_REPORT);
        verify(userAiUsageLimitService).rollback(userId, UserAiConstants.USAGE_TYPE_INTERVIEW_REPORT);
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
                "buildFallbackEvaluationReport", InterviewSession.class, com.airesume.server.dto.interview.InterviewJobTargetContext.class, String.class);
        method.setAccessible(true);
        InterviewEvaluationReport report = (InterviewEvaluationReport) method.invoke(interviewService, session, null, "AI JSON parse failed");

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

        when(interviewSessionMapper.selectOne(any())).thenReturn(session);
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
    void shouldReturnSessionStatusWithoutLoadingChatLogsOrJobContext() {
        String sessionId = "session-status";
        Long userId = 123L;
        var status = com.airesume.server.dto.interview.InterviewSessionStatusResponse.builder()
                .sessionId(sessionId)
                .status(1)
                .openingPending(false)
                .reportReady(true)
                .comprehensiveScore(86)
                .updateTime(LocalDateTime.now())
                .build();

        when(interviewSessionMapper.selectOwnedStatus(sessionId, userId)).thenReturn(status);

        var response = interviewService.getSessionStatus(userId, sessionId);

        assertEquals(sessionId, response.getSessionId());
        assertEquals(1, response.getStatus());
        assertEquals("已结束", response.getStatusDesc());
        assertFalse(response.getOpeningPending());
        assertTrue(response.getReportReady());
        assertEquals(86, response.getComprehensiveScore());
        verify(interviewSessionMapper).selectOwnedStatus(sessionId, userId);
        verify(interviewMessageService, never()).getMessageList(anyString());
        verify(mockInterviewJobTargetService, never()).getSessionContext(anyLong(), anyString());
    }

    @Test
    void shouldPassImmediateFeedbackModeWhenSendingMessage() {
        String sessionId = "session-feedback";
        Long userId = 123L;
        InterviewSession session = buildInProgressSession(sessionId, userId);
        session.setFeedbackMode("immediate");
        SendMessageRequest request = new SendMessageRequest();
        request.setContent("我负责支付模块的幂等和回调处理");

        when(interviewSessionMapper.selectOne(any())).thenReturn(session);
        when(interviewMessageService.getMessageList(sessionId)).thenReturn(List.of(
                buildChatLog(1L, sessionId, "assistant", "你负责过什么项目？", 0)
        ));
        when(interviewAiService.generateReply(
                eq(sessionId),
                anyList(),
                eq(request.getContent()),
                eq(session.getJobRoleCode()),
                eq(session.getJobRole()),
                eq(session.getDifficulty()),
                any(),
                eq("immediate"),
                eq("normal"),
                eq(0),
                eq(userId),
                eq(false)
        )).thenReturn("请继续说明幂等方案。\n\n<FEEDBACK>\n本题反馈：回答方向清晰，但还需要补充具体处理细节。\n</FEEDBACK>");

        interviewService.sendMessage(userId, sessionId, request);

        verify(interviewAiService).generateReply(
                eq(sessionId),
                anyList(),
                eq(request.getContent()),
                eq(session.getJobRoleCode()),
                eq(session.getJobRole()),
                eq(session.getDifficulty()),
                any(),
                eq("immediate"),
                eq("normal"),
                eq(0),
                eq(userId),
                eq(false)
        );
        verify(interviewMessageService).saveMessage(session, "user", request.getContent());
        verify(interviewMessageService).saveMessage(session, "assistant", "请继续说明幂等方案。\n\n<FEEDBACK>\n本题反馈：回答方向清晰，但还需要补充具体处理细节。\n</FEEDBACK>");
    }

    @Test
    void shouldFallbackToLatestResumeContextWhenCachedEmptyJobTargetContext() {
        String sessionId = "session-empty-context";
        Long userId = 123L;
        InterviewSession session = buildInProgressSession(sessionId, userId);
        SendMessageRequest request = new SendMessageRequest();
        request.setContent("请继续追问项目经历");
        InterviewJobTargetContext emptyContext = InterviewJobTargetContext.builder()
                .jobTargeted(false)
                .sourceType("none")
                .build();
        InterviewJobTargetContext latestResumeContext = InterviewJobTargetContext.builder()
                .jobTargeted(false)
                .sourceType("latest_resume")
                .resumeText("候选人有前端项目经验")
                .build();

        when(interviewSessionMapper.selectOne(any())).thenReturn(session);
        when(interviewMessageService.getMessageList(sessionId)).thenReturn(List.of());
        when(mockInterviewJobTargetService.getSessionContext(userId, sessionId)).thenReturn(emptyContext);
        when(mockInterviewJobTargetService.resolveLatestResumeContext(userId)).thenReturn(latestResumeContext);
        when(interviewAiService.generateReply(
                eq(sessionId),
                anyList(),
                eq(request.getContent()),
                eq(session.getJobRoleCode()),
                eq(session.getJobRole()),
                eq(session.getDifficulty()),
                same(latestResumeContext),
                eq("after_interview"),
                eq("normal"),
                eq(0),
                eq(userId),
                eq(false)
        )).thenReturn("继续追问");

        interviewService.sendMessage(userId, sessionId, request);

        verify(mockInterviewJobTargetService).resolveLatestResumeContext(userId);
        verify(interviewAiService).generateReply(
                eq(sessionId),
                anyList(),
                eq(request.getContent()),
                eq(session.getJobRoleCode()),
                eq(session.getJobRole()),
                eq(session.getDifficulty()),
                same(latestResumeContext),
                eq("after_interview"),
                eq("normal"),
                eq(0),
                eq(userId),
                eq(false)
        );
    }

    @Test
    void shouldReturnNormalizedFeedbackModeInSessionDetail() {
        String sessionId = "session-feedback-detail";
        Long userId = 123L;
        InterviewSession session = buildInProgressSession(sessionId, userId);
        session.setFeedbackMode("immediate");

        when(interviewSessionMapper.selectOne(any())).thenReturn(session);
        when(interviewMessageService.getMessageList(sessionId)).thenReturn(List.of());

        InterviewSessionResponse response = interviewService.getSessionDetail(userId, sessionId);

        assertEquals("immediate", response.getFeedbackMode());
    }

    @Test
    void shouldAllowSharedReportReaderWhenCommunityPostReferencesSession() {
        String sessionId = "session-shared-report";
        Long ownerId = 123L;
        Long readerId = 456L;
        InterviewSession session = buildEndedSession(sessionId, ownerId, 86);
        session.setEvaluationReport("{\"summary\":\"公开分享的报告摘要\"}");

        when(interviewSessionMapper.selectOne(any()))
                .thenReturn(null)
                .thenReturn(session);
        when(communityPostMapper.selectCount(any())).thenReturn(1L);

        InterviewSessionResponse response = interviewService.getSessionDetail(readerId, sessionId);

        assertEquals(sessionId, response.getSessionId());
        assertEquals("{\"summary\":\"公开分享的报告摘要\"}", response.getEvaluationReport());
        assertEquals(86, response.getComprehensiveScore());
        verify(mockInterviewJobTargetService, never()).getSessionContext(readerId, sessionId);
    }

    @Test
    void shouldRejectForeignReportReaderWhenNoCommunityShareExists() {
        String sessionId = "session-private-report";
        Long ownerId = 123L;
        Long readerId = 456L;
        InterviewSession session = buildEndedSession(sessionId, ownerId, 86);

        when(interviewSessionMapper.selectOne(any()))
                .thenReturn(null)
                .thenReturn(session);
        when(communityPostMapper.selectCount(any())).thenReturn(0L);

        BusinessException exception = assertThrows(BusinessException.class,
                () -> interviewService.getSessionDetail(readerId, sessionId));

        assertEquals("会话不存在或无权访问", exception.getMessage());
        verify(interviewMessageService, never()).getMessageList(sessionId);
    }

    @Test
    void shouldDefaultInteractionTypeToTextWhenCreatingSession() {
        CreateSessionRequest request = buildCreateRequest();
        ArgumentCaptor<InterviewSession> sessionCaptor = ArgumentCaptor.forClass(InterviewSession.class);
        when(userQuotaService.checkInterviewQuota(123L)).thenReturn(true);
        when(sysJobRoleService.isActiveRoleName("Java工程师")).thenReturn(true);
        when(mockInterviewJobTargetService.resolveContext(eq(123L), any())).thenReturn(null);

        InterviewSessionResponse response = interviewService.createSession(123L, request);

        verify(interviewSessionMapper).insert(sessionCaptor.capture());
        assertEquals(0, sessionCaptor.getValue().getInteractionType());
        assertEquals(0, response.getInteractionType());

    }

    @Test
    void shouldMarkCustomAiBillingSourceWhenCreatingSessionWithCustomAi() {
        CreateSessionRequest request = buildCreateRequest();
        ArgumentCaptor<InterviewSession> sessionCaptor = ArgumentCaptor.forClass(InterviewSession.class);
        when(userAiConfigResolver.resolve(123L, "interview", false))
                .thenReturn(ResolvedAiConfig.builder().configType("interview").build());
        when(sysJobRoleService.isActiveRoleName(request.getJobRole())).thenReturn(true);
        when(mockInterviewJobTargetService.resolveContext(eq(123L), any())).thenReturn(null);

        interviewService.createSession(123L, request);

        verify(interviewSessionMapper).insert(sessionCaptor.capture());
        assertEquals(UserAiConstants.BILLING_SOURCE_USER_CUSTOM, sessionCaptor.getValue().getAiBillingSource());
        verify(userQuotaService, never()).deductInterviewQuota(anyLong());
    }

    @Test
    void shouldDeductInterviewQuotaOnceWhenCustomAiSessionFallsBackToPlatform() {
        String sessionId = "session-platform-fallback";
        Long userId = 123L;
        SendMessageRequest request = new SendMessageRequest();
        request.setContent("please continue");
        request.setFallbackToPlatform(true);
        InterviewSession session = buildInProgressSession(sessionId, userId);
        session.setAiBillingSource(UserAiConstants.BILLING_SOURCE_USER_CUSTOM);

        when(interviewSessionMapper.selectOne(any())).thenReturn(session);
        when(interviewMessageService.getMessageList(sessionId)).thenReturn(List.of());
        when(interviewSessionMapper.markPlatformFallbackBillingIfCustom(
                eq(sessionId),
                eq(userId),
                eq(UserAiConstants.BILLING_SOURCE_PLATFORM_FALLBACK),
                any(LocalDateTime.class))).thenReturn(1);
        when(interviewAiService.generateReply(
                eq(sessionId),
                anyList(),
                eq(request.getContent()),
                eq(session.getJobRoleCode()),
                eq(session.getJobRole()),
                eq(session.getDifficulty()),
                any(),
                eq("after_interview"),
                eq("normal"),
                eq(0),
                eq(userId),
                eq(true)
        )).thenReturn("platform reply");

        SendMessageResponse response = interviewService.sendMessage(userId, sessionId, request);

        assertEquals("platform reply", response.getReplyContent());
        verify(userQuotaService).deductInterviewQuota(userId);
        verify(userAiUsageLimitService, never()).checkAndIncrement(anyLong());
        verify(interviewSessionMapper).markPlatformFallbackBillingIfCustom(
                eq(sessionId),
                eq(userId),
                eq(UserAiConstants.BILLING_SOURCE_PLATFORM_FALLBACK),
                any(LocalDateTime.class));
    }

    @Test
    void shouldNotDeductInterviewQuotaAgainWhenPlatformFallbackAlreadyMarked() {
        String sessionId = "session-platform-fallback-repeat";
        Long userId = 123L;
        SendMessageRequest request = new SendMessageRequest();
        request.setContent("use platform again");
        request.setFallbackToPlatform(true);
        InterviewSession session = buildInProgressSession(sessionId, userId);
        session.setAiBillingSource(UserAiConstants.BILLING_SOURCE_PLATFORM_FALLBACK);

        when(interviewSessionMapper.selectOne(any())).thenReturn(session);
        when(interviewMessageService.getMessageList(sessionId)).thenReturn(List.of());
        when(interviewAiService.generateReply(
                eq(sessionId),
                anyList(),
                eq(request.getContent()),
                eq(session.getJobRoleCode()),
                eq(session.getJobRole()),
                eq(session.getDifficulty()),
                any(),
                eq("after_interview"),
                eq("normal"),
                eq(0),
                eq(userId),
                eq(true)
        )).thenReturn("platform repeat reply");

        SendMessageResponse response = interviewService.sendMessage(userId, sessionId, request);

        assertEquals("platform repeat reply", response.getReplyContent());
        verify(userQuotaService, never()).deductInterviewQuota(anyLong());
        verify(interviewSessionMapper, never()).markPlatformFallbackBillingIfCustom(
                anyString(),
                anyLong(),
                anyString(),
                any(LocalDateTime.class));
    }

    @Test
    void shouldKeepUsingPlatformAfterFallbackSessionAlreadyMarkedWithoutRequestFlag() {
        String sessionId = "session-platform-fallback-locked";
        Long userId = 123L;
        SendMessageRequest request = new SendMessageRequest();
        request.setContent("continue after fallback");
        InterviewSession session = buildInProgressSession(sessionId, userId);
        session.setAiBillingSource(UserAiConstants.BILLING_SOURCE_PLATFORM_FALLBACK);

        when(interviewSessionMapper.selectOne(any())).thenReturn(session);
        when(interviewMessageService.getMessageList(sessionId)).thenReturn(List.of());
        when(interviewAiService.generateReply(
                eq(sessionId),
                anyList(),
                eq(request.getContent()),
                eq(session.getJobRoleCode()),
                eq(session.getJobRole()),
                eq(session.getDifficulty()),
                any(),
                eq("after_interview"),
                eq("normal"),
                eq(0),
                eq(userId),
                eq(true)
        )).thenReturn("platform locked reply");

        SendMessageResponse response = interviewService.sendMessage(userId, sessionId, request);

        assertEquals("platform locked reply", response.getReplyContent());
        verify(userQuotaService, never()).deductInterviewQuota(anyLong());
        verify(userAiUsageLimitService, never()).checkAndIncrement(anyLong(), anyString());
        verify(interviewSessionMapper, never()).markPlatformFallbackBillingIfCustom(
                anyString(),
                anyLong(),
                anyString(),
                any(LocalDateTime.class));
    }

    @Test
    void shouldKeepPlatformSessionOnPlatformEvenWhenUserHasCustomAiConfig() {
        String sessionId = "session-platform-locked";
        Long userId = 123L;
        SendMessageRequest request = new SendMessageRequest();
        request.setContent("continue platform session");
        InterviewSession session = buildInProgressSession(sessionId, userId);
        session.setAiBillingSource(UserAiConstants.BILLING_SOURCE_PLATFORM);

        lenient().when(userAiConfigResolver.resolve(userId, "interview", false))
                .thenReturn(ResolvedAiConfig.builder().configType("interview").build());
        when(interviewSessionMapper.selectOne(any())).thenReturn(session);
        when(interviewMessageService.getMessageList(sessionId)).thenReturn(List.of());
        when(interviewAiService.generateReply(
                eq(sessionId),
                anyList(),
                eq(request.getContent()),
                eq(session.getJobRoleCode()),
                eq(session.getJobRole()),
                eq(session.getDifficulty()),
                any(),
                eq("after_interview"),
                eq("normal"),
                eq(0),
                eq(userId),
                eq(true)
        )).thenReturn("platform session reply");

        SendMessageResponse response = interviewService.sendMessage(userId, sessionId, request);

        assertEquals("platform session reply", response.getReplyContent());
        verify(userAiConfigResolver).resolve(userId, "interview", true);
        verify(userAiConfigResolver, never()).resolve(userId, "interview", false);
        verify(userAiUsageLimitService, never()).checkAndIncrement(anyLong(), anyString());
        verify(userQuotaService, never()).deductInterviewQuota(anyLong());
    }

    @Test
    void shouldSaveVoiceInteractionTypeWhenCreatingSession() {
        CreateSessionRequest request = buildCreateRequest();
        request.setInteractionType(1);
        ArgumentCaptor<InterviewSession> sessionCaptor = ArgumentCaptor.forClass(InterviewSession.class);
        when(userQuotaService.checkInterviewQuota(123L)).thenReturn(true);
        when(sysJobRoleService.isActiveRoleName("Java工程师")).thenReturn(true);
        when(mockInterviewJobTargetService.resolveContext(eq(123L), any())).thenReturn(null);

        InterviewSessionResponse response = interviewService.createSession(123L, request);

        verify(interviewSessionMapper).insert(sessionCaptor.capture());
        assertEquals(1, sessionCaptor.getValue().getInteractionType());
        assertEquals(1, response.getInteractionType());
    }

    @Test
    void shouldRejectUnsupportedInteractionTypeWhenCreatingSession() {
        CreateSessionRequest request = buildCreateRequest();
        request.setInteractionType(3);
        when(sysJobRoleService.isActiveRoleName("Java工程师")).thenReturn(true);

        assertThrows(BusinessException.class, () -> interviewService.createSession(123L, request));
        verify(interviewSessionMapper, never()).insert(any(InterviewSession.class));
    }

    @Test
    void shouldReturnFeedbackModeInHistory() {
        String sessionId = "session-feedback-history";
        Long userId = 123L;
        InterviewSession session = buildInProgressSession(sessionId, userId);
        session.setFeedbackMode("immediate");

        doAnswer(invocation -> {
            com.baomidou.mybatisplus.extension.plugins.pagination.Page<InterviewSession> page = invocation.getArgument(0);
            page.setRecords(List.of(session));
            page.setTotal(1);
            return page;
        }).when(interviewSessionMapper).selectPage(any(), any());
        when(interviewMessageService.getMessageCountMap(any())).thenReturn(java.util.Map.of(sessionId, 2));
        when(mockInterviewJobTargetService.getSessionContextSummaryMap(eq(userId), any())).thenReturn(java.util.Map.of());

        List<InterviewHistoryResponse> history = interviewService.getHistory(userId, 1, 5).getList();

        assertEquals(1, history.size());
        assertEquals("immediate", history.get(0).getFeedbackMode());
    }

    @Test
    void shouldReturnInteractionTypeInSessionDetailAndHistory() {
        String sessionId = "session-voice-detail";
        Long userId = 123L;
        InterviewSession session = buildInProgressSession(sessionId, userId);
        session.setInteractionType(1);

        when(interviewSessionMapper.selectOne(any())).thenReturn(session);
        when(interviewMessageService.getMessageList(sessionId)).thenReturn(List.of());
        InterviewSessionResponse detail = interviewService.getSessionDetail(userId, sessionId);
        assertEquals(1, detail.getInteractionType());

        doAnswer(invocation -> {
            com.baomidou.mybatisplus.extension.plugins.pagination.Page<InterviewSession> page = invocation.getArgument(0);
            page.setRecords(List.of(session));
            page.setTotal(1);
            return page;
        }).when(interviewSessionMapper).selectPage(any(), any());
        when(interviewMessageService.getMessageCountMap(any())).thenReturn(java.util.Map.of(sessionId, 0));
        when(mockInterviewJobTargetService.getSessionContextSummaryMap(eq(userId), any())).thenReturn(java.util.Map.of());
        List<InterviewHistoryResponse> history = interviewService.getHistory(userId, 1, 5).getList();
        assertEquals(1, history.get(0).getInteractionType());
    }

    @Test
    void shouldSkipSavingDuplicateUserMessageWhenLatestMessageMatchesContent() {
        String sessionId = "session-duplicate";
        Long userId = 123L;
        String content = "这是同一条用户回答";
        InterviewSession session = buildInProgressSession(sessionId, userId);
        InterviewChatLog latestMessage = buildChatLog(99L, sessionId, "user", content, 5);

        when(interviewSessionMapper.selectOne(any())).thenReturn(session);
        when(interviewChatLogMapper.selectOne(any())).thenReturn(latestMessage);

        interviewService.saveUserMessage(sessionId, userId, content);

        verify(interviewChatLogMapper, never()).insert(any(InterviewChatLog.class));
    }

    @Test
    void shouldClearOwnedInterviewHistoryWithRelatedRecords() {
        Long userId = 123L;
        List<String> sessionIds = List.of("session-a", "session-b");
        when(interviewSessionMapper.selectActiveSessionIdsByUserId(userId)).thenReturn(sessionIds);
        when(interviewSessionMapper.logicalDeleteByUserId(eq(userId), any(LocalDateTime.class))).thenReturn(2);

        int deletedCount = interviewService.clearHistory(userId);

        assertEquals(2, deletedCount);
        verify(interviewChatLogMapper).logicalDeleteBySessionIdIn(eq(sessionIds), any(LocalDateTime.class));
        verify(mockInterviewJobTargetService).logicalDeleteByUserId(userId);
        verify(dimensionScoreMapper).logicalDeleteBySessionIds(eq(sessionIds), any(LocalDateTime.class));
        verify(interviewSessionMapper).logicalDeleteByUserId(eq(userId), any(LocalDateTime.class));
        verify(interviewRadarCache).evict(userId);
        verify(growthOverviewCache).evict(userId);
    }

    @Test
    void shouldReturnZeroWhenNoInterviewHistoryToClear() {
        Long userId = 123L;
        when(interviewSessionMapper.selectActiveSessionIdsByUserId(userId)).thenReturn(List.of());

        int deletedCount = interviewService.clearHistory(userId);

        assertEquals(0, deletedCount);
        verify(interviewChatLogMapper, never()).logicalDeleteBySessionIdIn(any(), any());
        verify(mockInterviewJobTargetService, never()).logicalDeleteByUserId(anyLong());
        verify(dimensionScoreMapper, never()).logicalDeleteBySessionIds(any(List.class), any(LocalDateTime.class));
        verify(interviewSessionMapper, never()).logicalDeleteByUserId(anyLong(), any());
        verify(interviewRadarCache, never()).evict(any());
        verify(growthOverviewCache, never()).evict(any());
    }

    @Test
    void shouldDeleteSingleInterviewHistoryWithDimensionScoresAndGrowthCaches() {
        Long userId = 123L;
        String sessionId = "session-delete";
        InterviewSession session = buildEndedSession(sessionId, userId, 80);
        when(interviewSessionMapper.selectOne(any())).thenReturn(session);

        boolean deleted = interviewService.deleteSession(userId, sessionId);

        assertTrue(deleted);
        verify(interviewChatLogMapper).logicalDeleteBySessionIdIn(eq(List.of(sessionId)), any(LocalDateTime.class));
        verify(mockInterviewJobTargetService).logicalDeleteBySessionIds(List.of(sessionId));
        verify(dimensionScoreMapper).logicalDeleteBySessionIds(eq(List.of(sessionId)), any(LocalDateTime.class));
        verify(interviewSessionMapper).logicalDeleteBySessionIdIn(eq(List.of(sessionId)), any(LocalDateTime.class));
        verify(interviewRadarCache).evict(userId);
        verify(growthOverviewCache).evict(userId);
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

        when(interviewSessionMapper.selectOne(any())).thenReturn(session);
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

        when(interviewSessionMapper.selectOne(any())).thenReturn(session);
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

        when(interviewSessionMapper.selectOne(any())).thenReturn(session);
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

        when(interviewSessionMapper.selectOne(any())).thenReturn(session);
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

    private InterviewSession buildEndedSession(String sessionId, Long userId, Integer score) {
        InterviewSession session = buildInProgressSession(sessionId, userId);
        session.setStatus(1);
        session.setComprehensiveScore(score);
        return session;
    }

    private CreateSessionRequest buildCreateRequest() {
        CreateSessionRequest request = new CreateSessionRequest();
        request.setJobRole("Java工程师");
        request.setJobRoleCode("java_backend");
        request.setDifficulty(2);
        request.setInterviewMode("normal");
        request.setFeedbackMode("after_interview");
        return request;
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
