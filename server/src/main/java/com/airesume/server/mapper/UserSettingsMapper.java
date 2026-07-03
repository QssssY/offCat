package com.airesume.server.mapper;

import com.airesume.server.entity.UserSettings;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

/**
 * 用户设置 Mapper。
 * 自动清理任务只读取已启用保留天数的用户，避免每天扫描所有业务记录。
 */
@Mapper
public interface UserSettingsMapper extends BaseMapper<UserSettings> {

    @Select("""
            SELECT *
            FROM user_settings
            WHERE is_deleted = 0
              AND interview_retention_days > 0
            """)
    List<UserSettings> selectInterviewRetentionEnabled();

    @Select("""
            SELECT *
            FROM user_settings
            WHERE is_deleted = 0
              AND resume_retention_days > 0
            """)
    List<UserSettings> selectResumeRetentionEnabled();

    @Select("""
            SELECT *
            FROM user_settings
            WHERE user_id = #{userId}
              AND is_deleted = 0
            LIMIT 1
            """)
    UserSettings selectActiveByUserId(@Param("userId") Long userId);
}
