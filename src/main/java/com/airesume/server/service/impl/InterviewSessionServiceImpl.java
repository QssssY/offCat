package com.airesume.server.service.impl;

import com.airesume.server.common.constants.InterviewConstants;
import com.airesume.server.common.exception.BusinessException;
import com.airesume.server.dto.interview.ChatMessageResponse;
import com.airesume.server.dto.interview.InterviewHistoryResponse;
import com.airesume.server.dto.interview.InterviewSessionResponse;
import com.airesume.server.entity.InterviewChatLog;
import com.airesume.server.entity.InterviewSession;
import com.airesume.server.mapper.InterviewChatLogMapper;
import com.airesume.server.mapper.InterviewSessionMapper;
import com.airesume.server.service.InterviewSessionService;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class InterviewSessionServiceImpl extends ServiceImpl<InterviewSessionMapper, InterviewSession> implements InterviewSessionService {

    private final InterviewChatLogMapper interviewChatLogMapper;

    @Override
    public InterviewSessionResponse getSessionById(String sessionId, Long userId) {
        log.info("Getting session detail, sessionId: {}, userId: {}", sessionId, userId);

        InterviewSession session = requireOwnedSession(sessionId, userId);
        List<InterviewChatLog> chatLogs = getChatLogsBySessionId(sessionId);
        List<ChatMessageResponse> chatMessageResponses = chatLogs.stream()
                .map(this::buildChatMessageResponse)
                .collect(Collectors.toList());

        log.info("Session detail fetched successfully, sessionId: {}, chatLogCount: {}", sessionId, chatLogs.size());
        return buildSessionResponse(session, chatMessageResponses);
    }

    @Override
    public List<InterviewHistoryResponse> getHistoryByUserId(Long userId) {
        log.info("Getting interview history, userId: {}", userId);

        LambdaQueryWrapper<InterviewSession> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(InterviewSession::getUserId, userId);
        wrapper.orderByDesc(InterviewSession::getCreateTime);

        List<InterviewSession> sessions = list(wrapper);
        List<InterviewHistoryResponse> history = sessions.stream()
                .map(this::buildHistoryResponse)
                .collect(Collectors.toList());

        log.info("Interview history fetched successfully, userId: {}, count: {}", userId, history.size());
        return history;
    }

    @Override
    public String getStatusDescription(Integer status) {
        return switch (status) {
            case InterviewConstants.STATUS_IN_PROGRESS -> "进行中";
            case InterviewConstants.STATUS_ENDED -> "已结束";
            default -> "未知状态";
        };
    }

    @Override
    public String getDifficultyDescription(Integer difficulty) {
        return InterviewConstants.getDifficultyLabel(difficulty);
    }

    /**
     * 只保留查询链路，因此这里仍校验会话归属，但不再承载任何写操作入口。
     */
    private InterviewSession requireOwnedSession(String sessionId, Long userId) {
        InterviewSession session = getSessionBySessionId(sessionId);
        if (session == null) {
            throw new BusinessException("会话不存在");
        }
        if (!session.getUserId().equals(userId)) {
            throw new BusinessException("无权访问该会话");
        }
        return session;
    }

    private InterviewSession getSessionBySessionId(String sessionId) {
        LambdaQueryWrapper<InterviewSession> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(InterviewSession::getSessionId, sessionId);
        return getOne(wrapper);
    }

    private List<InterviewChatLog> getChatLogsBySessionId(String sessionId) {
        LambdaQueryWrapper<InterviewChatLog> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(InterviewChatLog::getSessionId, sessionId);
        wrapper.orderByAsc(InterviewChatLog::getCreateTime);
        return interviewChatLogMapper.selectList(wrapper);
    }

    private InterviewSessionResponse buildSessionResponse(InterviewSession session, List<ChatMessageResponse> chatLogs) {
        return InterviewSessionResponse.builder()
                .id(session.getId())
                .sessionId(session.getSessionId())
                .userId(session.getUserId())
                .jobRole(session.getJobRole())
                .difficulty(session.getDifficulty())
                .difficultyDesc(getDifficultyDescription(session.getDifficulty()))
                .status(session.getStatus())
                .statusDesc(getStatusDescription(session.getStatus()))
                .comprehensiveScore(session.getComprehensiveScore())
                .evaluationReport(session.getEvaluationReport())
                .chatLogs(chatLogs)
                .createTime(session.getCreateTime())
                .updateTime(session.getUpdateTime())
                .build();
    }

    private InterviewHistoryResponse buildHistoryResponse(InterviewSession session) {
        return InterviewHistoryResponse.builder()
                .id(session.getId())
                .sessionId(session.getSessionId())
                .jobRole(session.getJobRole())
                .difficulty(session.getDifficulty())
                .difficultyDesc(getDifficultyDescription(session.getDifficulty()))
                .status(session.getStatus())
                .statusDesc(getStatusDescription(session.getStatus()))
                .comprehensiveScore(session.getComprehensiveScore())
                .createTime(session.getCreateTime())
                .updateTime(session.getUpdateTime())
                .build();
    }

    private ChatMessageResponse buildChatMessageResponse(InterviewChatLog chatLog) {
        return ChatMessageResponse.builder()
                .id(chatLog.getId())
                .messageRole(chatLog.getMessageRole())
                .content(chatLog.getContent())
                .createTime(chatLog.getCreateTime())
                .build();
    }
}
