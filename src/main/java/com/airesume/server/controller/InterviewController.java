package com.airesume.server.controller;

import com.airesume.server.common.result.PageResult;
import com.airesume.server.common.result.Result;
import com.airesume.server.common.constants.InterviewConstants;
import com.airesume.server.dto.interview.CreateSessionRequest;
import com.airesume.server.dto.interview.InterviewHistoryResponse;
import com.airesume.server.dto.interview.InterviewJobRoleResponse;
import com.airesume.server.dto.interview.InterviewJobTargetContext;
import com.airesume.server.dto.interview.InterviewSessionResponse;
import com.airesume.server.dto.interview.SendMessageRequest;
import com.airesume.server.dto.interview.SendMessageResponse;
import com.airesume.server.dto.user.DataCleanupResponse;
import com.airesume.server.entity.InterviewChatLog;
import com.airesume.server.entity.InterviewSession;
import com.airesume.server.entity.SysJobRole;
import com.airesume.server.service.InterviewAiService;
import com.airesume.server.service.InterviewService;
import com.airesume.server.service.MockInterviewJobTargetService;
import com.airesume.server.service.SysJobRoleService;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.reactivestreams.Publisher;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.mvc.method.annotation.ResponseBodyEmitter;

import java.util.List;
import java.util.concurrent.Executor;
import java.util.concurrent.atomic.AtomicBoolean;
/**
 * 模拟面试控制器。
 */
@RestController
@Validated
@RequestMapping("/api/interview")
@RequiredArgsConstructor
@Slf4j
public class InterviewController {

    private final InterviewService interviewService;
    private final InterviewAiService interviewAiService;
    private final SysJobRoleService sysJobRoleService;
    private final MockInterviewJobTargetService mockInterviewJobTargetService;
    @Qualifier("aiAsyncExecutor")
    private final Executor aiAsyncExecutor;

    /**
     * 查询当前启用的面试岗位选项。
     */
    @GetMapping("/job-roles")
    public Result<List<InterviewJobRoleResponse>> getJobRoles() {
        List<InterviewJobRoleResponse> responses = sysJobRoleService.listActiveOrdered().stream()
                .map(this::buildInterviewJobRoleResponse)
                .toList();
        return Result.success(responses);
    }

    /**
     * 创建面试会话。
     */
    @PostMapping("/session")
    public Result<InterviewSessionResponse> createSession(
            @RequestBody @Validated CreateSessionRequest request,
            Authentication authentication) {
        Long userId = (Long) authentication.getPrincipal();
        log.info("创建面试会话, userId: {}, 岗位: {}, 难度: {}, 模式: {}, feedbackMode: {}",
                userId, request.getJobRole(), request.getDifficulty(), request.getInterviewMode(), request.getFeedbackMode());
        InterviewSessionResponse response = interviewService.createSession(userId, request);
        return Result.success(response);
    }

    /**
     * 发送消息并通过 SSE 返回流式回复。
     */
    @PostMapping("/session/{sessionId}/message/stream")
    public ResponseBodyEmitter streamMessage(
            @PathVariable String sessionId,
            @RequestBody @Validated SendMessageRequest request,
            Authentication authentication) {
        Long userId = (Long) authentication.getPrincipal();
        log.info("收到流式消息请求, userId: {}, sessionId: {}, requestFeedbackMode: {}",
                userId, sessionId, request.getFeedbackMode());

        ResponseBodyEmitter emitter = new ResponseBodyEmitter(120_000L);
        AtomicBoolean streamClosed = new AtomicBoolean(false);
        // SSE 生命周期回调统一由 service 注册并独占同一份 streamClosed，避免回调被重复注册互相覆盖。
        interviewService.attachStreamLifecycleCallbacks(sessionId, emitter, streamClosed);

        Authentication authenticationForThread = authentication;
        aiAsyncExecutor.execute(() -> {
            try {
                if (authenticationForThread != null) {
                    SecurityContextHolder.getContext().setAuthentication(authenticationForThread);
                }
                if (shouldSkipClosedStream(sessionId, streamClosed)) {
                    return;
                }

                StringBuilder fullReply = new StringBuilder();
                List<InterviewChatLog> chatLogs = interviewService.getChatLogsForStream(sessionId, userId);
                if (shouldSkipClosedStream(sessionId, streamClosed)) {
                    return;
                }

                List<InterviewAiService.ChatMessageItem> history = chatLogs == null
                        ? List.of()
                        : chatLogs.stream()
                        .map(log -> new InterviewAiService.ChatMessageItem(log.getMessageRole(), log.getContent()))
                        .toList();

                interviewService.validateSessionForStream(sessionId, userId);
                if (shouldSkipClosedStream(sessionId, streamClosed)) {
                    return;
                }

                // 先落用户消息，再让服务层按会话归属继续完成流式问答。
                interviewService.saveUserMessage(sessionId, userId, request.getContent());
                if (shouldSkipClosedStream(sessionId, streamClosed)) {
                    return;
                }

                InterviewSession session = interviewService.getSessionByOwner(sessionId, userId);
                String jobRoleCode = session != null ? session.getJobRoleCode() : null;
                Integer difficulty = session != null ? session.getDifficulty() : null;
                String interviewMode = session != null ? session.getInterviewMode() : null;
                Integer interactionType = session != null
                        ? interviewService.resolveInteractionType(session.getInteractionType())
                        : InterviewConstants.INTERACTION_TYPE_TEXT;
                InterviewJobTargetContext jobTargetContext =
                        mockInterviewJobTargetService.getSessionContext(userId, sessionId);
                if (jobTargetContext == null) {
                    jobTargetContext = mockInterviewJobTargetService.resolveLatestResumeContext(userId);
                }
                String resolvedFeedbackMode = interviewService.resolveFeedbackMode(request.getFeedbackMode(), session);
                log.info("流式面试消息配置解析完成, sessionId: {}, requestFeedbackMode: {}, sessionFeedbackMode: {}, resolvedFeedbackMode: {}",
                        sessionId, request.getFeedbackMode(), session == null ? null : session.getFeedbackMode(), resolvedFeedbackMode);

                Publisher<String> publisher = interviewAiService.generateReplyStream(
                        sessionId,
                        history,
                        request.getContent(),
                        jobRoleCode,
                        difficulty,
                        jobTargetContext,
                        resolvedFeedbackMode,
                        interviewMode,
                        interactionType
                );
                interviewService.subscribeAndWriteStream(sessionId, emitter, publisher, fullReply, streamClosed);
            } catch (Exception e) {
                if (!streamClosed.get()) {
                    log.error("流式处理异常, sessionId: {}", sessionId, e);
                    try {
                        emitter.send("event: error\ndata: 系统异常，请稍后重试\n\n");
                    } catch (Exception sendError) {
                        log.error("发送流式错误事件失败, sessionId: {}", sessionId, sendError);
                    }
                    try {
                        emitter.completeWithError(e);
                    } catch (Exception completeError) {
                        log.error("关闭流式连接失败, sessionId: {}", sessionId, completeError);
                    }
                }
            } finally {
                SecurityContextHolder.clearContext();
            }
        });

        return emitter;
    }

    /**
     * 发送消息。
     */
    @PostMapping("/session/{sessionId}/message")
    public Result<SendMessageResponse> sendMessage(
            @PathVariable String sessionId,
            @RequestBody @Validated SendMessageRequest request,
            Authentication authentication) {
        Long userId = (Long) authentication.getPrincipal();
        log.info("发送消息, userId: {}, sessionId: {}, requestFeedbackMode: {}",
                userId, sessionId, request.getFeedbackMode());
        SendMessageResponse response = interviewService.sendMessage(userId, sessionId, request);
        return Result.success(response);
    }

    /**
     * 获取会话详情。
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
     * 结束面试。
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
     * 获取分页历史记录。
     */
    @GetMapping("/history")
    public Result<PageResult<InterviewHistoryResponse>> getHistory(
            @RequestParam(defaultValue = "1") @Min(1) Integer pageNum,
            @RequestParam(defaultValue = "5") @Min(1) @Max(value = 100, message = "每页最多 100 条") Integer pageSize,
            Authentication authentication) {
        Long userId = (Long) authentication.getPrincipal();
        log.info("获取面试历史, userId: {}, pageNum: {}, pageSize: {}", userId, pageNum, pageSize);
        PageResult<InterviewHistoryResponse> result = interviewService.getHistory(userId, pageNum, pageSize);
        return Result.success(result);
    }

    /**
     * 清理当前用户的全部面试历史。
     */
    @DeleteMapping("/history")
    public Result<DataCleanupResponse> clearHistory(Authentication authentication) {
        Long userId = (Long) authentication.getPrincipal();
        log.info("清理面试历史, userId: {}", userId);
        int deletedCount = interviewService.clearHistory(userId);
        return Result.success(new DataCleanupResponse(deletedCount));
    }

    /**
     * 获取不分页历史记录，兼容旧版本。
     */
    @Deprecated
    @GetMapping("/history/all")
    public Result<List<InterviewHistoryResponse>> getAllHistory(Authentication authentication) {
        Long userId = (Long) authentication.getPrincipal();
        log.warn("调用已废弃接口 GET /api/interview/history/all, userId: {}", userId);
        List<InterviewHistoryResponse> list = interviewService.getAllHistory(userId);
        if (list.size() > 100) {
            list = list.subList(0, 100);
        }
        return Result.success(list);
    }

    /**
     * 浏览器断开、超时或主动完成后，都要尽快终止后续 AI 链路。
     * 该控制层只在异步任务里读取 streamClosed 决定是否跳过剩余处理；
     * onTimeout/onCompletion/onError 的注册已交给 InterviewService#attachStreamLifecycleCallbacks 独家管理，
     * 避免 ResponseBodyEmitter "每类回调只保留最后一次注册" 的特性导致回调被覆盖。
     */
    private boolean shouldSkipClosedStream(String sessionId, AtomicBoolean streamClosed) {
        if (!streamClosed.get()) {
            return false;
        }
        log.info("流式面试连接已关闭，跳过剩余处理, sessionId: {}", sessionId);
        return true;
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
