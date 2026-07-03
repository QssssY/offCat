package com.airesume.server.service;

import com.airesume.server.common.result.PageResult;
import com.airesume.server.dto.quota.ConsumptionLogResponse;
import com.airesume.server.entity.QuotaConsumptionLog;
import com.baomidou.mybatisplus.extension.service.IService;

/**
 * 用户额度消费记录服务接口
 * 提供消费记录写入、分页查询和定时清理能力
 */
public interface QuotaConsumptionLogService extends IService<QuotaConsumptionLog> {

    /**
     * 记录额度消费（扣减或退款）
     * 在额度扣减成功后、同一事务提交前调用
     *
     * @param userId        用户ID
     * @param quotaType     额度类型（INTERVIEW/RESUME/POLISH/JD_MATCH/TEMPLATE/OFFER）
     * @param changeAmount  变动数量（正=消耗，负=退款）
     * @param balanceAfter  变动后余额（可为null）
     * @param source        来源（FREE/VIP_DAILY/VIP_CYCLE）
     * @param billingSource AI计费来源（可为null）
     * @param businessId    关联业务ID（可为null）
     * @param businessType  业务类型（可为null）
     * @param description   描述
     */
    void logConsumption(Long userId, String quotaType, int changeAmount,
                        Integer balanceAfter, String source,
                        String billingSource, Long businessId,
                        String businessType, String description);

    /**
     * 用户查询自己的消费记录（分页 + 类型筛选）
     *
     * @param userId   用户ID
     * @param quotaType 额度类型筛选（null=全部）
     * @param pageNum  页码（从1开始）
     * @param pageSize 每页条数
     * @return 分页结果
     */
    PageResult<ConsumptionLogResponse> getUserConsumptionLog(Long userId, String quotaType,
                                                              int pageNum, int pageSize);

    /**
     * 管理员查询指定用户的消费记录
     *
     * @param userId   目标用户ID
     * @param quotaType 额度类型筛选（null=全部）
     * @param pageNum  页码
     * @param pageSize 每页条数
     * @return 分页结果
     */
    PageResult<ConsumptionLogResponse> getAdminConsumptionLog(Long userId, String quotaType,
                                                               int pageNum, int pageSize);

    /**
     * 定时清理过期消费记录（逻辑删除）
     * 保留天数从 sys_config 读取，默认90天
     */
    void cleanExpiredLogs();
}
