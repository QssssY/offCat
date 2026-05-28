package com.airesume.server.mapper;

import com.airesume.server.entity.ResumeDiagnosisTask;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;
import java.util.Map;

/**
 * 简历诊断任务Mapper接口
 * 提供基础的数据库操作能力
 */
@Mapper
public interface ResumeDiagnosisTaskMapper extends BaseMapper<ResumeDiagnosisTask> {

    /**
     * 按创建日期聚合简历诊断任务数量，避免管理后台趋势图逐日循环查询。
     */
    @Select("""
            SELECT DATE(create_time) AS statDate,
                   COUNT(*) AS totalCount
            FROM resume_diagnosis_task
            WHERE create_time >= #{startTime}
              AND create_time < #{endExclusiveTime}
              AND is_deleted = 0
            GROUP BY DATE(create_time)
            ORDER BY statDate ASC
            """)
    List<Map<String, Object>> countByCreateDate(@Param("startTime") LocalDateTime startTime,
                                                @Param("endExclusiveTime") LocalDateTime endExclusiveTime);

    /**
     * 原子抢占待处理任务，避免同一任务被多个线程重复消费。
     *
     * @param taskId 任务 ID
     * @param pendingStatus 待处理状态
     * @param processingStatus 处理中状态
     * @return 受影响行数，1 表示抢占成功，0 表示任务已被其他线程处理或不存在
     */
    @Update("""
            UPDATE resume_diagnosis_task
            SET status = #{processingStatus},
                update_time = NOW()
            WHERE id = #{taskId}
              AND status = #{pendingStatus}
              AND is_deleted = 0
            """)
    int claimPendingTask(@Param("taskId") Long taskId,
                         @Param("pendingStatus") Integer pendingStatus,
                         @Param("processingStatus") Integer processingStatus);

    /**
     * 查询当前用户未删除的简历文件路径，用于清理服务端上传文件。
     */
    @Select("""
            SELECT file_url
            FROM resume_diagnosis_task
            WHERE user_id = #{userId}
              AND is_deleted = 0
              AND file_url IS NOT NULL
              AND file_url <> ''
            """)
    List<String> selectActiveFileUrlsByUserId(@Param("userId") Long userId);

    /**
     * 逻辑删除当前用户的简历诊断任务。
     */
    @Update("""
            UPDATE resume_diagnosis_task
            SET is_deleted = 1,
                update_time = NOW()
            WHERE user_id = #{userId}
              AND is_deleted = 0
            """)
    int logicalDeleteByUserId(@Param("userId") Long userId);

    /**
     * 分批查询超过保留期且已终态的简历诊断任务。
     * 自动清理只处理完成/失败任务，避免删除仍在排队或处理中的任务。
     */
    @Select("""
            <script>
            SELECT id
            FROM resume_diagnosis_task
            WHERE user_id = #{userId}
              AND status IN
              <foreach collection="terminalStatuses" item="status" open="(" separator="," close=")">
                #{status}
              </foreach>
              AND create_time &lt; #{cutoffTime}
              AND is_deleted = 0
            ORDER BY create_time ASC
            LIMIT #{limit}
            </script>
            """)
    List<Long> selectExpiredTerminalTaskIds(@Param("userId") Long userId,
                                            @Param("terminalStatuses") Collection<Integer> terminalStatuses,
                                            @Param("cutoffTime") LocalDateTime cutoffTime,
                                            @Param("limit") int limit);

    /**
     * 分批查询使用全局默认保留期的过期终态简历诊断任务。
     * 已显式配置保留天数的用户由用户级清理逻辑处理，避免默认 30 天覆盖用户设置。
     */
    @Select("""
            <script>
            SELECT t.id
            FROM resume_diagnosis_task t
            LEFT JOIN user_settings s
              ON s.user_id = t.user_id
             AND s.is_deleted = 0
            WHERE t.status IN
              <foreach collection="terminalStatuses" item="status" open="(" separator="," close=")">
                #{status}
              </foreach>
              AND t.create_time &lt; #{cutoffTime}
              AND t.is_deleted = 0
              AND (s.id IS NULL OR s.resume_retention_days IS NULL OR s.resume_retention_days &lt;= 0)
            ORDER BY t.create_time ASC
            LIMIT #{limit}
            </script>
            """)
    List<Long> selectDefaultExpiredTerminalTaskIds(@Param("terminalStatuses") Collection<Integer> terminalStatuses,
                                                   @Param("cutoffTime") LocalDateTime cutoffTime,
                                                   @Param("limit") int limit);

    /**
     * 查询指定任务对应的上传文件路径，文件清理必须在主记录逻辑删除前拿到路径。
     */
    @Select("""
            <script>
            SELECT file_url
            FROM resume_diagnosis_task
            WHERE is_deleted = 0
              AND file_url IS NOT NULL
              AND file_url &lt;&gt; ''
              AND id IN
              <foreach collection="taskIds" item="taskId" open="(" separator="," close=")">
                #{taskId}
              </foreach>
            </script>
            """)
    List<String> selectActiveFileUrlsByTaskIds(@Param("taskIds") Collection<Long> taskIds);

    /**
     * 按任务批量逻辑删除简历诊断主记录。
     */
    @Update("""
            <script>
            UPDATE resume_diagnosis_task
            SET is_deleted = 1,
                update_time = NOW()
            WHERE is_deleted = 0
              AND id IN
              <foreach collection="taskIds" item="taskId" open="(" separator="," close=")">
                #{taskId}
              </foreach>
            </script>
            """)
    int logicalDeleteByTaskIds(@Param("taskIds") Collection<Long> taskIds);
}
