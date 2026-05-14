package com.airesume.server.mapper;

import com.airesume.server.entity.UserQuota;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

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
}
