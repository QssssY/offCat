package com.airesume.server.service;

import com.airesume.server.common.result.PageResult;
import com.airesume.server.dto.interview.*;
import com.airesume.server.entity.InterviewChatLog;
import com.airesume.server.entity.InterviewSession;
import com.airesume.server.mock.MockInterviewService;
import com.airesume.server.repository.InterviewMessageRepository;
import com.airesume.server.repository.InterviewSessionRepository;
import com.baomidou.mybatisplus.core.toolkit.IdWorker;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.servlet.mvc.method.annotation.ResponseBodyEmitter;
import org.springframework.web.servlet.mvc.method.annotation.StreamingResponseBody;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicBoolean;
import org.reactivestreams.Publisher;
import org.reactivestreams.Subscriber;
import org.reactivestreams.Subscription;
import java.util.stream.Collectors;

/**
 * 模拟面试服务
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

    /**
     * 创建面试会话
     */
    @Transactional(rollbackFor = Exception.class)
    public InterviewSessionResponse createSession(Long userId, CreateSessionRequest request) {
        // 参数校验
        validateCreateRequest(request);

        // 处理面试模式，默认 normal
        String interviewMode = resolveInterviewMode(request.getInterviewMode());

        // 创建会话
        InterviewSession session = new InterviewSession();
        // 手动分配雪花算法 ID（JPA 不会自动处理 MyBatis-Plus 的 ASSIGN_ID）
        session.setId(IdWorker.getId());
        session.setUserId(userId);
        session.setSessionId(UUID.randomUUID().toString().replace("-", ""));
        session.setJobRole(request.getJobRole());
        session.setDifficulty(request.getDifficulty());
        session.setInterviewMode(interviewMode);

        // 设置状态为进行中
        session.setStatus(0);

        session.setCreateTime(LocalDateTime.now());
        session.setUpdateTime(LocalDateTime.now());

        interviewSessionRepository.save(session);

        log.info("面试会话创建成功, id: {}, sessionId: {}, userId: {}", session.getId(), session.getSessionId(), userId);

        // 保存初始欢迎语
        String openingMessage = mockInterviewService.generateMockOpening(request.getJobRole(), request.getDifficulty());
        InterviewChatLog welcomeMessage = new InterviewChatLog();
        welcomeMessage.setId(IdWorker.getId());
        welcomeMessage.setSessionId(session.getSessionId());
        welcomeMessage.setMessageRole("assistant");
        welcomeMessage.setContent(openingMessage);
        welcomeMessage.setCreateTime(LocalDateTime.now());
        welcomeMessage.setUpdateTime(LocalDateTime.now());
        welcomeMessage.setIsDeleted(0);
        interviewMessageRepository.save(welcomeMessage);

        log.info("初始欢迎语保存成功, sessionId: {}, messageId: {}", session.getSessionId(), welcomeMessage.getId());

        return convertToSessionResponse(session);
    }

    /**
     * 发送消息
     */
    @Transactional(rollbackFor = Exception.class)
    public SendMessageResponse sendMessage(Long userId, String sessionId, SendMessageRequest request) {
        // 获取会话
        InterviewSession session = interviewSessionRepository.findBySessionIdAndUserId(sessionId, userId)
                .orElseThrow(() -> new RuntimeException("会话不存在或无权访问"));

        // 检查会话状态
        if (session.getStatus() != 0) {
            throw new RuntimeException("会话已结束，无法发送消息");
        }

        // 处理消息并获取AI回复
        String reply = interviewMessageService.processMessageAndGetReply(session, request.getContent());

        return SendMessageResponse.builder()
                .sessionId(sessionId)
                .replyContent(reply)
                .build();
    }

    /**
     * 获取会话详情
     */
    public InterviewSessionResponse getSessionDetail(Long userId, String sessionId) {
        // 获取会话
        InterviewSession session = interviewSessionRepository.findBySessionIdAndUserId(sessionId, userId)
                .orElseThrow(() -> new RuntimeException("会话不存在或无权访问"));

        // 获取聊天记录
        List<com.airesume.server.entity.InterviewChatLog> chatLogs = interviewMessageService.getMessageList(sessionId);

        return convertToSessionResponse(session, chatLogs);
    }

    /**
     * 结束面试
     *
     * 【修复说明】
     * 优先使用真实 AI 生成评价报告，失败时降级为 Mock 数据
     */
    @Transactional(rollbackFor = Exception.class)
    public void endSession(Long userId, String sessionId) {
        // 获取会话
        InterviewSession session = interviewSessionRepository.findBySessionIdAndUserId(sessionId, userId)
                .orElseThrow(() -> new RuntimeException("会话不存在或无权访问"));

        // 检查会话状态
        if (session.getStatus() != 0) {
            throw new RuntimeException("会话已结束");
        }

        // 更新状态为已结束
        session.setStatus(1);
        session.setUpdateTime(LocalDateTime.now());

        // 获取历史对话记录（用于 AI 评价）
        List<InterviewChatLog> chatLogs = interviewMessageService.getMessageList(sessionId);
        List<InterviewAiService.ChatMessageItem> history = chatLogs.stream()
                .map(log -> new InterviewAiService.ChatMessageItem(log.getMessageRole(), log.getContent()))
                .toList();

        log.info("开始生成面试评价, sessionId: {}, 历史消息数: {}", sessionId, history.size());

        // 【核心逻辑】优先调用真实 AI 生成评价，失败时降级为 Mock
        Integer score = null;
        String evaluationReportJson = null;

        try {
            // 调用真实 AI 生成结构化评价报告
            com.airesume.server.dto.interview.InterviewEvaluationReport report =
                    interviewAiService.generateEvaluationReport(
                            sessionId,
                            history,
                            session.getJobRole(),
                            session.getDifficulty(),
                            session.getInterviewMode()
                    );

            score = report.getOverallScore();
            evaluationReportJson = objectMapper.writeValueAsString(report);

            log.info("AI 评价报告生成成功, sessionId: {}, score: {}", sessionId, score);

        } catch (Exception e) {
            log.warn("AI 评价生成失败，降级使用 Mock 数据, sessionId: {}, error: {}",
                    sessionId, e.getMessage());

            // 降级为 Mock 数据
            score = mockInterviewService.generateMockScore(sessionId);
            evaluationReportJson = mockInterviewService.generateMockEvaluationReport(sessionId, score);

            log.info("Mock 评价报告生成成功, sessionId: {}, score: {}", sessionId, score);
        }

        // 设置评分和报告
        session.setComprehensiveScore(score);
        session.setEvaluationReport(evaluationReportJson);

        interviewSessionRepository.save(session);

        log.info("面试会话结束, sessionId: {}, userId: {}, score: {}", sessionId, userId, score);
    }

    /**
     * 获取面试历史（分页）
     */
    public PageResult<InterviewHistoryResponse> getHistory(Long userId, Integer pageNum, Integer pageSize) {
        // 构建分页参数
        Pageable pageable = PageRequest.of(pageNum - 1, pageSize,
                Sort.by(Sort.Direction.DESC, "createTime"));

        // 查询分页数据
        Page<InterviewSession> page = interviewSessionRepository.findByUserId(userId, pageable);

        // 转换为响应对象
        List<InterviewHistoryResponse> list = page.getContent().stream()
                .map(this::convertToHistoryResponse)
                .collect(Collectors.toList());

        return PageResult.of(list, page.getTotalElements(), pageNum, pageSize);
    }

    /**
     * 获取全部面试历史（不分页，兼容旧版）
     * @deprecated 请使用分页接口
     */
    @Deprecated
    public List<InterviewHistoryResponse> getAllHistory(Long userId) {
        List<InterviewSession> sessions = interviewSessionRepository.findByUserIdOrderByCreateTimeDesc(userId);

        return sessions.stream()
                .map(this::convertToHistoryResponse)
                .collect(Collectors.toList());
    }

    public List<InterviewChatLog> getChatLogsForStream(String sessionId, Long userId) {
        InterviewSession session = interviewSessionRepository.findBySessionIdAndUserId(sessionId, userId)
                .orElseThrow(() -> new RuntimeException("会话不存在或无权访问"));
        if (session.getStatus() != 0) {
            throw new RuntimeException("会话已结束，无法发送消息");
        }
        return interviewMessageService.getMessageList(sessionId);
    }

    public void validateSessionForStream(String sessionId, Long userId) {
        InterviewSession session = interviewSessionRepository.findBySessionIdAndUserId(sessionId, userId)
                .orElseThrow(() -> new RuntimeException("会话不存在或无权访问"));
        if (session.getStatus() != 0) {
            throw new RuntimeException("会话已结束，无法发送消息");
        }
    }

    @Transactional(rollbackFor = Exception.class)
    public void saveUserMessage(String sessionId, String content) {
        InterviewChatLog userMessage = new InterviewChatLog();
        userMessage.setId(IdWorker.getId());
        userMessage.setSessionId(sessionId);
        userMessage.setMessageRole("user");
        userMessage.setContent(content);
        userMessage.setCreateTime(LocalDateTime.now());
        userMessage.setUpdateTime(LocalDateTime.now());
        userMessage.setIsDeleted(0);
        interviewMessageRepository.save(userMessage);
        log.debug("用户消息已保存, sessionId: {}", sessionId);
    }

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
                // 【订阅成功】仅打印一次，表示下游已就绪
                log.info("[SSE-落库] 订阅成功，开始接收流数据");
                subscriptionRef[0] = s;
                s.request(Long.MAX_VALUE);
            }

            @Override
            public void onNext(String item) {
                // 【收到 chunk】跳过空内容，避免无效累积
                if (item == null || item.isBlank()) {
                    return;
                }
                fullReply.append(item);
                try {
                    // 【修改】统一结构化 JSON 格式，不再发送裸文本
                    // 格式：event: message\ndata: {"type":"content","content":"文本内容"}\n\n
                    // content 中可能包含特殊字符，需要 JSON 转义
                    String escapedContent = escapeJsonForSse(item);
                    String jsonData = "{\"type\":\"content\",\"content\":\"" + escapedContent + "\"}";
                    emitter.send("event: message\ndata: " + jsonData + "\n\n");
                } catch (IOException e) {
                    // 客户端可能已断开（如页面切换），取消订阅
                    log.warn("[SSE-落库] SSE发送失败，客户端可能已断开: {}", e.getMessage());
                    if (subscriptionRef[0] != null) {
                        subscriptionRef[0].cancel();
                    }
                }
            }

            @Override
            public void onError(Throwable t) {
                if (done.compareAndSet(false, true)) {
                    log.error("[SSE-落库] 【第七层-onError】收到错误: {}", t.getMessage(), t);
                    try {
                        // 【修改】统一结构化 JSON 格式
                        String jsonData = "{\"type\":\"error\",\"message\":\"" + escapeJsonForSse(t.getMessage()) + "\"}";
                        emitter.send("event: message\ndata: " + jsonData + "\n\n");
                        emitter.completeWithError(t);
                    } catch (Exception e) {
                        log.warn("[SSE-落库] 发送错误事件失败", e);
                    }
                }
            }

            @Override
            public void onComplete() {
                if (done.compareAndSet(false, true)) {
                    String finalContent = fullReply.toString();
                    log.info("[SSE-落库] 【第八层-onComplete】流结束, 当前累积内容长度: {}, contentPreview: {}",
                            finalContent.length(),
                            finalContent.length() > 100 ? finalContent.substring(0, 100) : finalContent);

                    // 【关键修复】空内容不落库，记录明确错误日志
                    if (finalContent.isBlank()) {
                        log.error("【严重】AI 流式回复内容为空, sessionId: {}, 不落库", sessionId);
                        try {
                            // 【修改】统一结构化 JSON 格式
                            String jsonData = "{\"type\":\"error\",\"message\":\"AI 回复内容为空，请稍后重试\"}";
                            emitter.send("event: message\ndata: " + jsonData + "\n\n");
                            emitter.complete();
                        } catch (Exception e) {
                            log.warn("发送空内容错误事件失败", e);
                        }
                        return;
                    }

                    try {
                        // 【修改】统一结构化 JSON 格式
                        String jsonData = "{\"type\":\"done\"}";
                        emitter.send("event: message\ndata: " + jsonData + "\n\n");
                        emitter.complete();

                        // 落库前再次校验
                        if (fullReply.length() > 0) {
                            InterviewChatLog assistantMessage = new InterviewChatLog();
                            assistantMessage.setId(IdWorker.getId());
                            assistantMessage.setSessionId(sessionId);
                            assistantMessage.setMessageRole("assistant");
                            assistantMessage.setContent(fullReply.toString());
                            assistantMessage.setCreateTime(LocalDateTime.now());
                            assistantMessage.setUpdateTime(LocalDateTime.now());
                            assistantMessage.setIsDeleted(0);
                            interviewMessageRepository.save(assistantMessage);
                            log.info("assistant回复已落库, sessionId: {}, contentLength: {}", sessionId, fullReply.length());
                        } else {
                            log.error("【严重】落库前发现内容为空, sessionId: {}, 拒绝落库", sessionId);
                        }
                    } catch (Exception e) {
                        log.error("流结束时保存assistant消息失败", e);
                    }
                }
            }
        });
    }

    /**
     * 校验创建请求
     */
    private void validateCreateRequest(CreateSessionRequest request) {
        if (request.getJobRole() == null || request.getJobRole().trim().isEmpty()) {
            throw new RuntimeException("面试岗位不能为空");
        }
        if (request.getDifficulty() == null || request.getDifficulty() < 1 || request.getDifficulty() > 3) {
            throw new RuntimeException("难度级别必须在1-3之间");
        }
    }

    /**
     * 解析并校验面试模式
     *
     * @param interviewMode 前端传入的面试模式
     * @return 标准化后的面试模式，默认返回 normal
     */
    private String resolveInterviewMode(String interviewMode) {
        if (interviewMode == null || interviewMode.isBlank()) {
            return "normal";
        }
        String lowerMode = interviewMode.toLowerCase().trim();
        // 只允许 normal 或 stress，其他值兜底为 normal
        if ("stress".equals(lowerMode)) {
            return "stress";
        }
        return "normal";
    }

    /**
     * 获取面试模式描述
     *
     * @param interviewMode 面试模式
     * @return 面试模式描述
     */
    private String getInterviewModeDescription(String interviewMode) {
        if ("stress".equals(interviewMode)) {
            return "压力面试";
        }
        return "普通面试";
    }

    /**
     * 转换为会话响应
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
                .statusDesc(session.getStatus() != null && session.getStatus() == 0 ? "进行中" : "已结束")
                .comprehensiveScore(session.getComprehensiveScore())
                .createTime(session.getCreateTime())
                .updateTime(session.getUpdateTime())
                .build();
    }

    private InterviewSessionResponse convertToSessionResponse(
            InterviewSession session,
            List<com.airesume.server.entity.InterviewChatLog> chatLogs) {

        List<com.airesume.server.dto.interview.ChatMessageResponse> dtoLogs =
                (chatLogs == null ? java.util.Collections.<com.airesume.server.entity.InterviewChatLog>emptyList() : chatLogs)
                        .stream()
                        .map(log -> com.airesume.server.dto.interview.ChatMessageResponse.builder()
                                .id(log.getId())
                                .messageRole(log.getMessageRole())
                                .content(log.getContent())
                                .createTime(log.getCreateTime())
                                .build())
                        .collect(java.util.stream.Collectors.toList());

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
                .statusDesc(session.getStatus() != null && session.getStatus() == 0 ? "进行中" : "已结束")
                .comprehensiveScore(session.getComprehensiveScore())
                .evaluationReport(session.getEvaluationReport())
                .chatLogs(dtoLogs)
                .createTime(session.getCreateTime())
                .updateTime(session.getUpdateTime())
                .build();
    }
    /**
     * 转换为历史响应
     */
    private InterviewHistoryResponse convertToHistoryResponse(InterviewSession session) {
        // 获取消息数量
        Integer messageCount = interviewMessageService.getMessageCount(session.getSessionId());

        // 获取面试模式，兼容旧数据（null 时返回 normal）
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
                .statusDesc(session.getStatus() != null && session.getStatus() == 0 ? "进行中" : "已结束")
                .comprehensiveScore(session.getComprehensiveScore())
                .messageCount(messageCount)
                .createTime(session.getCreateTime())
                .updateTime(session.getUpdateTime())
                .build();
    }

    /**
     * 转换难度为描述
     */
    private String convertDifficultyToDesc(Integer difficulty) {
        if (difficulty == null) {
            return "未知";
        }
        switch (difficulty) {
            case 1:
                return "初级";
            case 2:
                return "中级";
            case 3:
                return "高级";
            default:
                return "未知";
        }
    }

    /**
     * SSE JSON 内容转义
     *
     * 【背景】
     * SSE 的 data 字段传输 JSON 字符串时，字符串内容必须经过转义
     * 否则特殊字符（如换行、引号、反斜杠）会导致 JSON 解析失败
     *
     * 【转义规则】
     * - \ → \\ (反斜杠)
     * - " → \" (双引号)
     * - \n → \\n (换行)
     * - \r → \\r (回车)
     * - \t → \\t (制表符)
     *
     * @param raw 原始文本
     * @return 转义后的文本，可安全嵌入 JSON 字符串
     */
    private String escapeJsonForSse(String raw) {
        if (raw == null || raw.isEmpty()) {
            return raw;
        }
        // 按顺序转义：先转义反斜杠（避免把后续转义的字符又转义）
        return raw
                .replace("\\", "\\\\")   // 反斜杠
                .replace("\"", "\\\"")   // 双引号
                .replace("\n", "\\n")    // 换行
                .replace("\r", "\\r")    // 回车
                .replace("\t", "\\t");   // 制表符
    }
}
