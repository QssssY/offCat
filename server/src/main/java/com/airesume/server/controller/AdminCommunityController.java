package com.airesume.server.controller;

import com.airesume.server.common.constants.UserRoleConstants;
import com.airesume.server.common.exception.BusinessException;
import com.airesume.server.common.result.PageResult;
import com.airesume.server.common.result.Result;
import com.airesume.server.dto.admin.AdminCommunityCommentResponse;
import com.airesume.server.dto.admin.AdminCommunityPostResponse;
import com.airesume.server.dto.admin.AdminCommunityReviewRequest;
import com.airesume.server.entity.SysUser;
import com.airesume.server.service.AdminCommunityModerationService;
import com.airesume.server.service.SysUserService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.Authentication;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * 管理端社区审核控制器。
 * 作用：让管理员集中处理待审帖子和评论，避免未审核内容进入用户端公开社区。
 */
@Slf4j
@Validated
@RestController
@RequestMapping("/api/admin/community")
@RequiredArgsConstructor
public class AdminCommunityController {

    private final AdminCommunityModerationService moderationService;
    private final SysUserService sysUserService;

    /**
     * 查询帖子审核队列。
     */
    @GetMapping("/posts")
    public Result<PageResult<AdminCommunityPostResponse>> listPosts(
            Authentication authentication,
            @RequestParam(required = false) String reviewStatus,
            @RequestParam(required = false) String category,
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) Long userId,
            @RequestParam(defaultValue = "1") @Min(1) Integer page,
            @RequestParam(defaultValue = "20") @Min(1) @Max(100) Integer size) {
        Long adminUserId = resolveAdminUserId(authentication);
        log.info("[管理端社区] 查询帖子审核队列, adminUserId: {}, reviewStatus: {}, page: {}", adminUserId, reviewStatus, page);
        return Result.success(moderationService.listPosts(reviewStatus, category, keyword, userId, page, size));
    }

    /**
     * 查询评论审核队列。
     */
    @GetMapping("/comments")
    public Result<PageResult<AdminCommunityCommentResponse>> listComments(
            Authentication authentication,
            @RequestParam(required = false) String reviewStatus,
            @RequestParam(required = false) Long postId,
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) Long userId,
            @RequestParam(defaultValue = "1") @Min(1) Integer page,
            @RequestParam(defaultValue = "20") @Min(1) @Max(100) Integer size) {
        Long adminUserId = resolveAdminUserId(authentication);
        log.info("[管理端社区] 查询评论审核队列, adminUserId: {}, reviewStatus: {}, page: {}", adminUserId, reviewStatus, page);
        return Result.success(moderationService.listComments(reviewStatus, postId, keyword, userId, page, size));
    }

    /**
     * 审核帖子。
     */
    @PutMapping("/posts/{postId}/review")
    public Result<Void> reviewPost(
            Authentication authentication,
            @PathVariable Long postId,
            @Valid @RequestBody AdminCommunityReviewRequest request) {
        Long adminUserId = resolveAdminUserId(authentication);
        moderationService.reviewPost(postId, request.getReviewStatus(), request.getReviewReason(), adminUserId);
        return Result.success("帖子审核状态已更新", null);
    }

    /**
     * 审核评论。
     */
    @PutMapping("/comments/{commentId}/review")
    public Result<Void> reviewComment(
            Authentication authentication,
            @PathVariable Long commentId,
            @Valid @RequestBody AdminCommunityReviewRequest request) {
        Long adminUserId = resolveAdminUserId(authentication);
        moderationService.reviewComment(commentId, request.getReviewStatus(), request.getReviewReason(), adminUserId);
        return Result.success("评论审核状态已更新", null);
    }

    /**
     * 管理端接口再次校验角色，避免仅依赖路由前缀权限造成越权风险。
     */
    private Long resolveAdminUserId(Authentication authentication) {
        Long userId = (Long) authentication.getPrincipal();
        SysUser user = sysUserService.getById(userId);
        if (user == null || user.getRole() == null || user.getRole() != UserRoleConstants.ROLE_ADMIN) {
            throw new BusinessException("无权限访问");
        }
        return userId;
    }
}
