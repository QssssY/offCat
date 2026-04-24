package com.airesume.server.service;

import com.airesume.server.dto.interview.SendMessageResponse;
import com.airesume.server.entity.InterviewChatLog;
import com.airesume.server.entity.InterviewSession;
import com.airesume.server.repository.InterviewMessageRepository;
import com.baomidou.mybatisplus.core.toolkit.IdWorker;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 面试消息服务
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class InterviewMessageService {

    private final InterviewMessageRepository interviewMessageRepository;

    /**
     * 保存单条消息
     */
    @Transactional(rollbackFor = Exception.class)
    public void saveMessage(InterviewSession session, String messageRole, String content) {
        InterviewChatLog message = new InterviewChatLog();
        message.setId(IdWorker.getId());
        message.setSessionId(session.getSessionId());
        message.setMessageRole(messageRole);
        message.setContent(content);
        message.setCreateTime(LocalDateTime.now());
        message.setUpdateTime(LocalDateTime.now());
        message.setIsDeleted(0);
        interviewMessageRepository.save(message);
        log.info("消息已保存, sessionId: {}, role: {}, messageId: {}",
                session.getSessionId(), messageRole, message.getId());
    }

    /**
     * 处理消息并获取AI回复
     */
    @Transactional(rollbackFor = Exception.class)
    public String processMessageAndGetReply(InterviewSession session, String userContent) {
        // 保存用户消息
        InterviewChatLog userMessage = new InterviewChatLog();
        userMessage.setId(IdWorker.getId());
        userMessage.setSessionId(session.getSessionId());
        userMessage.setMessageRole("user");
        userMessage.setContent(userContent);
        userMessage.setCreateTime(LocalDateTime.now());
        userMessage.setUpdateTime(LocalDateTime.now());
        userMessage.setIsDeleted(0);
        interviewMessageRepository.save(userMessage);

        // TODO: 调用AI服务生成回复
        // 暂时返回模拟回复
        String aiReply = generateMockReply(session, userContent);

        // 保存AI消息
        InterviewChatLog aiMessage = new InterviewChatLog();
        aiMessage.setId(IdWorker.getId());
        aiMessage.setSessionId(session.getSessionId());
        aiMessage.setMessageRole("assistant");
        aiMessage.setContent(aiReply);
        aiMessage.setCreateTime(LocalDateTime.now());
        aiMessage.setUpdateTime(LocalDateTime.now());
        aiMessage.setIsDeleted(0);
        interviewMessageRepository.save(aiMessage);

        log.info("面试消息处理完成, sessionId: {}, userMessageId: {}, aiMessageId: {}",
                session.getSessionId(), userMessage.getId(), aiMessage.getId());

        return aiReply;
    }

    /**
     * 获取会话消息数量
     */
    public Integer getMessageCount(String sessionId) {
        return (int) interviewMessageRepository.countBySessionId(sessionId);
    }

    /**
     * 获取会话消息列表，按创建时间升序排列
     */
    public List<com.airesume.server.entity.InterviewChatLog> getMessageList(String sessionId) {
        return interviewMessageRepository.findBySessionIdOrderByCreateTimeAsc(sessionId);
    }

    /**
     * 生成模拟回复（TODO: 后续替换为真实AI调用）
     */
    private String generateMockReply(InterviewSession session, String userContent) {
        return String.format("感谢你的回答。针对你提到的内容，我想进一步了解一下你在%s方面的具体经验。" +
                "能否详细描述一下你在这个领域遇到的最大挑战以及你是如何解决的？", session.getJobRole());
    }
}
