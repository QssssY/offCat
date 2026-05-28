package com.airesume.server.service;

import com.airesume.server.entity.InterviewChatLog;
import com.airesume.server.entity.InterviewSession;
import com.airesume.server.mapper.InterviewChatLogMapper;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.IdWorker;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 面试消息服务
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class InterviewMessageService {

    private final InterviewChatLogMapper interviewChatLogMapper;

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
        interviewChatLogMapper.insert(message);
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
        interviewChatLogMapper.insert(userMessage);

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
        interviewChatLogMapper.insert(aiMessage);

        log.info("面试消息处理完成, sessionId: {}, userMessageId: {}, aiMessageId: {}",
                session.getSessionId(), userMessage.getId(), aiMessage.getId());

        return aiReply;
    }

    /**
     * 获取会话消息数量
     */
    public Integer getMessageCount(String sessionId) {
        return Math.toIntExact(interviewChatLogMapper.selectCount(new QueryWrapper<InterviewChatLog>()
                .eq("session_id", sessionId)
                .eq("is_deleted", 0)));
    }

    /**
     * 批量获取会话消息数，避免历史列表逐条 count。
     */
    public Map<String, Integer> getMessageCountMap(Collection<String> sessionIds) {
        if (sessionIds == null || sessionIds.isEmpty()) {
            return Map.of();
        }
        // 批量统计只返回 session_id/count，避免历史列表逐会话 count。
        QueryWrapper<InterviewChatLog> wrapper = new QueryWrapper<>();
        wrapper.select("session_id", "COUNT(*) AS message_count")
                .eq("is_deleted", 0)
                .in("session_id", sessionIds)
                .groupBy("session_id");
        return interviewChatLogMapper.selectMaps(wrapper).stream()
                .collect(Collectors.toMap(
                        item -> String.valueOf(item.get("session_id")),
                        item -> Math.toIntExact(((Number) item.get("message_count")).longValue()),
                        (left, right) -> left));
    }

    /**
     * 获取会话消息列表，按创建时间升序排列
     */
    public List<com.airesume.server.entity.InterviewChatLog> getMessageList(String sessionId) {
        return interviewChatLogMapper.selectList(new QueryWrapper<InterviewChatLog>()
                .eq("session_id", sessionId)
                .eq("is_deleted", 0)
                .orderByAsc("create_time"));
    }

    /**
     * 生成模拟回复（TODO: 后续替换为真实AI调用）
     */
    private String generateMockReply(InterviewSession session, String userContent) {
        return String.format("感谢你的回答。针对你提到的内容，我想进一步了解一下你在%s方面的具体经验。" +
                "能否详细描述一下你在这个领域遇到的最大挑战以及你是如何解决的？", session.getJobRole());
    }
}
