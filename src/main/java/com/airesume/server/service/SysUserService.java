package com.airesume.server.service;

import com.airesume.server.entity.MembershipPlan;
import com.airesume.server.entity.SysUser;
import com.baomidou.mybatisplus.extension.service.IService;

/**
 * 用户服务接口
 */
public interface SysUserService extends IService<SysUser> {

    /**
     * 根据用户名查询用户
     *
     * @param username 用户名
     * @return 用户信息
     */
    SysUser getByUsername(String username);

    /**
     * 检查用户名是否已存在
     *
     * @param username 用户名
     * @return 存在返回true，否则返回false
     */
    boolean existsByUsername(String username);

    /**
     * 判断用户是否为VIP会员
     *
     * @param userId 用户ID
     * @return 是VIP返回true，否则返回false
     */
    boolean isVipUser(Long userId);

    int getVipDailyResumeLimit(Long userId);

    int getVipDailyInterviewLimit(Long userId);

    /** VIP每日AI润色次数限制。 */
    int getVipDailyPolishLimit(Long userId);

    /** VIP每日JD匹配次数限制。 */
    int getVipDailyJdMatchLimit(Long userId);

    /** VIP每日模板使用次数限制。 */
    int getVipDailyTemplateLimit(Long userId);

    /** VIP每日Offer辅助次数限制。 */
    int getVipDailyOfferLimit(Long userId);

    /** 获取用户当前生效的会员套餐（非VIP或已过期返回null）。 */
    MembershipPlan getActiveMembershipPlan(Long userId);

    /** VIP套餐周期内功能总额度（0=不限）。featureType: polish/jd_match/template/offer/resume/interview */
    int getVipCycleLimit(Long userId, String featureType);

}
