package com.airesume.server.mapper;

import com.airesume.server.entity.ResumePolishRecord;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Update;

import java.util.Collection;

/**
 * AI 简历润色记录 Mapper。
 */
@Mapper
public interface ResumePolishRecordMapper extends BaseMapper<ResumePolishRecord> {

    /**
     * 逻辑删除当前用户的 AI 简历润色记录。
     */
    @Update("""
            UPDATE resume_polish_record
            SET is_deleted = 1,
                update_time = NOW()
            WHERE user_id = #{userId}
              AND is_deleted = 0
            """)
    int logicalDeleteByUserId(@Param("userId") Long userId);

    /**
     * 按简历诊断任务批量逻辑删除 AI 润色记录。
     */
    @Update("""
            <script>
            UPDATE resume_polish_record
            SET is_deleted = 1,
                update_time = NOW()
            WHERE is_deleted = 0
              AND resume_task_id IN
              <foreach collection="taskIds" item="taskId" open="(" separator="," close=")">
                #{taskId}
              </foreach>
            </script>
            """)
    int logicalDeleteByResumeTaskIds(@Param("taskIds") Collection<Long> taskIds);
}
