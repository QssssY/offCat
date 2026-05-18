package com.airesume.server.mapper;

import com.airesume.server.entity.UserQuota;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Update;

@Mapper
public interface UserQuotaMapper extends BaseMapper<UserQuota> {

    /**
     * 原子扣减面试配额，返回受影响行数（0表示配额不足）
     */
    int deductInterviewQuotaAtomic(@Param("userId") Long userId);

    /**
     * 原子扣减简历配额，返回受影响行数（0表示配额不足）
     */
    int deductResumeQuotaAtomic(@Param("userId") Long userId);

    int consumeVipDailyInterviewQuotaAtomic(@Param("userId") Long userId, @Param("dailyLimit") int dailyLimit);

    int consumeVipDailyResumeQuotaAtomic(@Param("userId") Long userId, @Param("dailyLimit") int dailyLimit);

    int refundResumeQuotaAtomic(@Param("userId") Long userId, @Param("dailyLimit") int dailyLimit);

    /**
     * 账号注销时逻辑删除额度记录，保留外键引用但不再参与业务查询。
     */
    @Update("""
            UPDATE user_quota
            SET is_deleted = 1,
                update_time = NOW()
            WHERE user_id = #{userId}
              AND is_deleted = 0
            """)
    int logicalDeleteByUserId(@Param("userId") Long userId);
}
