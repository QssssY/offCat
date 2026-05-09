package com.airesume.server.controller;

import com.airesume.server.common.result.Result;
import com.airesume.server.entity.InterviewSession;
import com.airesume.server.entity.ResumeDiagnosisTask;
import com.airesume.server.mapper.InterviewSessionMapper;
import com.airesume.server.mapper.ResumeDiagnosisTaskMapper;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.HashMap;
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

    private final ResumeDiagnosisTaskMapper resumeDiagnosisTaskMapper;
    private final InterviewSessionMapper interviewSessionMapper;

    /**
     * 获取当前用户的月度统计
     * 返回本月简历诊断完成数、本月模拟面试完成数
     */
    @GetMapping("/monthly")
    @Cacheable(value = "user:monthlyStats", key = "#authentication.principal")
    public Result<Map<String, Long>> getMonthlyStats(Authentication authentication) {
        Long userId = (Long) authentication.getPrincipal();
        // 本月起始时间
        LocalDateTime monthStart = LocalDate.now().withDayOfMonth(1).atStartOfDay();

        // 本月简历诊断完成数（status=2 表示已完成）
        long resumeCount = resumeDiagnosisTaskMapper.selectCount(
                new LambdaQueryWrapper<ResumeDiagnosisTask>()
                        .eq(ResumeDiagnosisTask::getStatus, 2)
                        .eq(ResumeDiagnosisTask::getUserId, userId)
                        .ge(ResumeDiagnosisTask::getCreateTime, monthStart)
        );

        // 本月模拟面试完成数（status=1 表示已结束）
        long interviewCount = interviewSessionMapper.selectCount(
                new LambdaQueryWrapper<InterviewSession>()
                        .eq(InterviewSession::getStatus, 1)
                        .eq(InterviewSession::getUserId, userId)
                        .ge(InterviewSession::getCreateTime, monthStart)
        );

        Map<String, Long> stats = new HashMap<>();
        stats.put("resumeCountThisMonth", resumeCount);
        stats.put("interviewCountThisMonth", interviewCount);

        log.info("User monthly stats queried, userId: {}, resumeCount: {}, interviewCount: {}",
                userId, resumeCount, interviewCount);

        return Result.success(stats);
    }
}
