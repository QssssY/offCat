package com.airesume.server.mapper;

import com.airesume.server.entity.MockInterviewJobTargetRecord;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Update;

import java.util.Collection;

/**
 * 岗位定向模拟面试记录 Mapper。
 */
@Mapper
public interface MockInterviewJobTargetRecordMapper extends BaseMapper<MockInterviewJobTargetRecord> {

    /**
     * 逻辑删除当前用户的岗位定向面试上下文记录。
     */
    @Update("""
            UPDATE mock_interview_job_target_record
            SET is_deleted = 1,
                update_time = NOW()
            WHERE user_id = #{userId}
              AND is_deleted = 0
            """)
    int logicalDeleteByUserId(@Param("userId") Long userId);

    /**
     * 按会话批量逻辑删除岗位定向上下文，供自动清理任务复用。
     */
    @Update("""
            <script>
            UPDATE mock_interview_job_target_record
            SET is_deleted = 1,
                update_time = NOW()
            WHERE is_deleted = 0
              AND session_id IN
              <foreach collection="sessionIds" item="sessionId" open="(" separator="," close=")">
                #{sessionId}
              </foreach>
            </script>
            """)
    int logicalDeleteBySessionIds(@Param("sessionIds") Collection<String> sessionIds);
}
