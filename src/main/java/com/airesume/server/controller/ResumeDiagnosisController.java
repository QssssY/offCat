package com.airesume.server.controller;

import com.airesume.server.common.exception.BusinessException;
import com.airesume.server.common.result.PageResult;
import com.airesume.server.common.result.Result;
import com.airesume.server.dto.resume.ResumeDocumentUpdateRequest;
import com.airesume.server.dto.resume.ResumeDiagnosisHistoryResponse;
import com.airesume.server.dto.resume.ResumeDiagnosisTaskResponse;
import com.airesume.server.dto.resume.ResumeJobMatchAnalyzeRequest;
import com.airesume.server.dto.resume.ResumeJobMatchAnalyzeResponse;
import com.airesume.server.dto.resume.ResumePolishAnalyzeRequest;
import com.airesume.server.dto.resume.ResumePolishAnalyzeResponse;
import com.airesume.server.dto.user.DataCleanupResponse;
import com.airesume.server.service.ResumeDiagnosisTaskService;
import com.airesume.server.service.ResumeJobMatchService;
import com.airesume.server.service.ResumePolishService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.security.core.Authentication;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.util.Locale;
import java.util.Set;

/**
 * 简历诊断控制器。
 */
@Slf4j
@Validated
@RestController
@RequestMapping("/api/resume")
@RequiredArgsConstructor
public class ResumeDiagnosisController {

    private static final Set<String> ALLOWED_CONTENT_TYPES = Set.of(
            "application/pdf",
            "application/msword",
            "application/vnd.openxmlformats-officedocument.wordprocessingml.document",
            "application/octet-stream"
    );

    private static final Set<String> ALLOWED_FILE_EXTENSIONS = Set.of(".pdf", ".doc", ".docx");

    private final ResumeDiagnosisTaskService resumeDiagnosisTaskService;
    private final ResumeJobMatchService resumeJobMatchService;
    private final ResumePolishService resumePolishService;

    /**
     * 上传简历文件并创建诊断任务。
     */
    @PostMapping(value = "/upload", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public Result<String> uploadResume(@RequestParam("file") MultipartFile file,
                                       Authentication authentication) {
        if (file.isEmpty()) {
            throw new BusinessException("上传文件不能为空");
        }
        if (!isAllowedResumeFile(file)) {
            throw new BusinessException("仅支持 PDF、DOC、DOCX 格式的文件");
        }

        Long userId = (Long) authentication.getPrincipal();
        log.info("Upload resume request, userId: {}, fileName: {}, fileSize: {}",
                userId, file.getOriginalFilename(), file.getSize());

        String taskId = resumeDiagnosisTaskService.createTask(userId, file);
        return Result.success("简历诊断任务已提交", taskId);
    }

    /**
     * 查询任务详情。
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
     * 查询当前用户的简历诊断历史记录。
     */
    @GetMapping("/history")
    public Result<PageResult<ResumeDiagnosisHistoryResponse>> getHistory(
            @RequestParam(defaultValue = "1") @Min(1) Integer pageNum,
            @RequestParam(defaultValue = "10") @Min(1) @Max(value = 100, message = "每页最多100条") Integer pageSize,
            Authentication authentication) {
        Long userId = (Long) authentication.getPrincipal();
        log.info("Get history request, userId: {}, pageNum: {}, pageSize: {}", userId, pageNum, pageSize);

        PageResult<ResumeDiagnosisHistoryResponse> history =
                resumeDiagnosisTaskService.getHistoryByUserId(userId, pageNum, pageSize);
        return Result.success(history);
    }

    /**
     * 清理当前用户的全部简历诊断历史及衍生记录。
     */
    @DeleteMapping("/history")
    public Result<DataCleanupResponse> clearHistory(Authentication authentication) {
        Long userId = (Long) authentication.getPrincipal();
        log.info("Clear resume history request, userId: {}", userId);
        int deletedCount = resumeDiagnosisTaskService.clearHistory(userId);
        return Result.success(new DataCleanupResponse(deletedCount));
    }

    /**
     * 执行岗位 JD 对比分析。
     */
    @PostMapping("/job-match/analyze")
    public Result<ResumeJobMatchAnalyzeResponse> analyzeJobMatch(
            @Valid @RequestBody ResumeJobMatchAnalyzeRequest request,
            Authentication authentication) {
        Long userId = (Long) authentication.getPrincipal();
        log.info("Analyze job match request, userId: {}, resumeTaskId: {}", userId, request.getResumeTaskId());

        ResumeJobMatchAnalyzeResponse response = resumeJobMatchService.analyzeJobMatch(userId, request);
        return Result.success("岗位 JD 对比分析完成", response);
    }

    /**
     * 执行 AI 简历润色。
     */
    @PostMapping("/polish/analyze")
    public Result<ResumePolishAnalyzeResponse> analyzeResumePolish(
            @Valid @RequestBody ResumePolishAnalyzeRequest request,
            Authentication authentication) {
        Long userId = (Long) authentication.getPrincipal();
        log.info("Analyze resume polish request, userId: {}, resumeTaskId: {}", userId, request.getResumeTaskId());

        ResumePolishAnalyzeResponse response = resumePolishService.analyzeResumePolish(userId, request);
        return Result.success("AI 简历润色完成", response);
    }

    /**
     * 保存用户编辑的简历文档。
     */
    @PutMapping("/polish-records/{polishRecordId}/document")
    public Result<Void> updatePolishDocument(
            @PathVariable Long polishRecordId,
            @Valid @RequestBody ResumeDocumentUpdateRequest request,
            Authentication authentication) {
        Long userId = (Long) authentication.getPrincipal();
        log.info("Save polish document request, userId: {}, polishRecordId: {}", userId, polishRecordId);

        resumePolishService.updateDocument(userId, polishRecordId, request);
        return Result.success();
    }

    private boolean isAllowedResumeFile(MultipartFile file) {
        String contentType = file.getContentType();
        if (contentType != null && ALLOWED_CONTENT_TYPES.contains(contentType.toLowerCase(Locale.ROOT))) {
            return true;
        }

        String fileName = file.getOriginalFilename();
        if (fileName == null) {
            return false;
        }

        String lowerCaseFileName = fileName.toLowerCase(Locale.ROOT);
        return ALLOWED_FILE_EXTENSIONS.stream().anyMatch(lowerCaseFileName::endsWith);
    }
}
