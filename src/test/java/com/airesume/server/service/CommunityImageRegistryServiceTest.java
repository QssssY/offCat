package com.airesume.server.service;

import com.airesume.server.common.exception.BusinessException;
import com.airesume.server.entity.CommunityImage;
import com.airesume.server.mapper.CommunityImageMapper;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CommunityImageRegistryServiceTest {

    @Mock
    private CommunityImageMapper imageMapper;

    @Mock
    private OssService ossService;

    @Test
    void shouldRecordUploadedCommunityImage() {
        CommunityImageRegistryService service = new CommunityImageRegistryService(imageMapper, ossService);

        service.recordUpload(
                1001L,
                "community/1001/20260605/abcdef.jpg",
                "/api/community/images/community/1001/20260605/abcdef.jpg");

        ArgumentCaptor<CommunityImage> captor = ArgumentCaptor.forClass(CommunityImage.class);
        verify(imageMapper).insert(captor.capture());
        assertEquals(1001L, captor.getValue().getUserId());
        assertEquals("community/1001/20260605/abcdef.jpg", captor.getValue().getObjectKey());
        assertEquals("/api/community/images/community/1001/20260605/abcdef.jpg", captor.getValue().getProxyUrl());
        assertEquals("uploaded", captor.getValue().getStatus());
    }

    @Test
    void shouldBindOnlyCurrentUsersUnboundImages() {
        CommunityImageRegistryService service = new CommunityImageRegistryService(imageMapper, ossService);
        CommunityImage image = buildUploadedImage(3001L, 1001L,
                "/api/community/images/community/1001/20260605/abcdef.jpg");
        when(imageMapper.selectOne(any())).thenReturn(image);
        when(imageMapper.update(any(), any())).thenReturn(1);

        service.bindUploadedImages(
                1001L,
                List.of("/api/community/images/community/1001/20260605/abcdef.jpg"),
                "post",
                2001L);

        ArgumentCaptor<CommunityImage> captor = ArgumentCaptor.forClass(CommunityImage.class);
        verify(imageMapper).update(captor.capture(), any());
        assertEquals("bound", captor.getValue().getStatus());
        assertEquals("post", captor.getValue().getBoundType());
        assertEquals(2001L, captor.getValue().getBoundId());
    }

    @Test
    void shouldRejectImageWhenConcurrentBindingAlreadyChangedStatus() {
        CommunityImageRegistryService service = new CommunityImageRegistryService(imageMapper, ossService);
        CommunityImage image = buildUploadedImage(3001L, 1001L,
                "/api/community/images/community/1001/20260605/abcdef.jpg");
        when(imageMapper.selectOne(any())).thenReturn(image);
        when(imageMapper.update(any(), any())).thenReturn(0);

        BusinessException exception = assertThrows(BusinessException.class,
                () -> service.bindUploadedImages(
                        1001L,
                        List.of("/api/community/images/community/1001/20260605/abcdef.jpg"),
                        "post",
                        2001L));

        assertEquals("只能使用当前账号上传且未绑定的社区图片", exception.getMessage());
    }

    @Test
    void shouldRejectImageNotUploadedByCurrentUser() {
        CommunityImageRegistryService service = new CommunityImageRegistryService(imageMapper, ossService);
        when(imageMapper.selectOne(any())).thenReturn(null);

        BusinessException exception = assertThrows(BusinessException.class,
                () -> service.bindUploadedImages(
                        1001L,
                        List.of("/api/community/images/community/1002/20260605/abcdef.jpg"),
                        "post",
                        2001L));

        assertEquals("只能使用当前账号上传且未绑定的社区图片", exception.getMessage());
    }

    @Test
    void shouldCleanupExpiredUnboundImagesFromOssAndRegistry() {
        CommunityImageRegistryService service = new CommunityImageRegistryService(imageMapper, ossService);
        CommunityImage image = buildUploadedImage(3001L, 1001L,
                "/api/community/images/community/1001/20260605/abcdef.jpg");
        image.setObjectKey("community/1001/20260605/abcdef.jpg");
        when(ossService.isEnabled()).thenReturn(true);
        when(imageMapper.selectList(any())).thenReturn(List.of(image));

        int cleanupCount = service.cleanupExpiredUnboundImages(LocalDateTime.now().minusDays(1), 50);

        assertEquals(1, cleanupCount);
        verify(ossService).deleteObject("community/1001/20260605/abcdef.jpg");
        verify(imageMapper).deleteById(3001L);
    }

    private CommunityImage buildUploadedImage(Long id, Long userId, String proxyUrl) {
        CommunityImage image = new CommunityImage();
        image.setId(id);
        image.setUserId(userId);
        image.setObjectKey(proxyUrl.substring("/api/community/images/".length()));
        image.setProxyUrl(proxyUrl);
        image.setStatus("uploaded");
        return image;
    }
}
