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

    /** VIP原子扣减AI润色日额度。 */
    int consumeVipDailyPolishQuotaAtomic(@Param("userId") Long userId, @Param("dailyLimit") int dailyLimit, @Param("cycleLimit") int cycleLimit);

    /** VIP原子扣减JD匹配日额度。 */
    int consumeVipDailyJdMatchQuotaAtomic(@Param("userId") Long userId, @Param("dailyLimit") int dailyLimit, @Param("cycleLimit") int cycleLimit);

    /** VIP原子扣减模板使用日额度。 */
    int consumeVipDailyTemplateQuotaAtomic(@Param("userId") Long userId, @Param("dailyLimit") int dailyLimit, @Param("cycleLimit") int cycleLimit);

    /** VIP原子扣减Offer辅助日额度。 */
    int consumeVipDailyOfferQuotaAtomic(@Param("userId") Long userId, @Param("dailyLimit") int dailyLimit, @Param("cycleLimit") int cycleLimit);

    /** 非会员原子扣减免费AI润色次数。 */
    int deductFreePolishAtomic(@Param("userId") Long userId);

    /** 非会员原子扣减免费JD匹配次数。 */
    int deductFreeJdMatchAtomic(@Param("userId") Long userId);

    /** 非会员原子扣减免费模板使用次数。 */
    int deductFreeTemplateAtomic(@Param("userId") Long userId);

    /** 非会员原子扣减免费Offer辅助次数。 */
    int deductFreeOfferAtomic(@Param("userId") Long userId);

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
