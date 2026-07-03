package com.airesume.server.mapper;

import com.airesume.server.entity.UserAiDailyUsage;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import java.time.LocalDate;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Update;

/**
 * 用户自定义 AI 每日用量 Mapper。
 */
@Mapper
public interface UserAiDailyUsageMapper extends BaseMapper<UserAiDailyUsage> {

    /**
     * 已存在当日记录时，在未超限前提下原子递增。
     */
    @Update("""
            UPDATE user_ai_daily_usage
            SET call_count = call_count + 1,
                update_time = NOW()
            WHERE user_id = #{userId}
              AND usage_date = #{usageDate}
              AND call_count < #{limit}
              AND is_deleted = 0
            """)
    int incrementIfBelowLimit(@Param("userId") Long userId,
                              @Param("usageDate") LocalDate usageDate,
                              @Param("limit") int limit);

    /**
     * 当日首次调用时插入计数记录，唯一键兜住并发重复插入。
     */
    @Insert("""
            INSERT IGNORE INTO user_ai_daily_usage
                (id, user_id, usage_date, call_count, create_time, update_time, is_deleted)
            VALUES
                (#{id}, #{userId}, #{usageDate}, 0, NOW(), NOW(), 0)
            """)
    int insertEmptyUsageIfAbsent(@Param("id") Long id,
                                 @Param("userId") Long userId,
                                 @Param("usageDate") LocalDate usageDate);

    /**
     * AI 调用失败时回滚自定义 AI 计数，不能降到 0 以下。
     */
    @Update("""
            UPDATE user_ai_daily_usage
            SET call_count = GREATEST(call_count - 1, 0),
                update_time = NOW()
            WHERE user_id = #{userId}
              AND usage_date = #{usageDate}
              AND is_deleted = 0
            """)
    int rollbackToday(@Param("userId") Long userId, @Param("usageDate") LocalDate usageDate);
}
