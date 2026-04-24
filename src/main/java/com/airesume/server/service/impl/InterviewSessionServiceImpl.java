package com.airesume.server.service.impl;

import com.airesume.server.common.constants.InterviewConstants;
import com.airesume.server.common.exception.BusinessException;
import com.airesume.server.dto.interview.ChatMessageResponse;
import com.airesume.server.dto.interview.InterviewHistoryResponse;
import com.airesume.server.dto.interview.InterviewSessionResponse;
import com.airesume.server.dto.interview.SendMessageResponse;
import com.airesume.server.entity.InterviewChatLog;
import com.airesume.server.entity.InterviewSession;
import com.airesume.server.mapper.InterviewChatLogMapper;
import com.airesume.server.mapper.InterviewSessionMapper;
import com.airesume.server.service.InterviewAiService;
import com.airesume.server.service.InterviewSessionService;
import com.airesume.server.service.UserQuotaService;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class InterviewSessionServiceImpl extends ServiceImpl<InterviewSessionMapper, InterviewSession> implements InterviewSessionService {

    private final InterviewChatLogMapper interviewChatLogMapper;
    private final UserQuotaService userQuotaService;
    private final InterviewAiService interviewAiService;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public String createSession(Long userId, String jobRole, String jobRoleCode, Integer difficulty) {
        log.info("Creating interview session, userId: {}, jobRole: {}, jobRoleCode: {}, difficulty: {}",
                userId, jobRole, jobRoleCode, difficulty);

        boolean hasQuota = userQuotaService.checkInterviewQuota(userId);
        if (!hasQuota) {
            throw new BusinessException("模拟面试次数已用完");
        }

        String sessionId = UUID.randomUUID().toString().replace("-", "");
        log.debug("Generated sessionId: {}", sessionId);

        InterviewSession session = new InterviewSession();
        session.setSessionId(sessionId);
        session.setUserId(userId);
        session.setJobRole(jobRole);
        session.setJobRoleCode(jobRoleCode);
        session.setDifficulty(difficulty);
        session.setStatus(InterviewConstants.STATUS_IN_PROGRESS);
        session.setComprehensiveScore(null);
        session.setEvaluationReport(null);
        save(session);

        log.info("Interview session created, id: {}, sessionId: {}", session.getId(), sessionId);

        userQuotaService.deductInterviewQuota(userId);

        String openingMessage = interviewAiService.generateOpening(jobRole, jobRoleCode, difficulty);
        saveChatMessage(sessionId, InterviewConstants.ROLE_ASSISTANT, openingMessage);

        log.info("Interview session initialization completed, sessionId: {}", sessionId);
        return sessionId;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public SendMessageResponse sendMessage(String sessionId, Long userId, String content, String jobRoleCode, Integer difficulty) {
        log.info("Processing send message request, sessionId: {}, userId: {}, jobRoleCode: {}", sessionId, userId, jobRoleCode);

        InterviewSession session = getSessionBySessionId(sessionId);
        if (session == null) {
            throw new BusinessException("会话不存在");
        }
        if (!session.getUserId().equals(userId)) {
            throw new BusinessException("无权访问该会话");
        }
        if (session.getStatus() == InterviewConstants.STATUS_ENDED) {
            throw new BusinessException("该面试已结束");
        }

        saveChatMessage(sessionId, InterviewConstants.ROLE_USER, content);

        List<InterviewChatLog> chatLogs = getChatLogsBySessionId(sessionId);
        List<InterviewAiService.ChatMessageItem> history = chatLogs.stream()
                .map(log -> new InterviewAiService.ChatMessageItem(log.getMessageRole(), log.getContent()))
                .collect(Collectors.toList());

        String replyContent = interviewAiService.generateReply(sessionId, history, content, jobRoleCode, difficulty);

        saveChatMessage(sessionId, InterviewConstants.ROLE_ASSISTANT, replyContent);

        log.info("Message processed successfully, sessionId: {}, messageCount: {}", sessionId, chatLogs.size() + 1);

        return SendMessageResponse.builder()
                .sessionId(sessionId)
                .replyContent(replyContent)
                .build();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void endInterview(String sessionId, Long userId) {
        log.info("Ending interview, sessionId: {}, userId: {}", sessionId, userId);

        InterviewSession session = getSessionBySessionId(sessionId);
        if (session == null) {
            throw new BusinessException("会话不存在");
        }
        if (!session.getUserId().equals(userId)) {
            throw new BusinessException("无权访问该会话");
        }
        if (session.getStatus() == InterviewConstants.STATUS_ENDED) {
            log.warn("Interview already ended, sessionId: {}", sessionId);
            return;
        }

        session.setStatus(InterviewConstants.STATUS_ENDED);

        List<InterviewChatLog> chatLogs = getChatLogsBySessionId(sessionId);
        List<InterviewAiService.ChatMessageItem> history = chatLogs.stream()
                .map(log -> new InterviewAiService.ChatMessageItem(log.getMessageRole(), log.getContent()))
                .collect(Collectors.toList());

        InterviewAiService.EvaluationResult result = interviewAiService.generateEvaluation(sessionId, history);
        session.setComprehensiveScore(result.score());
        session.setEvaluationReport(result.evaluationReport());

        updateById(session);
        log.info("Interview ended successfully, sessionId: {}, score: {}", sessionId, result.score());
    }

    @Override
    public InterviewSessionResponse getSessionById(String sessionId, Long userId) {
        log.info("Getting session detail, sessionId: {}, userId: {}", sessionId, userId);

        InterviewSession session = getSessionBySessionId(sessionId);
        if (session == null) {
            throw new BusinessException("会话不存在");
        }
        if (!session.getUserId().equals(userId)) {
            throw new BusinessException("无权访问该会话");
        }

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
        return switch (difficulty) {
            case InterviewConstants.DIFFICULTY_EASY -> "初级";
            case InterviewConstants.DIFFICULTY_MEDIUM -> "中级";
            case InterviewConstants.DIFFICULTY_HARD -> "高级";
            default -> "未知难度";
        };
    }

    private InterviewSession getSessionBySessionId(String sessionId) {
        LambdaQueryWrapper<InterviewSession> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(InterviewSession::getSessionId, sessionId);
        return getOne(wrapper);
    }

    private void saveChatMessage(String sessionId, String messageRole, String content) {
        InterviewChatLog chatLog = new InterviewChatLog();
        chatLog.setSessionId(sessionId);
        chatLog.setMessageRole(messageRole);
        chatLog.setContent(content);
        interviewChatLogMapper.insert(chatLog);
        log.debug("Chat message saved, sessionId: {}, role: {}", sessionId, messageRole);
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
