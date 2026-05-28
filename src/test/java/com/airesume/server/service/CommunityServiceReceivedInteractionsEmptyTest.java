package com.airesume.server.service;

import com.airesume.server.dto.community.ReceivedInteractionVO;
import com.airesume.server.entity.CommunityComment;
import com.airesume.server.entity.CommunityPostFavorite;
import com.airesume.server.entity.CommunityPostLike;
import com.airesume.server.entity.SysUser;
import com.airesume.server.mapper.CommunityCommentMapper;
import com.airesume.server.mapper.CommunityPostFavoriteMapper;
import com.airesume.server.mapper.CommunityPostLikeMapper;
import com.airesume.server.mapper.CommunityPostMapper;
import com.airesume.server.mapper.InterviewSessionMapper;
import com.airesume.server.mapper.SysUserMapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Collections;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CommunityServiceReceivedInteractionsEmptyTest {

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

    @Test
    void shouldNotQueryPostsWhenReceivedInteractionsAreEmpty() {
        CommunityService service = new CommunityService(
                postMapper, commentMapper, likeMapper, favoriteMapper, userMapper, interviewSessionMapper, objectMapper
        );
        when(likeMapper.selectPage(any(Page.class), any())).thenReturn(emptyPage());
        when(commentMapper.selectPage(any(Page.class), any())).thenReturn(emptyPage());
        when(favoriteMapper.selectPage(any(Page.class), any())).thenReturn(emptyPage());

        ReceivedInteractionVO result = service.listReceivedInteractions(1001L, 1, 20);

        assertTrue(result.getLikes().isEmpty());
        assertTrue(result.getComments().isEmpty());
        assertTrue(result.getReplies().isEmpty());
        assertTrue(result.getFavorites().isEmpty());
        assertEquals(0, result.getTotalLikes());
        assertEquals(0, result.getTotalComments());
        assertEquals(0, result.getTotalReplies());
        assertEquals(0, result.getTotalFavorites());
        verifyNoInteractions(postMapper, userMapper);
    }

    private <T> Page<T> emptyPage() {
        Page<T> page = new Page<>(1, 20);
        page.setRecords(Collections.emptyList());
        page.setTotal(0);
        return page;
    }
}
