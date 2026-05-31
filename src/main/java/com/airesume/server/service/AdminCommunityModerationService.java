package com.airesume.server.service;

import com.airesume.server.common.constants.CommunityConstants;
import com.airesume.server.common.exception.BusinessException;
import com.airesume.server.common.result.PageResult;
import com.airesume.server.dto.admin.AdminCommunityCommentResponse;
import com.airesume.server.dto.admin.AdminCommunityPostResponse;
import com.airesume.server.entity.CommunityComment;
import com.airesume.server.entity.CommunityPost;
import com.airesume.server.entity.SysUser;
import com.airesume.server.mapper.CommunityCommentMapper;
import com.airesume.server.mapper.CommunityPostMapper;
import com.airesume.server.mapper.SysUserMapper;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * 管理端社区内容审核服务。
 * 作用：提供帖子/评论审核队列查询和通过、拒绝、隐藏操作，避免社区公开区暴露未审核内容。
 */
@Service
@RequiredArgsConstructor
public class AdminCommunityModerationService {

    private final CommunityPostMapper postMapper;
    private final CommunityCommentMapper commentMapper;
    private final SysUserMapper userMapper;

    /**
     * 分页查询社区帖子审核队列。
     */
    public PageResult<AdminCommunityPostResponse> listPosts(
            String reviewStatus, String category, String keyword, Long userId, Integer pageNum, Integer pageSize) {
        int safePageNum = normalizePageNum(pageNum);
        int safePageSize = normalizePageSize(pageSize);

        LambdaQueryWrapper<CommunityPost> wrapper = new LambdaQueryWrapper<>();
        applyReviewStatusFilter(wrapper, reviewStatus, CommunityPost::getReviewStatus);
        if (category != null && !category.isBlank()) {
            wrapper.eq(CommunityPost::getCategory, category.trim());
        }
        if (userId != null) {
            wrapper.eq(CommunityPost::getUserId, userId);
        }
        if (keyword != null && !keyword.isBlank()) {
            String normalizedKeyword = keyword.trim();
            wrapper.and(w -> w.like(CommunityPost::getTitle, normalizedKeyword)
                    .or()
                    .like(CommunityPost::getContent, normalizedKeyword));
        }
        wrapper.orderByAsc(CommunityPost::getReviewStatus)
                .orderByDesc(CommunityPost::getCreateTime);

        Page<CommunityPost> page = postMapper.selectPage(new Page<>(safePageNum, safePageSize), wrapper);
        Map<Long, SysUser> users = batchGetUsers(page.getRecords().stream()
                .map(CommunityPost::getUserId)
                .collect(Collectors.toSet()));

        return PageResult.of(page.getRecords().stream()
                        .map(post -> toPostResponse(post, users.get(post.getUserId())))
                        .toList(),
                page.getTotal(), safePageNum, safePageSize);
    }

    /**
     * 分页查询社区评论审核队列。
     */
    public PageResult<AdminCommunityCommentResponse> listComments(
            String reviewStatus, Long postId, String keyword, Long userId, Integer pageNum, Integer pageSize) {
        int safePageNum = normalizePageNum(pageNum);
        int safePageSize = normalizePageSize(pageSize);

        LambdaQueryWrapper<CommunityComment> wrapper = new LambdaQueryWrapper<>();
        applyReviewStatusFilter(wrapper, reviewStatus, CommunityComment::getReviewStatus);
        if (postId != null) {
            wrapper.eq(CommunityComment::getPostId, postId);
        }
        if (userId != null) {
            wrapper.eq(CommunityComment::getUserId, userId);
        }
        if (keyword != null && !keyword.isBlank()) {
            wrapper.like(CommunityComment::getContent, keyword.trim());
        }
        wrapper.orderByAsc(CommunityComment::getReviewStatus)
                .orderByDesc(CommunityComment::getCreateTime);

        Page<CommunityComment> page = commentMapper.selectPage(new Page<>(safePageNum, safePageSize), wrapper);
        Set<Long> userIds = page.getRecords().stream()
                .map(CommunityComment::getUserId)
                .collect(Collectors.toSet());
        Set<Long> postIds = page.getRecords().stream()
                .map(CommunityComment::getPostId)
                .collect(Collectors.toSet());
        Map<Long, SysUser> users = batchGetUsers(userIds);
        Map<Long, CommunityPost> posts = batchGetPosts(postIds);

        return PageResult.of(page.getRecords().stream()
                        .map(comment -> toCommentResponse(comment, users.get(comment.getUserId()), posts.get(comment.getPostId())))
                        .toList(),
                page.getTotal(), safePageNum, safePageSize);
    }

    /**
     * 审核帖子：通过后可在用户端社区公开展示，拒绝或隐藏后不可见。
     */
    @Transactional(rollbackFor = Exception.class)
    public void reviewPost(Long postId, String reviewStatus, String reviewReason, Long adminUserId) {
        String normalizedStatus = normalizeReviewStatus(reviewStatus);
        CommunityPost post = postMapper.selectById(postId);
        if (post == null) {
            throw new BusinessException("帖子不存在");
        }
        post.setReviewStatus(normalizedStatus);
        post.setReviewReason(normalizeReviewReason(normalizedStatus, reviewReason));
        post.setReviewedBy(adminUserId);
        post.setReviewedTime(LocalDateTime.now());
        postMapper.updateById(post);
    }

    /**
     * 审核评论：评论通过时才计入帖子评论数，隐藏或拒绝已通过评论时同步回退计数。
     */
    @Transactional(rollbackFor = Exception.class)
    public void reviewComment(Long commentId, String reviewStatus, String reviewReason, Long adminUserId) {
        String normalizedStatus = normalizeReviewStatus(reviewStatus);
        CommunityComment comment = commentMapper.selectById(commentId);
        if (comment == null) {
            throw new BusinessException("评论不存在");
        }
        String previousStatus = comment.getReviewStatus();
        comment.setReviewStatus(normalizedStatus);
        comment.setReviewReason(normalizeReviewReason(normalizedStatus, reviewReason));
        comment.setReviewedBy(adminUserId);
        comment.setReviewedTime(LocalDateTime.now());
        commentMapper.updateById(comment);

        if (!CommunityConstants.REVIEW_STATUS_APPROVED.equals(previousStatus)
                && CommunityConstants.REVIEW_STATUS_APPROVED.equals(normalizedStatus)) {
            updateCommentCount(comment.getPostId(), 1);
        } else if (CommunityConstants.REVIEW_STATUS_APPROVED.equals(previousStatus)
                && !CommunityConstants.REVIEW_STATUS_APPROVED.equals(normalizedStatus)) {
            updateCommentCount(comment.getPostId(), -1);
        }
    }

    private <T> void applyReviewStatusFilter(
            LambdaQueryWrapper<T> wrapper,
            String reviewStatus,
            com.baomidou.mybatisplus.core.toolkit.support.SFunction<T, String> column) {
        if (reviewStatus == null || reviewStatus.isBlank() || "all".equals(reviewStatus)) {
            return;
        }
        wrapper.eq(column, normalizeReviewStatus(reviewStatus));
    }

    private String normalizeReviewStatus(String reviewStatus) {
        String normalized = reviewStatus == null ? "" : reviewStatus.trim();
        if (CommunityConstants.REVIEW_STATUS_PENDING.equals(normalized)
                || CommunityConstants.REVIEW_STATUS_APPROVED.equals(normalized)
                || CommunityConstants.REVIEW_STATUS_REJECTED.equals(normalized)
                || CommunityConstants.REVIEW_STATUS_HIDDEN.equals(normalized)) {
            return normalized;
        }
        throw new BusinessException("无效的审核状态");
    }

    private String normalizeReviewReason(String reviewStatus, String reviewReason) {
        String normalized = reviewReason == null ? null : reviewReason.trim();
        if (CommunityConstants.REVIEW_STATUS_APPROVED.equals(reviewStatus)) {
            return normalized == null || normalized.isBlank() ? null : normalized;
        }
        if (normalized == null || normalized.isBlank()) {
            throw new BusinessException("拒绝或隐藏内容时必须填写原因");
        }
        return normalized;
    }

    private void updateCommentCount(Long postId, int delta) {
        LambdaUpdateWrapper<CommunityPost> wrapper = new LambdaUpdateWrapper<>();
        wrapper.eq(CommunityPost::getId, postId)
                .setSql(delta > 0
                        ? "comment_count = comment_count + 1"
                        : "comment_count = GREATEST(comment_count - 1, 0)");
        postMapper.update(null, wrapper);
    }

    private int normalizePageNum(Integer pageNum) {
        return pageNum == null || pageNum < 1 ? 1 : pageNum;
    }

    private int normalizePageSize(Integer pageSize) {
        if (pageSize == null || pageSize < 1) {
            return 20;
        }
        return Math.min(pageSize, 100);
    }

    private Map<Long, SysUser> batchGetUsers(Set<Long> userIds) {
        if (userIds.isEmpty()) {
            return Collections.emptyMap();
        }
        return userMapper.selectList(new LambdaQueryWrapper<SysUser>().in(SysUser::getId, userIds))
                .stream()
                .collect(Collectors.toMap(SysUser::getId, user -> user));
    }

    private Map<Long, CommunityPost> batchGetPosts(Set<Long> postIds) {
        if (postIds.isEmpty()) {
            return Collections.emptyMap();
        }
        return postMapper.selectList(new LambdaQueryWrapper<CommunityPost>().in(CommunityPost::getId, postIds))
                .stream()
                .collect(Collectors.toMap(CommunityPost::getId, post -> post));
    }

    private AdminCommunityPostResponse toPostResponse(CommunityPost post, SysUser author) {
        return AdminCommunityPostResponse.builder()
                .id(post.getId())
                .userId(post.getUserId())
                .authorName(formatUserName(author))
                .category(post.getCategory())
                .title(post.getTitle())
                .content(post.getContent())
                .images(post.getImages())
                .reviewStatus(post.getReviewStatus())
                .reviewReason(post.getReviewReason())
                .reviewedBy(post.getReviewedBy())
                .reviewedTime(post.getReviewedTime())
                .createTime(post.getCreateTime())
                .build();
    }

    private AdminCommunityCommentResponse toCommentResponse(CommunityComment comment, SysUser author, CommunityPost post) {
        return AdminCommunityCommentResponse.builder()
                .id(comment.getId())
                .postId(comment.getPostId())
                .userId(comment.getUserId())
                .authorName(formatUserName(author))
                .postTitle(post == null ? null : post.getTitle())
                .content(comment.getContent())
                .images(comment.getImages())
                .reviewStatus(comment.getReviewStatus())
                .reviewReason(comment.getReviewReason())
                .reviewedBy(comment.getReviewedBy())
                .reviewedTime(comment.getReviewedTime())
                .createTime(comment.getCreateTime())
                .build();
    }

    private String formatUserName(SysUser user) {
        if (user == null) {
            return "匿名用户";
        }
        return user.getNickname() != null ? user.getNickname() : user.getUsername();
    }
}
