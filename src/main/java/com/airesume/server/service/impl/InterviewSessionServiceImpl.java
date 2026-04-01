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
import com.airesume.server.mock.MockInterviewService;
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

/**
 * 面试会话服务实现类
 * 实现面试会话的创建、消息发送、结束面试、查询等业务逻辑
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class InterviewSessionServiceImpl extends ServiceImpl<InterviewSessionMapper, InterviewSession> implements InterviewSessionService {

    private final InterviewChatLogMapper interviewChatLogMapper;
    private final UserQuotaService userQuotaService;
    private final MockInterviewService mockInterviewService;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public String createSession(Long userId, String jobRole, Integer difficulty) {
        log.info("Creating interview session, userId: {}, jobRole: {}, difficulty: {}", userId, jobRole, difficulty);

        // 1. 校验用户额度
        boolean hasQuota = userQuotaService.checkInterviewQuota(userId);
        if (!hasQuota) {
            throw new BusinessException("模拟面试次数已用完");
        }

        // 2. 生成会话ID
        String sessionId = UUID.randomUUID().toString().replace("-", "");
        log.debug("Generated sessionId: {}", sessionId);

        // 3. 创建会话记录
        InterviewSession session = new InterviewSession();
        session.setSessionId(sessionId);
        session.setUserId(userId);
        session.setJobRole(jobRole);
        session.setDifficulty(difficulty);
        session.setStatus(InterviewConstants.STATUS_IN_PROGRESS);
        session.setComprehensiveScore(null);
        session.setEvaluationReport(null);
        save(session);

        log.info("Interview session created, id: {}, sessionId: {}", session.getId(), sessionId);

        // 4. 扣减用户额度
        userQuotaService.deductInterviewQuota(userId);

        // 5. 添加系统欢迎消息
        String openingMessage = mockInterviewService.generateMockOpening(jobRole, difficulty);
        saveChatMessage(sessionId, InterviewConstants.ROLE_ASSISTANT, openingMessage);

        log.info("Interview session initialization completed, sessionId: {}", sessionId);

        return sessionId;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public SendMessageResponse sendMessage(String sessionId, Long userId, String content) {
        log.info("Processing send message request, sessionId: {}, userId: {}", sessionId, userId);

        // 1. 查询并校验会话
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

        // 2. 保存用户消息
        saveChatMessage(sessionId, InterviewConstants.ROLE_USER, content);

        // 3. 查询当前消息数量（用于生成连贯的对话）
        int messageCount = countMessagesBySessionId(sessionId);

        // 4. 生成模拟面试官回复
        String replyContent = mockInterviewService.generateMockReply(sessionId, content, messageCount);

        // 5. 保存面试官回复
        saveChatMessage(sessionId, InterviewConstants.ROLE_ASSISTANT, replyContent);

        log.info("Message processed successfully, sessionId: {}, messageCount: {}", sessionId, messageCount + 2);

        return SendMessageResponse.builder()
                .sessionId(sessionId)
                .replyContent(replyContent)
                .build();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void endInterview(String sessionId, Long userId) {
        log.info("Ending interview, sessionId: {}, userId: {}", sessionId, userId);

        // 1. 查询并校验会话
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

        // 2. 更新会话状态为已结束
        session.setStatus(InterviewConstants.STATUS_ENDED);

        // 3. 生成模拟评分和评价报告
        int score = mockInterviewService.generateMockScore(sessionId);
        String evaluationReport = mockInterviewService.generateMockEvaluationReport(sessionId, score);
        session.setComprehensiveScore(score);
        session.setEvaluationReport(evaluationReport);

        updateById(session);
        log.info("Interview ended successfully, sessionId: {}, score: {}", sessionId, score);
    }

    @Override
    public InterviewSessionResponse getSessionById(String sessionId, Long userId) {
        log.info("Getting session detail, sessionId: {}, userId: {}", sessionId, userId);

        // 1. 查询并校验会话
        InterviewSession session = getSessionBySessionId(sessionId);
        if (session == null) {
            throw new BusinessException("会话不存在");
        }
        if (!session.getUserId().equals(userId)) {
            throw new BusinessException("无权访问该会话");
        }

        // 2. 查询聊天记录
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

    /**
     * 根据sessionId查询会话
     *
     * @param sessionId 会话ID
     * @return 会话实体
     */
    private InterviewSession getSessionBySessionId(String sessionId) {
        LambdaQueryWrapper<InterviewSession> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(InterviewSession::getSessionId, sessionId);
        return getOne(wrapper);
    }

    /**
     * 保存聊天消息
     *
     * @param sessionId   会话ID
     * @param messageRole 消息角色
     * @param content     消息内容
     */
    private void saveChatMessage(String sessionId, String messageRole, String content) {
        InterviewChatLog chatLog = new InterviewChatLog();
        chatLog.setSessionId(sessionId);
        chatLog.setMessageRole(messageRole);
        chatLog.setContent(content);
        interviewChatLogMapper.insert(chatLog);
        log.debug("Chat message saved, sessionId: {}, role: {}", sessionId, messageRole);
    }

    /**
     * 统计会话中的消息数量
     *
     * @param sessionId 会话ID
     * @return 消息数量
     */
    private int countMessagesBySessionId(String sessionId) {
        LambdaQueryWrapper<InterviewChatLog> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(InterviewChatLog::getSessionId, sessionId);
        return Math.toIntExact(interviewChatLogMapper.selectCount(wrapper));
    }

    /**
     * 查询会话的聊天记录
     *
     * @param sessionId 会话ID
     * @return 聊天记录列表
     */
    private List<InterviewChatLog> getChatLogsBySessionId(String sessionId) {
        LambdaQueryWrapper<InterviewChatLog> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(InterviewChatLog::getSessionId, sessionId);
        wrapper.orderByAsc(InterviewChatLog::getCreateTime);
        return interviewChatLogMapper.selectList(wrapper);
    }

    /**
     * 构建会话详情响应对象
     *
     * @param session     会话实体
     * @param chatLogs    聊天记录列表
     * @return 会话详情响应
     */
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

    /**
     * 构建历史记录响应对象
     *
     * @param session 会话实体
     * @return 历史记录响应
     */
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

    /**
     * 构建聊天消息响应对象
     *
     * @param chatLog 聊天记录实体
     * @return 聊天消息响应
     */
    private ChatMessageResponse buildChatMessageResponse(InterviewChatLog chatLog) {
        return ChatMessageResponse.builder()
                .id(chatLog.getId())
                .messageRole(chatLog.getMessageRole())
                .content(chatLog.getContent())
                .createTime(chatLog.getCreateTime())
                .build();
    }
}
