package com.airesume.server.repository;

import com.airesume.server.entity.InterviewSession;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

/**
 * 面试会话仓储
 */
@Repository
public interface InterviewSessionRepository extends JpaRepository<InterviewSession, Long> {

    /**
     * 根据会话ID查询
     */
    Optional<InterviewSession> findBySessionId(String sessionId);

    /**
     * 根据会话ID和用户ID查询
     */
    Optional<InterviewSession> findBySessionIdAndUserId(String sessionId, Long userId);

    /**
     * 根据用户ID查询所有会话（不分页）
     */
    List<InterviewSession> findByUserIdOrderByCreateTimeDesc(Long userId);

    /**
     * 根据用户ID分页查询会话
     */
    Page<InterviewSession> findByUserId(Long userId, Pageable pageable);

    /**
     * 根据用户ID和状态查询
     */
    List<InterviewSession> findByUserIdAndStatusOrderByCreateTimeDesc(Long userId, Integer status);

    /**
     * 根据用户ID统计会话数量
     */
    long countByUserId(Long userId);

    /**
     * 根据用户ID和状态统计会话数量
     */
    long countByUserIdAndStatus(Long userId, Integer status);

    /**
     * 更新会话状态
     */
    @Modifying
    @Query("UPDATE InterviewSession s SET s.status = :status, s.updateTime = :updateTime WHERE s.sessionId = :sessionId")
    int updateStatus(@Param("sessionId") String sessionId, @Param("status") Integer status, @Param("updateTime") LocalDateTime updateTime);

    /**
     * 更新会话综合评分
     */
    @Modifying
    @Query("UPDATE InterviewSession s SET s.comprehensiveScore = :score, s.updateTime = :updateTime WHERE s.sessionId = :sessionId")
    int updateScore(@Param("sessionId") String sessionId, @Param("score") Integer score, @Param("updateTime") LocalDateTime updateTime);
}
