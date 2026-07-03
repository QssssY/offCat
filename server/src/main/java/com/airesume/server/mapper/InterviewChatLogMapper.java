package com.airesume.server.mapper;

import com.airesume.server.entity.InterviewChatLog;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Update;

import java.time.LocalDateTime;
import java.util.Collection;

/**
 * 面试聊天记录Mapper接口
 * 提供基础的数据库操作能力
 */
@Mapper
public interface InterviewChatLogMapper extends BaseMapper<InterviewChatLog> {

    /** 逻辑删除一批会话下的聊天记录，供用户清理和保留期任务复用。 */
    @Update("""
            <script>
            UPDATE interview_chat_log
            SET is_deleted = 1,
                update_time = #{updateTime}
            WHERE is_deleted = 0
              AND session_id IN
              <foreach collection="sessionIds" item="sessionId" open="(" separator="," close=")">
                #{sessionId}
              </foreach>
            </script>
            """)
    int logicalDeleteBySessionIdIn(@Param("sessionIds") Collection<String> sessionIds,
                                   @Param("updateTime") LocalDateTime updateTime);
}
