package com.airesume.server.service;

import com.airesume.server.dto.resume.ResumeJobMatchAnalyzeRequest;
import com.airesume.server.dto.resume.ResumeJobMatchAnalyzeResponse;
import com.airesume.server.entity.ResumeJobMatchRecord;
import com.baomidou.mybatisplus.extension.service.IService;

/**
 * 岗位 JD 对比分析服务接口。
 */
public interface ResumeJobMatchService extends IService<ResumeJobMatchRecord> {

    /**
     * 执行岗位 JD 对比分析。
     *
     * @param userId 当前用户 ID
     * @param request 分析请求
     * @return 分析结果
     */
    ResumeJobMatchAnalyzeResponse analyzeJobMatch(Long userId, ResumeJobMatchAnalyzeRequest request);

    /**
     * 查询当前简历任务最近一次岗位 JD 对比结果。
     *
     * @param userId 当前用户 ID
     * @param resumeTaskId 简历诊断任务 ID
     * @return 最近一次分析结果
     */
    ResumeJobMatchAnalyzeResponse getLatestAnalysis(Long userId, Long resumeTaskId);
}
