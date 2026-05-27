package com.airesume.server.mapper;

import com.airesume.server.entity.InterviewSession;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

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
}
