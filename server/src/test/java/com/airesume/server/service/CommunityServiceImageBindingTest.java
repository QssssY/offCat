package com.airesume.server.service;

import com.airesume.server.dto.community.CreatePostRequest;
import com.airesume.server.entity.CommunityPost;
import com.airesume.server.mapper.CommunityCommentMapper;
import com.airesume.server.mapper.CommunityPostFavoriteMapper;
import com.airesume.server.mapper.CommunityPostLikeMapper;
import com.airesume.server.mapper.CommunityPostMapper;
import com.airesume.server.mapper.InterviewSessionMapper;
import com.airesume.server.mapper.SysUserMapper;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CommunityServiceImageBindingTest {

    @Mock private CommunityPostMapper postMapper;
    @Mock private CommunityCommentMapper commentMapper;
    @Mock private CommunityPostLikeMapper likeMapper;
    @Mock private CommunityPostFavoriteMapper favoriteMapper;
    @Mock private SysUserMapper userMapper;
    @Mock private InterviewSessionMapper interviewSessionMapper;
    @Mock private NotificationService notificationService;
    @Mock private OssService ossService;
    @Mock private CommunityImageRegistryService imageRegistryService;

    private CommunityService service;

    @BeforeEach
    void setUp() {
        service = new CommunityService(
                postMapper,
                commentMapper,
                likeMapper,
                favoriteMapper,
                userMapper,
                interviewSessionMapper,
                new ObjectMapper(),
                new CommunityTextModerationService(),
                notificationService,
                ossService,
                imageRegistryService);
        ReflectionTestUtils.setField(service, "maxFileSize", 5 * 1024 * 1024L);
        ReflectionTestUtils.setField(service, "dailyUploadLimit", 30);
        ReflectionTestUtils.setField(service, "communityPlaceholderImageUrl", "https://example.test/community-placeholder.jpg");
    }

    @Test
    void shouldRecordOssUploadInImageRegistry() {
        MockMultipartFile jpgFile = new MockMultipartFile(
                "file",
                "photo.jpg",
                "image/jpeg",
                new byte[]{(byte) 0xFF, (byte) 0xD8, (byte) 0xFF});
        when(ossService.isEnabled()).thenReturn(true);
        when(ossService.upload(jpgFile, 1001L, "jpg"))
                .thenReturn("community/1001/20260605/abcdef.jpg");

        String url = service.uploadImage(jpgFile, 1001L);

        assertEquals("/api/community/images/community/1001/20260605/abcdef.jpg", url);
        verify(imageRegistryService).recordUpload(
                1001L,
                "community/1001/20260605/abcdef.jpg",
                "/api/community/images/community/1001/20260605/abcdef.jpg");
    }

    @Test
    void shouldBindUploadedPostImagesAfterPostCreated() {
        doAnswer(invocation -> {
            CommunityPost post = invocation.getArgument(0);
            post.setId(2001L);
            return 1;
        }).when(postMapper).insert(any(CommunityPost.class));
        CreatePostRequest request = new CreatePostRequest();
        request.setCategory("interview_exp");
        request.setTitle("面试复盘截图");
        request.setContent("这次主要复盘了 JVM 和索引。");
        request.setImages(List.of("/api/community/images/community/1001/20260605/abcdef.jpg"));

        service.createPost(1001L, request);

        verify(imageRegistryService).bindUploadedImages(
                eq(1001L),
                eq(List.of("/api/community/images/community/1001/20260605/abcdef.jpg")),
                eq("post"),
                eq(2001L));
    }

    @Test
    void shouldAllowPlaceholderImageWhenOssIsDisabled() {
        when(ossService.isEnabled()).thenReturn(false);
        MockMultipartFile jpgFile = new MockMultipartFile(
                "file",
                "photo.jpg",
                "image/jpeg",
                new byte[]{(byte) 0xFF, (byte) 0xD8, (byte) 0xFF});
        String placeholderUrl = service.uploadImage(jpgFile, 1001L);
        doAnswer(invocation -> {
            CommunityPost post = invocation.getArgument(0);
            post.setId(2001L);
            return 1;
        }).when(postMapper).insert(any(CommunityPost.class));
        CreatePostRequest request = new CreatePostRequest();
        request.setCategory("interview_exp");
        request.setTitle("本地占位图复盘");
        request.setContent("OSS 未启用时仍允许使用占位图完成本地发帖验证。");
        request.setImages(List.of(placeholderUrl));

        service.createPost(1001L, request);

        verify(imageRegistryService, never()).bindUploadedImages(
                eq(1001L),
                eq(List.of(placeholderUrl)),
                eq("post"),
                eq(2001L));
    }
}
