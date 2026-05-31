package com.airesume.server.service;

import com.airesume.server.common.constants.CommunityConstants;
import com.airesume.server.common.exception.BusinessException;
import com.airesume.server.entity.CommunityComment;
import com.airesume.server.entity.CommunityPost;
import com.airesume.server.mapper.CommunityCommentMapper;
import com.airesume.server.mapper.CommunityPostMapper;
import com.airesume.server.mapper.SysUserMapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * 管理端社区审核服务测试。
 */
@ExtendWith(MockitoExtension.class)
class AdminCommunityModerationServiceTest {

    @Mock
    private CommunityPostMapper postMapper;

    @Mock
    private CommunityCommentMapper commentMapper;

    @Mock
    private SysUserMapper userMapper;

    private AdminCommunityModerationService service;

    @BeforeEach
    void setUp() {
        service = new AdminCommunityModerationService(postMapper, commentMapper, userMapper);
    }

    @Test
    void shouldRequireReasonWhenRejectingPost() {
        CommunityPost post = new CommunityPost();
        post.setId(2001L);
        post.setReviewStatus(CommunityConstants.REVIEW_STATUS_PENDING);
        when(postMapper.selectById(2001L)).thenReturn(post);

        BusinessException exception = assertThrows(BusinessException.class,
                () -> service.reviewPost(2001L, CommunityConstants.REVIEW_STATUS_REJECTED, " ", 9001L));

        assertEquals("拒绝或隐藏内容时必须填写原因", exception.getMessage());
        verify(postMapper, never()).updateById(any(CommunityPost.class));
    }

    @Test
    void shouldIncrementCommentCountWhenPendingCommentApproved() {
        CommunityComment comment = new CommunityComment();
        comment.setId(3001L);
        comment.setPostId(2001L);
        comment.setReviewStatus(CommunityConstants.REVIEW_STATUS_PENDING);
        when(commentMapper.selectById(3001L)).thenReturn(comment);

        service.reviewComment(3001L, CommunityConstants.REVIEW_STATUS_APPROVED, "", 9001L);

        ArgumentCaptor<CommunityComment> commentCaptor = ArgumentCaptor.forClass(CommunityComment.class);
        verify(commentMapper).updateById(commentCaptor.capture());
        assertEquals(CommunityConstants.REVIEW_STATUS_APPROVED, commentCaptor.getValue().getReviewStatus());
        assertEquals(9001L, commentCaptor.getValue().getReviewedBy());

        // 评论通过审核后才计入帖子评论数，避免未公开评论影响前台统计。
        verify(postMapper).update(isNull(), any(LambdaUpdateWrapper.class));
    }

    @Test
    void shouldDecrementCommentCountWhenApprovedCommentHidden() {
        CommunityComment comment = new CommunityComment();
        comment.setId(3001L);
        comment.setPostId(2001L);
        comment.setReviewStatus(CommunityConstants.REVIEW_STATUS_APPROVED);
        when(commentMapper.selectById(3001L)).thenReturn(comment);

        service.reviewComment(3001L, CommunityConstants.REVIEW_STATUS_HIDDEN, "不适合公开展示", 9001L);

        ArgumentCaptor<CommunityComment> commentCaptor = ArgumentCaptor.forClass(CommunityComment.class);
        verify(commentMapper).updateById(commentCaptor.capture());
        assertEquals(CommunityConstants.REVIEW_STATUS_HIDDEN, commentCaptor.getValue().getReviewStatus());
        assertEquals("不适合公开展示", commentCaptor.getValue().getReviewReason());

        // 已通过评论被隐藏时同步回退评论数，保持公开统计与可见评论一致。
        verify(postMapper).update(isNull(), any(LambdaUpdateWrapper.class));
    }
}
