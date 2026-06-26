package com.airesume.server.mapper;

import com.airesume.server.entity.QuotaConsumptionLog;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;

/**
 * 用户额度消费记录 Mapper
 */
@Mapper
public interface QuotaConsumptionLogMapper extends BaseMapper<QuotaConsumptionLog> {
}
