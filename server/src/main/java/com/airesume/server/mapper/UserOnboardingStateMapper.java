package com.airesume.server.mapper;

import com.airesume.server.entity.UserOnboardingState;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Update;

/**
 * 用户新手引导状态 Mapper
 */
@Mapper
public interface UserOnboardingStateMapper extends BaseMapper<UserOnboardingState> {

    /**
     * 账号注销时逻辑删除用户新手引导状态。
     */
    @Update("""
            UPDATE user_onboarding_state
            SET is_deleted = 1,
                update_time = NOW()
            WHERE user_id = #{userId}
              AND is_deleted = 0
            """)
    int logicalDeleteByUserId(@Param("userId") Long userId);
}
