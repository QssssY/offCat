package com.airesume.server.mapper;

import com.airesume.server.entity.InterviewDimensionScore;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Update;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 面试维度评分 Mapper。
 */
@Mapper
public interface InterviewDimensionScoreMapper extends BaseMapper<InterviewDimensionScore> {

    /**
     * 按面试会话批量逻辑删除维度评分，避免清理历史后遗留孤儿雷达数据。
     */
    @Update("""
            <script>
            UPDATE interview_dimension_score
               SET is_deleted = 1,
                   update_time = #{updateTime}
             WHERE session_id IN
             <foreach collection="sessionIds" item="sessionId" open="(" separator="," close=")">
                 #{sessionId}
             </foreach>
               AND is_deleted = 0
            </script>
            """)
    int logicalDeleteBySessionIds(@Param("sessionIds") List<String> sessionIds,
                                  @Param("updateTime") LocalDateTime updateTime);
}
