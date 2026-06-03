package com.airesume.server.service;

import com.airesume.server.common.constants.InterviewConstants;
import com.airesume.server.common.constants.AiEngineConstants;
import com.airesume.server.common.constants.UserAiConstants;
import com.airesume.server.common.exception.BusinessException;
import com.airesume.server.common.result.PageResult;
import com.airesume.server.common.result.ResultCode;
import com.airesume.server.dto.interview.ChatMessageResponse;
import com.airesume.server.dto.interview.CreateSessionRequest;
import com.airesume.server.dto.interview.InterviewEvaluationReport;
import com.airesume.server.dto.interview.InterviewHistoryResponse;
import com.airesume.server.dto.interview.InterviewJobTargetContext;
import com.airesume.server.dto.interview.InterviewReplayRoundResponse;
import com.airesume.server.dto.interview.InterviewSessionResponse;
import com.airesume.server.dto.interview.InterviewSessionStatusResponse;
import com.airesume.server.dto.interview.SendMessageRequest;
import com.airesume.server.dto.interview.SendMessageResponse;
import com.airesume.server.entity.InterviewChatLog;
import com.airesume.server.entity.InterviewDimensionScore;
import com.airesume.server.entity.InterviewSession;
import com.airesume.server.mapper.InterviewDimensionScoreMapper;
import com.airesume.server.mapper.InterviewSessionMapper;
import com.airesume.server.mapper.InterviewChatLogMapper;
import com.airesume.server.mapper.CommunityPostMapper;
import com.airesume.server.mock.MockInterviewService;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.core.toolkit.IdWorker;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.reactivestreams.Publisher;
import org.reactivestreams.Subscriber;
import org.reactivestreams.Subscription;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.transaction.support.TransactionTemplate;
import org.springframework.web.servlet.mvc.method.annotation.ResponseBodyEmitter;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
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
@Slf4j
public class InterviewService {

    private final InterviewSessionMapper interviewSessionMapper;
    private final InterviewMessageService interviewMessageService;
    private final MockInterviewService mockInterviewService;
    private final InterviewChatLogMapper interviewChatLogMapper;
    private final InterviewAiService interviewAiService;
    private final ObjectMapper objectMapper;
    private final SysJobRoleService sysJobRoleService;
    private final TransactionTemplate transactionTemplate;
    private final UserQuotaService userQuotaService;
    private final MockInterviewJobTargetService mockInterviewJobTargetService;
    private final NotificationService notificationService;
    private final InterviewDimensionScoreMapper dimensionScoreMapper;
    private final CommunityPostMapper communityPostMapper;
    private final InterviewDimensionScoreService dimensionScoreService;
    private final Executor aiAsyncExecutor;
    private final UserAiConfigResolver userAiConfigResolver;
    private final UserAiUsageLimitService userAiUsageLimitService;

    /**
     * 生产注入构造器，包含用户自定义 AI 解析与用量服务。
     */
    @Autowired
    public InterviewService(
            InterviewSessionMapper interviewSessionMapper,
            InterviewMessageService interviewMessageService,
            MockInterviewService mockInterviewService,
            InterviewChatLogMapper interviewChatLogMapper,
            InterviewAiService interviewAiService,
            ObjectMapper objectMapper,
            SysJobRoleService sysJobRoleService,
            TransactionTemplate transactionTemplate,
            UserQuotaService userQuotaService,
            MockInterviewJobTargetService mockInterviewJobTargetService,
            NotificationService notificationService,
            InterviewDimensionScoreMapper dimensionScoreMapper,
            CommunityPostMapper communityPostMapper,
            InterviewDimensionScoreService dimensionScoreService,
            Executor aiAsyncExecutor,
            UserAiConfigResolver userAiConfigResolver,
            UserAiUsageLimitService userAiUsageLimitService) {
        this.interviewSessionMapper = interviewSessionMapper;
        this.interviewMessageService = interviewMessageService;
        this.mockInterviewService = mockInterviewService;
        this.interviewChatLogMapper = interviewChatLogMapper;
        this.interviewAiService = interviewAiService;
        this.objectMapper = objectMapper;
        this.sysJobRoleService = sysJobRoleService;
        this.transactionTemplate = transactionTemplate;
        this.userQuotaService = userQuotaService;
        this.mockInterviewJobTargetService = mockInterviewJobTargetService;
        this.notificationService = notificationService;
        this.dimensionScoreMapper = dimensionScoreMapper;
        this.communityPostMapper = communityPostMapper;
        this.dimensionScoreService = dimensionScoreService;
        this.aiAsyncExecutor = aiAsyncExecutor;
        this.userAiConfigResolver = userAiConfigResolver;
        this.userAiUsageLimitService = userAiUsageLimitService;
    }

    /**
     * 兼容既有单元测试的构造器，生产注入使用包含自定义 AI 依赖的完整构造器。
     */
    public InterviewService(
            InterviewSessionMapper interviewSessionMapper,
            InterviewMessageService interviewMessageService,
            MockInterviewService mockInterviewService,
            InterviewChatLogMapper interviewChatLogMapper,
            InterviewAiService interviewAiService,
            ObjectMapper objectMapper,
            SysJobRoleService sysJobRoleService,
            TransactionTemplate transactionTemplate,
            UserQuotaService userQuotaService,
            MockInterviewJobTargetService mockInterviewJobTargetService,
            NotificationService notificationService,
            InterviewDimensionScoreMapper dimensionScoreMapper,
            CommunityPostMapper communityPostMapper,
            InterviewDimensionScoreService dimensionScoreService,
            Executor aiAsyncExecutor) {
        this(interviewSessionMapper, interviewMessageService, mockInterviewService, interviewChatLogMapper,
                interviewAiService, objectMapper, sysJobRoleService, transactionTemplate, userQuotaService,
                mockInterviewJobTargetService, notificationService, dimensionScoreMapper, communityPostMapper,
                dimensionScoreService, aiAsyncExecutor, null, null);
    }

    /** Spring Cache 管理器，用于按逻辑缓存名清理成长中心聚合数据。 */
    @Autowired(required = false)
    private CacheManager cacheManager;

    /**
     * 创建面试会话。
     * 先解析岗位定向上下文，再统一复用同一条会话创建链路。
     * 开场白异步生成，避免前端长时间等待。
     */
    @Transactional(rollbackFor = Exception.class)
    public InterviewSessionResponse createSession(Long userId, CreateSessionRequest request) {
        validateCreateRequest(request);
        boolean fallbackToPlatform = Boolean.TRUE.equals(request.getFallbackToPlatform());
        boolean useCustomAi = shouldUseCustomAi(userId, fallbackToPlatform);
        if (!useCustomAi) {
            if (!userQuotaService.checkInterviewQuota(userId)) {
                // 额度不足时创建通知（带防重，独立事务不受回滚影响）
                notificationService.createQuotaNotificationIfNeeded(userId);
                throw new BusinessException("模拟面试次数已用完");
            }

            // 平台 AI 保持原面试会话额度扣减；用户自定义 AI 不消耗平台额度。
            userQuotaService.deductInterviewQuota(userId);
        }

        InterviewJobTargetContext jobTargetContext = mockInterviewJobTargetService.resolveContext(userId, request);
        String interviewMode = resolveInterviewMode(request.getInterviewMode(), jobTargetContext);
        String feedbackMode = resolveFeedbackMode(request.getFeedbackMode());
        Integer interactionType = resolveInteractionType(request.getInteractionType());
        log.info("创建面试会话配置解析完成, userId: {}, requestFeedbackMode: {}, resolvedFeedbackMode: {}, requestInterviewMode: {}, resolvedInterviewMode: {}, interactionType: {}",
                userId, request.getFeedbackMode(), feedbackMode, request.getInterviewMode(), interviewMode, interactionType);

        InterviewSession session = new InterviewSession();
        session.setId(IdWorker.getId());
        session.setSessionId(UUID.randomUUID().toString().replace("-", ""));
        session.setUserId(userId);
        session.setJobRole(request.getJobRole());
        session.setJobRoleCode(request.getJobRoleCode());
        session.setDifficulty(request.getDifficulty());
        session.setInterviewMode(interviewMode);
        session.setFeedbackMode(feedbackMode);
        session.setInteractionType(interactionType);
        session.setAiBillingSource(useCustomAi
                ? UserAiConstants.BILLING_SOURCE_USER_CUSTOM
                : UserAiConstants.BILLING_SOURCE_PLATFORM);
        session.setStatus(InterviewConstants.STATUS_IN_PROGRESS);
        session.setOpeningGenerated(0);
        session.setCreateTime(LocalDateTime.now());
        session.setUpdateTime(LocalDateTime.now());
        interviewSessionMapper.insert(session);

        // 事务提交后异步生成开场白，避免前端长时间等待 AI 响应。
        if (TransactionSynchronizationManager.isActualTransactionActive()
                && TransactionSynchronizationManager.isSynchronizationActive()) {
            TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                @Override
                public void afterCommit() {
                    generateOpeningAsync(session.getSessionId(), userId, request, jobTargetContext, interviewMode);
                }
            });
        } else {
            generateOpeningAsync(session.getSessionId(), userId, request, jobTargetContext, interviewMode);
        }

        log.info("面试会话创建成功(开场白异步生成中), sessionId: {}, userId: {}, feedbackMode: {}",
                session.getSessionId(), userId, feedbackMode);
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
            InterviewJobTargetContext jobTargetContext,
            String interviewMode
    ) {
        CompletableFuture.runAsync(() -> {
            try {
                // 硬编码开场白模板，不调用 AI
                String difficultyDesc = com.airesume.server.common.constants.InterviewConstants.getDifficultyLabel(request.getDifficulty() == null ? 2 : request.getDifficulty());
                boolean hasResume = jobTargetContext != null
                        && jobTargetContext.getResumeText() != null
                        && !jobTargetContext.getResumeText().isBlank();
                String resumeHint = hasResume ? "我已经看过你的简历，" : "";
                String personaHint = buildOpeningPersonaHint(interviewMode);
                String openingMessage = String.format(InterviewConstants.OPENING_TEMPLATE,
                        difficultyDesc,
                        request.getJobRole() != null ? request.getJobRole() : "软件工程师",
                        resumeHint) + personaHint;

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
                    interviewChatLogMapper.insert(welcomeMessage);

                    mockInterviewJobTargetService.saveSessionContext(userId, sessionId, jobTargetContext, welcomeMessage.getContent());

                    interviewSessionMapper.updateOpeningGenerated(sessionId, 1, LocalDateTime.now());
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
                        interviewChatLogMapper.insert(errorMessage);

                        interviewSessionMapper.updateOpeningGenerated(sessionId, 1, LocalDateTime.now());
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
        String resolvedFeedbackMode = resolveFeedbackMode(request.getFeedbackMode(), session);
        log.info("发送面试消息配置解析完成, sessionId: {}, requestFeedbackMode: {}, sessionFeedbackMode: {}, resolvedFeedbackMode: {}",
                sessionId, request.getFeedbackMode(), session.getFeedbackMode(), resolvedFeedbackMode);

        boolean fallbackToPlatform = Boolean.TRUE.equals(request.getFallbackToPlatform());
        boolean useCustomAi = shouldUseCustomAi(userId, fallbackToPlatform);
        if (useCustomAi) {
            userAiUsageLimitService.checkAndIncrement(userId, UserAiConstants.USAGE_TYPE_INTERVIEW_MESSAGE);
        } else {
            chargePlatformFallbackQuotaIfNeeded(session, fallbackToPlatform);
        }
        String reply;
        try {
            reply = interviewAiService.generateReply(
                    sessionId,
                    history,
                    request.getContent(),
                    session.getJobRoleCode(),
                    session.getJobRole(),
                    session.getDifficulty(),
                    jobTargetContext,
                    resolvedFeedbackMode,
                    session.getInterviewMode(),
                    resolveInteractionType(session.getInteractionType()),
                    userId,
                    fallbackToPlatform
            );
        } catch (RuntimeException e) {
            if (useCustomAi) {
                userAiUsageLimitService.rollback(userId, UserAiConstants.USAGE_TYPE_INTERVIEW_MESSAGE);
            }
            throw e;
        }

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
    @Transactional(readOnly = true)
    public InterviewSessionResponse getSessionDetail(Long userId, String sessionId) {
        InterviewSession session = getSessionByOwner(sessionId, userId);
        if (session == null) {
            session = getSharedReportSessionOrThrow(sessionId);
            return convertToSharedReportResponse(session);
        }
        List<InterviewChatLog> chatLogs = interviewMessageService.getMessageList(sessionId);
        InterviewJobTargetContext jobTargetContext =
                mockInterviewJobTargetService.getSessionContext(userId, sessionId);
        return convertToSessionResponse(session, chatLogs, jobTargetContext);
    }

    /**
     * 查询会话轻量状态。
     * 该接口专用于前端轮询，不加载聊天记录、岗位上下文和评估报告大字段。
     */
    @Transactional(readOnly = true)
    public InterviewSessionStatusResponse getSessionStatus(Long userId, String sessionId) {
        InterviewSessionStatusResponse response = interviewSessionMapper.selectOwnedStatus(sessionId, userId);
        if (response == null) {
            throw new BusinessException("会话不存在或无权访问");
        }
        response.setStatusDesc(response.getStatus() != null
                && response.getStatus() == InterviewConstants.STATUS_IN_PROGRESS ? "进行中" : "已结束");
        response.setOpeningPending(Boolean.TRUE.equals(response.getOpeningPending()));
        response.setReportReady(Boolean.TRUE.equals(response.getReportReady()));
        return response;
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

        int updatedRows = interviewSessionMapper.updateStatusIfCurrentStatus(
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
        InterviewSession session = selectSessionBySessionId(sessionId, true);
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
        boolean useCustomAiForReport = UserAiConstants.BILLING_SOURCE_USER_CUSTOM.equals(session.getAiBillingSource());
        boolean fallbackToPlatformForReport = !useCustomAiForReport;
        boolean customAiReportCounted = false;
        try {
            if (useCustomAiForReport && userAiUsageLimitService != null) {
                // 报告生成属于真实用户自定义 AI 调用，需要和问答一样计入用户每日自定义 AI 次数。
                userAiUsageLimitService.checkAndIncrement(session.getUserId(), UserAiConstants.USAGE_TYPE_INTERVIEW_REPORT);
                customAiReportCounted = true;
            }
            InterviewEvaluationReport report = interviewAiService.generateEvaluationReport(
                    sessionId,
                    history,
                    session.getJobRole(),
                    session.getJobRoleCode(),
                    session.getDifficulty(),
                    session.getInterviewMode(),
                    jobTargetContext,
                    session.getUserId(),
                    fallbackToPlatformForReport
            );
            score = report.getOverallScore();
            mockInterviewJobTargetService.updateFeedback(
                    sessionId,
                    mockInterviewJobTargetService.buildFeedback(report, jobTargetContext)
            );
            evaluationReportJson = writeEvaluationReport(report);
            log.info("异步 AI 评估报告生成成功, sessionId: {}, score: {}, aiBillingSource: {}, fallbackToPlatform: {}",
                    sessionId, score, session.getAiBillingSource(), fallbackToPlatformForReport);
        } catch (Exception e) {
            if (customAiReportCounted && userAiUsageLimitService != null) {
                userAiUsageLimitService.rollback(session.getUserId(), UserAiConstants.USAGE_TYPE_INTERVIEW_REPORT);
            }
            log.warn("异步 AI 评估失败，降级使用 Mock 报告, sessionId: {}, error: {}", sessionId, e.getMessage());
            InterviewEvaluationReport fallbackReport = buildFallbackEvaluationReport(session, jobTargetContext, e.getMessage());
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
        transactionTemplate.executeWithoutResult(status -> updatedRows[0] = interviewSessionMapper.updateEvaluationReportIfAbsent(
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

        // 写入维度评分到独立表，供成长中心雷达图使用
        persistDimensionScores(session, finalReportJson);
    }

    /**
     * 从评估报告 JSON 中提取 6 维度评分并批量写入 interview_dimension_score 表。
     * 使用 transactionTemplate 保证事务，失败时回滚并记录日志，不影响报告主流程。
     */
    private void persistDimensionScores(InterviewSession session, String reportJson) {
        try {
            InterviewEvaluationReport report = objectMapper.readValue(reportJson, InterviewEvaluationReport.class);
            Map<String, InterviewEvaluationReport.DimensionScore> dims = Map.of(
                    "technicalDepth", Objects.requireNonNullElse(report.getTechnicalDepth(), new InterviewEvaluationReport.DimensionScore()),
                    "projectExpression", Objects.requireNonNullElse(report.getProjectExpression(), new InterviewEvaluationReport.DimensionScore()),
                    "communication", Objects.requireNonNullElse(report.getCommunication(), new InterviewEvaluationReport.DimensionScore()),
                    "problemSolving", Objects.requireNonNullElse(report.getProblemSolving(), new InterviewEvaluationReport.DimensionScore()),
                    "pressureResistance", Objects.requireNonNullElse(report.getPressureResistance(), new InterviewEvaluationReport.DimensionScore()),
                    "jobMatch", Objects.requireNonNullElse(report.getJobMatch(), new InterviewEvaluationReport.DimensionScore())
            );

            List<InterviewDimensionScore> entities = new ArrayList<>();
            for (Map.Entry<String, InterviewEvaluationReport.DimensionScore> entry : dims.entrySet()) {
                InterviewEvaluationReport.DimensionScore ds = entry.getValue();
                if (ds.getScore() == null) continue;
                InterviewDimensionScore entity = new InterviewDimensionScore();
                entity.setUserId(session.getUserId());
                entity.setSessionId(session.getSessionId());
                entity.setDimensionKey(entry.getKey());
                entity.setScore(ds.getScore());
                entity.setComment(ds.getComment());
                entity.setStrengths(ds.getStrengths() != null ? objectMapper.writeValueAsString(ds.getStrengths()) : null);
                entity.setWeaknesses(ds.getWeaknesses() != null ? objectMapper.writeValueAsString(ds.getWeaknesses()) : null);
                entities.add(entity);
            }

            if (!entities.isEmpty()) {
                try {
                    transactionTemplate.executeWithoutResult(status -> dimensionScoreService.saveBatch(entities));
                } catch (DuplicateKeyException ignored) {
                    // CAS 保护下重复写入概率极低，UNIQUE INDEX 兜底
                } catch (Exception e) {
                    log.warn("[面试维度评分] 批量写入失败，sessionId={}: {}", session.getSessionId(), e.getMessage());
                }
            }

            evictGrowthCaches(session.getUserId());
            log.info("[面试维度评分] 写入完成, sessionId={}, userId={}", session.getSessionId(), session.getUserId());
        } catch (Exception e) {
            log.warn("[面试维度评分] 解析报告失败，sessionId={}: {}", session.getSessionId(), e.getMessage());
        }
    }

    /**
     * 清除成长中心相关缓存，确保报告生成、历史删除后前端读取最新聚合数据。
     */
    private void evictGrowthCaches(Long userId) {
        try {
            evictCacheValue("user:interviewRadar", userId);
            evictCacheValue("user:growthOverview", userId);
        } catch (Exception e) {
            log.warn("[面试维度评分] 清除成长缓存失败, userId={}: {}", userId, e.getMessage());
        }
    }

    /**
     * 按 Spring Cache 的缓存名和业务 key 驱逐缓存，避免依赖 Redis 序列化后的物理 key 格式。
     */
    private void evictCacheValue(String cacheName, Long userId) {
        if (cacheManager == null) {
            return;
        }
        Cache cache = cacheManager.getCache(cacheName);
        if (cache != null) {
            cache.evict(userId);
        }
    }

    /**
     * 历史记录分页查询。
     */
    @Transactional(readOnly = true)
    public PageResult<InterviewHistoryResponse> getHistory(Long userId, Integer pageNum, Integer pageSize) {
        com.baomidou.mybatisplus.extension.plugins.pagination.Page<InterviewSession> page =
                new com.baomidou.mybatisplus.extension.plugins.pagination.Page<>(pageNum, pageSize);
        QueryWrapper<InterviewSession> wrapper = new QueryWrapper<>();
        // 历史列表不需要 evaluation_report 大字段，保持轻查询。
        wrapper.select("id", "session_id", "user_id", "job_role", "job_role_code", "difficulty",
                        "interview_mode", "status", "comprehensive_score", "opening_generated",
                        "feedback_mode", "interaction_type", "create_time", "update_time")
                .eq("user_id", userId)
                .eq("is_deleted", 0)
                .orderByDesc("create_time");
        com.baomidou.mybatisplus.extension.plugins.pagination.Page<InterviewSession> resultPage =
                interviewSessionMapper.selectPage(page, wrapper);
        List<InterviewHistoryResponse> list = buildHistoryResponses(userId, resultPage.getRecords());
        return PageResult.of(list, resultPage.getTotal(), pageNum, pageSize);
    }

    /**
     * 兼容旧版全量历史接口。
     */
    @Deprecated
    @Transactional(readOnly = true)
    public List<InterviewHistoryResponse> getAllHistory(Long userId) {
        List<InterviewSession> sessions = interviewSessionMapper.selectList(new QueryWrapper<InterviewSession>()
                .select("id", "session_id", "user_id", "job_role", "job_role_code", "difficulty",
                        "interview_mode", "status", "comprehensive_score", "opening_generated",
                        "feedback_mode", "interaction_type", "create_time", "update_time")
                .eq("user_id", userId)
                .eq("is_deleted", 0)
                .orderByDesc("create_time"));
        return buildHistoryResponses(userId, sessions);
    }

    /**
     * 清理当前用户的全部面试历史。
     * 主记录按会话数计数，聊天记录和岗位定向上下文同步逻辑删除，避免历史页和详情页继续读到旧数据。
     */
    @Transactional(rollbackFor = Exception.class)
    public int clearHistory(Long userId) {
        List<String> sessionIds = interviewSessionMapper.selectActiveSessionIdsByUserId(userId);
        if (sessionIds == null || sessionIds.isEmpty()) {
            return 0;
        }

        LocalDateTime now = LocalDateTime.now();
        interviewChatLogMapper.logicalDeleteBySessionIdIn(sessionIds, now);
        mockInterviewJobTargetService.logicalDeleteByUserId(userId);
        dimensionScoreMapper.logicalDeleteBySessionIds(sessionIds, now);
        int deletedCount = interviewSessionMapper.logicalDeleteByUserId(userId, now);
        evictGrowthCaches(userId);
        return deletedCount;
    }

    /**
     * 删除单条面试会话及其关联数据（聊天记录、岗位定向上下文）。
     *
     * @param userId    当前登录用户 ID
     * @param sessionId 会话 ID
     * @return 是否删除成功
     */
    @Transactional(rollbackFor = Exception.class)
    public boolean deleteSession(Long userId, String sessionId) {
        InterviewSession session = getSessionByOwnerOrThrow(sessionId, userId);
        LocalDateTime now = LocalDateTime.now();
        interviewChatLogMapper.logicalDeleteBySessionIdIn(List.of(sessionId), now);
        mockInterviewJobTargetService.logicalDeleteBySessionIds(List.of(sessionId));
        dimensionScoreMapper.logicalDeleteBySessionIds(List.of(sessionId), now);
        interviewSessionMapper.logicalDeleteBySessionIdIn(List.of(sessionId), now);
        evictGrowthCaches(userId);
        return true;
    }

    /**
     * 流式发送前统一拉取历史并校验状态。
     */
    @Transactional(readOnly = true)
    public List<InterviewChatLog> getChatLogsForStream(String sessionId, Long userId) {
        InterviewSession session = getSessionByOwnerOrThrow(sessionId, userId);
        assertSessionInProgress(session);
        return interviewMessageService.getMessageList(sessionId);
    }

    /**
     * 流式发送前二次校验会话状态。
     */
    @Transactional(readOnly = true)
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

        // 流式回答断线后可能重复请求同一条消息；若最新一条仍是相同用户消息，则直接跳过，避免重复落库。
        InterviewChatLog latestMessage = selectLatestMessage(sessionId);
        if (latestMessage != null
                && InterviewConstants.ROLE_USER.equalsIgnoreCase(latestMessage.getMessageRole())
                && Objects.equals(latestMessage.getContent(), content)) {
            log.warn("检测到重复的流式用户消息，跳过写入, sessionId: {}, userId: {}", sessionId, userId);
            return;
        }

        InterviewChatLog userMessage = new InterviewChatLog();
        userMessage.setId(IdWorker.getId());
        userMessage.setSessionId(sessionId);
        userMessage.setMessageRole(InterviewConstants.ROLE_USER);
        userMessage.setContent(content);
        userMessage.setCreateTime(LocalDateTime.now());
        userMessage.setUpdateTime(LocalDateTime.now());
        userMessage.setIsDeleted(0);
        interviewChatLogMapper.insert(userMessage);
    }

    // ===== 预加载 session 的重载方法（避免流式路径重复查询 interview_session）=====

    @Transactional(readOnly = true)
    public List<InterviewChatLog> getChatLogsForStream(InterviewSession session) {
        assertSessionInProgress(session);
        return interviewMessageService.getMessageList(session.getSessionId());
    }

    public void validateSessionForStream(InterviewSession session) {
        assertSessionInProgress(session);
    }

    @Transactional(rollbackFor = Exception.class)
    public void saveUserMessage(InterviewSession session, String content) {
        assertSessionInProgress(session);
        if (session.getOpeningGenerated() == null || session.getOpeningGenerated() == 0) {
            throw new BusinessException("开场白正在生成中，请稍候再发送消息");
        }
        String sessionId = session.getSessionId();

        InterviewChatLog latestMessage = selectLatestMessage(sessionId);
        if (latestMessage != null
                && InterviewConstants.ROLE_USER.equalsIgnoreCase(latestMessage.getMessageRole())
                && Objects.equals(latestMessage.getContent(), content)) {
            log.warn("检测到重复的流式用户消息，跳过写入, sessionId: {}", sessionId);
            return;
        }

        InterviewChatLog userMessage = new InterviewChatLog();
        userMessage.setId(IdWorker.getId());
        userMessage.setSessionId(sessionId);
        userMessage.setMessageRole(InterviewConstants.ROLE_USER);
        userMessage.setContent(content);
        userMessage.setCreateTime(LocalDateTime.now());
        userMessage.setUpdateTime(LocalDateTime.now());
        userMessage.setIsDeleted(0);
        interviewChatLogMapper.insert(userMessage);
    }

    /**
     * 订阅并写出 SSE 数据。
     * 注意：调用方需先通过 {@link #attachStreamLifecycleCallbacks} 把 streamClosed 注册到 emitter 上，
     * 这里只接收已注册好的同一份 streamClosed，避免控制层与服务层重复注册 onTimeout/onCompletion/onError 导致回调互相覆盖。
     */
    public void subscribeAndWriteStream(
            String sessionId,
            ResponseBodyEmitter emitter,
            Publisher<String> publisher,
            StringBuilder fullReply,
            AtomicBoolean streamClosed,
            AtomicBoolean done,
            AtomicReference<Subscription> subscriptionRef
    ) throws IOException {
        subscribeAndWriteStream(sessionId, emitter, publisher, fullReply, streamClosed, done, subscriptionRef, null);
    }

    /**
     * 订阅并写出 SSE 数据，允许调用方在上游 AI 异步失败时执行计数回滚。
     */
    public void subscribeAndWriteStream(
            String sessionId,
            ResponseBodyEmitter emitter,
            Publisher<String> publisher,
            StringBuilder fullReply,
            AtomicBoolean streamClosed,
            AtomicBoolean done,
            AtomicReference<Subscription> subscriptionRef,
            Runnable upstreamErrorCallback
    ) throws IOException {
        publisher.subscribe(new Subscriber<>() {
            @Override
            public void onSubscribe(Subscription s) {
                subscriptionRef.set(s);
                if (streamClosed.get()) {
                    s.cancel();
                    subscriptionRef.compareAndSet(s, null);
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
                    log.warn("SSE 推送失败，取消订阅, sessionId: {}, error: {}", sessionId, e.getMessage());
                    cancelActiveStream(sessionId, streamClosed, done, subscriptionRef, "send_failed");
                }
            }

            @Override
            public void onError(Throwable t) {
                if (streamClosed.get()) {
                    return;
                }
                if (done.compareAndSet(false, true)) {
                    if (upstreamErrorCallback != null) {
                        upstreamErrorCallback.run();
                    }
                    // 安全考虑：不把上游异常详情透传给前端，只记录服务端日志。
                    log.warn("SSE 上游异常, sessionId: {}", sessionId, t);
                    try {
                        String jsonData = buildStreamErrorPayload(t);
                        emitter.send("event: message\ndata: " + jsonData + "\n\n");
                    } catch (Exception ex) {
                        log.warn("SSE 错误事件发送失败, sessionId: {}", sessionId, ex);
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
                            String jsonData = "{\"type\":\"error\",\"message\":\"AI 回复为空，请稍后重试\"}";
                            emitter.send("event: message\ndata: " + jsonData + "\n\n");
                            emitter.complete();
                            return;
                        }

                        persistAssistantMessage(sessionId, fullReply.toString());
                        emitter.send("event: message\ndata: {\"type\":\"done\"}\n\n");
                        emitter.complete();
                    } catch (Exception e) {
                        log.error("SSE 完成后落库 assistant 消息失败, sessionId: {}", sessionId, e);
                        try {
                            String jsonData = "{\"type\":\"error\",\"message\":\"消息保存失败，请刷新会话历史\"}";
                            emitter.send("event: message\ndata: " + jsonData + "\n\n");
                        } catch (Exception ex) {
                            log.warn("SSE 错误事件发送失败, sessionId: {}", sessionId, ex);
                        }
                        emitter.completeWithError(e);
                    }
                }
            }
        });
    }

    private String buildStreamErrorPayload(Throwable throwable) {
        if (throwable instanceof BusinessException businessException
                && isCustomAiErrorCode(businessException.getCode())) {
            return "{\"type\":\"error\",\"code\":" + businessException.getCode()
                    + ",\"message\":\"" + escapeJsonForSse(businessException.getMessage()) + "\"}";
        }
        return "{\"type\":\"error\",\"message\":\"AI 服务暂时不可用，请稍后重试\"}";
    }

    private boolean isCustomAiErrorCode(Integer code) {
        return ResultCode.CUSTOM_AI_CALL_FAILED.getCode().equals(code)
                || ResultCode.CUSTOM_AI_DAILY_LIMIT_EXCEEDED.getCode().equals(code)
                || ResultCode.CUSTOM_AI_CONFIG_INVALID.getCode().equals(code);
    }

    /**
     * 统一注册 SSE 生命周期回调。
     * ResponseBodyEmitter 每类回调只保留最后一次注册，因此必须由本方法独家持有 onTimeout/onCompletion/onError，
     * 防止 controller 与 service 各自重复注册导致回调互相覆盖、streamClosed 状态分裂。
     */
    public void attachStreamLifecycleCallbacks(String sessionId,
                                               ResponseBodyEmitter emitter,
                                               AtomicBoolean streamClosed,
                                               AtomicBoolean done,
                                               AtomicReference<Subscription> subscriptionRef) {
        emitter.onTimeout(() -> {
            if (cancelActiveStream(sessionId, streamClosed, done, subscriptionRef, "timeout")) {
                log.info("SSE 连接超时，停止后续处理, sessionId: {}", sessionId);
                try {
                    emitter.complete();
                } catch (Exception e) {
                    log.warn("SSE 超时关闭失败, sessionId: {}", sessionId, e);
                }
            }
        });
        emitter.onCompletion(() -> cancelActiveStream(sessionId, streamClosed, done, subscriptionRef, "completion"));
        emitter.onError(error -> {
            if (cancelActiveStream(sessionId, streamClosed, done, subscriptionRef, "error")) {
                log.warn("SSE 连接异常关闭, sessionId: {}", sessionId, error);
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
            Subscription subscription = subscriptionRef.getAndSet(null);
            if (subscription != null) {
                subscription.cancel();
            }
            return false;
        }
        boolean firstClose = streamClosed.compareAndSet(false, true);
        if (!firstClose && subscriptionRef.get() == null) {
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
            interviewChatLogMapper.insert(assistantMessage);
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
        if (request.getInteractionType() != null
                && !InterviewConstants.isSupportedInteractionType(request.getInteractionType())) {
            throw new BusinessException("交互方式只能是文字或语音");
        }
    }

    /**
     * 规范化交互方式。
     * 空值或非语音值都按文字面试处理；非 0/1 已在创建入口拦截，历史脏数据也统一按文字面试回显。
     */
    public Integer resolveInteractionType(Integer interactionType) {
        if (interactionType != null && interactionType == InterviewConstants.INTERACTION_TYPE_VOICE) {
            return InterviewConstants.INTERACTION_TYPE_VOICE;
        }
        return InterviewConstants.INTERACTION_TYPE_TEXT;
    }

    /**
     * 规范化面试模式。
     */
    private String resolveInterviewMode(String interviewMode, InterviewJobTargetContext jobTargetContext) {
        String lowerMode = interviewMode == null ? "" : interviewMode.toLowerCase().trim();
        // 岗位定向可以叠加固定人设；历史未传人设时才回落到 job_targeted 展示语义。
        if ((lowerMode.isBlank() || InterviewConstants.MODE_NORMAL.equals(lowerMode))
                && jobTargetContext != null && Boolean.TRUE.equals(jobTargetContext.getJobTargeted())) {
            return InterviewConstants.MODE_JOB_TARGETED;
        }
        if (lowerMode.isBlank()) {
            return InterviewConstants.MODE_NORMAL;
        }
        return InterviewConstants.isSupportedInterviewMode(lowerMode)
                ? lowerMode
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
        if (InterviewConstants.MODE_BIG_COMPANY_HR.equals(interviewMode)) {
            return "大厂 HR 面";
        }
        if (InterviewConstants.MODE_TECH_LEADER.equals(interviewMode)) {
            return "技术 Leader 面";
        }
        if (InterviewConstants.MODE_FOREIGN_INTERVIEWER.equals(interviewMode)) {
            return "外企面试官";
        }
        return "普通面试";
    }

    /**
     * 统一生成对前端返回的面试模式。
     * 兼容历史数据中“岗位定向=true，但面试模式仍为 normal”的旧记录。
     */
    private String resolveResponseInterviewMode(String storedInterviewMode, InterviewJobTargetContext jobTargetContext) {
        String normalizedMode = storedInterviewMode == null ? "" : storedInterviewMode.toLowerCase().trim();
        if ((normalizedMode.isBlank() || InterviewConstants.MODE_NORMAL.equals(normalizedMode))
                && jobTargetContext != null && Boolean.TRUE.equals(jobTargetContext.getJobTargeted())) {
            return InterviewConstants.MODE_JOB_TARGETED;
        }
        if (normalizedMode.isBlank()) {
            return InterviewConstants.MODE_NORMAL;
        }
        return InterviewConstants.isSupportedInterviewMode(normalizedMode)
                ? normalizedMode
                : InterviewConstants.MODE_NORMAL;
    }

    /**
     * 根据固定人设补充开场语气提示。
     * 开场白仍走硬编码模板，不额外调用 AI，避免创建会话时增加 token 消耗。
     */
    private String buildOpeningPersonaHint(String interviewMode) {
        if (InterviewConstants.MODE_STRESS.equals(interviewMode)) {
            return "需要提前说明，这是一场压力面试，我会对你的回答进行深入追问和质疑，请做好准备。";
        }
        if (InterviewConstants.MODE_BIG_COMPANY_HR.equals(interviewMode)) {
            return "接下来我会更关注你的行为经历、团队协作和文化匹配。";
        }
        if (InterviewConstants.MODE_TECH_LEADER.equals(interviewMode)) {
            return "接下来我会重点追问技术细节、项目取舍和你的个人贡献。";
        }
        if (InterviewConstants.MODE_FOREIGN_INTERVIEWER.equals(interviewMode)) {
            return "This interview will be conducted mainly in English, and I will pay attention to your communication logic.";
        }
        return "";
    }

    /**
     * 规范化即时反馈开关。
     * 该值只影响本次 AI 回复，不写入会话表，避免扩大存储范围。
     */
    public String resolveFeedbackMode(String feedbackMode) {
        return resolveFeedbackMode(feedbackMode, null);
    }

    /**
     * 解析反馈模式。优先使用请求参数，其次读会话记录值，最后兜底默认值。
     */
    public String resolveFeedbackMode(String feedbackMode, InterviewSession session) {
        if (InterviewConstants.FEEDBACK_MODE_IMMEDIATE.equalsIgnoreCase(feedbackMode)) {
            return InterviewConstants.FEEDBACK_MODE_IMMEDIATE;
        }
        if (session != null && InterviewConstants.FEEDBACK_MODE_IMMEDIATE.equalsIgnoreCase(session.getFeedbackMode())) {
            return InterviewConstants.FEEDBACK_MODE_IMMEDIATE;
        }
        return InterviewConstants.FEEDBACK_MODE_AFTER_INTERVIEW;
    }

    /**
     * 为问答阶段兜底补齐普通模拟面试的简历上下文。
     * 说明：岗位定向会优先返回会话上下文；普通模拟面试则回退到最近一次简历诊断。
     */
    private InterviewJobTargetContext resolveConversationContext(Long userId, String sessionId) {
        InterviewJobTargetContext sessionContext = mockInterviewJobTargetService.getSessionContext(userId, sessionId);
        if (sessionContext != null
                && (Boolean.TRUE.equals(sessionContext.getJobTargeted())
                || (sessionContext.getResumeText() != null && !sessionContext.getResumeText().isBlank()))) {
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
                .feedbackMode(resolveFeedbackMode(null, session))
                .interactionType(resolveInteractionType(session.getInteractionType()))
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
                .feedbackMode(resolveFeedbackMode(null, session))
                .interactionType(resolveInteractionType(session.getInteractionType()))
                .chatLogs(dtoLogs)
                .replayRounds(buildReplayRounds(chatLogs))
                .openingPending(session.getOpeningGenerated() == null || session.getOpeningGenerated() == 0)
                .createTime(session.getCreateTime())
                .updateTime(session.getUpdateTime())
                .build();
    }

    /**
     * 从原始聊天记录派生“问题-回答-反馈”回放轮次。
     * 同一条 assistant 消息既是上一轮反馈，也可能是下一轮追问，因此不新增存储，只在返回时组织视图数据。
     */
    private List<InterviewReplayRoundResponse> buildReplayRounds(List<InterviewChatLog> chatLogs) {
        if (chatLogs == null || chatLogs.isEmpty()) {
            return List.of();
        }

        List<InterviewReplayRoundResponse> rounds = new java.util.ArrayList<>();
        InterviewChatLog currentQuestion = null;
        InterviewReplayRoundResponse pendingRound = null;

        for (InterviewChatLog log : chatLogs) {
            if (log == null || log.getMessageRole() == null || log.getContent() == null || log.getContent().isBlank()) {
                continue;
            }
            if (InterviewConstants.ROLE_ASSISTANT.equalsIgnoreCase(log.getMessageRole())) {
                if (pendingRound != null && pendingRound.getFeedbackContent() == null) {
                    pendingRound.setFeedbackMessageId(log.getId());
                    pendingRound.setFeedbackContent(log.getContent());
                    pendingRound.setFeedbackTime(log.getCreateTime());
                }
                currentQuestion = log;
                continue;
            }
            if (!InterviewConstants.ROLE_USER.equalsIgnoreCase(log.getMessageRole()) || currentQuestion == null) {
                continue;
            }

            InterviewReplayRoundResponse round = InterviewReplayRoundResponse.builder()
                    .roundNo(rounds.size() + 1)
                    .questionMessageId(currentQuestion.getId())
                    .questionContent(currentQuestion.getContent())
                    .answerMessageId(log.getId())
                    .answerContent(log.getContent())
                    .answerTime(log.getCreateTime())
                    .build();
            rounds.add(round);
            pendingRound = round;
            currentQuestion = null;
        }

        return rounds;
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
                .feedbackMode(resolveFeedbackMode(null, session))
                .interactionType(resolveInteractionType(session.getInteractionType()))
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
            InterviewJobTargetContext jobTargetContext,
            String failureReason
    ) {
        int score = mockInterviewService.generateMockScore(session.getSessionId());
        boolean jobTargeted = jobTargetContext != null && Boolean.TRUE.equals(jobTargetContext.getJobTargeted());
        String summary = jobTargeted
                ? "本次岗位定向模拟面试已结合目标岗位要求给出基础评估，建议继续围绕岗位关键能力补齐案例表达。"
                : "本次模拟面试已生成基础评估，建议继续补强案例细节与表达完整性。";
        String safeFailureReason = failureReason == null || failureReason.isBlank() ? "AI 报告生成失败" : failureReason;

        return InterviewEvaluationReport.builder()
                .overallScore(score)
                .level(resolveLevel(score))
                .summary(summary)
                .finalVerdict("AI 深度报告生成失败，当前为基础评估：" + safeFailureReason)
                .strengths(List.of("回答具备基本结构", "能够围绕问题给出业务表达"))
                .weaknesses(jobTargeted
                        ? List.of("岗位要求与案例关联仍可加强")
                        : List.of("案例细节不够充分"))
                .followUpLossPoints(List.of("追问到项目细节时，回答需要补充更多事实证据"))
                .commonLossPatterns(List.of("回答结构具备基础框架，但项目证据和结果量化不足"))
                .immediateActions(List.of(
                        "明天选 1 个核心项目，按 STAR 结构重写 2 分钟回答",
                        "整理 3 个可能被追问的技术细节，每个补充边界和取舍理由",
                        "回看本次记录，标出 1 个最空泛回答并补充量化结果"
                ))
                .improvementSuggestions(jobTargeted
                        ? List.of("补充与目标岗位最相关的项目证据", "强化对 JD 关键能力项的量化表达")
                        : List.of("补充更多项目细节", "提升回答的结构化程度"))
                .suggestions(jobTargeted
                        ? List.of("补充与目标岗位最相关的项目证据", "强化对 JD 关键能力项的量化表达")
                        : List.of("补充更多项目细节", "提升回答的结构化程度"))
                .improvements(jobTargeted
                        ? List.of("加强岗位匹配表达")
                        : List.of("加强案例细节表达"))
                .technicalDepth(InterviewEvaluationReport.DimensionScore.builder()
                        .score(score)
                        .comment("兜底报告仅提供基础技术深度判断，建议结合原始问答继续复盘。")
                        .build())
                .projectExpression(InterviewEvaluationReport.DimensionScore.builder()
                        .score(score)
                        .comment("项目表达需要进一步补充场景、个人动作和结果证据。")
                        .weaknesses(List.of("案例细节和结果量化不足"))
                        .build())
                .communication(InterviewEvaluationReport.DimensionScore.builder()
                        .score(score)
                        .comment("表达具备基础结构，但需要减少泛化描述。")
                        .build())
                .problemSolving(InterviewEvaluationReport.DimensionScore.builder()
                        .score(score)
                        .comment("问题拆解有基础框架，仍需补充取舍依据。")
                        .build())
                .pressureResistance(InterviewEvaluationReport.DimensionScore.builder()
                        .score(score)
                        .comment("兜底报告暂不做复杂抗压细分。")
                        .build())
                .jobMatch(InterviewEvaluationReport.DimensionScore.builder()
                        .score(jobTargeted ? score : null)
                        .comment(jobTargeted
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
    public InterviewSession getSessionByOwnerOrThrow(String sessionId, Long userId) {
        InterviewSession session = selectSessionBySessionIdAndUserId(sessionId, userId, true);
        if (session == null) {
            throw new BusinessException("会话不存在或无权访问");
        }
        return session;
    }

    /**
     * 获取用户归属会话，兼容旧 Controller 的空值判断。
     */
    public InterviewSession getSessionByOwner(String sessionId, Long userId) {
        return selectSessionBySessionIdAndUserId(sessionId, userId, true);
    }

    /**
     * 社区分享后的报告允许其他登录用户查看，但只开放报告必要字段。
     */
    private InterviewSession getSharedReportSessionOrThrow(String sessionId) {
        InterviewSession session = selectSessionBySessionId(sessionId, true);
        if (session == null || !hasCommunitySharedReportPost(sessionId, session.getUserId())) {
            throw new BusinessException("会话不存在或无权访问");
        }
        return session;
    }

    /**
     * 以社区未删除帖子作为报告公开访问授权来源，避免所有面试报告被直接枚举访问。
     */
    private boolean hasCommunitySharedReportPost(String sessionId, Long ownerId) {
        if (sessionId == null || sessionId.isBlank()) {
            return false;
        }
        return communityPostMapper.selectCount(new QueryWrapper<com.airesume.server.entity.CommunityPost>()
                .eq("shared_interview_session_id", sessionId)
                .eq("user_id", ownerId)
                .eq("is_deleted", 0)) > 0;
    }

    /**
     * 跨用户查看社区分享报告时不返回聊天记录和岗位上下文，只返回报告页展示所需字段。
     */
    private InterviewSessionResponse convertToSharedReportResponse(InterviewSession session) {
        return InterviewSessionResponse.builder()
                .id(session.getId())
                .sessionId(session.getSessionId())
                .jobRole(session.getJobRole())
                .jobRoleCode(session.getJobRoleCode())
                .difficulty(session.getDifficulty())
                .difficultyDesc(convertDifficultyToDesc(session.getDifficulty()))
                .interviewMode(resolveResponseInterviewMode(session.getInterviewMode(), null))
                .interviewModeDesc(getInterviewModeDescription(resolveResponseInterviewMode(session.getInterviewMode(), null)))
                .status(session.getStatus())
                .statusDesc(isSessionInProgress(session) ? "进行中" : "已结束")
                .comprehensiveScore(session.getComprehensiveScore())
                .evaluationReport(session.getEvaluationReport())
                .jobTargeted(false)
                .feedbackMode(resolveFeedbackMode(null, session))
                .interactionType(resolveInteractionType(session.getInteractionType()))
                .chatLogs(List.of())
                .replayRounds(List.of())
                .openingPending(session.getOpeningGenerated() == null || session.getOpeningGenerated() == 0)
                .createTime(session.getCreateTime())
                .updateTime(session.getUpdateTime())
                .build();
    }

    /**
     * 按 sessionId 查询会话；详情和报告生成需要 evaluation_report 时由调用方显式开启。
     */
    private InterviewSession selectSessionBySessionId(String sessionId, boolean includeEvaluationReport) {
        QueryWrapper<InterviewSession> wrapper = baseSessionQuery(includeEvaluationReport)
                .eq("session_id", sessionId)
                .eq("is_deleted", 0)
                .last("limit 1");
        return interviewSessionMapper.selectOne(wrapper);
    }

    /**
     * 查询当前用户归属的会话，显式控制是否补回评估报告大字段。
     */
    private InterviewSession selectSessionBySessionIdAndUserId(String sessionId, Long userId, boolean includeEvaluationReport) {
        QueryWrapper<InterviewSession> wrapper = baseSessionQuery(includeEvaluationReport)
                .eq("session_id", sessionId)
                .eq("user_id", userId)
                .eq("is_deleted", 0)
                .last("limit 1");
        return interviewSessionMapper.selectOne(wrapper);
    }

    private QueryWrapper<InterviewSession> baseSessionQuery(boolean includeEvaluationReport) {
        QueryWrapper<InterviewSession> wrapper = new QueryWrapper<>();
        String columns = "id,session_id,user_id,job_role,job_role_code,difficulty,interview_mode,status,"
                + "comprehensive_score,opening_generated,feedback_mode,interaction_type,ai_billing_source,create_time,update_time,is_deleted";
        if (includeEvaluationReport) {
            columns += ",evaluation_report";
        }
        return wrapper.select(columns);
    }

    /**
     * 查询最近一条未删除聊天消息，用于流式断线重试去重。
     */
    private InterviewChatLog selectLatestMessage(String sessionId) {
        return interviewChatLogMapper.selectOne(new QueryWrapper<InterviewChatLog>()
                .eq("session_id", sessionId)
                .eq("is_deleted", 0)
                .orderByDesc("create_time")
                .last("limit 1"));
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
    public void assertSessionInProgress(InterviewSession session) {
        if (!isSessionInProgress(session)) {
            throw new BusinessException("会话已结束，无法继续发送消息");
        }
    }

    /**
     * 面试业务统一判断是否使用用户自定义 AI；显式 fallback 时强制走平台 AI。
     */
    private boolean shouldUseCustomAi(Long userId, boolean fallbackToPlatform) {
        return userAiConfigResolver != null
                && userAiConfigResolver.resolve(userId, AiEngineConstants.BUSINESS_TYPE_INTERVIEW, fallbackToPlatform) != null;
    }

    /**
     * 自定义 AI 面试会话手动切平台时，按整场面试只扣一次平台额度。
     * 原子标记成功才扣费，防止同一个 session 重复点击或并发请求重复消耗用户额度。
     */
    @Transactional(rollbackFor = Exception.class)
    public void chargePlatformFallbackQuotaIfNeeded(InterviewSession session, boolean fallbackToPlatform) {
        if (!fallbackToPlatform || session == null) {
            return;
        }
        if (!UserAiConstants.BILLING_SOURCE_USER_CUSTOM.equals(session.getAiBillingSource())) {
            return;
        }

        int marked = interviewSessionMapper.markPlatformFallbackBillingIfCustom(
                session.getSessionId(),
                session.getUserId(),
                UserAiConstants.BILLING_SOURCE_PLATFORM_FALLBACK,
                LocalDateTime.now());
        if (marked > 0) {
            userQuotaService.deductInterviewQuota(session.getUserId());
            session.setAiBillingSource(UserAiConstants.BILLING_SOURCE_PLATFORM_FALLBACK);
        }
    }
}
