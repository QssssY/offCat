package com.airesume.server.mapper;

import com.airesume.server.dto.admin.CustomAiUsageTrendActiveUserRow;
import com.airesume.server.dto.admin.CustomAiUsageTrendTypeStatRow;
import com.airesume.server.dto.admin.CustomAiUsageTypeStatResponse;
import com.airesume.server.dto.admin.CustomAiUserUsageStatResponse;
import com.airesume.server.dto.admin.CustomAiUserUsageTypeStatResponse;
import com.airesume.server.entity.UserAiUsageDetail;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import java.time.LocalDate;
import java.util.List;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

/**
 * 用户自定义 AI 按功能用量 Mapper。
 */
@Mapper
public interface UserAiUsageDetailMapper extends BaseMapper<UserAiUsageDetail> {

    /**
     * 当日某功能首次调用时插入明细记录，唯一键兜住并发重复插入。
     */
    @Insert("""
            INSERT IGNORE INTO user_ai_usage_detail
                (id, user_id, usage_date, usage_type, call_count, create_time, update_time, is_deleted)
            VALUES
                (#{id}, #{userId}, #{usageDate}, #{usageType}, 0, NOW(), NOW(), 0)
            """)
    int insertEmptyDetailIfAbsent(@Param("id") Long id,
                                  @Param("userId") Long userId,
                                  @Param("usageDate") LocalDate usageDate,
                                  @Param("usageType") String usageType);

    /**
     * 按用户、日期和功能口径递增调用次数。
     */
    @Update("""
            UPDATE user_ai_usage_detail
            SET call_count = call_count + 1,
                update_time = NOW()
            WHERE user_id = #{userId}
              AND usage_date = #{usageDate}
              AND usage_type = #{usageType}
              AND is_deleted = 0
            """)
    int incrementToday(@Param("userId") Long userId,
                       @Param("usageDate") LocalDate usageDate,
                       @Param("usageType") String usageType);

    /**
     * 自定义 AI 调用失败时回滚对应功能计数，不能降到 0 以下。
     */
    @Update("""
            UPDATE user_ai_usage_detail
            SET call_count = GREATEST(call_count - 1, 0),
                update_time = NOW()
            WHERE user_id = #{userId}
              AND usage_date = #{usageDate}
              AND usage_type = #{usageType}
              AND is_deleted = 0
            """)
    int rollbackToday(@Param("userId") Long userId,
                      @Param("usageDate") LocalDate usageDate,
                      @Param("usageType") String usageType);

    /**
     * 查询已经配置过自定义 AI 的用户数。
     */
    @Select("""
            SELECT COUNT(DISTINCT user_id)
            FROM user_ai_config
            WHERE is_deleted = 0
            """)
    int countConfiguredUsers();

    /**
     * 查询指定日期范围内实际使用过自定义 AI 的用户数。
     */
    @Select("""
            SELECT COUNT(DISTINCT user_id)
            FROM user_ai_usage_detail
            WHERE usage_date BETWEEN #{startDate} AND #{endDate}
              AND call_count > 0
              AND is_deleted = 0
            """)
    int countActiveUsers(@Param("startDate") LocalDate startDate,
                         @Param("endDate") LocalDate endDate);

    /**
     * 查询指定日期范围内自定义 AI 总调用次数。
     */
    @Select("""
            SELECT COALESCE(SUM(call_count), 0)
            FROM user_ai_usage_detail
            WHERE usage_date BETWEEN #{startDate} AND #{endDate}
              AND is_deleted = 0
            """)
    int sumTotalCalls(@Param("startDate") LocalDate startDate,
                      @Param("endDate") LocalDate endDate);

    /**
     * 查询指定日期范围内按功能聚合的调用次数。
     */
    @Select("""
            SELECT usage_type AS usageType,
                   SUM(call_count) AS callCount
            FROM user_ai_usage_detail
            WHERE usage_date BETWEEN #{startDate} AND #{endDate}
              AND is_deleted = 0
            GROUP BY usage_type
            ORDER BY callCount DESC, usage_type ASC
            """)
    List<CustomAiUsageTypeStatResponse> selectTypeStats(@Param("startDate") LocalDate startDate,
                                                        @Param("endDate") LocalDate endDate);

    /**
     * 查询指定日期范围内有调用记录的用户数，用于分页。
     */
    @Select("""
            SELECT COUNT(*)
            FROM (
                SELECT user_id
                FROM user_ai_usage_detail
                WHERE usage_date BETWEEN #{startDate} AND #{endDate}
                  AND call_count > 0
                  AND is_deleted = 0
                GROUP BY user_id
            ) t
            """)
    long countUserStats(@Param("startDate") LocalDate startDate,
                        @Param("endDate") LocalDate endDate);

    /**
     * 查询指定日期范围内按用户聚合的调用明细页。
     */
    @Select("""
            SELECT d.user_id AS userId,
                   u.username AS username,
                   u.nickname AS nickname,
                   SUM(d.call_count) AS totalCalls
            FROM user_ai_usage_detail d
            LEFT JOIN sys_user u ON u.id = d.user_id
            WHERE d.usage_date BETWEEN #{startDate} AND #{endDate}
              AND d.call_count > 0
              AND d.is_deleted = 0
            GROUP BY d.user_id, u.username, u.nickname
            ORDER BY totalCalls DESC, d.user_id DESC
            LIMIT #{limit} OFFSET #{offset}
            """)
    List<CustomAiUserUsageStatResponse> selectUserStatsPage(@Param("startDate") LocalDate startDate,
                                                            @Param("endDate") LocalDate endDate,
                                                            @Param("offset") int offset,
                                                            @Param("limit") int limit);

    /**
     * 查询一组用户在指定日期范围内的功能拆分明细。
     */
    @Select("""
            <script>
            SELECT user_id AS userId,
                   usage_type AS usageType,
                   SUM(call_count) AS callCount
            FROM user_ai_usage_detail
            WHERE usage_date BETWEEN #{startDate} AND #{endDate}
              AND call_count > 0
              AND is_deleted = 0
              AND user_id IN
              <foreach collection="userIds" item="userId" open="(" separator="," close=")">
                #{userId}
              </foreach>
            GROUP BY user_id, usage_type
            ORDER BY user_id DESC, callCount DESC, usage_type ASC
            </script>
            """)
    List<CustomAiUserUsageTypeStatResponse> selectUserTypeStats(@Param("startDate") LocalDate startDate,
                                                                @Param("endDate") LocalDate endDate,
                                                                @Param("userIds") List<Long> userIds);

    /**
     * 趋势图按日期和功能口径聚合调用次数，Service 负责未知口径归一和缺失日期补 0。
     */
    @Select("""
            SELECT usage_date AS date,
                   usage_type AS usageType,
                   SUM(call_count) AS callCount
            FROM user_ai_usage_detail
            WHERE usage_date BETWEEN #{startDate} AND #{endDate}
              AND call_count > 0
              AND is_deleted = 0
            GROUP BY usage_date, usage_type
            ORDER BY usage_date ASC, callCount DESC, usage_type ASC
            """)
    List<CustomAiUsageTrendTypeStatRow> selectTrendTypeStats(@Param("startDate") LocalDate startDate,
                                                             @Param("endDate") LocalDate endDate);

    /**
     * 趋势图按日期统计当日实际发生过自定义 AI 调用的用户数。
     */
    @Select("""
            SELECT usage_date AS date,
                   COUNT(DISTINCT user_id) AS activeUserCount
            FROM user_ai_usage_detail
            WHERE usage_date BETWEEN #{startDate} AND #{endDate}
              AND call_count > 0
              AND is_deleted = 0
            GROUP BY usage_date
            ORDER BY usage_date ASC
            """)
    List<CustomAiUsageTrendActiveUserRow> selectTrendActiveUserCounts(@Param("startDate") LocalDate startDate,
                                                                      @Param("endDate") LocalDate endDate);
}
