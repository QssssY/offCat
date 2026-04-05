package com.airesume.server.controller;

import com.airesume.server.common.result.PageResult;
import com.airesume.server.common.result.Result;
import com.airesume.server.dto.interview.*;
import com.airesume.server.service.InterviewService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.Authentication;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 模拟面试控制器
 */
@RestController
@RequestMapping("/api/interview")
@RequiredArgsConstructor
@Slf4j
public class InterviewController {

    private final InterviewService interviewService;

    /**
     * 创建面试会话
     */
    @PostMapping("/session")
    public Result<InterviewSessionResponse> createSession(
            @RequestBody @Validated CreateSessionRequest request,
            Authentication authentication) {
        Long userId = (Long) authentication.getPrincipal();
        log.info("创建面试会话, userId: {}, 岗位: {}, 难度: {}",
                userId, request.getJobRole(), request.getDifficulty());
        InterviewSessionResponse response = interviewService.createSession(userId, request);
        return Result.success(response);
    }

    /**
     * 发送消息
     */
    @PostMapping("/session/{sessionId}/message")
    public Result<SendMessageResponse> sendMessage(
            @PathVariable String sessionId,
            @RequestBody @Validated SendMessageRequest request,
            Authentication authentication) {
        Long userId = (Long) authentication.getPrincipal();
        log.info("发送消息, userId: {}, sessionId: {}", userId, sessionId);
        SendMessageResponse response = interviewService.sendMessage(userId, sessionId, request);
        return Result.success(response);
    }

    /**
     * 获取会话详情
     */
    @GetMapping("/session/{sessionId}")
    public Result<InterviewSessionResponse> getSessionDetail(
            @PathVariable String sessionId,
            Authentication authentication) {
        Long userId = (Long) authentication.getPrincipal();
        log.info("获取会话详情, userId: {}, sessionId: {}", userId, sessionId);
        InterviewSessionResponse response = interviewService.getSessionDetail(userId, sessionId);
        return Result.success(response);
    }

    /**
     * 结束面试
     */
    @PostMapping("/session/{sessionId}/end")
    public Result<Void> endSession(
            @PathVariable String sessionId,
            Authentication authentication) {
        Long userId = (Long) authentication.getPrincipal();
        log.info("结束面试, userId: {}, sessionId: {}", userId, sessionId);
        interviewService.endSession(userId, sessionId);
        return Result.success();
    }

    /**
     * 获取面试历史（分页）
     */
    @GetMapping("/history")
    public Result<PageResult<InterviewHistoryResponse>> getHistory(
            @RequestParam(defaultValue = "1") Integer pageNum,
            @RequestParam(defaultValue = "5") Integer pageSize,
            Authentication authentication) {
        Long userId = (Long) authentication.getPrincipal();
        log.info("获取面试历史, userId: {}, pageNum: {}, pageSize: {}", userId, pageNum, pageSize);
        PageResult<InterviewHistoryResponse> result = interviewService.getHistory(userId, pageNum, pageSize);
        return Result.success(result);
    }

    /**
     * 获取面试历史（不分页，兼容旧版）
     * @deprecated 请使用分页接口 GET /api/interview/history
     */
    @Deprecated
    @GetMapping("/history/all")
    public Result<List<InterviewHistoryResponse>> getAllHistory(Authentication authentication) {
        Long userId = (Long) authentication.getPrincipal();
        log.warn("调用已废弃接口 GET /api/interview/history/all, userId: {}", userId);
        List<InterviewHistoryResponse> list = interviewService.getAllHistory(userId);
        return Result.success(list);
    }
}
