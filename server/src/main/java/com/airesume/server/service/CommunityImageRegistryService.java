package com.airesume.server.service;

import com.airesume.server.common.exception.BusinessException;
import com.airesume.server.entity.CommunityImage;
import com.airesume.server.mapper.CommunityImageMapper;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 社区图片登记服务。
 * 核心规则：图片必须由当前用户上传，且只能被一个帖子或评论绑定一次。
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class CommunityImageRegistryService {

    public static final String STATUS_UPLOADED = "uploaded";
    public static final String STATUS_BOUND = "bound";

    private final CommunityImageMapper imageMapper;
    private final OssService ossService;

    /**
     * 记录一次 OSS 上传结果，后续发帖/评论会按 proxyUrl 做所有权与未绑定校验。
     */
    public void recordUpload(Long userId, String objectKey, String proxyUrl) {
        CommunityImage image = new CommunityImage();
        image.setUserId(userId);
        image.setObjectKey(objectKey);
        image.setProxyUrl(proxyUrl);
        image.setStatus(STATUS_UPLOADED);
        imageMapper.insert(image);
    }

    /**
     * 将前端提交的图片绑定到具体内容。
     * 任一图片不是当前用户上传或已被绑定时直接失败，事务回滚内容创建。
     */
    @Transactional(rollbackFor = Exception.class)
    public void bindUploadedImages(Long userId, List<String> imageUrls, String boundType, Long boundId) {
        if (imageUrls == null || imageUrls.isEmpty()) {
            return;
        }
        LocalDateTime now = LocalDateTime.now();
        for (String imageUrl : imageUrls) {
            CommunityImage image = imageMapper.selectOne(new LambdaQueryWrapper<CommunityImage>()
                    .eq(CommunityImage::getUserId, userId)
                    .eq(CommunityImage::getProxyUrl, imageUrl)
                    .eq(CommunityImage::getStatus, STATUS_UPLOADED)
                    .last("LIMIT 1"));
            if (image == null) {
                throw new BusinessException("只能使用当前账号上传且未绑定的社区图片");
            }
            CommunityImage update = new CommunityImage();
            update.setStatus(STATUS_BOUND);
            update.setBoundType(boundType);
            update.setBoundId(boundId);
            update.setBoundTime(now);
            int affected = imageMapper.update(update, new LambdaUpdateWrapper<CommunityImage>()
                    .eq(CommunityImage::getId, image.getId())
                    .eq(CommunityImage::getStatus, STATUS_UPLOADED));
            if (affected == 0) {
                throw new BusinessException("只能使用当前账号上传且未绑定的社区图片");
            }
        }
    }

    /**
     * 清理超过保留时间仍未绑定的图片，释放 OSS 存储并逻辑删除登记记录。
     */
    public int cleanupExpiredUnboundImages(LocalDateTime expireBefore, int batchSize) {
        int safeBatchSize = Math.max(1, Math.min(batchSize, 500));
        List<CommunityImage> images = imageMapper.selectList(new LambdaQueryWrapper<CommunityImage>()
                .eq(CommunityImage::getStatus, STATUS_UPLOADED)
                .lt(CommunityImage::getCreateTime, expireBefore)
                .orderByAsc(CommunityImage::getCreateTime)
                .last("LIMIT " + safeBatchSize));
        int cleaned = 0;
        for (CommunityImage image : images) {
            try {
                if (ossService.isEnabled()) {
                    ossService.deleteObject(image.getObjectKey());
                }
                imageMapper.deleteById(image.getId());
                cleaned++;
            } catch (Exception ex) {
                log.warn("社区未绑定图片清理失败, imageId: {}, objectKey: {}", image.getId(), image.getObjectKey(), ex);
            }
        }
        return cleaned;
    }
}
