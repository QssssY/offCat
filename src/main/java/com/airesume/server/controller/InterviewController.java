package com.airesume.server.controller;

import com.airesume.server.common.result.Result;
import com.airesume.server.dto.interview.*;
import com.airesume.server.service.InterviewSessionService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 模拟面试控制器
 * 提供面试会话创建、消息发送、结束面试、历史查询等接口
 */
@Slf4j
@RestController
@RequestMapping("/api/interview")
@RequiredArgsConstructor
public class InterviewController {

    private final InterviewSessionService interviewSessionService;

    /**
     * 创建面试会话
     *
     * @param request 创建会话请求，包含岗位和难度
     * @param authentication Spring Security 认证对象
     * @return 会话ID
     */
    @PostMapping("/session")
    public Result<String> createSession(@Valid @RequestBody CreateSessionRequest request,
                                         Authentication authentication) {
        Long userId = (Long) authentication.getPrincipal();
        log.info("Create interview session request, userId: {}, jobRole: {}, difficulty: {}",
                userId, request.getJobRole(), request.getDifficulty());

        String sessionId = interviewSessionService.createSession(userId, request.getJobRole(), request.getDifficulty());
        return Result.success("面试会话创建成功", sessionId);
    }

    /**
     * 发送面试消息
     *
     * @param request 发送消息请求，包含会话ID和消息内容
     * @param authentication Spring Security 认证对象
     * @return 面试官回复
     */
    @PostMapping("/message")
    public Result<SendMessageResponse> sendMessage(@Valid @RequestBody SendMessageRequest request,
                                                    Authentication authentication) {
        Long userId = (Long) authentication.getPrincipal();
        log.info("Send message request, sessionId: {}, userId: {}", request.getSessionId(), userId);

        SendMessageResponse response = interviewSessionService.sendMessage(request.getSessionId(), userId, request.getContent());
        return Result.success(response);
    }

    /**
     * 结束面试
     *
     * @param sessionId 会话ID
     * @param authentication Spring Security 认证对象
     * @return 空结果
     */
    @PostMapping("/session/{sessionId}/end")
    public Result<Void> endInterview(@PathVariable String sessionId,
                                     Authentication authentication) {
        Long userId = (Long) authentication.getPrincipal();
        log.info("End interview request, sessionId: {}, userId: {}", sessionId, userId);

        interviewSessionService.endInterview(sessionId, userId);
        return Result.success("面试已结束", null);
    }

    /**
     * 查询会话详情
     *
     * @param sessionId 会话ID
     * @param authentication Spring Security 认证对象
     * @return 会话详情，包含聊天记录
     */
    @GetMapping("/session/{sessionId}")
    public Result<InterviewSessionResponse> getSessionDetail(@PathVariable String sessionId,
                                                               Authentication authentication) {
        Long userId = (Long) authentication.getPrincipal();
        log.info("Get session detail request, sessionId: {}, userId: {}", sessionId, userId);

        InterviewSessionResponse session = interviewSessionService.getSessionById(sessionId, userId);
        return Result.success(session);
    }

    /**
     * 查询当前用户的面试历史记录
     *
     * @param authentication Spring Security 认证对象
     * @return 历史记录列表
     */
    @GetMapping("/history")
    public Result<List<InterviewHistoryResponse>> getHistory(Authentication authentication) {
        Long userId = (Long) authentication.getPrincipal();
        log.info("Get interview history request, userId: {}", userId);

        List<InterviewHistoryResponse> history = interviewSessionService.getHistoryByUserId(userId);
        return Result.success(history);
    }
}
