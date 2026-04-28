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

    /**
     * 查询指定用户、指定简历任务的最近一次岗位 JD 对比记录。
     */
    ResumeJobMatchRecord getLatestRecord(Long userId, Long resumeTaskId);

    /**
     * 查询指定用户的最近一次岗位 JD 对比记录。
     */
    ResumeJobMatchRecord getLatestRecord(Long userId);

    /**
     * 查询当前用户指定的岗位 JD 对比记录。
     */
    ResumeJobMatchRecord getOwnedRecordById(Long userId, Long recordId);
}
