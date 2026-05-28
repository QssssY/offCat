package com.airesume.server.mapper;

import com.airesume.server.entity.InterviewSession;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

/**
 * 面试会话Mapper接口
 * 提供基础的数据库操作能力
 */
@Mapper
public interface InterviewSessionMapper extends BaseMapper<InterviewSession> {

    /**
     * 按创建日期聚合面试会话数量，供管理后台趋势图一次性加载整段日期数据。
     */
    @Select("""
            SELECT DATE(create_time) AS statDate,
                   COUNT(*) AS totalCount
            FROM interview_session
            WHERE create_time >= #{startTime}
              AND create_time < #{endExclusiveTime}
              AND is_deleted = 0
            GROUP BY DATE(create_time)
            ORDER BY statDate ASC
            """)
    List<Map<String, Object>> countByCreateDate(@Param("startTime") LocalDateTime startTime,
                                                @Param("endExclusiveTime") LocalDateTime endExclusiveTime);

    /** 会话结束采用状态 CAS，避免并发结束重复触发评估报告。 */
    @Update("""
            UPDATE interview_session
            SET status = #{targetStatus},
                update_time = #{updateTime}
            WHERE session_id = #{sessionId}
              AND user_id = #{userId}
              AND status = #{expectedStatus}
              AND is_deleted = 0
            """)
    int updateStatusIfCurrentStatus(@Param("sessionId") String sessionId,
                                    @Param("userId") Long userId,
                                    @Param("expectedStatus") Integer expectedStatus,
                                    @Param("targetStatus") Integer targetStatus,
                                    @Param("updateTime") LocalDateTime updateTime);

    /** 只在报告为空时写入评估结果，防止异步重复任务互相覆盖。 */
    @Update("""
            UPDATE interview_session
            SET comprehensive_score = #{score},
                evaluation_report = #{evaluationReport},
                status = #{targetStatus},
                update_time = #{updateTime}
            WHERE session_id = #{sessionId}
              AND (evaluation_report IS NULL OR evaluation_report = '')
              AND is_deleted = 0
            """)
    int updateEvaluationReportIfAbsent(@Param("sessionId") String sessionId,
                                       @Param("score") Integer score,
                                       @Param("evaluationReport") String evaluationReport,
                                       @Param("targetStatus") Integer targetStatus,
                                       @Param("updateTime") LocalDateTime updateTime);

    /** 更新开场白生成标记，供异步开场白任务结束轮询。 */
    @Update("""
            UPDATE interview_session
            SET opening_generated = #{openingGenerated},
                update_time = #{updateTime}
            WHERE session_id = #{sessionId}
              AND is_deleted = 0
            """)
    int updateOpeningGenerated(@Param("sessionId") String sessionId,
                               @Param("openingGenerated") Integer openingGenerated,
                               @Param("updateTime") LocalDateTime updateTime);

    /** 查询当前用户未删除的会话 ID，用于级联清理聊天记录和岗位定向上下文。 */
    @Select("""
            SELECT session_id
            FROM interview_session
            WHERE user_id = #{userId}
              AND is_deleted = 0
            """)
    List<String> selectActiveSessionIdsByUserId(@Param("userId") Long userId);

    /** 分批查询超过保留期的已结束面试会话，跳过进行中会话。 */
    @Select("""
            SELECT session_id
            FROM interview_session
            WHERE user_id = #{userId}
              AND status = #{status}
              AND create_time < #{cutoffTime}
              AND is_deleted = 0
            ORDER BY create_time ASC
            LIMIT #{limit}
            """)
    List<String> selectExpiredSessionIds(@Param("userId") Long userId,
                                         @Param("status") Integer status,
                                         @Param("cutoffTime") LocalDateTime cutoffTime,
                                         @Param("limit") int limit);

    /** 逻辑删除当前用户所有面试会话。 */
    @Update("""
            UPDATE interview_session
            SET is_deleted = 1,
                update_time = #{updateTime}
            WHERE user_id = #{userId}
              AND is_deleted = 0
            """)
    int logicalDeleteByUserId(@Param("userId") Long userId,
                              @Param("updateTime") LocalDateTime updateTime);

    /** 按会话 ID 批量逻辑删除面试会话。 */
    @Update("""
            <script>
            UPDATE interview_session
            SET is_deleted = 1,
                update_time = #{updateTime}
            WHERE is_deleted = 0
              AND session_id IN
              <foreach collection="sessionIds" item="sessionId" open="(" separator="," close=")">
                #{sessionId}
              </foreach>
            </script>
            """)
    int logicalDeleteBySessionIdIn(@Param("sessionIds") List<String> sessionIds,
                                   @Param("updateTime") LocalDateTime updateTime);
}
