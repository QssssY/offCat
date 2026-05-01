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
 * 面试会话仓储。
 */
@Repository
public interface InterviewSessionRepository extends JpaRepository<InterviewSession, Long> {

    /**
     * 根据会话 ID 查询。
     */
    Optional<InterviewSession> findBySessionId(String sessionId);

    /**
     * 根据会话 ID 和用户 ID 查询。
     */
    Optional<InterviewSession> findBySessionIdAndUserId(String sessionId, Long userId);

    /**
     * 根据用户 ID 查询所有会话（不分页）。
     */
    List<InterviewSession> findByUserIdOrderByCreateTimeDesc(Long userId);

    /**
     * 根据用户 ID 分页查询会话。
     */
    Page<InterviewSession> findByUserId(Long userId, Pageable pageable);

    /**
     * 根据用户 ID 和状态查询。
     */
    List<InterviewSession> findByUserIdAndStatusOrderByCreateTimeDesc(Long userId, Integer status);

    /**
     * 根据用户 ID 统计会话数量。
     */
    long countByUserId(Long userId);

    /**
     * 根据用户 ID 和状态统计会话数量。
     */
    long countByUserIdAndStatus(Long userId, Integer status);

    /**
     * 更新会话状态。
     */
    @Modifying
    @Query("UPDATE InterviewSession s SET s.status = :status, s.updateTime = :updateTime WHERE s.sessionId = :sessionId")
    int updateStatus(
            @Param("sessionId") String sessionId,
            @Param("status") Integer status,
            @Param("updateTime") LocalDateTime updateTime
    );

    /**
     * 仅当当前状态匹配时更新状态，用于会话结束的幂等保护。
     */
    @Modifying
    @Query("""
            UPDATE InterviewSession s
               SET s.status = :targetStatus, s.updateTime = :updateTime
             WHERE s.sessionId = :sessionId
               AND s.userId = :userId
               AND s.status = :expectedStatus
            """)
    int updateStatusIfCurrentStatus(
            @Param("sessionId") String sessionId,
            @Param("userId") Long userId,
            @Param("expectedStatus") Integer expectedStatus,
            @Param("targetStatus") Integer targetStatus,
            @Param("updateTime") LocalDateTime updateTime
    );

    /**
     * 更新会话综合评分。
     */
    @Modifying
    @Query("UPDATE InterviewSession s SET s.comprehensiveScore = :score, s.updateTime = :updateTime WHERE s.sessionId = :sessionId")
    int updateScore(
            @Param("sessionId") String sessionId,
            @Param("score") Integer score,
            @Param("updateTime") LocalDateTime updateTime
    );

    /**
     * 仅回写报告字段，避免异步线程覆盖会话终态。
     */
    @Modifying
    @Query("""
            UPDATE InterviewSession s
               SET s.comprehensiveScore = :score,
                   s.evaluationReport = :evaluationReport,
                   s.status = :targetStatus,
                   s.updateTime = :updateTime
             WHERE s.sessionId = :sessionId
            """)
    int updateEvaluationReport(
            @Param("sessionId") String sessionId,
            @Param("score") Integer score,
            @Param("evaluationReport") String evaluationReport,
            @Param("targetStatus") Integer targetStatus,
            @Param("updateTime") LocalDateTime updateTime
    );

    /**
     * 更新开场白生成状态。
     */
    @Modifying
    @Query("UPDATE InterviewSession s SET s.openingGenerated = :openingGenerated, s.updateTime = :updateTime WHERE s.sessionId = :sessionId")
    int updateOpeningGenerated(
            @Param("sessionId") String sessionId,
            @Param("openingGenerated") Integer openingGenerated,
            @Param("updateTime") LocalDateTime updateTime
    );

    /**
     * 查询用户已结束且有评分的面试会话（最近10条，按时间倒序）。
     * 用于个人成长中心的面试评分趋势展示。
     */
    List<InterviewSession> findTop10ByUserIdAndStatusAndComprehensiveScoreIsNotNullOrderByCreateTimeDesc(Long userId, Integer status);
}
