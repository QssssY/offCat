package com.airesume.server.controller;

import com.airesume.server.common.result.Result;
import com.airesume.server.service.UserStatsService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

/**
 * 用户维度统计接口（需要登录）
 * 提供当前用户的月度统计数据
 */
@Slf4j
@RestController
@RequestMapping("/api/user/stats")
@RequiredArgsConstructor
public class UserStatsController {

    private final UserStatsService userStatsService;

    /**
     * 获取当前用户的月度统计
     * 返回本月简历诊断完成数、本月模拟面试完成数
     */
    @GetMapping("/monthly")
    @Cacheable(value = "user:monthlyStats", key = "#authentication.principal")
    public Result<Map<String, Long>> getMonthlyStats(Authentication authentication) {
        Long userId = (Long) authentication.getPrincipal();
        return Result.success(userStatsService.getMonthlyStats(userId));
    }
}
