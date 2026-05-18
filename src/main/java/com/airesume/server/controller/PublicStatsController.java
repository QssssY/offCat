package com.airesume.server.controller;

import com.airesume.server.common.result.Result;
import com.airesume.server.service.PublicStatsService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

/**
 * 公开统计接口（无需登录）
 * 提供首页展示的平台统计数据
 */
@Slf4j
@RestController
@RequestMapping("/api/stats")
@RequiredArgsConstructor
public class PublicStatsController {

    private final PublicStatsService publicStatsService;

    /**
     * 获取平台公开统计数据
     * 返回用户总数、简历诊断完成数、模拟面试完成数
     * 优先从 Redis 缓存读取，缓存 TTL 5 分钟
     */
    @GetMapping
    public Result<Map<String, Long>> getPublicStats() {
        return Result.success(publicStatsService.getPublicStats());
    }
}
