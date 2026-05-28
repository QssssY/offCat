package com.airesume.server.mapper;

import com.airesume.server.entity.MembershipOrder;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

@Mapper
public interface MembershipOrderMapper extends BaseMapper<MembershipOrder> {

    /**
     * 按日聚合订单数量和已支付订单收入。
     * 返回字段: statDate (DATE), totalCount (BIGINT), totalRevenue (DECIMAL)
     */
    @Select("""
            SELECT DATE(create_time) AS statDate,
                   COUNT(*) AS totalCount,
                   COALESCE(SUM(CASE WHEN order_status = 'PAID' THEN order_amount ELSE 0 END), 0) AS totalRevenue
            FROM membership_order
            WHERE create_time >= #{startTime}
              AND create_time < #{endExclusiveTime}
              AND is_deleted = 0
            GROUP BY DATE(create_time)
            ORDER BY statDate ASC
            """)
    List<Map<String, Object>> countByCreateDate(@Param("startTime") LocalDateTime startTime,
                                                 @Param("endExclusiveTime") LocalDateTime endExclusiveTime);

    /**
     * 统计指定时间范围内已支付订单的收入总额（SQL 聚合，避免全量加载）。
     */
    @Select("""
            SELECT COALESCE(SUM(order_amount), 0) AS totalRevenue
            FROM membership_order
            WHERE order_status = 'PAID'
              AND create_time >= #{startTime}
              AND create_time < #{endExclusiveTime}
              AND is_deleted = 0
            """)
    BigDecimal sumPaidRevenue(@Param("startTime") LocalDateTime startTime,
                              @Param("endExclusiveTime") LocalDateTime endExclusiveTime);
}
