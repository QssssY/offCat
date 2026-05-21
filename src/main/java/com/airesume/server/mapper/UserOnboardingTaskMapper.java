package com.airesume.server.mapper;

import com.airesume.server.entity.UserOnboardingTask;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Update;

/**
 * 用户新手任务 Mapper
 */
@Mapper
public interface UserOnboardingTaskMapper extends BaseMapper<UserOnboardingTask> {

    /** 账号注销时逻辑删除该用户所有新手任务 */
    @Update("""
            UPDATE user_onboarding_task
            SET is_deleted = 1, update_time = NOW()
            WHERE user_id = #{userId} AND is_deleted = 0
            """)
    int logicalDeleteByUserId(@Param("userId") Long userId);
}
