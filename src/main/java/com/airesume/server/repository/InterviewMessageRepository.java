package com.airesume.server.repository;

import com.airesume.server.entity.InterviewChatLog;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;
import java.util.Optional;

/**
 * 面试消息仓储
 */
@Repository
public interface InterviewMessageRepository extends JpaRepository<InterviewChatLog, Long> {

    interface SessionMessageCountProjection {
        String getSessionId();

        Long getMessageCount();
    }

    /**
     * 根据会话ID查询所有消息（按时间升序）
     */
    @Query("select l from InterviewChatLog l where l.sessionId = :sessionId and l.isDeleted = 0 order by l.createTime asc")
    List<InterviewChatLog> findBySessionIdOrderByCreateTimeAsc(@Param("sessionId") String sessionId);

    /**
     * 根据会话ID查询所有消息（按时间降序）
     */
    @Query("select l from InterviewChatLog l where l.sessionId = :sessionId and l.isDeleted = 0 order by l.createTime desc")
    List<InterviewChatLog> findBySessionIdOrderByCreateTimeDesc(@Param("sessionId") String sessionId);

    Optional<InterviewChatLog> findFirstBySessionIdAndIsDeletedOrderByCreateTimeDesc(String sessionId, Integer isDeleted);

    /**
     * 根据会话ID统计消息数量
     */
    @Query("select count(l) from InterviewChatLog l where l.sessionId = :sessionId and l.isDeleted = 0")
    long countBySessionId(@Param("sessionId") String sessionId);

    @Query("""
            select l.sessionId as sessionId, count(l) as messageCount
            from InterviewChatLog l
            where l.isDeleted = 0 and l.sessionId in :sessionIds
            group by l.sessionId
            """)
    List<SessionMessageCountProjection> countBySessionIdIn(@Param("sessionIds") Collection<String> sessionIds);

    /**
     * 根据会话ID和角色统计消息数量
     */
    @Query("select count(l) from InterviewChatLog l where l.sessionId = :sessionId and l.messageRole = :messageRole and l.isDeleted = 0")
    long countBySessionIdAndMessageRole(@Param("sessionId") String sessionId, @Param("messageRole") String messageRole);

    /**
     * 根据会话ID删除所有消息
     */
    void deleteBySessionId(String sessionId);

    /**
     * 逻辑删除一批会话下的聊天记录。
     */
    @Modifying
    @Query("""
            UPDATE InterviewChatLog l
               SET l.isDeleted = 1,
                   l.updateTime = :updateTime
             WHERE l.sessionId in :sessionIds
               AND l.isDeleted = 0
            """)
    int logicalDeleteBySessionIdIn(@Param("sessionIds") Collection<String> sessionIds,
                                   @Param("updateTime") LocalDateTime updateTime);
}
