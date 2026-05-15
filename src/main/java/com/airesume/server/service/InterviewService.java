package com.airesume.server.service;

import com.airesume.server.common.constants.InterviewConstants;
import com.airesume.server.common.exception.BusinessException;
import com.airesume.server.common.result.PageResult;
import com.airesume.server.dto.interview.ChatMessageResponse;
import com.airesume.server.dto.interview.CreateSessionRequest;
import com.airesume.server.dto.interview.InterviewEvaluationReport;
import com.airesume.server.dto.interview.InterviewHistoryResponse;
import com.airesume.server.dto.interview.InterviewJobTargetContext;
import com.airesume.server.dto.interview.InterviewSessionResponse;
import com.airesume.server.dto.interview.SendMessageRequest;
import com.airesume.server.dto.interview.SendMessageResponse;
import com.airesume.server.entity.InterviewChatLog;
import com.airesume.server.entity.InterviewSession;
import com.airesume.server.mock.MockInterviewService;
import com.airesume.server.repository.InterviewMessageRepository;
import com.airesume.server.repository.InterviewSessionRepository;
import com.baomidou.mybatisplus.core.toolkit.IdWorker;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.reactivestreams.Publisher;
import org.reactivestreams.Subscriber;
import org.reactivestreams.Subscription;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.transaction.support.TransactionTemplate;
import org.springframework.web.servlet.mvc.method.annotation.ResponseBodyEmitter;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;
import java.util.stream.Collectors;

/**
 * 模拟面试主服务。
 * 本轮在不破坏原有普通模拟面试链路的前提下，补齐岗位定向上下文解析、落库和反馈回写能力。
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class InterviewService {

    private final InterviewSessionRepository interviewSessionRepository;
    private final InterviewMessageService interviewMessageService;
    private final MockInterviewService mockInterviewService;
    private final InterviewMessageRepository interviewMessageRepository;
    private final InterviewAiService interviewAiService;
    private final ObjectMapper objectMapper;
    private final SysJobRoleService sysJobRoleService;
    private final TransactionTemplate transactionTemplate;
    private final UserQuotaService userQuotaService;
    private final MockInterviewJobTargetService mockInterviewJobTargetService;
    private final NotificationService notificationService;
    private final Executor aiAsyncExecutor;

    /**
     * 创建面试会话。
     * 先解析岗位定向上下文，再统一复用同一条会话创建链路。
     * 开场白异步生成，避免前端长时间等待。
     */
    @Transactional(rollbackFor = Exception.class)
    public InterviewSessionResponse createSession(Long userId, CreateSessionRequest request) {
        validateCreateRequest(request);
        if (!userQuotaService.checkInterviewQuota(userId)) {
            // 额度不足时创建通知（带防重，独立事务不受回滚影响）
            notificationService.createQuotaNotificationIfNeeded(userId);
            throw new BusinessException("模拟面试次数已用完");
        }

        InterviewJobTargetContext jobTargetContext = mockInterviewJobTargetService.resolveContext(userId, request);
        String interviewMode = resolveInterviewMode(request.getInterviewMode(), jobTargetContext);

        InterviewSession session = new InterviewSession();
        session.setId(IdWorker.getId());
        session.setSessionId(UUID.randomUUID().toString().replace("-", ""));
        session.setUserId(userId);
        session.setJobRole(request.getJobRole());
        session.setJobRoleCode(request.getJobRoleCode());
        session.setDifficulty(request.getDifficulty());
        session.setInterviewMode(interviewMode);
        session.setStatus(InterviewConstants.STATUS_IN_PROGRESS);
        session.setOpeningGenerated(0);
        session.setCreateTime(LocalDateTime.now());
        session.setUpdateTime(LocalDateTime.now());
        interviewSessionRepository.saveAndFlush(session);

        // 仅在会话成功创建后扣减次数，避免无效扣费。
        userQuotaService.deductInterviewQuota(userId);

        // 事务提交后异步生成开场白，避免前端长时间等待 AI 响应。
        if (TransactionSynchronizationManager.isActualTransactionActive()
                && TransactionSynchronizationManager.isSynchronizationActive()) {
            TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                @Override
                public void afterCommit() {
                    generateOpeningAsync(session.getSessionId(), userId, request, jobTargetContext);
                }
            });
        } else {
            generateOpeningAsync(session.getSessionId(), userId, request, jobTargetContext);
        }

        log.info("面试会话创建成功(开场白异步生成中), sessionId: {}, userId: {}", session.getSessionId(), userId);
        InterviewSessionResponse response = convertToSessionResponse(session, jobTargetContext);
        response.setOpeningPending(true);
        return response;
    }

    /**
     * 异步生成开场白并保存。
     * 开场白使用硬编码模板，不调用 AI，零 token 消耗。
     */
    private void generateOpeningAsync(
            String sessionId,
            Long userId,
            CreateSessionRequest request,
            InterviewJobTargetContext jobTargetContext
    ) {
        CompletableFuture.runAsync(() -> {
            try {
                // 硬编码开场白模板，不调用 AI
                String difficultyDesc = com.airesume.server.common.constants.InterviewConstants.getDifficultyLabel(request.getDifficulty() == null ? 2 : request.getDifficulty());
                boolean hasResume = jobTargetContext != null
                        && jobTargetContext.getResumeText() != null
                        && !jobTargetContext.getResumeText().isBlank();
                String resumeHint = hasResume ? "我已经看过你的简历，" : "";
                String stressHint = "stress".equalsIgnoreCase(request.getInterviewMode())
                        ? "需要提前说明，这是一场压力面试，我会对你的回答进行深入追问和质疑，请做好准备。"
                        : "";
                String openingMessage = String.format(InterviewConstants.OPENING_TEMPLATE,
                        difficultyDesc,
                        request.getJobRole() != null ? request.getJobRole() : "软件工程师",
                        resumeHint) + stressHint;

                // 使用事务模板确保所有数据库操作在事务中
                transactionTemplate.executeWithoutResult(status -> {
                    InterviewChatLog welcomeMessage = new InterviewChatLog();
                    welcomeMessage.setId(IdWorker.getId());
                    welcomeMessage.setSessionId(sessionId);
                    welcomeMessage.setMessageRole(InterviewConstants.ROLE_ASSISTANT);
                    welcomeMessage.setContent(openingMessage);
                    welcomeMessage.setCreateTime(LocalDateTime.now());
                    welcomeMessage.setUpdateTime(LocalDateTime.now());
                    welcomeMessage.setIsDeleted(0);
                    interviewMessageRepository.save(welcomeMessage);

                    mockInterviewJobTargetService.saveSessionContext(userId, sessionId, jobTargetContext, welcomeMessage.getContent());

                    interviewSessionRepository.updateOpeningGenerated(sessionId, 1, LocalDateTime.now());
                });

                log.info("开场白已生成(硬编码模板), sessionId: {}", sessionId);
            } catch (Exception e) {
                log.error("生成开场白失败, sessionId: {}, error: {}", sessionId, e.getMessage(), e);
                // 生成失败时，插入一条错误提示消息，并更新 openingGenerated 避免前端无限轮询
                try {
                    transactionTemplate.executeWithoutResult(status -> {
                        InterviewChatLog errorMessage = new InterviewChatLog();
                        errorMessage.setId(IdWorker.getId());
                        errorMessage.setSessionId(sessionId);
                        errorMessage.setMessageRole(InterviewConstants.ROLE_ASSISTANT);
                        errorMessage.setContent("抱歉，开场白生成失败，请刷新页面重试或直接开始提问。");
                        errorMessage.setCreateTime(LocalDateTime.now());
                        errorMessage.setUpdateTime(LocalDateTime.now());
                        errorMessage.setIsDeleted(0);
                        interviewMessageRepository.save(errorMessage);

                        interviewSessionRepository.updateOpeningGenerated(sessionId, 1, LocalDateTime.now());
                    });
                } catch (Exception ex) {
                    log.error("保存开场白失败消息时发生异常, sessionId: {}", sessionId, ex);
                }
            }
        }, aiAsyncExecutor);
    }

    /**
     * 非流式发送消息。
     */
    @Transactional(rollbackFor = Exception.class)
    public SendMessageResponse sendMessage(Long userId, String sessionId, SendMessageRequest request) {
        InterviewSession session = getSessionByOwnerOrThrow(sessionId, userId);
        assertSessionInProgress(session);
        // 开场白尚未生成完成时，拒绝发送消息，避免上下文缺失
        if (session.getOpeningGenerated() == null || session.getOpeningGenerated() == 0) {
            throw new BusinessException("开场白正在生成中，请稍候再发送消息");
        }

        List<InterviewChatLog> chatLogs = interviewMessageService.getMessageList(sessionId);
        List<InterviewAiService.ChatMessageItem> history = chatLogs.stream()
                .map(log -> new InterviewAiService.ChatMessageItem(log.getMessageRole(), log.getContent()))
                .toList();
        InterviewJobTargetContext jobTargetContext = resolveConversationContext(userId, sessionId);

        String reply = interviewAiService.generateReply(
                sessionId,
                history,
                request.getContent(),
                session.getJobRoleCode(),
                session.getDifficulty(),
                jobTargetContext
        );

        interviewMessageService.saveMessage(session, InterviewConstants.ROLE_USER, request.getContent());
        interviewMessageService.saveMessage(session, InterviewConstants.ROLE_ASSISTANT, reply);

        return SendMessageResponse.builder()
                .sessionId(sessionId)
                .replyContent(reply)
                .build();
    }

    /**
     * 查询会话详情。
     */
    public InterviewSessionResponse getSessionDetail(Long userId, String sessionId) {
        InterviewSession session = getSessionByOwnerOrThrow(sessionId, userId);
        List<InterviewChatLog> chatLogs = interviewMessageService.getMessageList(sessionId);
        InterviewJobTargetContext jobTargetContext =
                mockInterviewJobTargetService.getSessionContext(userId, sessionId);
        return convertToSessionResponse(session, chatLogs, jobTargetContext);
    }

    /**
     * 结束会话。
     */
    @Transactional(rollbackFor = Exception.class)
    public void endSession(Long userId, String sessionId) {
        InterviewSession session = getSessionByOwnerOrThrow(sessionId, userId);
        if (!isSessionInProgress(session)) {
            log.info("会话已结束，忽略重复结束请求, sessionId: {}, userId: {}", sessionId, userId);
            return;
        }

        int updatedRows = interviewSessionRepository.updateStatusIfCurrentStatus(
                sessionId,
                userId,
                InterviewConstants.STATUS_IN_PROGRESS,
                InterviewConstants.STATUS_ENDED,
                LocalDateTime.now()
        );
        if (updatedRows == 0) {
            log.info("会话已被并发请求结束，忽略重复结束, sessionId: {}, userId: {}", sessionId, userId);
            return;
        }

        Runnable reportTask = () -> triggerEvaluationReportAsync(sessionId);
        if (TransactionSynchronizationManager.isActualTransactionActive()
                && TransactionSynchronizationManager.isSynchronizationActive()) {
            TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                @Override
                public void afterCommit() {
                    reportTask.run();
                }
            });
        } else {
            reportTask.run();
        }

        log.info("面试会话结束成功，已提交报告生成任务, sessionId: {}, userId: {}", sessionId, userId);
    }

    /**
     * 异步触发面试评估报告生成。
     */
    private void triggerEvaluationReportAsync(String sessionId) {
        CompletableFuture.runAsync(() -> generateAndPersistEvaluationReport(sessionId), aiAsyncExecutor);
    }

    /**
     * 生成并落库评估报告。
     * 岗位定向反馈由同一份结构化报告二次提取，避免再起一套 AI 调用。
     */
    private void generateAndPersistEvaluationReport(String sessionId) {
        InterviewSession session = interviewSessionRepository.findBySessionId(sessionId).orElse(null);
        if (session == null) {
            log.warn("异步生成报告时会话不存在, sessionId: {}", sessionId);
            return;
        }
        if (isSessionInProgress(session)) {
            log.warn("会话仍在进行中，跳过报告生成, sessionId: {}", sessionId);
            return;
        }
        if (session.getEvaluationReport() != null && !session.getEvaluationReport().isBlank()) {
            log.info("会话报告已存在，跳过重复生成, sessionId: {}", sessionId);
            return;
        }

        List<InterviewChatLog> chatLogs = interviewMessageService.getMessageList(sessionId);
        List<InterviewAiService.ChatMessageItem> history = chatLogs.stream()
                .map(log -> new InterviewAiService.ChatMessageItem(log.getMessageRole(), log.getContent()))
                .toList();
        InterviewJobTargetContext jobTargetContext =
                mockInterviewJobTargetService.getSessionContext(session.getUserId(), sessionId);

        Integer score;
        String evaluationReportJson;
        try {
            InterviewEvaluationReport report = interviewAiService.generateEvaluationReport(
                    sessionId,
                    history,
                    session.getJobRole(),
                    session.getJobRoleCode(),
                    session.getDifficulty(),
                    session.getInterviewMode(),
                    jobTargetContext
            );
            score = report.getOverallScore();
            mockInterviewJobTargetService.updateFeedback(
                    sessionId,
                    mockInterviewJobTargetService.buildFeedback(report, jobTargetContext)
            );
            evaluationReportJson = writeEvaluationReport(report);
            log.info("异步 AI 评估报告生成成功, sessionId: {}, score: {}", sessionId, score);
        } catch (Exception e) {
            log.warn("异步 AI 评估失败，降级使用 Mock 报告, sessionId: {}, error: {}", sessionId, e.getMessage());
            InterviewEvaluationReport fallbackReport = buildFallbackEvaluationReport(session, jobTargetContext);
            score = fallbackReport.getOverallScore();
            mockInterviewJobTargetService.updateFeedback(
                    sessionId,
                    mockInterviewJobTargetService.buildFeedback(fallbackReport, jobTargetContext)
            );
            evaluationReportJson = writeEvaluationReport(fallbackReport);
            log.info("异步 Mock 评估报告生成成功, sessionId: {}, score: {}", sessionId, score);
        }

        Integer finalScore = score;
        String finalReportJson = evaluationReportJson;
        final int[] updatedRows = {0};
        transactionTemplate.executeWithoutResult(status -> updatedRows[0] = interviewSessionRepository.updateEvaluationReportIfAbsent(
                sessionId,
                finalScore,
                finalReportJson,
                InterviewConstants.STATUS_ENDED,
                LocalDateTime.now()
        ));

        // 只允许首个异步结果落库，避免并发结束同一场面试时互相覆盖评估报告。
        if (updatedRows[0] == 0) {
            log.info("面试评估报告已存在，跳过重复回写, sessionId: {}", sessionId);
            return;
        }

        // 创建模拟面试完成通知
        notificationService.createNotification(
                session.getUserId(), "interview", "模拟面试完成",
                "你的模拟面试反馈已生成，点击查看详情。",
                "mock_interview", sessionId);
    }

    /**
     * 历史记录分页查询。
     */
    public PageResult<InterviewHistoryResponse> getHistory(Long userId, Integer pageNum, Integer pageSize) {
        Pageable pageable = PageRequest.of(pageNum - 1, pageSize, Sort.by(Sort.Direction.DESC, "createTime"));
        Page<InterviewSession> page = interviewSessionRepository.findByUserId(userId, pageable);
        List<InterviewHistoryResponse> list = buildHistoryResponses(userId, page.getContent());
        return PageResult.of(list, page.getTotalElements(), pageNum, pageSize);
    }

    /**
     * 兼容旧版全量历史接口。
     */
    @Deprecated
    public List<InterviewHistoryResponse> getAllHistory(Long userId) {
        List<InterviewSession> sessions = interviewSessionRepository.findByUserIdOrderByCreateTimeDesc(userId);
        return buildHistoryResponses(userId, sessions);
    }

    /**
     * 流式发送前统一拉取历史并校验状态。
     */
    public List<InterviewChatLog> getChatLogsForStream(String sessionId, Long userId) {
        InterviewSession session = getSessionByOwnerOrThrow(sessionId, userId);
        assertSessionInProgress(session);
        return interviewMessageService.getMessageList(sessionId);
    }

    /**
     * 流式发送前二次校验会话状态。
     */
    public void validateSessionForStream(String sessionId, Long userId) {
        InterviewSession session = getSessionByOwnerOrThrow(sessionId, userId);
        assertSessionInProgress(session);
    }

    /**
     * 在流式返回前先保存用户消息，避免历史上下文丢失。
     */
    @Transactional(rollbackFor = Exception.class)
    public void saveUserMessage(String sessionId, Long userId, String content) {
        InterviewSession session = getSessionByOwnerOrThrow(sessionId, userId);
        assertSessionInProgress(session);
        // 开场白尚未生成完成时，拒绝发送消息，避免上下文缺失
        if (session.getOpeningGenerated() == null || session.getOpeningGenerated() == 0) {
            throw new BusinessException("开场白正在生成中，请稍候再发送消息");
        }

        InterviewChatLog userMessage = new InterviewChatLog();
        userMessage.setId(IdWorker.getId());
        userMessage.setSessionId(sessionId);
        userMessage.setMessageRole(InterviewConstants.ROLE_USER);
        userMessage.setContent(content);
        userMessage.setCreateTime(LocalDateTime.now());
        userMessage.setUpdateTime(LocalDateTime.now());
        userMessage.setIsDeleted(0);
        interviewMessageRepository.save(userMessage);
    }

    /**
     * 订阅并写出 SSE 数据。
     */
    public void subscribeAndWriteStream(
            String sessionId,
            ResponseBodyEmitter emitter,
            Publisher<String> publisher,
            StringBuilder fullReply
    ) throws IOException {
        AtomicReference<Subscription> subscriptionRef = new AtomicReference<>();
        AtomicBoolean done = new AtomicBoolean(false);
        AtomicBoolean streamClosed = new AtomicBoolean(false);

        // SSE 连接关闭后立即取消订阅，避免 AI 流继续占用线程与网络资源。
        emitter.onTimeout(() -> {
            if (cancelActiveStream(sessionId, streamClosed, done, subscriptionRef, "timeout")) {
                emitter.complete();
            }
        });
        emitter.onCompletion(() -> cancelActiveStream(sessionId, streamClosed, done, subscriptionRef, "completion"));
        emitter.onError(error -> cancelActiveStream(sessionId, streamClosed, done, subscriptionRef, "error"));

        publisher.subscribe(new Subscriber<>() {
            @Override
            public void onSubscribe(Subscription s) {
                subscriptionRef.set(s);
                if (streamClosed.get()) {
                    s.cancel();
                    return;
                }
                s.request(Long.MAX_VALUE);
            }

            @Override
            public void onNext(String item) {
                if (streamClosed.get()) {
                    cancelActiveStream(sessionId, streamClosed, done, subscriptionRef, "emitter_closed_before_chunk");
                    return;
                }
                if (item == null || item.isBlank()) {
                    return;
                }
                fullReply.append(item);
                try {
                    String escaped = escapeJsonForSse(item);
                    String jsonData = "{\"type\":\"content\",\"content\":\"" + escaped + "\"}";
                    emitter.send("event: message\ndata: " + jsonData + "\n\n");
                } catch (IOException e) {
                    log.warn("SSE send failed, cancel subscription, sessionId: {}, error: {}", sessionId, e.getMessage());
                    cancelActiveStream(sessionId, streamClosed, done, subscriptionRef, "send_failed");
                }
            }

            @Override
            public void onError(Throwable t) {
                if (streamClosed.get()) {
                    return;
                }
                if (done.compareAndSet(false, true)) {
                    try {
                        String errorMsg = escapeJsonForSse(t.getMessage() == null ? "System error" : t.getMessage());
                        String jsonData = "{\"type\":\"error\",\"message\":\"" + errorMsg + "\"}";
                        emitter.send("event: message\ndata: " + jsonData + "\n\n");
                    } catch (Exception ex) {
                        log.warn("SSE error event send failed, sessionId: {}", sessionId, ex);
                    } finally {
                        emitter.completeWithError(t);
                    }
                }
            }

            @Override
            public void onComplete() {
                if (streamClosed.get()) {
                    return;
                }
                if (done.compareAndSet(false, true)) {
                    try {
                        if (fullReply.isEmpty()) {
                            String jsonData = "{\"type\":\"error\",\"message\":\"AI response is empty, please retry later\"}";
                            emitter.send("event: message\ndata: " + jsonData + "\n\n");
                            emitter.complete();
                            return;
                        }

                        persistAssistantMessage(sessionId, fullReply.toString());
                        emitter.send("event: message\ndata: {\"type\":\"done\"}\n\n");
                        emitter.complete();
                    } catch (Exception e) {
                        log.error("Failed to persist assistant message after stream, sessionId: {}", sessionId, e);
                        try {
                            String errorMsg = escapeJsonForSse("Failed to persist message, please refresh conversation history");
                            String jsonData = "{\"type\":\"error\",\"message\":\"" + errorMsg + "\"}";
                            emitter.send("event: message\ndata: " + jsonData + "\n\n");
                        } catch (Exception ex) {
                            log.warn("SSE error event send failed, sessionId: {}", sessionId, ex);
                        }
                        emitter.completeWithError(e);
                    }
                }
            }
        });
    }

    /**
     * 流式链路一旦关闭，就同时终止订阅，避免后续 token 继续生成。
     */
    private boolean cancelActiveStream(String sessionId,
                                       AtomicBoolean streamClosed,
                                       AtomicBoolean done,
                                       AtomicReference<Subscription> subscriptionRef,
                                       String reason) {
        if (done.get()) {
            streamClosed.set(true);
            return false;
        }
        if (!streamClosed.compareAndSet(false, true)) {
            return false;
        }
        done.set(true);
        Subscription subscription = subscriptionRef.getAndSet(null);
        if (subscription != null) {
            subscription.cancel();
        }
        log.info("SSE stream cancelled, sessionId: {}, reason: {}", sessionId, reason);
        return true;
    }

    private void persistAssistantMessage(String sessionId, String content) {
        transactionTemplate.executeWithoutResult(status -> {
            InterviewChatLog assistantMessage = new InterviewChatLog();
            assistantMessage.setId(IdWorker.getId());
            assistantMessage.setSessionId(sessionId);
            assistantMessage.setMessageRole(InterviewConstants.ROLE_ASSISTANT);
            assistantMessage.setContent(content);
            assistantMessage.setCreateTime(LocalDateTime.now());
            assistantMessage.setUpdateTime(LocalDateTime.now());
            assistantMessage.setIsDeleted(0);
            interviewMessageRepository.save(assistantMessage);
        });
    }

    private void validateCreateRequest(CreateSessionRequest request) {
        if (request.getJobRole() == null || request.getJobRole().trim().isEmpty()) {
            throw new BusinessException("面试岗位不能为空");
        }
        if (!sysJobRoleService.isActiveRoleName(request.getJobRole())) {
            throw new BusinessException("面试岗位不存在或已禁用");
        }
        if (request.getDifficulty() == null || request.getDifficulty() < 1 || request.getDifficulty() > 3) {
            throw new BusinessException("难度级别必须在 1-3 之间");
        }
    }

    /**
     * 规范化面试模式。
     */
    private String resolveInterviewMode(String interviewMode, InterviewJobTargetContext jobTargetContext) {
        if (jobTargetContext != null && Boolean.TRUE.equals(jobTargetContext.getJobTargeted())) {
            return InterviewConstants.MODE_JOB_TARGETED;
        }
        if (interviewMode == null || interviewMode.isBlank()) {
            return InterviewConstants.MODE_NORMAL;
        }
        String lowerMode = interviewMode.toLowerCase().trim();
        return InterviewConstants.MODE_STRESS.equals(lowerMode)
                ? InterviewConstants.MODE_STRESS
                : InterviewConstants.MODE_NORMAL;
    }

    /**
     * 获取面试模式文案。
     */
    private String getInterviewModeDescription(String interviewMode) {
        if (InterviewConstants.MODE_JOB_TARGETED.equals(interviewMode)) {
            return "岗位定向模拟";
        }
        if (InterviewConstants.MODE_STRESS.equals(interviewMode)) {
            return "压力面试";
        }
        return "普通面试";
    }

    /**
     * 统一生成对前端返回的面试模式。
     * 兼容历史数据中“岗位定向=true，但面试模式仍为 normal”的旧记录。
     */
    private String resolveResponseInterviewMode(String storedInterviewMode, InterviewJobTargetContext jobTargetContext) {
        if (jobTargetContext != null && Boolean.TRUE.equals(jobTargetContext.getJobTargeted())) {
            return InterviewConstants.MODE_JOB_TARGETED;
        }
        if (storedInterviewMode == null || storedInterviewMode.isBlank()) {
            return InterviewConstants.MODE_NORMAL;
        }
        return storedInterviewMode;
    }

    /**
     * 为问答阶段兜底补齐普通模拟面试的简历上下文。
     * 说明：岗位定向会优先返回会话上下文；普通模拟面试则回退到最近一次简历诊断。
     */
    private InterviewJobTargetContext resolveConversationContext(Long userId, String sessionId) {
        InterviewJobTargetContext sessionContext = mockInterviewJobTargetService.getSessionContext(userId, sessionId);
        if (sessionContext != null) {
            return sessionContext;
        }
        return mockInterviewJobTargetService.resolveLatestResumeContext(userId);
    }

    /**
     * 转换为会话响应，不携带聊天记录。
     */
    private InterviewSessionResponse convertToSessionResponse(
            InterviewSession session,
            InterviewJobTargetContext jobTargetContext
    ) {
        String responseInterviewMode = resolveResponseInterviewMode(session.getInterviewMode(), jobTargetContext);
        boolean openingPending = session.getOpeningGenerated() == null || session.getOpeningGenerated() == 0;
        return InterviewSessionResponse.builder()
                .id(session.getId())
                .sessionId(session.getSessionId())
                .userId(session.getUserId())
                .jobRole(session.getJobRole())
                .jobRoleCode(session.getJobRoleCode())
                .difficulty(session.getDifficulty())
                .difficultyDesc(convertDifficultyToDesc(session.getDifficulty()))
                .interviewMode(responseInterviewMode)
                .interviewModeDesc(getInterviewModeDescription(responseInterviewMode))
                .status(session.getStatus())
                .statusDesc(isSessionInProgress(session) ? "进行中" : "已结束")
                .comprehensiveScore(session.getComprehensiveScore())
                .jobTargeted(jobTargetContext != null && Boolean.TRUE.equals(jobTargetContext.getJobTargeted()))
                .jobTargetContext(jobTargetContext)
                .openingPending(openingPending)
                .createTime(session.getCreateTime())
                .updateTime(session.getUpdateTime())
                .build();
    }

    /**
     * 转换为会话响应，携带聊天记录。
     */
    private InterviewSessionResponse convertToSessionResponse(
            InterviewSession session,
            List<InterviewChatLog> chatLogs,
            InterviewJobTargetContext jobTargetContext
    ) {
        String responseInterviewMode = resolveResponseInterviewMode(session.getInterviewMode(), jobTargetContext);
        List<ChatMessageResponse> dtoLogs = (chatLogs == null ? Collections.<InterviewChatLog>emptyList() : chatLogs)
                .stream()
                .map(log -> ChatMessageResponse.builder()
                        .id(log.getId())
                        .messageRole(log.getMessageRole())
                        .content(log.getContent())
                        .createTime(log.getCreateTime())
                        .build())
                .collect(Collectors.toList());

        return InterviewSessionResponse.builder()
                .id(session.getId())
                .sessionId(session.getSessionId())
                .userId(session.getUserId())
                .jobRole(session.getJobRole())
                .jobRoleCode(session.getJobRoleCode())
                .difficulty(session.getDifficulty())
                .difficultyDesc(convertDifficultyToDesc(session.getDifficulty()))
                .interviewMode(responseInterviewMode)
                .interviewModeDesc(getInterviewModeDescription(responseInterviewMode))
                .status(session.getStatus())
                .statusDesc(isSessionInProgress(session) ? "进行中" : "已结束")
                .comprehensiveScore(session.getComprehensiveScore())
                .evaluationReport(session.getEvaluationReport())
                .jobTargeted(jobTargetContext != null && Boolean.TRUE.equals(jobTargetContext.getJobTargeted()))
                .jobTargetContext(jobTargetContext)
                .chatLogs(dtoLogs)
                .openingPending(session.getOpeningGenerated() == null || session.getOpeningGenerated() == 0)
                .createTime(session.getCreateTime())
                .updateTime(session.getUpdateTime())
                .build();
    }

    /**
     * 转换历史记录响应。
     */
    /**
     * 批量构建历史记录响应，避免分页结果逐条查询附加信息。
     */
    private List<InterviewHistoryResponse> buildHistoryResponses(Long userId, List<InterviewSession> sessions) {
        if (sessions == null || sessions.isEmpty()) {
            return List.of();
        }

        Set<String> sessionIds = sessions.stream()
                .map(InterviewSession::getSessionId)
                .collect(Collectors.toCollection(LinkedHashSet::new));
        Map<String, Integer> messageCountMap = interviewMessageService.getMessageCountMap(sessionIds);
        Map<String, InterviewJobTargetContext> contextMap =
                mockInterviewJobTargetService.getSessionContextSummaryMap(userId, sessionIds);

        return sessions.stream()
                .map(session -> convertToHistoryResponse(
                        session,
                        messageCountMap.getOrDefault(session.getSessionId(), 0),
                        contextMap.get(session.getSessionId())))
                .collect(Collectors.toList());
    }

    private InterviewHistoryResponse convertToHistoryResponse(InterviewSession session,
                                                             Integer messageCount,
                                                             InterviewJobTargetContext jobTargetContext) {
        String interviewMode = session.getInterviewMode();
        if (interviewMode == null || interviewMode.isBlank()) {
            interviewMode = InterviewConstants.MODE_NORMAL;
        }
        String responseInterviewMode = resolveResponseInterviewMode(interviewMode, jobTargetContext);

        return InterviewHistoryResponse.builder()
                .id(session.getId())
                .sessionId(session.getSessionId())
                .jobRole(session.getJobRole())
                .difficulty(session.getDifficulty())
                .difficultyDesc(convertDifficultyToDesc(session.getDifficulty()))
                .interviewMode(responseInterviewMode)
                .interviewModeDesc(getInterviewModeDescription(responseInterviewMode))
                .status(session.getStatus())
                .statusDesc(isSessionInProgress(session) ? "进行中" : "已结束")
                .comprehensiveScore(session.getComprehensiveScore())
                .messageCount(messageCount == null ? 0 : messageCount)
                .jobTargeted(jobTargetContext != null && Boolean.TRUE.equals(jobTargetContext.getJobTargeted()))
                .sourceType(jobTargetContext == null ? null : jobTargetContext.getSourceType())
                .createTime(session.getCreateTime())
                .updateTime(session.getUpdateTime())
                .build();
    }

    /**
     * 生成 fallback 报告。
     * 当真实 AI 失败时，仍保持结构化字段完整，方便前端稳定展示岗位反馈区。
     */
    private InterviewEvaluationReport buildFallbackEvaluationReport(
            InterviewSession session,
            InterviewJobTargetContext jobTargetContext
    ) {
        int score = mockInterviewService.generateMockScore(session.getSessionId());
        String summary = Boolean.TRUE.equals(jobTargetContext != null ? jobTargetContext.getJobTargeted() : false)
                ? "本次岗位定向模拟面试已结合目标岗位要求给出基础评估，建议继续围绕岗位关键能力补齐案例表达。"
                : "本次模拟面试已生成基础评估，建议继续补强案例细节与表达完整性。";

        return InterviewEvaluationReport.builder()
                .overallScore(score)
                .level(resolveLevel(score))
                .summary(summary)
                .finalVerdict(score >= 80 ? "表现较好" : "仍有提升空间")
                .strengths(List.of("回答具备基本结构", "能够围绕问题给出业务表达"))
                .weaknesses(Boolean.TRUE.equals(jobTargetContext != null ? jobTargetContext.getJobTargeted() : false)
                        ? List.of("岗位要求与案例关联仍可加强")
                        : List.of("案例细节不够充分"))
                .improvementSuggestions(Boolean.TRUE.equals(jobTargetContext != null ? jobTargetContext.getJobTargeted() : false)
                        ? List.of("补充与目标岗位最相关的项目证据", "强化对 JD 关键能力项的量化表达")
                        : List.of("补充更多项目细节", "提升回答的结构化程度"))
                .suggestions(Boolean.TRUE.equals(jobTargetContext != null ? jobTargetContext.getJobTargeted() : false)
                        ? List.of("补充与目标岗位最相关的项目证据", "强化对 JD 关键能力项的量化表达")
                        : List.of("补充更多项目细节", "提升回答的结构化程度"))
                .improvements(Boolean.TRUE.equals(jobTargetContext != null ? jobTargetContext.getJobTargeted() : false)
                        ? List.of("加强岗位匹配表达")
                        : List.of("加强案例细节表达"))
                .jobMatch(InterviewEvaluationReport.DimensionScore.builder()
                        .score(Boolean.TRUE.equals(jobTargetContext != null ? jobTargetContext.getJobTargeted() : false) ? score : null)
                        .comment(Boolean.TRUE.equals(jobTargetContext != null ? jobTargetContext.getJobTargeted() : false)
                                ? "当前回答与目标岗位存在基础匹配度，但仍需提升案例与岗位要求的贴合程度。"
                                : null)
                        .build())
                .build();
    }

    /**
     * 统一序列化评估报告。
     */
    private String writeEvaluationReport(InterviewEvaluationReport report) {
        try {
            return objectMapper.writeValueAsString(report);
        } catch (JsonProcessingException e) {
            throw new BusinessException("评估报告序列化失败");
        }
    }

    /**
     * 根据分数生成等级，兼容前端既有展示逻辑。
     */
    private String resolveLevel(Integer score) {
        int safeScore = score == null ? 0 : score;
        if (safeScore >= 85) {
            return "A";
        }
        if (safeScore >= 75) {
            return "B";
        }
        if (safeScore >= 60) {
            return "C";
        }
        return "D";
    }

    /**
     * 难度描述转换。
     */
    private String convertDifficultyToDesc(Integer difficulty) {
        if (difficulty == null) {
            return "未知";
        }
        return com.airesume.server.common.constants.InterviewConstants.getDifficultyLabel(difficulty);
    }

    /**
     * SSE 返回中的 JSON 转义。
     */
    private String escapeJsonForSse(String raw) {
        if (raw == null || raw.isEmpty()) {
            return raw;
        }
        return raw
                .replace("\\", "\\\\")
                .replace("\"", "\\\"")
                .replace("\n", "\\n")
                .replace("\r", "\\r")
                .replace("\t", "\\t");
    }

    /**
     * 按归属查询会话，不存在则抛错。
     */
    private InterviewSession getSessionByOwnerOrThrow(String sessionId, Long userId) {
        return interviewSessionRepository.findBySessionIdAndUserId(sessionId, userId)
                .orElseThrow(() -> new BusinessException("会话不存在或无权访问"));
    }

    /**
     * 获取用户归属会话，兼容旧 Controller 的空值判断。
     */
    public InterviewSession getSessionByOwner(String sessionId, Long userId) {
        return interviewSessionRepository.findBySessionIdAndUserId(sessionId, userId).orElse(null);
    }

    /**
     * 判断会话是否进行中。
     */
    private boolean isSessionInProgress(InterviewSession session) {
        return session != null
                && session.getStatus() != null
                && session.getStatus() == InterviewConstants.STATUS_IN_PROGRESS;
    }

    /**
     * 统一拦截非进行中会话的继续发送行为。
     */
    private void assertSessionInProgress(InterviewSession session) {
        if (!isSessionInProgress(session)) {
            throw new BusinessException("会话已结束，无法继续发送消息");
        }
    }
}
