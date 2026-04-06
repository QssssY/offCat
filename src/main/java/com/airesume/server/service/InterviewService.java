package com.airesume.server.service;

import com.airesume.server.common.result.PageResult;
import com.airesume.server.dto.interview.*;
import com.airesume.server.entity.InterviewChatLog;
import com.airesume.server.entity.InterviewSession;
import com.airesume.server.mock.MockInterviewService;
import com.airesume.server.repository.InterviewMessageRepository;
import com.airesume.server.repository.InterviewSessionRepository;
import com.baomidou.mybatisplus.core.toolkit.IdWorker;
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

    /**
     * 创建面试会话
     */
    @Transactional(rollbackFor = Exception.class)
    public InterviewSessionResponse createSession(Long userId, CreateSessionRequest request) {
        // 参数校验
        validateCreateRequest(request);

        // 创建会话
        InterviewSession session = new InterviewSession();
        // 手动分配雪花算法 ID（JPA 不会自动处理 MyBatis-Plus 的 ASSIGN_ID）
        session.setId(IdWorker.getId());
        session.setUserId(userId);
        session.setSessionId(UUID.randomUUID().toString().replace("-", ""));
        session.setJobRole(request.getJobRole());
        session.setDifficulty(request.getDifficulty());

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

        // 生成综合评分和评价报告
        int score = mockInterviewService.generateMockScore(sessionId);
        String evaluationReport = mockInterviewService.generateMockEvaluationReport(sessionId, score);
        session.setComprehensiveScore(score);
        session.setEvaluationReport(evaluationReport);

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
                log.info("[SSE-落库] 【订阅成功】onSubscribe 被调用, subscription={}", s.getClass().getSimpleName());
                subscriptionRef[0] = s;
                s.request(Long.MAX_VALUE);
                log.info("[SSE-落库] 【订阅成功】已发送 request(Long.MAX_VALUE)");
            }

            @Override
            public void onNext(String item) {
                // 【第六层日志】InterviewService 收到上游发射的数据
                log.info("[SSE-落库] 【第六层-onNext收到】item={}, length={}, currentAccumulatedLength={}",
                        item == null ? "null" : ("\"" + item + "\""),
                        item == null ? 0 : item.length(),
                        fullReply.length());
                if (item == null || item.isBlank()) {
                    log.debug("[SSE-落库] onNext 收到空内容，跳过累积");
                    return;
                }
                fullReply.append(item);
                log.info("[SSE-落库] 【追加后】累积长度 now={}", fullReply.length());
                try {
                    emitter.send("event: content\ndata: " + item + "\n\n");
                    log.debug("[SSE-落库] SSE发送成功: {}", item);
                } catch (IOException e) {
                    log.warn("[SSE-落库] SSE发送失败，可能已被客户端断开: {}", e.getMessage());
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
                        emitter.send("event: error\ndata: 流处理异常: " + t.getMessage() + "\n\n");
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
                            emitter.send("event: error\ndata: AI 回复内容为空，请稍后重试\n\n");
                            emitter.complete();
                        } catch (Exception e) {
                            log.warn("发送空内容错误事件失败", e);
                        }
                        return;
                    }

                    try {
                        emitter.send("event: done\ndata: \n\n");
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

        return InterviewHistoryResponse.builder()
                .id(session.getId())
                .sessionId(session.getSessionId())
                .jobRole(session.getJobRole())
                .difficulty(session.getDifficulty())
                .difficultyDesc(convertDifficultyToDesc(session.getDifficulty()))
                .interviewMode("normal")
                .interviewModeDesc("普通面试")
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
}
