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
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicBoolean;
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

    /**
     * 创建面试会话。
     * 先解析岗位定向上下文，再统一复用同一条会话创建链路。
     */
    @Transactional(rollbackFor = Exception.class)
    public InterviewSessionResponse createSession(Long userId, CreateSessionRequest request) {
        validateCreateRequest(request);
        if (!userQuotaService.checkInterviewQuota(userId)) {
            throw new BusinessException("模拟面试次数已用完");
        }

        InterviewJobTargetContext jobTargetContext = mockInterviewJobTargetService.resolveContext(userId, request);
        String interviewMode = resolveInterviewMode(request.getInterviewMode());

        InterviewSession session = new InterviewSession();
        session.setId(IdWorker.getId());
        session.setUserId(userId);
        session.setSessionId(UUID.randomUUID().toString().replace("-", ""));
        session.setJobRole(request.getJobRole());
        session.setJobRoleCode(request.getJobRoleCode());
        session.setDifficulty(request.getDifficulty());
        session.setInterviewMode(interviewMode);
        session.setStatus(InterviewConstants.STATUS_IN_PROGRESS);
        session.setCreateTime(LocalDateTime.now());
        session.setUpdateTime(LocalDateTime.now());
        interviewSessionRepository.saveAndFlush(session);

        // 仅在会话成功创建后扣减次数，避免无效扣费。
        userQuotaService.deductInterviewQuota(userId);

        String openingMessage = interviewAiService.generateOpening(
                request.getJobRole(),
                request.getJobRoleCode(),
                request.getDifficulty(),
                jobTargetContext
        );

        InterviewChatLog welcomeMessage = new InterviewChatLog();
        welcomeMessage.setId(IdWorker.getId());
        welcomeMessage.setSessionId(session.getSessionId());
        welcomeMessage.setMessageRole(InterviewConstants.ROLE_ASSISTANT);
        welcomeMessage.setContent(openingMessage);
        welcomeMessage.setCreateTime(LocalDateTime.now());
        welcomeMessage.setUpdateTime(LocalDateTime.now());
        welcomeMessage.setIsDeleted(0);
        interviewMessageRepository.save(welcomeMessage);

        // 仅在真正启用岗位定向时记录独立上下文，不影响普通面试表结构。
        mockInterviewJobTargetService.saveSessionContext(userId, session.getSessionId(), jobTargetContext, openingMessage);

        log.info("面试会话创建成功, sessionId: {}, userId: {}", session.getSessionId(), userId);
        return convertToSessionResponse(session, jobTargetContext);
    }

    /**
     * 非流式发送消息。
     */
    @Transactional(rollbackFor = Exception.class)
    public SendMessageResponse sendMessage(Long userId, String sessionId, SendMessageRequest request) {
        InterviewSession session = getSessionByOwnerOrThrow(sessionId, userId);
        assertSessionInProgress(session);

        List<InterviewChatLog> chatLogs = interviewMessageService.getMessageList(sessionId);
        List<InterviewAiService.ChatMessageItem> history = chatLogs.stream()
                .map(log -> new InterviewAiService.ChatMessageItem(log.getMessageRole(), log.getContent()))
                .toList();
        InterviewJobTargetContext jobTargetContext =
                mockInterviewJobTargetService.getSessionContext(userId, sessionId);

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
        CompletableFuture.runAsync(() -> generateAndPersistEvaluationReport(sessionId));
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
        transactionTemplate.executeWithoutResult(status -> interviewSessionRepository.updateEvaluationReport(
                sessionId,
                finalScore,
                finalReportJson,
                InterviewConstants.STATUS_ENDED,
                LocalDateTime.now()
        ));
    }

    /**
     * 历史记录分页查询。
     */
    public PageResult<InterviewHistoryResponse> getHistory(Long userId, Integer pageNum, Integer pageSize) {
        Pageable pageable = PageRequest.of(pageNum - 1, pageSize, Sort.by(Sort.Direction.DESC, "createTime"));
        Page<InterviewSession> page = interviewSessionRepository.findByUserId(userId, pageable);
        List<InterviewHistoryResponse> list = page.getContent().stream()
                .map(this::convertToHistoryResponse)
                .collect(Collectors.toList());
        return PageResult.of(list, page.getTotalElements(), pageNum, pageSize);
    }

    /**
     * 兼容旧版全量历史接口。
     */
    @Deprecated
    public List<InterviewHistoryResponse> getAllHistory(Long userId) {
        List<InterviewSession> sessions = interviewSessionRepository.findByUserIdOrderByCreateTimeDesc(userId);
        return sessions.stream()
                .map(this::convertToHistoryResponse)
                .collect(Collectors.toList());
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
        Subscription[] subscriptionRef = new Subscription[1];
        AtomicBoolean done = new AtomicBoolean(false);

        publisher.subscribe(new Subscriber<>() {
            @Override
            public void onSubscribe(Subscription s) {
                subscriptionRef[0] = s;
                s.request(Long.MAX_VALUE);
            }

            @Override
            public void onNext(String item) {
                if (item == null || item.isBlank()) {
                    return;
                }
                fullReply.append(item);
                try {
                    String escaped = escapeJsonForSse(item);
                    String jsonData = "{\"type\":\"content\",\"content\":\"" + escaped + "\"}";
                    emitter.send("event: message\ndata: " + jsonData + "\n\n");
                } catch (IOException e) {
                    log.warn("SSE 发送失败，取消订阅, sessionId: {}, error: {}", sessionId, e.getMessage());
                    if (subscriptionRef[0] != null) {
                        subscriptionRef[0].cancel();
                    }
                }
            }

            @Override
            public void onError(Throwable t) {
                if (done.compareAndSet(false, true)) {
                    try {
                        String errorMsg = escapeJsonForSse(t.getMessage() == null ? "系统异常" : t.getMessage());
                        String jsonData = "{\"type\":\"error\",\"message\":\"" + errorMsg + "\"}";
                        emitter.send("event: message\ndata: " + jsonData + "\n\n");
                    } catch (Exception ex) {
                        log.warn("SSE 发送错误事件失败, sessionId: {}", sessionId, ex);
                    } finally {
                        emitter.completeWithError(t);
                    }
                }
            }

            @Override
            public void onComplete() {
                if (done.compareAndSet(false, true)) {
                    try {
                        if (fullReply.isEmpty()) {
                            String jsonData = "{\"type\":\"error\",\"message\":\"AI 回复内容为空，请稍后重试\"}";
                            emitter.send("event: message\ndata: " + jsonData + "\n\n");
                            emitter.complete();
                            return;
                        }

                        emitter.send("event: message\ndata: {\"type\":\"done\"}\n\n");
                        emitter.complete();

                        InterviewChatLog assistantMessage = new InterviewChatLog();
                        assistantMessage.setId(IdWorker.getId());
                        assistantMessage.setSessionId(sessionId);
                        assistantMessage.setMessageRole(InterviewConstants.ROLE_ASSISTANT);
                        assistantMessage.setContent(fullReply.toString());
                        assistantMessage.setCreateTime(LocalDateTime.now());
                        assistantMessage.setUpdateTime(LocalDateTime.now());
                        assistantMessage.setIsDeleted(0);
                        interviewMessageRepository.save(assistantMessage);
                    } catch (Exception e) {
                        log.error("流式结束时保存 assistant 消息失败, sessionId: {}", sessionId, e);
                    }
                }
            }
        });
    }

    /**
     * 校验创建请求。
     */
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
    private String resolveInterviewMode(String interviewMode) {
        if (interviewMode == null || interviewMode.isBlank()) {
            return "normal";
        }
        String lowerMode = interviewMode.toLowerCase().trim();
        return "stress".equals(lowerMode) ? "stress" : "normal";
    }

    /**
     * 获取面试模式文案。
     */
    private String getInterviewModeDescription(String interviewMode) {
        if ("stress".equals(interviewMode)) {
            return "压力面试";
        }
        return "普通面试";
    }

    /**
     * 转换为会话响应，不携带聊天记录。
     */
    private InterviewSessionResponse convertToSessionResponse(
            InterviewSession session,
            InterviewJobTargetContext jobTargetContext
    ) {
        return InterviewSessionResponse.builder()
                .id(session.getId())
                .sessionId(session.getSessionId())
                .userId(session.getUserId())
                .jobRole(session.getJobRole())
                .jobRoleCode(session.getJobRoleCode())
                .difficulty(session.getDifficulty())
                .difficultyDesc(convertDifficultyToDesc(session.getDifficulty()))
                .interviewMode(session.getInterviewMode())
                .interviewModeDesc(getInterviewModeDescription(session.getInterviewMode()))
                .status(session.getStatus())
                .statusDesc(isSessionInProgress(session) ? "进行中" : "已结束")
                .comprehensiveScore(session.getComprehensiveScore())
                .jobTargeted(jobTargetContext != null && Boolean.TRUE.equals(jobTargetContext.getJobTargeted()))
                .jobTargetContext(jobTargetContext)
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
                .interviewMode(session.getInterviewMode())
                .interviewModeDesc(getInterviewModeDescription(session.getInterviewMode()))
                .status(session.getStatus())
                .statusDesc(isSessionInProgress(session) ? "进行中" : "已结束")
                .comprehensiveScore(session.getComprehensiveScore())
                .evaluationReport(session.getEvaluationReport())
                .jobTargeted(jobTargetContext != null && Boolean.TRUE.equals(jobTargetContext.getJobTargeted()))
                .jobTargetContext(jobTargetContext)
                .chatLogs(dtoLogs)
                .createTime(session.getCreateTime())
                .updateTime(session.getUpdateTime())
                .build();
    }

    /**
     * 转换历史记录响应。
     */
    private InterviewHistoryResponse convertToHistoryResponse(InterviewSession session) {
        Integer messageCount = interviewMessageService.getMessageCount(session.getSessionId());
        String interviewMode = session.getInterviewMode();
        if (interviewMode == null || interviewMode.isBlank()) {
            interviewMode = "normal";
        }
        InterviewJobTargetContext jobTargetContext =
                mockInterviewJobTargetService.getSessionContext(session.getUserId(), session.getSessionId());

        return InterviewHistoryResponse.builder()
                .id(session.getId())
                .sessionId(session.getSessionId())
                .jobRole(session.getJobRole())
                .difficulty(session.getDifficulty())
                .difficultyDesc(convertDifficultyToDesc(session.getDifficulty()))
                .interviewMode(interviewMode)
                .interviewModeDesc(getInterviewModeDescription(interviewMode))
                .status(session.getStatus())
                .statusDesc(isSessionInProgress(session) ? "进行中" : "已结束")
                .comprehensiveScore(session.getComprehensiveScore())
                .messageCount(messageCount)
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
        return switch (difficulty) {
            case 1 -> "初级";
            case 2 -> "中级";
            case 3 -> "高级";
            default -> "未知";
        };
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
