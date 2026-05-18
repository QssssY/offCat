package com.airesume.server.controller;

import com.airesume.server.common.result.Result;
import com.airesume.server.dto.feedback.UserFeedbackCreateRequest;
import com.airesume.server.entity.UserFeedback;
import com.airesume.server.service.UserFeedbackService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 用户侧问题反馈/建议接口。
 */
@Slf4j
@RestController
@RequestMapping("/api/user/feedback")
@RequiredArgsConstructor
public class UserFeedbackController {

    private final UserFeedbackService userFeedbackService;

    /**
     * 提交反馈。
     * 说明：反馈默认进入待处理状态，管理端后续负责受理和记录处理备注。
     */
    @PostMapping
    public Result<Long> createFeedback(@Valid @RequestBody UserFeedbackCreateRequest request,
                                       Authentication authentication) {
        Long userId = (Long) authentication.getPrincipal();
        UserFeedback feedback = new UserFeedback();
        feedback.setUserId(userId);
        feedback.setType(request.getType());
        feedback.setTitle(request.getTitle().trim());
        feedback.setContent(request.getContent().trim());
        feedback.setContact(request.getContact() == null ? null : request.getContact().trim());
        feedback.setStatus(0);
        userFeedbackService.save(feedback);
        log.info("User submitted feedback, userId: {}, feedbackId: {}, type: {}", userId, feedback.getId(), feedback.getType());
        return Result.success("反馈已提交", feedback.getId());
    }
}
