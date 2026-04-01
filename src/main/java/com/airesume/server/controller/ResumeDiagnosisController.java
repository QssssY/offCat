package com.airesume.server.controller;

import com.airesume.server.common.result.Result;
import com.airesume.server.dto.resume.ResumeDiagnosisHistoryResponse;
import com.airesume.server.dto.resume.ResumeDiagnosisTaskResponse;
import com.airesume.server.dto.resume.ResumeUploadRequest;
import com.airesume.server.infrastructure.security.JwtUtil;
import com.airesume.server.service.ResumeDiagnosisTaskService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 简历诊断控制器
 * 提供简历上传、任务查询、历史记录等接口
 */
@Slf4j
@RestController
@RequestMapping("/api/resume")
@RequiredArgsConstructor
public class ResumeDiagnosisController {

    private final ResumeDiagnosisTaskService resumeDiagnosisTaskService;
    private final JwtUtil jwtUtil;

    /**
     * 上传简历并创建诊断任务
     *
     * @param request 上传请求，包含简历文件地址
     * @param token   JWT Token
     * @return 任务ID
     */
    @PostMapping("/upload")
    public Result<Long> uploadResume(@Valid @RequestBody ResumeUploadRequest request,
                                      @RequestHeader("Authorization") String token) {
        Long userId = jwtUtil.getUserIdFromToken(token);
        log.info("Upload resume request, userId: {}, fileUrl: {}", userId, request.getFileUrl());

        Long taskId = resumeDiagnosisTaskService.createTask(userId, request.getFileUrl());
        return Result.success("简历诊断任务已提交", taskId);
    }

    /**
     * 查询任务详情
     *
     * @param taskId 任务ID
     * @param token  JWT Token
     * @return 任务详情
     */
    @GetMapping("/task/{taskId}")
    public Result<ResumeDiagnosisTaskResponse> getTaskDetail(@PathVariable Long taskId,
                                                                @RequestHeader("Authorization") String token) {
        Long userId = jwtUtil.getUserIdFromToken(token);
        log.info("Get task detail request, taskId: {}, userId: {}", taskId, userId);

        ResumeDiagnosisTaskResponse task = resumeDiagnosisTaskService.getTaskById(taskId, userId);
        return Result.success(task);
    }

    /**
     * 查询当前用户的简历诊断历史记录
     *
     * @param token JWT Token
     * @return 历史记录列表
     */
    @GetMapping("/history")
    public Result<List<ResumeDiagnosisHistoryResponse>> getHistory(@RequestHeader("Authorization") String token) {
        Long userId = jwtUtil.getUserIdFromToken(token);
        log.info("Get history request, userId: {}", userId);

        List<ResumeDiagnosisHistoryResponse> history = resumeDiagnosisTaskService.getHistoryByUserId(userId);
        return Result.success(history);
    }
}
