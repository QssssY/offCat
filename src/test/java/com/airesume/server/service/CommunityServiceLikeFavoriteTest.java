package com.airesume.server.service;

import com.airesume.server.common.exception.BusinessException;
import com.airesume.server.entity.CommunityPost;
import com.airesume.server.entity.CommunityPostFavorite;
import com.airesume.server.entity.CommunityPostLike;
import com.airesume.server.mapper.CommunityCommentMapper;
import com.airesume.server.mapper.CommunityPostFavoriteMapper;
import com.airesume.server.mapper.CommunityPostLikeMapper;
import com.airesume.server.mapper.CommunityPostMapper;
import com.airesume.server.mapper.InterviewSessionMapper;
import com.airesume.server.mapper.SysUserMapper;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DuplicateKeyException;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Feature 4: 点赞/收藏并发安全 BDD测试
 * Issue #4 - 验证toggleLike/toggleFavorite在并发场景下的行为
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("Feature 4: 点赞/收藏并发安全")
class CommunityServiceLikeFavoriteTest {

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

    @Mock
    private ObjectMapper objectMapper;

    private CommunityService service;

    private static final Long USER_ID = 1001L;
    private static final Long POST_ID = 2001L;
    private static final Long NONEXISTENT_POST_ID = 99999L;

    private CommunityPost defaultPost;

    @BeforeEach
    void setUp() {
        service = new CommunityService(postMapper, commentMapper, likeMapper, favoriteMapper, userMapper, interviewSessionMapper, objectMapper);

        defaultPost = new CommunityPost();
        defaultPost.setId(POST_ID);
        defaultPost.setUserId(3001L);
        defaultPost.setContent("测试帖子内容");
        defaultPost.setLikeCount(5);
        defaultPost.setCommentCount(2);
    }

    // ==================== toggleLike 测试 ====================

    @Nested
    @DisplayName("4.1 [P0] 并发点赞不产生重复记录")
    class ConcurrentLikeTests {

        @Test
        @DisplayName("当insert抛出DuplicateKeyException时应幂等处理，视为已点赞")
        void shouldHandleDuplicateKeyGracefully() {
            // 模拟并发场景：selectOne返回null（两个线程同时看到无记录），但insert时另一个线程已插入
            when(postMapper.selectById(POST_ID)).thenReturn(defaultPost);
            when(likeMapper.selectOne(any(LambdaQueryWrapper.class))).thenReturn(null);
            when(likeMapper.insert(any(CommunityPostLike.class)))
                    .thenThrow(new DuplicateKeyException("Duplicate entry for key 'uk_post_user'"));

            // 当前代码会将DuplicateKeyException向上抛出
            // 修复后应捕获该异常并返回true（表示已点赞状态）
            // 此测试在修复前会FAIL，修复后PASS
            boolean result = service.toggleLike(USER_ID, POST_ID);

            assertTrue(result, "并发插入冲突时应幂等返回true，表示已处于点赞状态");
        }

        @Test
        @DisplayName("正常点赞应成功并返回true")
        void shouldLikeSuccessfully() {
            when(postMapper.selectById(POST_ID)).thenReturn(defaultPost);
            when(likeMapper.selectOne(any(LambdaQueryWrapper.class))).thenReturn(null);
            when(likeMapper.insert(any(CommunityPostLike.class))).thenReturn(1);

            boolean result = service.toggleLike(USER_ID, POST_ID);

            assertTrue(result, "首次点赞应返回true");
            verify(likeMapper).insert(any(CommunityPostLike.class));
            verify(postMapper).update(eq(null), any());
        }

        @Test
        @DisplayName("并发点赞时selectOne应只查到一条记录")
        void shouldFindAtMostOneExistingRecord() {
            CommunityPostLike existingLike = new CommunityPostLike();
            existingLike.setId(5001L);
            existingLike.setPostId(POST_ID);
            existingLike.setUserId(USER_ID);

            when(postMapper.selectById(POST_ID)).thenReturn(defaultPost);
            when(likeMapper.selectOne(any(LambdaQueryWrapper.class))).thenReturn(existingLike);
            when(likeMapper.deleteById(5001L)).thenReturn(1);

            boolean result = service.toggleLike(USER_ID, POST_ID);

            assertFalse(result, "已点赞状态下应取消点赞并返回false");
            verify(likeMapper).deleteById(5001L);
        }
    }

    @Nested
    @DisplayName("4.2 [P0] 并发取消点赞状态确定")
    class ConcurrentUnlikeTests {

        @Test
        @DisplayName("并发取消点赞时第二次deleteById返回0应不影响最终状态")
        void shouldHandleConcurrentUnlikeIdempotently() {
            CommunityPostLike existingLike = new CommunityPostLike();
            existingLike.setId(5001L);
            existingLike.setPostId(POST_ID);
            existingLike.setUserId(USER_ID);

            when(postMapper.selectById(POST_ID)).thenReturn(defaultPost);
            when(likeMapper.selectOne(any(LambdaQueryWrapper.class))).thenReturn(existingLike);
            // 模拟并发：第二次delete已被另一个线程删除，返回0
            when(likeMapper.deleteById(5001L)).thenReturn(0);

            boolean result = service.toggleLike(USER_ID, POST_ID);

            assertFalse(result, "取消点赞应返回false，即使deleteById影响行数为0");
            verify(likeMapper).deleteById(5001L);
            verify(postMapper).update(eq(null), any());
        }

        @Test
        @DisplayName("取消点赞后点赞数应减少")
        void shouldDecrementLikeCountOnUnlike() {
            CommunityPostLike existingLike = new CommunityPostLike();
            existingLike.setId(5001L);
            existingLike.setPostId(POST_ID);
            existingLike.setUserId(USER_ID);

            when(postMapper.selectById(POST_ID)).thenReturn(defaultPost);
            when(likeMapper.selectOne(any(LambdaQueryWrapper.class))).thenReturn(existingLike);
            when(likeMapper.deleteById(5001L)).thenReturn(1);

            service.toggleLike(USER_ID, POST_ID);

            // 验证decrementLikeCount被调用（通过postMapper.update）
            verify(postMapper).update(eq(null), any());
        }

        @Test
        @DisplayName("单次取消点赞应正常执行")
        void shouldUnlikeNormally() {
            CommunityPostLike existingLike = new CommunityPostLike();
            existingLike.setId(5001L);
            existingLike.setPostId(POST_ID);
            existingLike.setUserId(USER_ID);

            when(postMapper.selectById(POST_ID)).thenReturn(defaultPost);
            when(likeMapper.selectOne(any(LambdaQueryWrapper.class))).thenReturn(existingLike);
            when(likeMapper.deleteById(5001L)).thenReturn(1);

            boolean result = service.toggleLike(USER_ID, POST_ID);

            assertFalse(result, "取消点赞应返回false");
        }
    }

    @Nested
    @DisplayName("4.3 [P0] 并发收藏不产生重复")
    class ConcurrentFavoriteTests {

        @Test
        @DisplayName("当insert抛出DuplicateKeyException时应幂等处理，视为已收藏")
        void shouldHandleDuplicateKeyGracefully() {
            when(postMapper.selectById(POST_ID)).thenReturn(defaultPost);
            when(favoriteMapper.selectOne(any(LambdaQueryWrapper.class))).thenReturn(null);
            when(favoriteMapper.insert(any(CommunityPostFavorite.class)))
                    .thenThrow(new DuplicateKeyException("Duplicate entry for key 'uk_post_user_fav'"));

            // 修复后应捕获DuplicateKeyException并返回true
            boolean result = service.toggleFavorite(USER_ID, POST_ID);

            assertTrue(result, "并发收藏冲突时应幂等返回true，表示已处于收藏状态");
        }

        @Test
        @DisplayName("正常收藏应成功并返回true")
        void shouldFavoriteSuccessfully() {
            when(postMapper.selectById(POST_ID)).thenReturn(defaultPost);
            when(favoriteMapper.selectOne(any(LambdaQueryWrapper.class))).thenReturn(null);
            when(favoriteMapper.insert(any(CommunityPostFavorite.class))).thenReturn(1);

            boolean result = service.toggleFavorite(USER_ID, POST_ID);

            assertTrue(result, "首次收藏应返回true");
            verify(favoriteMapper).insert(any(CommunityPostFavorite.class));
        }

        @Test
        @DisplayName("并发取消收藏时第二次deleteById返回0应不影响最终状态")
        void shouldHandleConcurrentUnfavoriteIdempotently() {
            CommunityPostFavorite existingFav = new CommunityPostFavorite();
            existingFav.setId(6001L);
            existingFav.setPostId(POST_ID);
            existingFav.setUserId(USER_ID);

            when(postMapper.selectById(POST_ID)).thenReturn(defaultPost);
            when(favoriteMapper.selectOne(any(LambdaQueryWrapper.class))).thenReturn(existingFav);
            when(favoriteMapper.deleteById(6001L)).thenReturn(0);

            boolean result = service.toggleFavorite(USER_ID, POST_ID);

            assertFalse(result, "取消收藏应返回false，即使deleteById影响行数为0");
            verify(favoriteMapper).deleteById(6001L);
        }

        @Test
        @DisplayName("已收藏状态下应取消收藏并返回false")
        void shouldUnfavoriteWhenAlreadyFavorited() {
            CommunityPostFavorite existingFav = new CommunityPostFavorite();
            existingFav.setId(6001L);
            existingFav.setPostId(POST_ID);
            existingFav.setUserId(USER_ID);

            when(postMapper.selectById(POST_ID)).thenReturn(defaultPost);
            when(favoriteMapper.selectOne(any(LambdaQueryWrapper.class))).thenReturn(existingFav);
            when(favoriteMapper.deleteById(6001L)).thenReturn(1);

            boolean result = service.toggleFavorite(USER_ID, POST_ID);

            assertFalse(result, "已收藏状态下toggle应取消收藏并返回false");
        }
    }

    @Nested
    @DisplayName("4.4 [P0] 对不存在帖子点赞/收藏应抛出异常")
    class NonexistentPostTests {

        @Test
        @DisplayName("对不存在的帖子点赞应抛出BusinessException")
        void shouldThrowWhenLikingNonexistentPost() {
            when(postMapper.selectById(NONEXISTENT_POST_ID)).thenReturn(null);

            BusinessException exception = assertThrows(BusinessException.class,
                    () -> service.toggleLike(USER_ID, NONEXISTENT_POST_ID));

            assertEquals("帖子不存在", exception.getMessage());
            verify(likeMapper, never()).insert(any(CommunityPostLike.class));
            verify(likeMapper, never()).deleteById(any());
            verify(postMapper, never()).update(any(), any());
        }

        @Test
        @DisplayName("对不存在的帖子收藏应抛出BusinessException")
        void shouldThrowWhenFavoritingNonexistentPost() {
            when(postMapper.selectById(NONEXISTENT_POST_ID)).thenReturn(null);

            BusinessException exception = assertThrows(BusinessException.class,
                    () -> service.toggleFavorite(USER_ID, NONEXISTENT_POST_ID));

            assertEquals("帖子不存在", exception.getMessage());
            verify(favoriteMapper, never()).insert(any(CommunityPostFavorite.class));
            verify(favoriteMapper, never()).deleteById(any());
        }
    }

    @Nested
    @DisplayName("4.5 [P0] 取消后可再次点赞/收藏")
    class ToggleCycleTests {

        @Test
        @DisplayName("取消点赞后可再次点赞，状态正确切换")
        void shouldReLikeAfterUnlike() {
            CommunityPostLike existingLike = new CommunityPostLike();
            existingLike.setId(5001L);
            existingLike.setPostId(POST_ID);
            existingLike.setUserId(USER_ID);

            // 第一次调用：已点赞 -> 取消点赞
            when(postMapper.selectById(POST_ID)).thenReturn(defaultPost);
            when(likeMapper.selectOne(any(LambdaQueryWrapper.class)))
                    .thenReturn(existingLike)   // 第一次查询：找到已有点赞
                    .thenReturn(null);           // 第二次查询：无点赞记录
            when(likeMapper.deleteById(5001L)).thenReturn(1);
            when(likeMapper.insert(any(CommunityPostLike.class))).thenReturn(1);

            // 第一次toggle：取消点赞
            boolean firstResult = service.toggleLike(USER_ID, POST_ID);
            assertFalse(firstResult, "第一次toggle应取消点赞，返回false");

            // 第二次toggle：重新点赞
            boolean secondResult = service.toggleLike(USER_ID, POST_ID);
            assertTrue(secondResult, "第二次toggle应重新点赞，返回true");

            verify(likeMapper).deleteById(5001L);
            verify(likeMapper).insert(any(CommunityPostLike.class));
        }

        @Test
        @DisplayName("取消收藏后可再次收藏，状态正确切换")
        void shouldReFavoriteAfterUnfavorite() {
            CommunityPostFavorite existingFav = new CommunityPostFavorite();
            existingFav.setId(6001L);
            existingFav.setPostId(POST_ID);
            existingFav.setUserId(USER_ID);

            // 第一次调用：已收藏 -> 取消收藏
            when(postMapper.selectById(POST_ID)).thenReturn(defaultPost);
            when(favoriteMapper.selectOne(any(LambdaQueryWrapper.class)))
                    .thenReturn(existingFav)     // 第一次查询：找到已有收藏
                    .thenReturn(null);            // 第二次查询：无收藏记录
            when(favoriteMapper.deleteById(6001L)).thenReturn(1);
            when(favoriteMapper.insert(any(CommunityPostFavorite.class))).thenReturn(1);

            // 第一次toggle：取消收藏
            boolean firstResult = service.toggleFavorite(USER_ID, POST_ID);
            assertFalse(firstResult, "第一次toggle应取消收藏，返回false");

            // 第二次toggle：重新收藏
            boolean secondResult = service.toggleFavorite(USER_ID, POST_ID);
            assertTrue(secondResult, "第二次toggle应重新收藏，返回true");

            verify(favoriteMapper).deleteById(6001L);
            verify(favoriteMapper).insert(any(CommunityPostFavorite.class));
        }

        @Test
        @DisplayName("连续多次点赞/取消点赞状态应正确交替")
        void shouldToggleLikeCorrectlyMultipleTimes() {
            CommunityPostLike existingLike = new CommunityPostLike();
            existingLike.setId(5001L);
            existingLike.setPostId(POST_ID);
            existingLike.setUserId(USER_ID);

            when(postMapper.selectById(POST_ID)).thenReturn(defaultPost);
            when(likeMapper.selectOne(any(LambdaQueryWrapper.class)))
                    .thenReturn(null)             // 第1次：点赞
                    .thenReturn(existingLike)     // 第2次：取消点赞
                    .thenReturn(null);            // 第3次：再次点赞
            when(likeMapper.insert(any(CommunityPostLike.class))).thenReturn(1);
            when(likeMapper.deleteById(5001L)).thenReturn(1);

            boolean result1 = service.toggleLike(USER_ID, POST_ID);
            assertTrue(result1, "第1次toggle应点赞，返回true");

            boolean result2 = service.toggleLike(USER_ID, POST_ID);
            assertFalse(result2, "第2次toggle应取消点赞，返回false");

            boolean result3 = service.toggleLike(USER_ID, POST_ID);
            assertTrue(result3, "第3次toggle应再次点赞，返回true");
        }
    }
}
