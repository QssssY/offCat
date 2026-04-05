package com.airesume.server.service;

import com.airesume.server.common.result.PageResult;
import com.airesume.server.dto.interview.*;
import com.airesume.server.entity.InterviewSession;
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

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
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

        interviewSessionRepository.save(session);

        log.info("面试会话结束, sessionId: {}, userId: {}", sessionId, userId);
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
