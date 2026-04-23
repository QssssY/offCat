package com.airesume.server.controller;

import com.airesume.server.common.constants.InterviewConstants;
import com.airesume.server.common.exception.BusinessException;
import com.airesume.server.common.result.PageResult;
import com.airesume.server.common.result.Result;
import com.airesume.server.dto.interview.*;
import com.airesume.server.entity.InterviewChatLog;
import com.airesume.server.entity.SysJobRole;
import com.airesume.server.mapper.InterviewChatLogMapper;
import com.airesume.server.service.InterviewAiService;
import com.airesume.server.service.InterviewService;
import com.airesume.server.service.SysJobRoleService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.method.annotation.ResponseBodyEmitter;

import java.util.List;
import org.reactivestreams.Publisher;

/**
 * 模拟面试控制器
 */
@RestController
@RequestMapping("/api/interview")
@RequiredArgsConstructor
@Slf4j
public class InterviewController {

    private final InterviewService interviewService;
    private final InterviewAiService interviewAiService;
    private final InterviewChatLogMapper interviewChatLogMapper;
    private final SysJobRoleService sysJobRoleService;

    /**
     * 查询当前启用的面试岗位选项
     *
     * 作用：
     * 用户端面试岗位不能再由前端写死，必须统一读取后台配置。
     * 这里只返回启用岗位，保证用户侧只看到管理员允许使用的选项。
     */
    @GetMapping("/job-roles")
    public Result<List<InterviewJobRoleResponse>> getJobRoles() {
        List<InterviewJobRoleResponse> responses = sysJobRoleService.listActiveOrdered().stream()
                .map(this::buildInterviewJobRoleResponse)
                .toList();
        return Result.success(responses);
    }

    /**
     * 创建面试会话
     */
    @PostMapping("/session")
    public Result<InterviewSessionResponse> createSession(
            @RequestBody @Validated CreateSessionRequest request,
            Authentication authentication) {
        Long userId = (Long) authentication.getPrincipal();
        log.info("创建面试会话, userId: {}, 岗位: {}, 难度: {}, 模式: {}",
                userId, request.getJobRole(), request.getDifficulty(), request.getInterviewMode());
        InterviewSessionResponse response = interviewService.createSession(userId, request);
        return Result.success(response);
    }

    /**
     * 发送消息（流式回复）
     * 使用SSE实现流式输出
     */
    @PostMapping("/session/{sessionId}/message/stream")
    public ResponseBodyEmitter streamMessage(
            @PathVariable String sessionId,
            @RequestBody @Validated SendMessageRequest request,
            Authentication authentication) {
        Long userId = (Long) authentication.getPrincipal();
        log.info("收到流式消息请求, userId: {}, sessionId: {}", userId, sessionId);

        ResponseBodyEmitter emitter = new ResponseBodyEmitter(120_000L);

        Authentication authenticationForThread = authentication;

        new Thread(() -> {
            if (authenticationForThread != null) {
                SecurityContextHolder.getContext().setAuthentication(authenticationForThread);
            }
            StringBuilder fullReply = new StringBuilder();
            try {
                List<InterviewChatLog> chatLogs = interviewService.getChatLogsForStream(sessionId, userId);
                List<InterviewAiService.ChatMessageItem> history = chatLogs.stream()
                        .map(log -> new InterviewAiService.ChatMessageItem(log.getMessageRole(), log.getContent()))
                        .toList();

                interviewService.validateSessionForStream(sessionId, userId);

                // 二次传入 userId，让服务层在落库前再次校验会话终态。
                interviewService.saveUserMessage(sessionId, userId, request.getContent());

                String userMessage = request.getContent();

                Publisher<String> publisher = interviewAiService.generateReplyStream(sessionId, history, userMessage);

                interviewService.subscribeAndWriteStream(sessionId, emitter, publisher, fullReply);

            } catch (BusinessException e) {
                try {
                    emitter.send("event: error\ndata: " + e.getMessage() + "\n\n");
                } catch (Exception ex) {
                    log.error("发送错误事件失败", ex);
                }
                try {
                    emitter.completeWithError(e);
                } catch (Exception ex) {
                    log.error("完成emitter失败", ex);
                }
            } catch (Exception e) {
                log.error("流式处理异常, sessionId: {}", sessionId, e);
                try {
                    emitter.send("event: error\ndata: 系统异常: " + e.getMessage() + "\n\n");
                } catch (Exception ex) {
                    log.error("发送异常事件失败", ex);
                }
                try {
                    emitter.completeWithError(e);
                } catch (Exception ex) {
                    log.error("完成emitter失败", ex);
                }
            }
        }, "sse-interview-" + sessionId).start();

        return emitter;
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

    private InterviewJobRoleResponse buildInterviewJobRoleResponse(SysJobRole jobRole) {
        return InterviewJobRoleResponse.builder()
                .roleCode(jobRole.getRoleCode())
                .roleName(jobRole.getRoleName())
                .interviewTag(jobRole.getInterviewTag())
                .tagType(jobRole.getTagType())
                .build();
    }
}
