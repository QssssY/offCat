package com.airesume.server.service;

import com.airesume.server.dto.community.ReceivedInteractionVO;
import com.airesume.server.entity.CommunityComment;
import com.airesume.server.entity.CommunityPost;
import com.airesume.server.entity.CommunityPostFavorite;
import com.airesume.server.entity.CommunityPostLike;
import com.airesume.server.entity.SysUser;
import com.airesume.server.mapper.CommunityCommentMapper;
import com.airesume.server.mapper.CommunityPostFavoriteMapper;
import com.airesume.server.mapper.CommunityPostLikeMapper;
import com.airesume.server.mapper.CommunityPostMapper;
import com.airesume.server.mapper.SysUserMapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * BDD-style tests for Feature 1: 收到的互动信息分页查询
 * Covers issues #1 (full-table scan bug) and #10 (pagination correctness).
 *
 * Tests are structured with Given / When / Then comments
 * to describe the expected behaviour for each scenario.
 */
@ExtendWith(MockitoExtension.class)
class CommunityServiceInteractionTest {

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
    private ObjectMapper objectMapper;

    private CommunityService communityService;

    // Fixed user IDs used across tests
    private static final Long USER_A_ID = 1001L;
    private static final Long USER_B_ID = 2001L;
    private static final Long USER_C_ID = 3001L;
    private static final Long USER_D_ID = 4001L;
    private static final Long USER_E_ID = 5001L;

    @BeforeEach
    void setUp() {
        communityService = new CommunityService(
                postMapper, commentMapper, likeMapper, favoriteMapper, userMapper, objectMapper
        );
    }

    // ==================== Helper methods ====================

    private CommunityPost buildPost(Long postId, Long userId) {
        CommunityPost post = new CommunityPost();
        post.setId(postId);
        post.setUserId(userId);
        post.setCategory("interview_exp");
        post.setContent("Test post content for post " + postId);
        post.setLikeCount(0);
        post.setCommentCount(0);
        post.setCreateTime(LocalDateTime.now().minusDays(1));
        post.setUpdateTime(LocalDateTime.now());
        post.setIsDeleted(0);
        return post;
    }

    private CommunityPostLike buildLike(Long likeId, Long postId, Long userId) {
        CommunityPostLike like = new CommunityPostLike();
        like.setId(likeId);
        like.setPostId(postId);
        like.setUserId(userId);
        like.setCreateTime(LocalDateTime.now().minusHours(5));
        return like;
    }

    private CommunityComment buildComment(Long commentId, Long postId, Long userId,
                                           Long parentCommentId, Long replyToUserId) {
        CommunityComment comment = new CommunityComment();
        comment.setId(commentId);
        comment.setPostId(postId);
        comment.setUserId(userId);
        comment.setParentCommentId(parentCommentId);
        comment.setReplyToUserId(replyToUserId);
        comment.setContent("Comment " + commentId);
        comment.setCreateTime(LocalDateTime.now().minusHours(3));
        comment.setUpdateTime(LocalDateTime.now());
        comment.setIsDeleted(0);
        return comment;
    }

    private CommunityPostFavorite buildFavorite(Long favId, Long postId, Long userId) {
        CommunityPostFavorite fav = new CommunityPostFavorite();
        fav.setId(favId);
        fav.setPostId(postId);
        fav.setUserId(userId);
        fav.setCreateTime(LocalDateTime.now().minusHours(2));
        return fav;
    }

    private SysUser buildUser(Long userId, String username, String nickname) {
        SysUser user = new SysUser();
        user.setId(userId);
        user.setUsername(username);
        user.setNickname(nickname);
        return user;
    }

    private <T> Page<T> pageOf(long pageNum, long pageSize, long total, List<T> records) {
        Page<T> page = new Page<>(pageNum, pageSize);
        page.setTotal(total);
        page.setRecords(records);
        return page;
    }

    // ==========================================================
    //  Feature 1: 收到的互动信息分页查询 (listReceivedInteractions)
    // ==========================================================

    @Nested
    @DisplayName("Feature 1: 收到的互动信息分页查询")
    class ListReceivedInteractionsTest {

        @Test
        @DisplayName("Scenario 1.1 [P0] - 查询第1页收到的互动信息")
        void shouldReturnFirstPageOfAllInteractionTypes() {
            // ====== Given ======
            // User A has 5 posts
            List<CommunityPost> myPosts = new ArrayList<>();
            for (long i = 1; i <= 5; i++) {
                myPosts.add(buildPost(i * 100L, USER_A_ID));
            }

            // Users B liked all 5 posts
            List<CommunityPostLike> allLikes = new ArrayList<>();
            for (long i = 0; i < 5; i++) {
                allLikes.add(buildLike(1000L + i, myPosts.get((int) i).getId(), USER_B_ID));
            }

            // User C commented on 3 posts (top-level, parentCommentId = null)
            List<CommunityComment> allComments = new ArrayList<>();
            for (int i = 0; i < 3; i++) {
                allComments.add(buildComment(
                        2000L + i, myPosts.get(i).getId(), USER_C_ID,
                        null, null
                ));
            }

            // User D replied to User A's 2 comments
            List<CommunityComment> allReplies = new ArrayList<>();
            for (int i = 0; i < 2; i++) {
                allReplies.add(buildComment(
                        3000L + i, myPosts.get(i).getId(), USER_D_ID,
                        2000L + i, USER_A_ID
                ));
            }

            // User E favorited 4 posts
            List<CommunityPostFavorite> allFavorites = new ArrayList<>();
            for (int i = 0; i < 4; i++) {
                allFavorites.add(buildFavorite(4000L + i, myPosts.get(i).getId(), USER_E_ID));
            }

            // Related posts for display
            List<CommunityPost> relatedPosts = new ArrayList<>(myPosts);

            // Users for display
            List<SysUser> users = List.of(
                    buildUser(USER_B_ID, "userB", "User B"),
                    buildUser(USER_C_ID, "userC", "User C"),
                    buildUser(USER_D_ID, "userD", "User D"),
                    buildUser(USER_E_ID, "userE", "User E")
            );

            when(postMapper.selectList(any())).thenReturn(myPosts).thenReturn(relatedPosts);
            when(likeMapper.selectPage(any(Page.class), any())).thenReturn(pageOf(1, 10, 5, allLikes));
            when(commentMapper.selectPage(any(Page.class), any()))
                    .thenReturn(pageOf(1, 10, 3, allComments))
                    .thenReturn(pageOf(1, 10, 2, allReplies));
            when(commentMapper.selectList(any())).thenReturn(Collections.emptyList());
            when(favoriteMapper.selectPage(any(Page.class), any())).thenReturn(pageOf(1, 10, 4, allFavorites));
            when(userMapper.selectList(any())).thenReturn(users);

            // ====== When ======
            ReceivedInteractionVO result = communityService.listReceivedInteractions(USER_A_ID, 1, 10);

            // ====== Then ======
            assertNotNull(result);
            assertEquals(5, result.getTotalLikes(), "totalLikes should be 5 (B liked 5 posts)");
            assertEquals(3, result.getTotalComments(), "totalComments should be 3 (C commented on 3)");
            assertEquals(2, result.getTotalReplies(), "totalReplies should be 2 (D replied to 2)");
            assertEquals(4, result.getTotalFavorites(), "totalFavorites should be 4 (E favorited 4)");

            assertTrue(result.getLikes().size() <= 10, "likes list should not exceed page size 10");
            assertEquals(5, result.getLikes().size(), "likes list should contain all 5 likes (page 1, size 10)");
            assertEquals(3, result.getComments().size(), "comments list should contain all 3 comments");
            assertEquals(2, result.getReplies().size(), "replies list should contain all 2 replies");
            assertEquals(4, result.getFavorites().size(), "favorites list should contain all 4 favorites");

            verify(likeMapper).selectPage(any(Page.class), any());
            verify(commentMapper, times(2)).selectPage(any(Page.class), any());
            verify(favoriteMapper).selectPage(any(Page.class), any());
        }

        @Test
        @DisplayName("Scenario 1.2 [P0] - 查询第2页互动信息（点赞分页）")
        void shouldReturnSecondPageWithRemainingLikes() {
            // ====== Given ======
            // User A has 1 post
            CommunityPost myPost = buildPost(100L, USER_A_ID);
            List<CommunityPost> myPosts = List.of(myPost);

            // 12 different users liked User A's post
            List<CommunityPostLike> allLikes = new ArrayList<>();
            for (long i = 1; i <= 12; i++) {
                CommunityPostLike like = buildLike(1000L + i, 100L, 5000L + i);
                // Set createTime so ordering is deterministic: older first
                like.setCreateTime(LocalDateTime.now().minusHours(12 - i));
                allLikes.add(like);
            }

            List<CommunityComment> noComments = Collections.emptyList();
            List<CommunityComment> noReplies = Collections.emptyList();
            List<CommunityPostFavorite> noFavorites = Collections.emptyList();

            // Users for page 2 display (users 11 and 12)
            List<SysUser> users = List.of(
                    buildUser(5011L, "user11", "User 11"),
                    buildUser(5012L, "user12", "User 12")
            );

            when(postMapper.selectList(any())).thenReturn(myPosts).thenReturn(myPosts);
            when(likeMapper.selectPage(any(Page.class), any())).thenReturn(pageOf(2, 10, 12, allLikes.subList(10, 12)));
            when(commentMapper.selectPage(any(Page.class), any()))
                    .thenReturn(pageOf(2, 10, 0, noComments))
                    .thenReturn(pageOf(2, 10, 0, noReplies));
            when(favoriteMapper.selectPage(any(Page.class), any())).thenReturn(pageOf(2, 10, 0, noFavorites));
            when(userMapper.selectList(any())).thenReturn(users);

            // ====== When ======
            ReceivedInteractionVO result = communityService.listReceivedInteractions(USER_A_ID, 2, 10);

            // ====== Then ======
            assertNotNull(result);
            assertEquals(12, result.getTotalLikes(), "totalLikes should be 12 (all likes)");
            assertEquals(2, result.getLikes().size(), "page 2 should have 2 remaining likes (12 total - 10 page 1)");
            assertEquals(0, result.getTotalComments());
            assertEquals(0, result.getTotalReplies());
            assertEquals(0, result.getTotalFavorites());
        }

        @Test
        @DisplayName("Scenario 1.3 [P0] - 无互动时返回空结果")
        void shouldReturnEmptyWhenUserHasNoInteractions() {
            // ====== Given ======
            when(likeMapper.selectPage(any(Page.class), any())).thenReturn(pageOf(1, 10, 0, Collections.emptyList()));
            when(commentMapper.selectPage(any(Page.class), any()))
                    .thenReturn(pageOf(1, 10, 0, Collections.emptyList()))
                    .thenReturn(pageOf(1, 10, 0, Collections.emptyList()));
            when(favoriteMapper.selectPage(any(Page.class), any())).thenReturn(pageOf(1, 10, 0, Collections.emptyList()));

            // ====== When ======
            ReceivedInteractionVO result = communityService.listReceivedInteractions(USER_A_ID, 1, 10);

            // ====== Then ======
            assertNotNull(result, "Result should never be null");
            assertTrue(result.getLikes().isEmpty(), "likes should be empty");
            assertTrue(result.getComments().isEmpty(), "comments should be empty");
            assertTrue(result.getReplies().isEmpty(), "replies should be empty");
            assertTrue(result.getFavorites().isEmpty(), "favorites should be empty");
            assertEquals(0, result.getTotalLikes());
            assertEquals(0, result.getTotalComments());
            assertEquals(0, result.getTotalReplies());
            assertEquals(0, result.getTotalFavorites());

            verifyNoInteractions(postMapper, userMapper);
        }

        @Test
        @DisplayName("Scenario 1.4 [P0] - 自己的互动不计入")
        void shouldExcludeOwnInteractionsFromResults() {
            // ====== Given ======
            // The service uses .ne(CommunityPostLike::getUserId, userId) in the SQL query,
            // which means the database filters out self-likes before returning results.
            // The mock simulates what the DB would return: only other users' likes.
            CommunityPost myPost = buildPost(100L, USER_A_ID);
            List<CommunityPost> myPosts = List.of(myPost);

            // User A liked own post (filtered by SQL ne(userId), so NOT in mock results)
            // User B liked the same post -- this is what the DB returns
            CommunityPostLike otherLike = buildLike(1002L, 100L, USER_B_ID);
            List<CommunityPostLike> allLikes = List.of(otherLike);

            // No comments, no replies, no favorites
            List<CommunityComment> noComments = Collections.emptyList();
            List<CommunityComment> noReplies = Collections.emptyList();
            List<CommunityPostFavorite> noFavorites = Collections.emptyList();

            List<SysUser> users = List.of(buildUser(USER_B_ID, "userB", "User B"));

            when(postMapper.selectList(any())).thenReturn(myPosts).thenReturn(myPosts);
            when(likeMapper.selectPage(any(Page.class), any())).thenReturn(pageOf(1, 10, 1, allLikes));
            when(commentMapper.selectPage(any(Page.class), any()))
                    .thenReturn(pageOf(1, 10, 0, noComments))
                    .thenReturn(pageOf(1, 10, 0, noReplies));
            when(favoriteMapper.selectPage(any(Page.class), any())).thenReturn(pageOf(1, 10, 0, noFavorites));
            when(userMapper.selectList(any())).thenReturn(users);

            // ====== When ======
            ReceivedInteractionVO result = communityService.listReceivedInteractions(USER_A_ID, 1, 10);

            // ====== Then ======
            assertNotNull(result);
            assertEquals(1, result.getTotalLikes(),
                    "totalLikes should be 1 -- self-like is excluded by SQL ne(userId)");
            assertEquals(1, result.getLikes().size(),
                    "Only User B's like should appear in the likes list");
            assertEquals(USER_B_ID, result.getLikes().get(0).getUserId(),
                    "The remaining like should belong to User B");
        }

        @Test
        @DisplayName("Scenario 1.5 [P0] - 请求超出总页数时返回空列表但总数正确")
        void shouldReturnEmptyListWhenPageExceedsTotal() {
            // ====== Given ======
            // Only 1 like record
            List<CommunityComment> noComments = Collections.emptyList();
            List<CommunityComment> noReplies = Collections.emptyList();
            List<CommunityPostFavorite> noFavorites = Collections.emptyList();

            when(likeMapper.selectPage(any(Page.class), any())).thenReturn(pageOf(5, 10, 1, Collections.emptyList()));
            when(commentMapper.selectPage(any(Page.class), any()))
                    .thenReturn(pageOf(5, 10, 0, noComments))
                    .thenReturn(pageOf(5, 10, 0, noReplies));
            when(favoriteMapper.selectPage(any(Page.class), any())).thenReturn(pageOf(5, 10, 0, noFavorites));
            lenient().when(userMapper.selectList(any())).thenReturn(Collections.emptyList());

            // ====== When ======
            ReceivedInteractionVO result = communityService.listReceivedInteractions(USER_A_ID, 5, 10);

            // ====== Then ======
            assertNotNull(result);
            assertEquals(1, result.getTotalLikes(),
                    "totalLikes should still be 1 (total is unaffected by page number)");
            assertTrue(result.getLikes().isEmpty(),
                    "likes list should be empty -- page 5 with only 1 record is out of range");
            assertEquals(0, result.getTotalComments());
            assertEquals(0, result.getTotalReplies());
            assertEquals(0, result.getTotalFavorites());
        }
    }

    // ==========================================================
    //  Feature 1 (continued): 未读数量查询 (getUnreadInteractionCount)
    // ==========================================================

    @Nested
    @DisplayName("Feature 1: 未读互动数量查询")
    class GetUnreadInteractionCountTest {

        @Test
        @DisplayName("Scenario 1.6 [P2] - 未读数量查询正确统计4类互动")
        void shouldCountUnreadInteractionsAcrossAllTypes() {
            LocalDateTime since = LocalDateTime.now().minusDays(1);
            when(commentMapper.countUnreadInteractions(USER_A_ID, since)).thenReturn(7);

            // ====== When ======
            int count = communityService.getUnreadInteractionCount(USER_A_ID, since);

            // ====== Then ======
            // 2 likes + 1 comment + 3 replies + 1 favorite = 7
            assertEquals(7, count, "Total unread count should be 2+1+3+1=7");

            verify(commentMapper).countUnreadInteractions(USER_A_ID, since);
            verifyNoInteractions(postMapper, likeMapper, favoriteMapper);
        }

        @Test
        @DisplayName("Scenario 1.6b [P2] - 无未读互动时返回0")
        void shouldReturnZeroWhenThereAreNoUnreadInteractions() {
            LocalDateTime since = LocalDateTime.now().minusDays(1);
            when(commentMapper.countUnreadInteractions(USER_A_ID, since)).thenReturn(0);

            // ====== When ======
            int count = communityService.getUnreadInteractionCount(USER_A_ID, since);

            // ====== Then ======
            assertEquals(0, count, "No unread interactions should return zero");

            verify(commentMapper).countUnreadInteractions(USER_A_ID, since);
            verifyNoInteractions(postMapper, likeMapper, favoriteMapper);
        }
    }
}
