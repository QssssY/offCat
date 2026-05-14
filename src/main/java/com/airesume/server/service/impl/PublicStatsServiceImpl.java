package com.airesume.server.service.impl;

import com.airesume.server.entity.InterviewSession;
import com.airesume.server.entity.ResumeDiagnosisTask;
import com.airesume.server.entity.SysUser;
import com.airesume.server.mapper.InterviewSessionMapper;
import com.airesume.server.mapper.ResumeDiagnosisTaskMapper;
import com.airesume.server.mapper.SysUserMapper;
import com.airesume.server.service.PublicStatsService;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;

/**
 * 公开统计服务实现
 * 提供首页展示的平台统计数据，优先从 Redis 缓存读取
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class PublicStatsServiceImpl implements PublicStatsService {

    private static final String STATS_CACHE_KEY = "public:stats";
    private static final long STATS_CACHE_TTL_MINUTES = 5;

    private final SysUserMapper sysUserMapper;
    private final ResumeDiagnosisTaskMapper resumeDiagnosisTaskMapper;
    private final InterviewSessionMapper interviewSessionMapper;
    @Autowired(required = false)
    private RedisTemplate<String, Object> redisTemplate;

    @Override
    public Map<String, Long> getPublicStats() {
        // 尝试从缓存读取
        if (redisTemplate != null) try {
            Object cached = redisTemplate.opsForValue().get(STATS_CACHE_KEY);
            if (cached instanceof Map<?, ?> rawMap) {
                Map<String, Long> stats = new HashMap<>();
                rawMap.forEach((k, v) -> {
                    if (k instanceof String && v instanceof Number) {
                        stats.put((String) k, ((Number) v).longValue());
                    }
                });
                if (stats.size() == 3) return stats;
            }
        } catch (Exception e) {
            log.warn("读取公开统计缓存失败，降级查询数据库", e);
        }

        // 用户总数（@TableLogic 自动过滤已删除记录）
        long userCount = sysUserMapper.selectCount(new LambdaQueryWrapper<SysUser>());

        // 简历诊断完成数（status=2 表示已完成）
        long diagnosisCount = resumeDiagnosisTaskMapper.selectCount(
                new LambdaQueryWrapper<ResumeDiagnosisTask>()
                        .eq(ResumeDiagnosisTask::getStatus, 2)
        );

        // 模拟面试完成数（status=1 表示已结束）
        long interviewCount = interviewSessionMapper.selectCount(
                new LambdaQueryWrapper<InterviewSession>()
                        .eq(InterviewSession::getStatus, 1)
        );

        Map<String, Long> stats = new HashMap<>();
        stats.put("userCount", userCount);
        stats.put("diagnosisCount", diagnosisCount);
        stats.put("interviewCount", interviewCount);

        // 写入缓存
        if (redisTemplate != null) try {
            redisTemplate.opsForValue().set(STATS_CACHE_KEY, stats, STATS_CACHE_TTL_MINUTES, TimeUnit.MINUTES);
        } catch (Exception e) {
            log.warn("写入公开统计缓存失败", e);
        }

        return stats;
    }
}
