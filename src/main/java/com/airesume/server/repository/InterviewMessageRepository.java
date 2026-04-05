package com.airesume.server.repository;

import com.airesume.server.entity.InterviewChatLog;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * 面试消息仓储
 */
@Repository
public interface InterviewMessageRepository extends JpaRepository<InterviewChatLog, Long> {

    /**
     * 根据会话ID查询所有消息（按时间升序）
     */
    List<InterviewChatLog> findBySessionIdOrderByCreateTimeAsc(String sessionId);

    /**
     * 根据会话ID查询所有消息（按时间降序）
     */
    List<InterviewChatLog> findBySessionIdOrderByCreateTimeDesc(String sessionId);

    /**
     * 根据会话ID统计消息数量
     */
    long countBySessionId(String sessionId);

    /**
     * 根据会话ID和角色统计消息数量
     */
    long countBySessionIdAndMessageRole(String sessionId, String messageRole);

    /**
     * 根据会话ID删除所有消息
     */
    void deleteBySessionId(String sessionId);
}
