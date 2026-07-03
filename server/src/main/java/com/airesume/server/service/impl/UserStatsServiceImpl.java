package com.airesume.server.service.impl;

import com.airesume.server.entity.InterviewSession;
import com.airesume.server.entity.ResumeDiagnosisTask;
import com.airesume.server.mapper.InterviewSessionMapper;
import com.airesume.server.mapper.ResumeDiagnosisTaskMapper;
import com.airesume.server.service.UserStatsService;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

/**
 * 用户维度统计服务实现
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class UserStatsServiceImpl implements UserStatsService {

    private final ResumeDiagnosisTaskMapper resumeDiagnosisTaskMapper;
    private final InterviewSessionMapper interviewSessionMapper;

    @Override
    public Map<String, Long> getMonthlyStats(Long userId) {
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

        log.info("用户月度统计查询, userId: {}, resumeCount: {}, interviewCount: {}",
                userId, resumeCount, interviewCount);

        return stats;
    }
}
