package com.airesume.server.mapper;

import com.airesume.server.entity.ResumeDiagnosisTask;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Update;

/**
 * 简历诊断任务Mapper接口
 * 提供基础的数据库操作能力
 */
@Mapper
public interface ResumeDiagnosisTaskMapper extends BaseMapper<ResumeDiagnosisTask> {

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
}
