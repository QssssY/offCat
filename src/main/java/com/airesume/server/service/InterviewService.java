package com.airesume.server.service;

import com.airesume.server.common.constants.InterviewConstants;
import com.airesume.server.common.exception.BusinessException;
import com.airesume.server.common.result.PageResult;
import com.airesume.server.dto.interview.ChatMessageResponse;
import com.airesume.server.dto.interview.CreateSessionRequest;
import com.airesume.server.dto.interview.InterviewEvaluationReport;
import com.airesume.server.dto.interview.InterviewHistoryResponse;
import com.airesume.server.dto.interview.InterviewSessionResponse;
import com.airesume.server.dto.interview.SendMessageRequest;
import com.airesume.server.dto.interview.SendMessageResponse;
import com.airesume.server.entity.InterviewChatLog;
import com.airesume.server.entity.InterviewSession;
import com.airesume.server.mock.MockInterviewService;
import com.airesume.server.repository.InterviewMessageRepository;
import com.airesume.server.repository.InterviewSessionRepository;
import com.baomidou.mybatisplus.core.toolkit.IdWorker;
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
 * 模拟面试服务。
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
    private final UserQuotaService userQuotaService;

    /**
     * 创建面试会话。
     */
    @Transactional(rollbackFor = Exception.class)
    public InterviewSessionResponse createSession(Long userId, CreateSessionRequest request) {
        // 创建前先校验参数与配额，避免生成无效会话。
        validateCreateRequest(request);
        if (!userQuotaService.checkInterviewQuota(userId)) {
            throw new BusinessException("模拟面试次数已用完");
        }

        String interviewMode = resolveInterviewMode(request.getInterviewMode());
        InterviewSession session = new InterviewSession();
        session.setId(IdWorker.getId());
        session.setUserId(userId);
        session.setSessionId(UUID.randomUUID().toString().replace("-", ""));
        session.setJobRole(request.getJobRole());
        session.setDifficulty(request.getDifficulty());
        session.setInterviewMode(interviewMode);
        session.setStatus(InterviewConstants.STATUS_IN_PROGRESS);
        session.setCreateTime(LocalDateTime.now());
        session.setUpdateTime(LocalDateTime.now());
        interviewSessionRepository.save(session);

        // 会话创建成功后再扣减配额，保证计费链路一致。
        userQuotaService.deductInterviewQuota(userId);

        String openingMessage = mockInterviewService.generateMockOpening(request.getJobRole(), request.getDifficulty());
        InterviewChatLog welcomeMessage = new InterviewChatLog();
        welcomeMessage.setId(IdWorker.getId());
        welcomeMessage.setSessionId(session.getSessionId());
        welcomeMessage.setMessageRole(InterviewConstants.ROLE_ASSISTANT);
        welcomeMessage.setContent(openingMessage);
        welcomeMessage.setCreateTime(LocalDateTime.now());
        welcomeMessage.setUpdateTime(LocalDateTime.now());
        welcomeMessage.setIsDeleted(0);
        interviewMessageRepository.save(welcomeMessage);

        log.info("面试会话创建成功, sessionId: {}, userId: {}", session.getSessionId(), userId);
        return convertToSessionResponse(session);
    }

    /**
     * 普通发消息。
     */
    @Transactional(rollbackFor = Exception.class)
    public SendMessageResponse sendMessage(Long userId, String sessionId, SendMessageRequest request) {
        // 已结束会话不能继续发送消息。
        InterviewSession session = getSessionByOwnerOrThrow(sessionId, userId);
        assertSessionInProgress(session);

        String reply = interviewMessageService.processMessageAndGetReply(session, request.getContent());
        return SendMessageResponse.builder()
                .sessionId(sessionId)
                .replyContent(reply)
                .build();
    }

    /**
     * 获取会话详情。
     */
    public InterviewSessionResponse getSessionDetail(Long userId, String sessionId) {
        InterviewSession session = getSessionByOwnerOrThrow(sessionId, userId);
        List<InterviewChatLog> chatLogs = interviewMessageService.getMessageList(sessionId);
        return convertToSessionResponse(session, chatLogs);
    }

    /**
     * 结束会话。
     */
    @Transactional(rollbackFor = Exception.class)
    public void endSession(Long userId, String sessionId) {
        InterviewSession session = getSessionByOwnerOrThrow(sessionId, userId);

        // 结束接口做幂等保护，已结束时直接返回。
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
            // 并发场景下如果已被其他请求结束，也按幂等返回。
            log.info("会话已被并发请求结束，忽略重复结束, sessionId: {}, userId: {}", sessionId, userId);
            return;
        }

        // 报告任务必须在事务提交后触发，避免读取旧状态并覆盖终态。
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

        log.info("面试会话结束成功，报告任务已提交, sessionId: {}, userId: {}", sessionId, userId);
    }

    /**
     * 异步触发报告生成。
     */
    private void triggerEvaluationReportAsync(String sessionId) {
        CompletableFuture.runAsync(() -> generateAndPersistEvaluationReport(sessionId));
    }

    /**
     * 生成并持久化评估报告。
     */
    private void generateAndPersistEvaluationReport(String sessionId) {
        InterviewSession session = interviewSessionRepository.findBySessionId(sessionId).orElse(null);
        if (session == null) {
            log.warn("异步生成报告时会话不存在, sessionId: {}", sessionId);
            return;
        }
        if (isSessionInProgress(session)) {
            // 仅允许已结束会话生成报告。
            log.warn("会话仍为进行中，跳过报告生成, sessionId: {}", sessionId);
            return;
        }
        if (session.getEvaluationReport() != null && !session.getEvaluationReport().isBlank()) {
            // 已有报告时直接跳过，避免重复消耗 token。
            log.info("会话报告已存在，跳过重复生成, sessionId: {}", sessionId);
            return;
        }

        List<InterviewChatLog> chatLogs = interviewMessageService.getMessageList(sessionId);
        List<InterviewAiService.ChatMessageItem> history = chatLogs.stream()
                .map(log -> new InterviewAiService.ChatMessageItem(log.getMessageRole(), log.getContent()))
                .toList();

        Integer score;
        String evaluationReportJson;
        try {
            InterviewEvaluationReport report = interviewAiService.generateEvaluationReport(
                    sessionId,
                    history,
                    session.getJobRole(),
                    session.getDifficulty(),
                    session.getInterviewMode()
            );
            score = report.getOverallScore();
            evaluationReportJson = objectMapper.writeValueAsString(report);
            log.info("异步 AI 评估报告生成成功, sessionId: {}, score: {}", sessionId, score);
        } catch (Exception e) {
            // AI 失败时降级为 Mock，保证会话最终有报告结果。
            log.warn("异步 AI 评估失败，降级 Mock, sessionId: {}, error: {}", sessionId, e.getMessage());
            score = mockInterviewService.generateMockScore(sessionId);
            evaluationReportJson = mockInterviewService.generateMockEvaluationReport(sessionId, score);
            log.info("异步 Mock 评估报告生成成功, sessionId: {}, score: {}", sessionId, score);
        }

        // 仅回写报告相关字段并强制保持结束态，避免把 status 覆盖回进行中。
        interviewSessionRepository.updateEvaluationReport(
                sessionId,
                score,
                evaluationReportJson,
                InterviewConstants.STATUS_ENDED,
                LocalDateTime.now()
        );
        log.info("异步报告写回成功, sessionId: {}, userId: {}, score: {}", sessionId, session.getUserId(), score);
    }

    /**
     * 获取面试历史（分页）。
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
     * 获取全部面试历史（兼容旧版）。
     */
    @Deprecated
    public List<InterviewHistoryResponse> getAllHistory(Long userId) {
        List<InterviewSession> sessions = interviewSessionRepository.findByUserIdOrderByCreateTimeDesc(userId);
        return sessions.stream()
                .map(this::convertToHistoryResponse)
                .collect(Collectors.toList());
    }

    /**
     * 流式发送前拉取历史并校验终态。
     */
    public List<InterviewChatLog> getChatLogsForStream(String sessionId, Long userId) {
        InterviewSession session = getSessionByOwnerOrThrow(sessionId, userId);
        assertSessionInProgress(session);
        return interviewMessageService.getMessageList(sessionId);
    }

    /**
     * 流式发送前二次校验终态。
     */
    public void validateSessionForStream(String sessionId, Long userId) {
        InterviewSession session = getSessionByOwnerOrThrow(sessionId, userId);
        assertSessionInProgress(session);
    }

    /**
     * 保存用户消息。
     */
    @Transactional(rollbackFor = Exception.class)
    public void saveUserMessage(String sessionId, Long userId, String content) {
        // 落库前再次校验，防止已结束会话继续写入用户消息。
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
        log.debug("用户消息已保存, sessionId: {}", sessionId);
    }

    /**
     * 订阅并写出流式回复。
     */
    public void subscribeAndWriteStream(
            String sessionId,
            ResponseBodyEmitter emitter,
            Publisher<String> publisher,
            StringBuilder fullReply) throws IOException {

        Subscription[] subscriptionRef = new Subscription[1];
        AtomicBoolean done = new AtomicBoolean(false);

        publisher.subscribe(new Subscriber<String>() {
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
        // 岗位由后台配置，后端必须做最终校验，防止伪造岗位入参。
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
     * 获取面试模式描述。
     */
    private String getInterviewModeDescription(String interviewMode) {
        if ("stress".equals(interviewMode)) {
            return "压力面试";
        }
        return "普通面试";
    }

    /**
     * 转换为会话响应（不带聊天记录）。
     */
    private InterviewSessionResponse convertToSessionResponse(InterviewSession session) {
        return InterviewSessionResponse.builder()
                .id(session.getId())
                .sessionId(session.getSessionId())
                .userId(session.getUserId())
                .jobRole(session.getJobRole())
                .difficulty(session.getDifficulty())
                .difficultyDesc(convertDifficultyToDesc(session.getDifficulty()))
                .interviewMode(session.getInterviewMode())
                .interviewModeDesc(getInterviewModeDescription(session.getInterviewMode()))
                .status(session.getStatus())
                .statusDesc(isSessionInProgress(session) ? "进行中" : "已结束")
                .comprehensiveScore(session.getComprehensiveScore())
                .createTime(session.getCreateTime())
                .updateTime(session.getUpdateTime())
                .build();
    }

    /**
     * 转换为会话响应（带聊天记录）。
     */
    private InterviewSessionResponse convertToSessionResponse(InterviewSession session, List<InterviewChatLog> chatLogs) {
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
                .difficulty(session.getDifficulty())
                .difficultyDesc(convertDifficultyToDesc(session.getDifficulty()))
                .interviewMode(session.getInterviewMode())
                .interviewModeDesc(getInterviewModeDescription(session.getInterviewMode()))
                .status(session.getStatus())
                .statusDesc(isSessionInProgress(session) ? "进行中" : "已结束")
                .comprehensiveScore(session.getComprehensiveScore())
                .evaluationReport(session.getEvaluationReport())
                .chatLogs(dtoLogs)
                .createTime(session.getCreateTime())
                .updateTime(session.getUpdateTime())
                .build();
    }

    /**
     * 转换为历史响应。
     */
    private InterviewHistoryResponse convertToHistoryResponse(InterviewSession session) {
        Integer messageCount = interviewMessageService.getMessageCount(session.getSessionId());
        String interviewMode = session.getInterviewMode();
        if (interviewMode == null || interviewMode.isBlank()) {
            interviewMode = "normal";
        }

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
                .createTime(session.getCreateTime())
                .updateTime(session.getUpdateTime())
                .build();
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
     * SSE 数据中的 JSON 字符转义。
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
     * 校验并返回用户归属会话。
     */
    private InterviewSession getSessionByOwnerOrThrow(String sessionId, Long userId) {
        return interviewSessionRepository.findBySessionIdAndUserId(sessionId, userId)
                .orElseThrow(() -> new BusinessException("会话不存在或无权访问"));
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
     * 统一拦截非进行中会话的发送行为。
     */
    private void assertSessionInProgress(InterviewSession session) {
        if (!isSessionInProgress(session)) {
            throw new BusinessException("会话已结束，无法继续发送消息");
        }
    }
}
