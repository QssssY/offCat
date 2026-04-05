package com.airesume.server.controller;

import com.airesume.server.common.result.PageResult;
import com.airesume.server.common.result.Result;
import com.airesume.server.dto.resume.ResumeDiagnosisHistoryResponse;
import com.airesume.server.dto.resume.ResumeDiagnosisTaskResponse;
import com.airesume.server.service.ResumeDiagnosisTaskService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

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

    /**
     * 上传简历文件并创建诊断任务
     *
     * @param file 简历PDF文件
     * @param authentication Spring Security 认证对象
     * @return 任务ID
     */
    @PostMapping(value = "/upload", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public Result<String> uploadResume(@RequestParam("file") MultipartFile file,
                                      Authentication authentication) {
        Long userId = (Long) authentication.getPrincipal();
        log.info("Upload resume request, userId: {}, fileName: {}, fileSize: {}",
                userId, file.getOriginalFilename(), file.getSize());

        String taskId = resumeDiagnosisTaskService.createTask(userId, file);
        return Result.success("简历诊断任务已提交", taskId);
    }

    /**
     * 查询任务详情
     *
     * @param taskId 任务ID
     * @param authentication Spring Security 认证对象
     * @return 任务详情
     */
    @GetMapping("/task/{taskId}")
    public Result<ResumeDiagnosisTaskResponse> getTaskDetail(@PathVariable Long taskId,
                                                                Authentication authentication) {
        Long userId = (Long) authentication.getPrincipal();
        log.info("Get task detail request, taskId: {}, userId: {}", taskId, userId);

        ResumeDiagnosisTaskResponse task = resumeDiagnosisTaskService.getTaskById(taskId, userId);
        return Result.success(task);
    }

    /**
     * 查询当前用户的简历诊断历史记录（分页）
     *
     * @param pageNum 页码，默认1
     * @param pageSize 每页大小，默认10
     * @param authentication Spring Security 认证对象
     * @return 分页历史记录
     */
    @GetMapping("/history")
    public Result<PageResult<ResumeDiagnosisHistoryResponse>> getHistory(
            @RequestParam(defaultValue = "1") Integer pageNum,
            @RequestParam(defaultValue = "10") Integer pageSize,
            Authentication authentication) {
        Long userId = (Long) authentication.getPrincipal();
        log.info("Get history request, userId: {}, pageNum: {}, pageSize: {}", userId, pageNum, pageSize);

        PageResult<ResumeDiagnosisHistoryResponse> history = resumeDiagnosisTaskService.getHistoryByUserId(userId, pageNum, pageSize);
        return Result.success(history);
    }
}
