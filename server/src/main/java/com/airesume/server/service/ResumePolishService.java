package com.airesume.server.service;

import com.airesume.server.dto.resume.ResumeDocumentUpdateRequest;
import com.airesume.server.dto.resume.ResumePolishAnalyzeRequest;
import com.airesume.server.dto.resume.ResumePolishAnalyzeResponse;
import com.airesume.server.entity.ResumePolishRecord;
import com.baomidou.mybatisplus.extension.service.IService;
import java.util.function.Consumer;

/**
 * AI 简历润色服务接口。
 */
public interface ResumePolishService extends IService<ResumePolishRecord> {

    /**
     * 执行 AI 简历润色。
     *
     * @param userId 当前用户 ID
     * @param request 润色请求
     * @return 润色结果
     */
    ResumePolishAnalyzeResponse analyzeResumePolish(Long userId, ResumePolishAnalyzeRequest request);

    ResumePolishAnalyzeResponse analyzeResumePolish(
            Long userId,
            ResumePolishAnalyzeRequest request,
            Consumer<ResumePolishProgressEvent> progressConsumer);

    /**
     * 查询最近一次润色结果。
     *
     * @param userId 当前用户 ID
     * @param resumeTaskId 简历诊断任务 ID
     * @return 最近一次润色结果
     */
    ResumePolishAnalyzeResponse getLatestPolishResult(Long userId, Long resumeTaskId);

    /**
     * 保存用户编辑的简历文档。
     *
     * @param userId 当前用户 ID
     * @param polishRecordId 润色记录 ID
     * @param request 文档更新请求
     */
    void updateDocument(Long userId, Long polishRecordId, ResumeDocumentUpdateRequest request);

    record ResumePolishProgressEvent(
            String eventName,
            String stage,
            String message,
            int progress,
            Object data) {
    }
}
