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
    @Query("select s from InterviewSession s where s.sessionId = :sessionId and s.isDeleted = 0")
    Optional<InterviewSession> findBySessionId(@Param("sessionId") String sessionId);

    /**
     * 根据会话 ID 和用户 ID 查询。
     */
    @Query("select s from InterviewSession s where s.sessionId = :sessionId and s.userId = :userId and s.isDeleted = 0")
    Optional<InterviewSession> findBySessionIdAndUserId(@Param("sessionId") String sessionId, @Param("userId") Long userId);

    /**
     * 根据用户 ID 查询所有会话（不分页）。
     */
    @Query("select s from InterviewSession s where s.userId = :userId and s.isDeleted = 0 order by s.createTime desc")
    List<InterviewSession> findByUserIdOrderByCreateTimeDesc(@Param("userId") Long userId);

    /**
     * 根据用户 ID 分页查询会话。
     */
    @Query("select s from InterviewSession s where s.userId = :userId and s.isDeleted = 0")
    Page<InterviewSession> findByUserId(@Param("userId") Long userId, Pageable pageable);

    /**
     * 根据用户 ID 和状态查询。
     */
    @Query("select s from InterviewSession s where s.userId = :userId and s.status = :status and s.isDeleted = 0 order by s.createTime desc")
    List<InterviewSession> findByUserIdAndStatusOrderByCreateTimeDesc(@Param("userId") Long userId, @Param("status") Integer status);

    /**
     * 根据用户 ID 统计会话数量。
     */
    @Query("select count(s) from InterviewSession s where s.userId = :userId and s.isDeleted = 0")
    long countByUserId(@Param("userId") Long userId);

    /**
     * 根据用户 ID 和状态统计会话数量。
     */
    @Query("select count(s) from InterviewSession s where s.userId = :userId and s.status = :status and s.isDeleted = 0")
    long countByUserIdAndStatus(@Param("userId") Long userId, @Param("status") Integer status);

    /**
     * 更新会话状态。
     */
    @Modifying
    @Query("UPDATE InterviewSession s SET s.status = :status, s.updateTime = :updateTime WHERE s.sessionId = :sessionId AND s.isDeleted = 0")
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
               AND s.isDeleted = 0
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
    @Query("UPDATE InterviewSession s SET s.comprehensiveScore = :score, s.updateTime = :updateTime WHERE s.sessionId = :sessionId AND s.isDeleted = 0")
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
               AND (s.evaluationReport IS NULL OR s.evaluationReport = '')
               AND s.isDeleted = 0
            """)
    int updateEvaluationReportIfAbsent(
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
    @Query("UPDATE InterviewSession s SET s.openingGenerated = :openingGenerated, s.updateTime = :updateTime WHERE s.sessionId = :sessionId AND s.isDeleted = 0")
    int updateOpeningGenerated(
            @Param("sessionId") String sessionId,
            @Param("openingGenerated") Integer openingGenerated,
            @Param("updateTime") LocalDateTime updateTime
    );

    /**
     * 查询用户已结束且有评分的面试会话（最近10条，按时间倒序）。
     * 用于个人成长中心的面试评分趋势展示。
     */
    List<InterviewSession> findTop10ByUserIdAndStatusAndComprehensiveScoreIsNotNullAndIsDeletedOrderByCreateTimeDesc(
            @Param("userId") Long userId,
            @Param("status") Integer status,
            @Param("isDeleted") Integer isDeleted);

    /**
     * 查询用户已结束且有评估报告的面试会话，用于雷达图读取维度明细。
     */
    @Query("""
            select s
            from InterviewSession s
            where s.userId = :userId
              and s.status = :status
              and s.evaluationReport is not null
              and s.evaluationReport <> ''
              and s.isDeleted = :isDeleted
            order by s.createTime desc
            """)
    List<InterviewSession> findRecentEndedSessionsWithEvaluationReport(
            @Param("userId") Long userId,
            @Param("status") Integer status,
            @Param("isDeleted") Integer isDeleted,
            Pageable pageable);

    /**
     * 查询当前用户未删除的会话 ID，用于同步清理聊天记录和岗位定向上下文。
     */
    @Query("select s.sessionId from InterviewSession s where s.userId = :userId and s.isDeleted = 0")
    List<String> findActiveSessionIdsByUserId(@Param("userId") Long userId);

    /**
     * 分批查询超过保留期的已结束面试会话。
     * 自动清理跳过进行中的会话，避免定时任务打断用户正在使用的面试流程。
     */
    @Query("""
            select s.sessionId
            from InterviewSession s
            where s.userId = :userId
              and s.status = :status
              and s.createTime < :cutoffTime
              and s.isDeleted = 0
            order by s.createTime asc
            """)
    List<String> findExpiredSessionIds(@Param("userId") Long userId,
                                       @Param("status") Integer status,
                                       @Param("cutoffTime") LocalDateTime cutoffTime,
                                       Pageable pageable);

    /**
     * 逻辑删除当前用户所有面试会话。
     */
    @Modifying
    @Query("""
            UPDATE InterviewSession s
               SET s.isDeleted = 1,
                   s.updateTime = :updateTime
             WHERE s.userId = :userId
               AND s.isDeleted = 0
            """)
    int logicalDeleteByUserId(@Param("userId") Long userId, @Param("updateTime") LocalDateTime updateTime);

    /**
     * 逻辑删除指定会话，用于自动保留期清理的小批量处理。
     */
    @Modifying
    @Query("""
            UPDATE InterviewSession s
               SET s.isDeleted = 1,
                   s.updateTime = :updateTime
             WHERE s.sessionId in :sessionIds
               AND s.isDeleted = 0
            """)
    int logicalDeleteBySessionIdIn(@Param("sessionIds") List<String> sessionIds,
                                   @Param("updateTime") LocalDateTime updateTime);
}
