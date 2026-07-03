package com.airesume.server.mapper;

import com.airesume.server.entity.ResumeJobMatchRecord;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Update;

import java.util.Collection;

/**
 * 岗位 JD 对比分析记录 Mapper。
 */
@Mapper
public interface ResumeJobMatchRecordMapper extends BaseMapper<ResumeJobMatchRecord> {

    /**
     * 逻辑删除当前用户的岗位 JD 匹配记录。
     */
    @Update("""
            UPDATE resume_job_match_record
            SET is_deleted = 1,
                update_time = NOW()
            WHERE user_id = #{userId}
              AND is_deleted = 0
            """)
    int logicalDeleteByUserId(@Param("userId") Long userId);

    /**
     * 按简历诊断任务批量逻辑删除 JD 匹配记录。
     */
    @Update("""
            <script>
            UPDATE resume_job_match_record
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
