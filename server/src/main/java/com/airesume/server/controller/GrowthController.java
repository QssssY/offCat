package com.airesume.server.controller;

import com.airesume.server.common.result.Result;
import com.airesume.server.dto.growth.GrowthOverviewResponse;
import com.airesume.server.dto.growth.InterviewRadarResponse;
import com.airesume.server.service.GrowthService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 个人成长中心控制器。
 * 提供用户成长数据概览接口。
 */
@Slf4j
@RestController
@RequestMapping("/api/user/growth")
@RequiredArgsConstructor
public class GrowthController {

    private final GrowthService growthService;

    /**
     * 获取个人成长中心概览数据。
     * 聚合简历诊断、JD匹配、AI润色、模拟面试等维度的成长数据。
     *
     * @param authentication 当前登录用户身份
     * @return 成长中心概览数据
     */
    @GetMapping("/overview")
    public Result<GrowthOverviewResponse> getGrowthOverview(Authentication authentication) {
        Long userId = (Long) authentication.getPrincipal();
        log.info("[成长中心] 获取成长概览请求, userId: {}", userId);
        GrowthOverviewResponse response = growthService.getGrowthOverview(userId);
        return Result.success(response);
    }

    /**
     * 获取面试维度雷达数据。
     * 包含最新雷达评分、各维度趋势和盲区提示。
     *
     * @param authentication 当前登录用户身份
     * @return 面试维度雷达数据
     */
    @GetMapping("/interview-radar")
    public Result<InterviewRadarResponse> getInterviewRadar(Authentication authentication) {
        Long userId = (Long) authentication.getPrincipal();
        log.info("[成长中心] 获取面试维度雷达请求, userId: {}", userId);
        InterviewRadarResponse response = growthService.getInterviewRadar(userId);
        return Result.success(response);
    }
}
