package com.airesume.server.controller;

import com.airesume.server.common.result.Result;
import com.airesume.server.common.util.BatchValidator;
import com.airesume.server.dto.feedback.AdminFeedbackResponse;
import com.airesume.server.dto.feedback.AdminFeedbackStatusUpdateRequest;
import com.airesume.server.entity.SysUser;
import com.airesume.server.entity.UserFeedback;
import com.airesume.server.service.SysUserService;
import com.airesume.server.service.UserFeedbackService;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 管理端问题反馈/建议受理接口。
 */
@Slf4j
@Validated
@RestController
@RequestMapping("/api/admin/feedback")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')")
public class AdminFeedbackController {

    private final UserFeedbackService userFeedbackService;
    private final SysUserService sysUserService;

    /**
     * 分页查询用户反馈。
     * 说明：支持按类型、状态、用户筛选，方便运营侧聚焦待处理问题。
     */
    @GetMapping
    public Result<Map<String, Object>> getFeedbackList(
            @RequestParam(defaultValue = "1") @Min(1) int page,
            @RequestParam(defaultValue = "20") @Min(1) @Max(100) int size,
            @RequestParam(required = false) String type,
            @RequestParam(required = false) Integer status,
            @RequestParam(required = false) Long userId,
            Authentication authentication) {
        log.info("Admin get feedback list, page: {}, size: {}, type: {}, status: {}, userId: {}",
                page, size, type, status, userId);
        Page<UserFeedback> pageParam = new Page<>(page, Math.min(size, 100));
        LambdaQueryWrapper<UserFeedback> wrapper = new LambdaQueryWrapper<>();
        if (type != null && !type.isBlank()) {
            wrapper.eq(UserFeedback::getType, type.trim());
        }
        if (status != null) {
            wrapper.eq(UserFeedback::getStatus, status);
        }
        if (userId != null) {
            wrapper.eq(UserFeedback::getUserId, userId);
        }
        wrapper.orderByDesc(UserFeedback::getCreateTime);
        Page<UserFeedback> result = userFeedbackService.page(pageParam, wrapper);
        List<AdminFeedbackResponse> records = result.getRecords().stream()
                .map(this::buildResponse)
                .collect(Collectors.toList());

        Map<String, Object> data = new HashMap<>();
        data.put("records", records);
        data.put("total", (int) result.getTotal());
        data.put("page", (int) result.getCurrent());
        data.put("size", (int) result.getSize());
        return Result.success(data);
    }

    @GetMapping("/{id}")
    public Result<AdminFeedbackResponse> getFeedbackDetail(@PathVariable Long id,
                                                           Authentication authentication) {
        UserFeedback feedback = userFeedbackService.getById(id);
        if (feedback == null) {
            return Result.error("反馈记录不存在");
        }
        return Result.success(buildResponse(feedback));
    }

    /**
     * 更新反馈处理状态。
     * 说明：每次处理都记录当前管理员和处理时间，形成最小处理闭环。
     */
    @PutMapping("/{id}/status")
    public Result<Void> updateFeedbackStatus(@PathVariable Long id,
                                             @Valid @RequestBody AdminFeedbackStatusUpdateRequest request,
                                             Authentication authentication) {
        UserFeedback feedback = userFeedbackService.getById(id);
        if (feedback == null) {
            return Result.error("反馈记录不存在");
        }
        Long operatorUserId = (Long) authentication.getPrincipal();
        feedback.setStatus(request.getStatus());
        feedback.setAdminRemark(request.getAdminRemark() == null ? null : request.getAdminRemark().trim());
        feedback.setHandledBy(operatorUserId);
        feedback.setHandledAt(LocalDateTime.now());
        userFeedbackService.updateById(feedback);
        log.info("Admin updated feedback status, feedbackId: {}, status: {}, operatorUserId: {}",
                id, request.getStatus(), operatorUserId);
        return Result.success("反馈状态已更新", null);
    }

    @PostMapping("/batch-delete")
    public Result<Void> deleteFeedbackBatch(@RequestBody List<Long> ids,
                                            Authentication authentication) {
        List<Long> safeIds = BatchValidator.validate(ids);
        log.info("Admin batch delete feedback, ids: {}", safeIds);
        userFeedbackService.removeByIds(safeIds);
        return Result.success("反馈已批量删除", null);
    }

    private AdminFeedbackResponse buildResponse(UserFeedback feedback) {
        SysUser user = sysUserService.getById(feedback.getUserId());
        SysUser handler = feedback.getHandledBy() == null ? null : sysUserService.getById(feedback.getHandledBy());
        return AdminFeedbackResponse.builder()
                .id(feedback.getId())
                .userId(feedback.getUserId())
                .username(user != null ? user.getUsername() : "未知")
                .type(feedback.getType())
                .typeDesc(getTypeDesc(feedback.getType()))
                .title(feedback.getTitle())
                .content(feedback.getContent())
                .contact(feedback.getContact())
                .status(feedback.getStatus())
                .statusDesc(getStatusDesc(feedback.getStatus()))
                .adminRemark(feedback.getAdminRemark())
                .handledBy(feedback.getHandledBy())
                .handlerName(handler != null ? handler.getUsername() : "未处理")
                .handledAt(feedback.getHandledAt())
                .createTime(feedback.getCreateTime())
                .updateTime(feedback.getUpdateTime())
                .build();
    }

    private String getTypeDesc(String type) {
        return switch (type) {
            case "bug" -> "问题反馈";
            case "suggestion" -> "功能建议";
            case "experience" -> "体验问题";
            case "other" -> "其他";
            default -> "未知";
        };
    }

    private String getStatusDesc(Integer status) {
        if (status == null) return "未知";
        return switch (status) {
            case 0 -> "待处理";
            case 1 -> "处理中";
            case 2 -> "已处理";
            case 3 -> "已关闭";
            default -> "未知";
        };
    }
}
