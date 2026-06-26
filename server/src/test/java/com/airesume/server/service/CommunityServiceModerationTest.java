package com.airesume.server.service;

import com.airesume.server.common.exception.BusinessException;
import com.airesume.server.common.result.PageResult;
import com.airesume.server.dto.community.CreateCommentRequest;
import com.airesume.server.dto.community.CreateCommunityContentResponse;
import com.airesume.server.dto.community.CreatePostRequest;
import com.airesume.server.dto.community.PostVO;
import com.airesume.server.entity.CommunityComment;
import com.airesume.server.entity.CommunityPost;
import com.airesume.server.entity.SysUser;
import com.airesume.server.mapper.CommunityCommentMapper;
import com.airesume.server.mapper.CommunityPostFavoriteMapper;
import com.airesume.server.mapper.CommunityPostLikeMapper;
import com.airesume.server.mapper.CommunityPostMapper;
import com.airesume.server.mapper.InterviewSessionMapper;
import com.airesume.server.mapper.SysUserMapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * 社区内容审核回归测试。
 * 作用：锁定自动审核分流、严重违规直接拒绝、用户端只展示已通过内容的安全边界。
 */
@ExtendWith(MockitoExtension.class)
class CommunityServiceModerationTest {

    @Mock
    private CommunityPostMapper postMapper;

    @Mock
    private CommunityCommentMapper commentMapper;

    @Mock
    private CommunityPostLikeMapper likeMapper;

    @Mock
    private CommunityPostFavoriteMapper favoriteMapper;

    @Mock
    private SysUserMapper userMapper;

    @Mock
    private InterviewSessionMapper interviewSessionMapper;

    private CommunityService communityService;

    @BeforeEach
    void setUp() {
        communityService = new CommunityService(
                postMapper,
                commentMapper,
                likeMapper,
                favoriteMapper,
                userMapper,
                interviewSessionMapper,
                new ObjectMapper(),
                new CommunityTextModerationService()
        );
    }

    @Test
    void shouldAutoApproveLowRiskTextOnlyPost() {
        CreatePostRequest request = new CreatePostRequest();
        request.setCategory("interview_exp");
        request.setTitle("一次 Java 面试复盘");
        request.setContent("这次面试主要考察了并发和数据库索引。");

        CreateCommunityContentResponse response = communityService.createPost(1001L, request);

        ArgumentCaptor<CommunityPost> captor = ArgumentCaptor.forClass(CommunityPost.class);
        verify(postMapper).insert(captor.capture());
        assertEquals("approved", ReflectionTestUtils.getField(captor.getValue(), "reviewStatus"));
        assertEquals(null, ReflectionTestUtils.getField(captor.getValue(), "reviewReason"));
        assertEquals("approved", response.getReviewStatus());
    }

    @Test
    void shouldKeepImagePostPendingReview() {
        CreatePostRequest request = new CreatePostRequest();
        request.setCategory("interview_exp");
        request.setTitle("面试题截图");
        request.setContent("这是一张面试复盘截图。");
        request.setImages(List.of("https://example.com/review.png"));

        communityService.createPost(1001L, request);

        ArgumentCaptor<CommunityPost> captor = ArgumentCaptor.forClass(CommunityPost.class);
        verify(postMapper).insert(captor.capture());
        assertEquals("pending", ReflectionTestUtils.getField(captor.getValue(), "reviewStatus"));
        assertEquals("包含图片，需人工复核", ReflectionTestUtils.getField(captor.getValue(), "reviewReason"));
    }

    @Test
    void shouldKeepSuspiciousTextOnlyPostPendingReview() {
        CreatePostRequest request = new CreatePostRequest();
        request.setCategory("referral");
        request.setTitle("内推沟通");
        request.setContent("可以私聊我联系方式。");

        communityService.createPost(1001L, request);

        ArgumentCaptor<CommunityPost> captor = ArgumentCaptor.forClass(CommunityPost.class);
        verify(postMapper).insert(captor.capture());
        assertEquals("pending", ReflectionTestUtils.getField(captor.getValue(), "reviewStatus"));
        assertEquals("疑似风险词命中，需人工复核", ReflectionTestUtils.getField(captor.getValue(), "reviewReason"));
    }

    @Test
    void shouldKeepSuspiciousTextOnlyCommentPendingAndNotIncreaseCount() {
        CommunityPost approvedPost = buildPost(2001L, 1002L, "approved");
        when(postMapper.selectById(2001L)).thenReturn(approvedPost);

        CreateCommentRequest request = new CreateCommentRequest();
        request.setContent("可以私聊联系方式，或者加微信继续沟通。");

        CreateCommunityContentResponse response = communityService.createComment(1001L, 2001L, request);

        ArgumentCaptor<CommunityComment> captor = ArgumentCaptor.forClass(CommunityComment.class);
        verify(commentMapper).insert(captor.capture());
        assertEquals("pending", ReflectionTestUtils.getField(captor.getValue(), "reviewStatus"));
        assertEquals("疑似风险词命中，需人工复核", ReflectionTestUtils.getField(captor.getValue(), "reviewReason"));
        assertEquals("pending", response.getReviewStatus());
        verify(postMapper, never()).update(any(), any());
    }

    @Test
    void shouldRejectSevereIllegalPostBeforeInsert() {
        CreatePostRequest request = new CreatePostRequest();
        request.setCategory("interview_exp");
        request.setTitle("成人视频资源");
        request.setContent("这里发布色情成人视频和约炮引流内容。");

        BusinessException exception = assertThrows(BusinessException.class,
                () -> communityService.createPost(1001L, request));

        assertEquals("内容包含违规信息，请修改后再发布", exception.getMessage());
        verify(postMapper, never()).insert(any(CommunityPost.class));
    }

    @Test
    void shouldRejectSevereIllegalImagePostBeforeInsert() {
        CreatePostRequest request = new CreatePostRequest();
        request.setCategory("interview_exp");
        request.setTitle("面试截图");
        request.setContent("这里发布色情成人视频和约炮引流内容。");
        request.setImages(List.of("https://example.com/illegal.png"));

        BusinessException exception = assertThrows(BusinessException.class,
                () -> communityService.createPost(1001L, request));

        assertEquals("内容包含违规信息，请修改后再发布", exception.getMessage());
        verify(postMapper, never()).insert(any(CommunityPost.class));
    }

    @Test
    void shouldAutoApproveLowRiskTextOnlyCommentAndIncreaseCount() {
        CommunityPost approvedPost = buildPost(2001L, 1002L, "approved");
        when(postMapper.selectById(2001L)).thenReturn(approvedPost);

        CreateCommentRequest request = new CreateCommentRequest();
        request.setContent("我也遇到过类似的面试追问。");

        CreateCommunityContentResponse response = communityService.createComment(1001L, 2001L, request);

        ArgumentCaptor<CommunityComment> captor = ArgumentCaptor.forClass(CommunityComment.class);
        verify(commentMapper).insert(captor.capture());
        assertEquals("approved", ReflectionTestUtils.getField(captor.getValue(), "reviewStatus"));
        assertEquals("approved", response.getReviewStatus());
        verify(postMapper).update(any(), any());
    }

    @Test
    void shouldKeepImageCommentPendingAndNotIncreaseCount() {
        CommunityPost approvedPost = buildPost(2001L, 1002L, "approved");
        when(postMapper.selectById(2001L)).thenReturn(approvedPost);

        CreateCommentRequest request = new CreateCommentRequest();
        request.setContent("");
        request.setImages(List.of("https://example.com/comment.png"));

        communityService.createComment(1001L, 2001L, request);

        ArgumentCaptor<CommunityComment> captor = ArgumentCaptor.forClass(CommunityComment.class);
        verify(commentMapper).insert(captor.capture());
        assertEquals("pending", ReflectionTestUtils.getField(captor.getValue(), "reviewStatus"));
        assertEquals("包含图片，需人工复核", ReflectionTestUtils.getField(captor.getValue(), "reviewReason"));
        verify(postMapper, never()).update(any(), any());
    }

    @Test
    void shouldOnlyExposeApprovedPostsInPublicList() {
        CommunityPost approvedPost = buildPost(2001L, 1001L, "approved");
        CommunityPost pendingPost = buildPost(2002L, 1002L, "pending");
        Page<CommunityPost> page = new Page<>(1, 10);
        page.setRecords(List.of(approvedPost, pendingPost));
        page.setTotal(2);
        when(postMapper.selectPage(any(Page.class), any())).thenReturn(page);
        when(userMapper.selectList(any())).thenReturn(List.of(buildUser(1001L), buildUser(1002L)));
        when(likeMapper.selectList(any())).thenReturn(Collections.emptyList());
        when(favoriteMapper.selectList(any())).thenReturn(Collections.emptyList());

        PageResult<PostVO> result = communityService.listPosts(null, "latest", 1, 10, 1001L);

        assertEquals(1, result.getList().size());
        assertEquals(2001L, result.getList().get(0).getId());
    }

    private CommunityPost buildPost(Long postId, Long userId, String reviewStatus) {
        CommunityPost post = new CommunityPost();
        post.setId(postId);
        post.setUserId(userId);
        post.setCategory("interview_exp");
        post.setTitle("帖子标题");
        post.setContent("帖子内容");
        post.setLikeCount(0);
        post.setCommentCount(0);
        post.setIsDeleted(0);
        post.setCreateTime(LocalDateTime.now());
        ReflectionTestUtils.setField(post, "reviewStatus", reviewStatus);
        return post;
    }

    private SysUser buildUser(Long userId) {
        SysUser user = new SysUser();
        user.setId(userId);
        user.setUsername("user" + userId);
        user.setNickname("用户" + userId);
        return user;
    }
}
