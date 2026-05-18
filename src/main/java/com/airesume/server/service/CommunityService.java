package com.airesume.server.service;

import com.airesume.server.common.constants.CommunityConstants;
import com.airesume.server.common.exception.BusinessException;
import com.airesume.server.common.result.PageResult;
import com.airesume.server.dto.community.*;
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
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.IOException;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

/**
 * 社区模块业务服务
 * 提供帖子CRUD、点赞/取消点赞、评论、图片上传等能力
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class CommunityService {

    private final CommunityPostMapper postMapper;
    private final CommunityCommentMapper commentMapper;
    private final CommunityPostLikeMapper likeMapper;
    private final CommunityPostFavoriteMapper favoriteMapper;
    private final SysUserMapper userMapper;
    private final ObjectMapper objectMapper;

    /** 社区图片上传目录 */
    @Value("${app.upload.community-dir:}")
    private String configuredUploadDir;

    /** 最大文件大小（字节），默认5MB */
    @Value("${app.upload.community-max-size:5242880}")
    private long maxFileSize;

    // ==================== 帖子相关 ====================

    /**
     * 分页查询帖子列表
     *
     * @param category 板块筛选（可选）
     * @param sort     排序方式：latest/hot
     * @param pageNum  页码
     * @param pageSize 每页大小
     * @param userId   当前用户ID（用于判断是否已点赞）
     * @return 分页结果
     */
    public PageResult<PostVO> listPosts(String category, String sort, Integer pageNum, Integer pageSize, Long userId) {
        // 安全限制分页大小
        int safeSize = Math.min(pageSize, CommunityConstants.MAX_PAGE_SIZE);

        // 构建查询条件
        LambdaQueryWrapper<CommunityPost> wrapper = new LambdaQueryWrapper<>();
        if (category != null && !category.isBlank()) {
            wrapper.eq(CommunityPost::getCategory, category);
        }

        // 排序
        if (CommunityConstants.SORT_HOT.equals(sort)) {
            wrapper.orderByDesc(CommunityPost::getLikeCount);
        } else {
            wrapper.orderByDesc(CommunityPost::getCreateTime);
        }

        // 分页查询
        Page<CommunityPost> page = new Page<>(pageNum, safeSize);
        Page<CommunityPost> resultPage = postMapper.selectPage(page, wrapper);

        // 批量查询帖子作者信息
        Set<Long> userIds = resultPage.getRecords().stream()
                .map(CommunityPost::getUserId)
                .collect(Collectors.toSet());
        Map<Long, SysUser> userMap = batchGetUsers(userIds);

        // 批量查询当前用户的点赞状态
        Set<Long> likedPostIds = batchCheckLiked(userId, resultPage.getRecords());

        // 批量查询当前用户的收藏状态
        Set<Long> favoritedPostIds = batchCheckFavorited(userId, resultPage.getRecords());

        // 转换为VO
        List<PostVO> voList = resultPage.getRecords().stream()
                .map(post -> toPostVO(post, userMap, likedPostIds, favoritedPostIds))
                .toList();

        return PageResult.of(voList, resultPage.getTotal(), pageNum, safeSize);
    }

    /**
     * 获取帖子详情
     *
     * @param postId 帖子ID
     * @param userId 当前用户ID
     * @return 帖子详情VO
     */
    public PostVO getPostDetail(Long postId, Long userId) {
        CommunityPost post = postMapper.selectById(postId);
        if (post == null) {
            throw new BusinessException("帖子不存在");
        }

        // 查询作者信息
        SysUser author = userMapper.selectById(post.getUserId());

        // 查询当前用户是否已点赞
        boolean liked = checkLiked(userId, postId);

        // 查询当前用户是否已收藏
        boolean favorited = checkFavorited(userId, postId);

        return toPostVO(post, author, liked, favorited);
    }

    /**
     * 创建帖子
     *
     * @param userId  发布者用户ID
     * @param request 创建请求
     * @return 帖子ID
     */
    @Transactional(rollbackFor = Exception.class)
    public Long createPost(Long userId, CreatePostRequest request) {
        // 校验板块类型
        if (!CommunityConstants.CATEGORY_INTERVIEW_EXP.equals(request.getCategory())
                && !CommunityConstants.CATEGORY_REFERRAL.equals(request.getCategory())) {
            throw new BusinessException("无效的帖子板块类型");
        }

        // 校验内容长度
        if (request.getContent().length() > CommunityConstants.MAX_CONTENT_LENGTH) {
            throw new BusinessException("帖子内容不能超过" + CommunityConstants.MAX_CONTENT_LENGTH + "字");
        }

        // 校验图片数量
        if (request.getImages() != null && request.getImages().size() > CommunityConstants.MAX_IMAGE_COUNT) {
            throw new BusinessException("图片数量不能超过" + CommunityConstants.MAX_IMAGE_COUNT + "张");
        }

        CommunityPost post = new CommunityPost();
        post.setUserId(userId);
        post.setCategory(request.getCategory());
        post.setContent(request.getContent().trim());
        post.setLikeCount(0);
        post.setCommentCount(0);
        post.setIsDeleted(0);

        // 图片列表转JSON存储
        if (request.getImages() != null && !request.getImages().isEmpty()) {
            try {
                post.setImages(objectMapper.writeValueAsString(request.getImages()));
            } catch (JsonProcessingException e) {
                log.error("图片列表序列化失败", e);
                throw new BusinessException("图片数据处理失败");
            }
        }

        postMapper.insert(post);
        log.info("帖子创建成功, postId: {}, userId: {}, category: {}", post.getId(), userId, request.getCategory());
        return post.getId();
    }

    /**
     * 分页查询当前用户的帖子
     *
     * @param userId   用户ID
     * @param pageNum  页码
     * @param pageSize 每页大小
     * @return 分页结果
     */
    public PageResult<PostVO> listMyPosts(Long userId, Integer pageNum, Integer pageSize) {
        int safeSize = Math.min(pageSize, CommunityConstants.MAX_PAGE_SIZE);

        LambdaQueryWrapper<CommunityPost> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(CommunityPost::getUserId, userId)
                .orderByDesc(CommunityPost::getCreateTime);

        Page<CommunityPost> page = new Page<>(pageNum, safeSize);
        Page<CommunityPost> resultPage = postMapper.selectPage(page, wrapper);
        log.debug("[个人动态] userId: {}, 查询到 {} 条帖子, total: {}", userId, resultPage.getRecords().size(), resultPage.getTotal());

        // 批量查询作者信息
        Set<Long> userIds = resultPage.getRecords().stream()
                .map(CommunityPost::getUserId)
                .collect(Collectors.toSet());
        Map<Long, SysUser> userMap = batchGetUsers(userIds);

        // 批量查询点赞状态
        Set<Long> likedPostIds = batchCheckLiked(userId, resultPage.getRecords());

        // 批量查询收藏状态
        Set<Long> favoritedPostIds = batchCheckFavorited(userId, resultPage.getRecords());

        List<PostVO> voList = resultPage.getRecords().stream()
                .map(post -> toPostVO(post, userMap, likedPostIds, favoritedPostIds))
                .toList();

        return PageResult.of(voList, resultPage.getTotal(), pageNum, safeSize);
    }

    /**
     * 分页查询用户点赞过的帖子
     *
     * @param userId   用户ID
     * @param pageNum  页码
     * @param pageSize 每页大小
     * @return 分页结果
     */
    public PageResult<PostVO> listLikedPosts(Long userId, Integer pageNum, Integer pageSize) {
        int safeSize = Math.min(pageSize, CommunityConstants.MAX_PAGE_SIZE);

        // 第一步：分页查询用户点赞记录（获取帖子ID列表）
        LambdaQueryWrapper<CommunityPostLike> likeWrapper = new LambdaQueryWrapper<>();
        likeWrapper.eq(CommunityPostLike::getUserId, userId)
                .orderByDesc(CommunityPostLike::getCreateTime);
        Page<CommunityPostLike> likePage = new Page<>(pageNum, safeSize);
        Page<CommunityPostLike> likeResult = likeMapper.selectPage(likePage, likeWrapper);

        if (likeResult.getRecords().isEmpty()) {
            return PageResult.of(Collections.emptyList(), 0, pageNum, safeSize);
        }

        // 第二步：批量查询帖子详情
        Set<Long> postIds = likeResult.getRecords().stream()
                .map(CommunityPostLike::getPostId)
                .collect(Collectors.toSet());
        LambdaQueryWrapper<CommunityPost> postWrapper = new LambdaQueryWrapper<>();
        postWrapper.in(CommunityPost::getId, postIds);
        Map<Long, CommunityPost> postMap = postMapper.selectList(postWrapper).stream()
                .collect(Collectors.toMap(CommunityPost::getId, p -> p));

        // 按点赞时间排序组装列表
        List<CommunityPost> posts = likeResult.getRecords().stream()
                .map(like -> postMap.get(like.getPostId()))
                .filter(Objects::nonNull)
                .toList();

        Set<Long> userIds = posts.stream()
                .map(CommunityPost::getUserId)
                .collect(Collectors.toSet());
        Map<Long, SysUser> userMap = batchGetUsers(userIds);
        Set<Long> likedPostIds = batchCheckLiked(userId, posts);
        Set<Long> favoritedPostIds = batchCheckFavorited(userId, posts);

        List<PostVO> voList = posts.stream()
                .map(post -> toPostVO(post, userMap, likedPostIds, favoritedPostIds))
                .toList();

        // likeResult.getTotal() 包含已删帖的点赞记录，用实际返回数量判断是否有更多
        boolean hasMore = likeResult.getRecords().size() == safeSize;
        long total = hasMore ? likeResult.getTotal() : voList.size();
        return PageResult.of(voList, total, pageNum, safeSize);
    }

    /**
     * 分页查询用户收藏过的帖子
     *
     * @param userId   用户ID
     * @param pageNum  页码
     * @param pageSize 每页大小
     * @return 分页结果
     */
    public PageResult<PostVO> listFavoritedPosts(Long userId, Integer pageNum, Integer pageSize) {
        int safeSize = Math.min(pageSize, CommunityConstants.MAX_PAGE_SIZE);

        // 第一步：分页查询用户收藏记录（获取帖子ID列表）
        LambdaQueryWrapper<CommunityPostFavorite> favWrapper = new LambdaQueryWrapper<>();
        favWrapper.eq(CommunityPostFavorite::getUserId, userId)
                .orderByDesc(CommunityPostFavorite::getCreateTime);
        Page<CommunityPostFavorite> favPage = new Page<>(pageNum, safeSize);
        Page<CommunityPostFavorite> favResult = favoriteMapper.selectPage(favPage, favWrapper);

        if (favResult.getRecords().isEmpty()) {
            return PageResult.of(Collections.emptyList(), 0, pageNum, safeSize);
        }

        // 第二步：批量查询帖子详情
        Set<Long> postIds = favResult.getRecords().stream()
                .map(CommunityPostFavorite::getPostId)
                .collect(Collectors.toSet());
        LambdaQueryWrapper<CommunityPost> postWrapper = new LambdaQueryWrapper<>();
        postWrapper.in(CommunityPost::getId, postIds);
        Map<Long, CommunityPost> postMap = postMapper.selectList(postWrapper).stream()
                .collect(Collectors.toMap(CommunityPost::getId, p -> p));

        // 按收藏时间排序组装列表
        List<CommunityPost> posts = favResult.getRecords().stream()
                .map(fav -> postMap.get(fav.getPostId()))
                .filter(Objects::nonNull)
                .toList();

        Set<Long> userIds = posts.stream()
                .map(CommunityPost::getUserId)
                .collect(Collectors.toSet());
        Map<Long, SysUser> userMap = batchGetUsers(userIds);
        Set<Long> likedPostIds = batchCheckLiked(userId, posts);
        Set<Long> favoritedPostIds = batchCheckFavorited(userId, posts);

        List<PostVO> voList = posts.stream()
                .map(post -> toPostVO(post, userMap, likedPostIds, favoritedPostIds))
                .toList();

        boolean hasMore = favResult.getRecords().size() == safeSize;
        long total = hasMore ? favResult.getTotal() : voList.size();
        return PageResult.of(voList, total, pageNum, safeSize);
    }

    /**
     * 分页查询用户评论过的帖子
     *
     * @param userId   用户ID
     * @param pageNum  页码
     * @param pageSize 每页大小
     * @return 分页结果
     */
    public PageResult<PostVO> listCommentedPosts(Long userId, Integer pageNum, Integer pageSize) {
        int safeSize = Math.min(pageSize, CommunityConstants.MAX_PAGE_SIZE);

        // 第一步：查询用户所有评论，去重获取帖子ID并按最近评论时间排序
        LambdaQueryWrapper<CommunityComment> commentWrapper = new LambdaQueryWrapper<>();
        commentWrapper.eq(CommunityComment::getUserId, userId)
                .orderByDesc(CommunityComment::getCreateTime);
        List<CommunityComment> allComments = commentMapper.selectList(commentWrapper);

        // 去重：每个帖子只保留最新评论，按最近评论时间排序
        Set<Long> seen = new LinkedHashSet<>();
        for (CommunityComment c : allComments) {
            seen.add(c.getPostId());
        }
        List<Long> sortedPostIds = new ArrayList<>(seen);

        if (sortedPostIds.isEmpty()) {
            return PageResult.of(Collections.emptyList(), 0, pageNum, safeSize);
        }

        // 第二步：内存分页
        int total = sortedPostIds.size();
        int fromIndex = (pageNum - 1) * safeSize;
        int toIndex = Math.min(fromIndex + safeSize, total);
        if (fromIndex >= total) {
            return PageResult.of(Collections.emptyList(), total, pageNum, safeSize);
        }
        List<Long> pagePostIds = sortedPostIds.subList(fromIndex, toIndex);

        // 第三步：批量查询帖子详情
        LambdaQueryWrapper<CommunityPost> postWrapper = new LambdaQueryWrapper<>();
        postWrapper.in(CommunityPost::getId, pagePostIds);
        Map<Long, CommunityPost> postMap = postMapper.selectList(postWrapper).stream()
                .collect(Collectors.toMap(CommunityPost::getId, p -> p));

        List<CommunityPost> posts = pagePostIds.stream()
                .map(postMap::get)
                .filter(Objects::nonNull)
                .toList();

        Set<Long> userIds = posts.stream()
                .map(CommunityPost::getUserId)
                .collect(Collectors.toSet());
        Map<Long, SysUser> userMap = batchGetUsers(userIds);
        Set<Long> likedPostIds = batchCheckLiked(userId, posts);
        Set<Long> favoritedPostIds = batchCheckFavorited(userId, posts);

        List<PostVO> voList = posts.stream()
                .map(post -> toPostVO(post, userMap, likedPostIds, favoritedPostIds))
                .toList();

        // total 包含已删帖的评论记录，用实际返回数量判断最后一页
        boolean hasMore = sortedPostIds.size() > toIndex;
        long displayTotal = hasMore ? total : voList.size();
        return PageResult.of(voList, displayTotal, pageNum, safeSize);
    }

    /**
     * 分页查询当前用户发布的评论（附带所属帖子信息）
     *
     * @param userId   用户ID
     * @param pageNum  页码
     * @param pageSize 每页大小
     * @return 分页结果
     */
    public PageResult<MyCommentVO> listMyComments(Long userId, Integer pageNum, Integer pageSize) {
        int safeSize = Math.min(pageSize, CommunityConstants.MAX_PAGE_SIZE);

        // 第一步：分页查询当前用户的评论
        LambdaQueryWrapper<CommunityComment> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(CommunityComment::getUserId, userId)
                .orderByDesc(CommunityComment::getCreateTime);
        Page<CommunityComment> page = new Page<>(pageNum, safeSize);
        Page<CommunityComment> resultPage = commentMapper.selectPage(page, wrapper);

        if (resultPage.getRecords().isEmpty()) {
            return PageResult.of(Collections.emptyList(), 0, pageNum, safeSize);
        }

        // 第二步：批量查询评论所属的帖子
        Set<Long> postIds = resultPage.getRecords().stream()
                .map(CommunityComment::getPostId)
                .collect(Collectors.toSet());
        LambdaQueryWrapper<CommunityPost> postWrapper = new LambdaQueryWrapper<>();
        postWrapper.in(CommunityPost::getId, postIds);
        Map<Long, CommunityPost> postMap = postMapper.selectList(postWrapper).stream()
                .collect(Collectors.toMap(CommunityPost::getId, p -> p));

        // 第三步：批量查询帖子作者信息
        Set<Long> authorIds = postMap.values().stream()
                .map(CommunityPost::getUserId)
                .collect(Collectors.toSet());
        Map<Long, SysUser> userMap = batchGetUsers(authorIds);

        // 第四步：组装VO
        List<MyCommentVO> voList = resultPage.getRecords().stream()
                .map(comment -> {
                    CommunityPost post = postMap.get(comment.getPostId());
                    boolean deleted = post == null;
                    SysUser author = !deleted ? userMap.get(post.getUserId()) : null;
                    String authorName = deleted ? "帖子已被删除"
                            : (author != null ? (author.getNickname() != null ? author.getNickname() : author.getUsername()) : "匿名用户");
                    return MyCommentVO.builder()
                            .commentId(comment.getId())
                            .commentContent(comment.getContent())
                            .commentTime(comment.getCreateTime())
                            .postId(comment.getPostId())
                            .postCategory(!deleted ? post.getCategory() : null)
                            .postContent(!deleted ? post.getContent() : null)
                            .postImages(!deleted ? post.getImages() : null)
                            .postAuthorName(authorName)
                            .postDeleted(deleted)
                            .build();
                })
                .toList();

        return PageResult.of(voList, resultPage.getTotal(), pageNum, safeSize);
    }

    /**
     * 删除帖子（仅限帖子作者本人）
     *
     * @param userId 用户ID
     * @param postId 帖子ID
     */
    public void deletePost(Long userId, Long postId) {
        CommunityPost post = postMapper.selectById(postId);
        if (post == null) {
            throw new BusinessException("帖子不存在");
        }
        if (!post.getUserId().equals(userId)) {
            throw new BusinessException("只能删除自己发布的帖子");
        }
        postMapper.deleteById(postId);
        // 级联删除该帖子的所有收藏记录
        LambdaQueryWrapper<CommunityPostFavorite> favWrapper = new LambdaQueryWrapper<>();
        favWrapper.eq(CommunityPostFavorite::getPostId, postId);
        favoriteMapper.delete(favWrapper);
        log.info("帖子删除成功, postId: {}, userId: {}（含级联删除收藏记录）", postId, userId);
    }

    // ==================== 点赞相关 ====================

    /**
     * 点赞/取消点赞（幂等操作）
     * 已点赞则取消点赞，未点赞则点赞
     *
     * @param userId 用户ID
     * @param postId 帖子ID
     * @return 操作后的点赞状态：true-已点赞，false-已取消点赞
     */
    @Transactional(rollbackFor = Exception.class)
    public boolean toggleLike(Long userId, Long postId) {
        // 校验帖子是否存在
        CommunityPost post = postMapper.selectById(postId);
        if (post == null) {
            throw new BusinessException("帖子不存在");
        }

        // 查询是否已点赞
        LambdaQueryWrapper<CommunityPostLike> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(CommunityPostLike::getPostId, postId)
                .eq(CommunityPostLike::getUserId, userId);
        CommunityPostLike existingLike = likeMapper.selectOne(queryWrapper);

        if (existingLike != null) {
            // 已点赞 -> 取消点赞（物理删除）
            likeMapper.deleteById(existingLike.getId());
            // 帖子点赞数-1
            decrementLikeCount(postId);
            log.info("取消点赞成功, userId: {}, postId: {}", userId, postId);
            return false;
        } else {
            // 未点赞 -> 点赞
            CommunityPostLike like = new CommunityPostLike();
            like.setPostId(postId);
            like.setUserId(userId);
            likeMapper.insert(like);
            // 帖子点赞数+1
            incrementLikeCount(postId);
            log.info("点赞成功, userId: {}, postId: {}", userId, postId);
            return true;
        }
    }

    // ==================== 收藏相关 ====================

    /**
     * 收藏/取消收藏（幂等操作）
     * 已收藏则取消收藏，未收藏则收藏
     * 收藏成功时通知帖子作者
     *
     * @param userId 用户ID
     * @param postId 帖子ID
     * @return 操作后的收藏状态：true-已收藏，false-已取消收藏
     */
    @Transactional(rollbackFor = Exception.class)
    public boolean toggleFavorite(Long userId, Long postId) {
        // 校验帖子是否存在
        CommunityPost post = postMapper.selectById(postId);
        if (post == null) {
            throw new BusinessException("帖子不存在");
        }

        // 查询是否已收藏
        LambdaQueryWrapper<CommunityPostFavorite> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(CommunityPostFavorite::getPostId, postId)
                .eq(CommunityPostFavorite::getUserId, userId);
        CommunityPostFavorite existingFav = favoriteMapper.selectOne(queryWrapper);

        if (existingFav != null) {
            // 已收藏 -> 取消收藏（物理删除）
            favoriteMapper.deleteById(existingFav.getId());
            log.info("取消收藏成功, userId: {}, postId: {}", userId, postId);
            return false;
        } else {
            // 未收藏 -> 收藏
            CommunityPostFavorite fav = new CommunityPostFavorite();
            fav.setPostId(postId);
            fav.setUserId(userId);
            favoriteMapper.insert(fav);
            log.info("收藏成功, userId: {}, postId: {}", userId, postId);
            return true;
        }
    }

    // ==================== 评论相关 ====================

    /**
     * 分页查询帖子评论
     *
     * @param postId        帖子ID
     * @param currentUserId 当前登录用户ID
     * @param pageNum       页码
     * @param pageSize      每页大小
     * @return 评论分页结果
     */
    public PageResult<CommentVO> listComments(Long postId, Long currentUserId, Integer pageNum, Integer pageSize) {
        int safeSize = Math.min(pageSize, CommunityConstants.MAX_PAGE_SIZE);

        // 查询帖子作者ID
        CommunityPost post = postMapper.selectById(postId);
        Long postAuthorId = post != null ? post.getUserId() : null;

        // 只查询顶级评论（parentCommentId IS NULL）
        LambdaQueryWrapper<CommunityComment> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(CommunityComment::getPostId, postId)
                .isNull(CommunityComment::getParentCommentId)
                .orderByAsc(CommunityComment::getCreateTime);

        Page<CommunityComment> page = new Page<>(pageNum, safeSize);
        Page<CommunityComment> resultPage = commentMapper.selectPage(page, wrapper);

        if (resultPage.getRecords().isEmpty()) {
            return PageResult.of(Collections.emptyList(), 0, pageNum, safeSize);
        }

        // 批量查询评论者信息
        Set<Long> userIds = resultPage.getRecords().stream()
                .map(CommunityComment::getUserId)
                .collect(Collectors.toSet());
        Map<Long, SysUser> userMap = batchGetUsers(userIds);

        // 批量查询每个顶级评论的回复数量
        Set<Long> commentIds = resultPage.getRecords().stream()
                .map(CommunityComment::getId)
                .collect(Collectors.toSet());
        Map<Long, Long> replyCountMap;
        try {
            replyCountMap = batchGetReplyCounts(commentIds);
        } catch (Exception e) {
            log.error("查询回复数量失败, 降级为0", e);
            replyCountMap = Collections.emptyMap();
        }

        Map<Long, Long> finalReplyCountMap = replyCountMap;
        List<CommentVO> voList = resultPage.getRecords().stream()
                .map(comment -> {
                    CommentVO vo = toCommentVO(comment, userMap, postAuthorId, currentUserId);
                    vo.setReplyCount(finalReplyCountMap.getOrDefault(comment.getId(), 0L).intValue());
                    return vo;
                })
                .toList();

        return PageResult.of(voList, resultPage.getTotal(), pageNum, safeSize);
    }

    /**
     * 分页查询某条评论的回复
     *
     * @param postId        帖子ID
     * @param commentId     父评论ID
     * @param currentUserId 当前用户ID
     * @param pageNum       页码
     * @param pageSize      每页大小
     * @return 回复分页结果
     */
    public PageResult<CommentVO> listReplies(Long postId, Long commentId, Long currentUserId, Integer pageNum, Integer pageSize) {
        int safeSize = Math.min(pageSize, CommunityConstants.MAX_PAGE_SIZE);

        CommunityPost post = postMapper.selectById(postId);
        Long postAuthorId = post != null ? post.getUserId() : null;

        LambdaQueryWrapper<CommunityComment> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(CommunityComment::getPostId, postId)
                .eq(CommunityComment::getParentCommentId, commentId)
                .orderByAsc(CommunityComment::getCreateTime);

        Page<CommunityComment> page = new Page<>(pageNum, safeSize);
        Page<CommunityComment> resultPage = commentMapper.selectPage(page, wrapper);

        if (resultPage.getRecords().isEmpty()) {
            return PageResult.of(Collections.emptyList(), 0, pageNum, safeSize);
        }

        // 批量查询回复者 + 被回复者信息
        Set<Long> userIds = resultPage.getRecords().stream()
                .map(CommunityComment::getUserId)
                .collect(Collectors.toSet());
        resultPage.getRecords().stream()
                .map(CommunityComment::getReplyToUserId)
                .filter(Objects::nonNull)
                .forEach(userIds::add);
        Map<Long, SysUser> userMap = batchGetUsers(userIds);

        List<CommentVO> voList = resultPage.getRecords().stream()
                .map(reply -> {
                    CommentVO vo = toCommentVO(reply, userMap, postAuthorId, currentUserId);
                    if (reply.getReplyToUserId() != null) {
                        SysUser replyToUser = userMap.get(reply.getReplyToUserId());
                        vo.setReplyToUserName(replyToUser != null
                                ? (replyToUser.getNickname() != null ? replyToUser.getNickname() : replyToUser.getUsername())
                                : "匿名用户");
                    }
                    return vo;
                })
                .toList();

        return PageResult.of(voList, resultPage.getTotal(), pageNum, safeSize);
    }

    /**
     * 批量查询评论的回复数量
     */
    private Map<Long, Long> batchGetReplyCounts(Set<Long> commentIds) {
        if (commentIds.isEmpty()) {
            return Collections.emptyMap();
        }
        LambdaQueryWrapper<CommunityComment> wrapper = new LambdaQueryWrapper<>();
        wrapper.in(CommunityComment::getParentCommentId, commentIds);
        List<CommunityComment> replies = commentMapper.selectList(wrapper);
        Map<Long, Long> result = new HashMap<>();
        for (CommunityComment reply : replies) {
            result.merge(reply.getParentCommentId(), 1L, Long::sum);
        }
        return result;
    }

    /**
     * 创建评论
     *
     * @param userId  评论者用户ID
     * @param postId  帖子ID
     * @param request 评论请求
     * @return 评论ID
     */
    @Transactional(rollbackFor = Exception.class)
    public Long createComment(Long userId, Long postId, CreateCommentRequest request) {
        // 校验帖子是否存在
        CommunityPost post = postMapper.selectById(postId);
        if (post == null) {
            throw new BusinessException("帖子不存在");
        }

        // 校验内容长度
        if (request.getContent().length() > CommunityConstants.MAX_COMMENT_LENGTH) {
            throw new BusinessException("评论内容不能超过" + CommunityConstants.MAX_COMMENT_LENGTH + "字");
        }

        CommunityComment comment = new CommunityComment();
        comment.setPostId(postId);
        comment.setUserId(userId);
        comment.setContent(request.getContent().trim());

        // 处理回复逻辑
        Long parentCommentId = request.getParentCommentId();
        if (parentCommentId != null) {
            CommunityComment parentComment = commentMapper.selectById(parentCommentId);
            if (parentComment == null || !parentComment.getPostId().equals(postId)) {
                throw new BusinessException("被回复的评论不存在");
            }
            // 回复嵌套评论时，自动找到顶层评论作为 parentCommentId
            Long rootCommentId = parentCommentId;
            CommunityComment ancestor = parentComment;
            while (ancestor.getParentCommentId() != null) {
                rootCommentId = ancestor.getParentCommentId();
                ancestor = commentMapper.selectById(rootCommentId);
                if (ancestor == null) {
                    // 祖先评论已被删除，使用当前已知的 rootCommentId
                    break;
                }
            }
            comment.setParentCommentId(rootCommentId);
            // 优先使用前端传入的 replyToUserId，否则默认为被回复评论的作者
            Long replyToUserId = request.getReplyToUserId();
            comment.setReplyToUserId(replyToUserId != null ? replyToUserId : parentComment.getUserId());
        }

        commentMapper.insert(comment);

        // 帖子评论数+1
        incrementCommentCount(postId);
        log.info("评论创建成功, commentId: {}, postId: {}, userId: {}, parentId: {}", comment.getId(), postId, userId, parentCommentId);
        return comment.getId();
    }

    /**
     * 删除评论
     * 规则：评论作者本人可删除，帖子作者也可删除其帖子下的任意评论
     *
     * @param userId    当前操作用户ID
     * @param postId    帖子ID
     * @param commentId 评论ID
     */
    @Transactional(rollbackFor = Exception.class)
    public void deleteComment(Long userId, Long postId, Long commentId) {
        CommunityComment comment = commentMapper.selectById(commentId);
        if (comment == null || !comment.getPostId().equals(postId)) {
            throw new BusinessException("评论不存在");
        }

        // 判断权限：评论本人 或 帖子作者
        boolean isCommentOwner = comment.getUserId().equals(userId);
        boolean isPostOwner = false;
        if (!isCommentOwner) {
            CommunityPost post = postMapper.selectById(postId);
            isPostOwner = post != null && post.getUserId().equals(userId);
        }

        if (!isCommentOwner && !isPostOwner) {
            throw new BusinessException("没有权限删除此评论");
        }

        commentMapper.deleteById(commentId);
        // 帖子评论数-1
        decrementCommentCount(postId);
        log.info("评论删除成功, commentId: {}, postId: {}, userId: {}", commentId, postId, userId);
    }

    // ==================== 图片上传 ====================

    /**
     * 上传社区图片
     *
     * @param file   图片文件
     * @param userId 上传者用户ID
     * @return 图片访问URL
     */
    public String uploadImage(MultipartFile file, Long userId) {
        // 校验文件
        if (file.isEmpty()) {
            throw new BusinessException("上传文件不能为空");
        }

        // 校验文件大小
        if (file.getSize() > maxFileSize) {
            throw new BusinessException("图片大小不能超过" + (maxFileSize / 1024 / 1024) + "MB");
        }

        // 校验文件类型
        String originalFilename = file.getOriginalFilename();
        if (originalFilename == null) {
            throw new BusinessException("文件名不能为空");
        }
        String lowerName = originalFilename.toLowerCase();
        if (!lowerName.endsWith(".jpg") && !lowerName.endsWith(".jpeg")
                && !lowerName.endsWith(".png") && !lowerName.endsWith(".gif")
                && !lowerName.endsWith(".webp")) {
            throw new BusinessException("仅支持 JPG、PNG、GIF、WebP 格式图片");
        }

        // 生成唯一文件名
        String ext = lowerName.substring(lowerName.lastIndexOf("."));
        String fileName = UUID.randomUUID().toString().replace("-", "") + ext;

        // 确定上传目录
        String uploadDir;
        if (configuredUploadDir != null && !configuredUploadDir.isBlank()) {
            uploadDir = configuredUploadDir;
        } else {
            uploadDir = System.getProperty("user.dir") + "/uploads/community/";
        }

        try {
            File dir = new File(uploadDir);
            if (!dir.exists()) {
                dir.mkdirs();
            }

            File destFile = new File(dir, fileName);
            file.transferTo(destFile);

            String fileUrl = "/uploads/community/" + fileName;
            log.info("社区图片上传成功, userId: {}, fileName: {}, fileUrl: {}", userId, fileName, fileUrl);
            return fileUrl;
        } catch (IOException e) {
            log.error("社区图片上传失败, userId: {}, fileName: {}", userId, fileName, e);
            throw new BusinessException("图片上传失败，请稍后重试");
        }
    }

    /**
     * 获取当前用户收到的互动信息（别人对我帖子的点赞和评论）
     *
     * @param userId   当前用户ID
     * @param pageNum  页码
     * @param pageSize 每页大小
     * @return 互动信息VO（分组：点赞列表 + 评论列表）
     */
    public ReceivedInteractionVO listReceivedInteractions(Long userId, Integer pageNum, Integer pageSize) {
        int safeSize = Math.min(pageSize, CommunityConstants.MAX_PAGE_SIZE);

        // 1. 查询我的所有帖子ID
        LambdaQueryWrapper<CommunityPost> myPostWrapper = new LambdaQueryWrapper<>();
        myPostWrapper.eq(CommunityPost::getUserId, userId)
                .select(CommunityPost::getId);
        List<CommunityPost> myPosts = postMapper.selectList(myPostWrapper);
        Set<Long> myPostIds = myPosts.stream().map(CommunityPost::getId).collect(Collectors.toSet());

        if (myPostIds.isEmpty()) {
            return ReceivedInteractionVO.builder()
                    .likes(Collections.emptyList()).totalLikes(0)
                    .comments(Collections.emptyList()).totalComments(0)
                    .replies(Collections.emptyList()).totalReplies(0)
                    .favorites(Collections.emptyList()).totalFavorites(0)
                    .build();
        }

        // 2. 查询收到的点赞（排除自己的点赞）
        LambdaQueryWrapper<CommunityPostLike> likeWrapper = new LambdaQueryWrapper<>();
        likeWrapper.in(CommunityPostLike::getPostId, myPostIds)
                .ne(CommunityPostLike::getUserId, userId)
                .orderByDesc(CommunityPostLike::getCreateTime);
        List<CommunityPostLike> allLikes = likeMapper.selectList(likeWrapper);
        int totalLikes = allLikes.size();

        // 分页截取
        int likeFrom = (pageNum - 1) * safeSize;
        int likeTo = Math.min(likeFrom + safeSize, totalLikes);
        List<CommunityPostLike> pageLikes = likeFrom < totalLikes
                ? allLikes.subList(likeFrom, likeTo)
                : Collections.emptyList();

        // 3. 查询收到的评论（排除自己的评论，只查顶级评论）
        LambdaQueryWrapper<CommunityComment> commentWrapper = new LambdaQueryWrapper<>();
        commentWrapper.in(CommunityComment::getPostId, myPostIds)
                .ne(CommunityComment::getUserId, userId)
                .isNull(CommunityComment::getParentCommentId)
                .orderByDesc(CommunityComment::getCreateTime);
        List<CommunityComment> allComments = commentMapper.selectList(commentWrapper);
        int totalComments = allComments.size();

        // 分页截取
        int commentFrom = (pageNum - 1) * safeSize;
        int commentTo = Math.min(commentFrom + safeSize, totalComments);
        List<CommunityComment> pageComments = commentFrom < totalComments
                ? allComments.subList(commentFrom, commentTo)
                : Collections.emptyList();

        // 4. 查询收到的回复（别人回复了我的评论）
        // 先查我所有评论的ID
        LambdaQueryWrapper<CommunityComment> myCommentIdWrapper = new LambdaQueryWrapper<>();
        myCommentIdWrapper.eq(CommunityComment::getUserId, userId)
                .select(CommunityComment::getId);
        List<CommunityComment> myCommentEntities = commentMapper.selectList(myCommentIdWrapper);
        Set<Long> myCommentIds = myCommentEntities.stream()
                .map(CommunityComment::getId)
                .collect(Collectors.toSet());

        List<CommunityComment> allReplies = Collections.emptyList();
        int totalReplies = 0;
        if (!myCommentIds.isEmpty()) {
            LambdaQueryWrapper<CommunityComment> replyWrapper = new LambdaQueryWrapper<>();
            replyWrapper.in(CommunityComment::getParentCommentId, myCommentIds)
                    .ne(CommunityComment::getUserId, userId)
                    .orderByDesc(CommunityComment::getCreateTime);
            allReplies = commentMapper.selectList(replyWrapper);
            totalReplies = allReplies.size();
        }

        // 分页截取
        int replyFrom = (pageNum - 1) * safeSize;
        int replyTo = Math.min(replyFrom + safeSize, totalReplies);
        List<CommunityComment> pageReplies = replyFrom < totalReplies
                ? allReplies.subList(replyFrom, replyTo)
                : Collections.emptyList();

        // 4.5. 查询收到的收藏（别人收藏了我的帖子）
        LambdaQueryWrapper<CommunityPostFavorite> favWrapper = new LambdaQueryWrapper<>();
        favWrapper.in(CommunityPostFavorite::getPostId, myPostIds)
                .ne(CommunityPostFavorite::getUserId, userId)
                .orderByDesc(CommunityPostFavorite::getCreateTime);
        List<CommunityPostFavorite> allFavorites = favoriteMapper.selectList(favWrapper);
        int totalFavorites = allFavorites.size();

        // 分页截取
        int favFrom = (pageNum - 1) * safeSize;
        int favTo = Math.min(favFrom + safeSize, totalFavorites);
        List<CommunityPostFavorite> pageFavorites = favFrom < totalFavorites
                ? allFavorites.subList(favFrom, favTo)
                : Collections.emptyList();

        // 5. 批量查询帖子信息（用于显示帖子摘要）
        Set<Long> relatedPostIds = new HashSet<>();
        pageLikes.forEach(l -> relatedPostIds.add(l.getPostId()));
        pageComments.forEach(c -> relatedPostIds.add(c.getPostId()));
        pageReplies.forEach(r -> relatedPostIds.add(r.getPostId()));
        pageFavorites.forEach(f -> relatedPostIds.add(f.getPostId()));
        LambdaQueryWrapper<CommunityPost> postWrapper = new LambdaQueryWrapper<>();
        postWrapper.in(CommunityPost::getId, relatedPostIds);
        Map<Long, CommunityPost> postMap = postMapper.selectList(postWrapper).stream()
                .collect(Collectors.toMap(CommunityPost::getId, p -> p));

        // 6. 批量查询用户信息
        Set<Long> actorIds = new HashSet<>();
        pageLikes.forEach(l -> actorIds.add(l.getUserId()));
        pageComments.forEach(c -> actorIds.add(c.getUserId()));
        pageReplies.forEach(r -> actorIds.add(r.getUserId()));
        pageFavorites.forEach(f -> actorIds.add(f.getUserId()));

        // 回复条目还需要查询被回复的原评论内容
        Set<Long> parentCommentIds = pageReplies.stream()
                .map(CommunityComment::getParentCommentId)
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());
        Map<Long, String> parentCommentContentMap = new HashMap<>();
        if (!parentCommentIds.isEmpty()) {
            LambdaQueryWrapper<CommunityComment> parentWrapper = new LambdaQueryWrapper<>();
            parentWrapper.in(CommunityComment::getId, parentCommentIds);
            commentMapper.selectList(parentWrapper).forEach(c ->
                    parentCommentContentMap.put(c.getId(), c.getContent()));
        }

        Map<Long, SysUser> userMap = batchGetUsers(actorIds);

        // 7. 组装点赞条目
        List<ReceivedInteractionVO.LikeItem> likeItems = pageLikes.stream()
                .map(like -> {
                    SysUser user = userMap.get(like.getUserId());
                    CommunityPost post = postMap.get(like.getPostId());
                    return ReceivedInteractionVO.LikeItem.builder()
                            .userId(like.getUserId())
                            .userName(formatUserName(user))
                            .postId(like.getPostId())
                            .postContent(post != null ? truncate(post.getContent(), 80) : "已删除")
                            .postCategory(post != null ? post.getCategory() : null)
                            .createTime(like.getCreateTime())
                            .build();
                })
                .toList();

        // 8. 组装评论条目
        List<ReceivedInteractionVO.CommentItem> commentItems = pageComments.stream()
                .map(comment -> {
                    SysUser user = userMap.get(comment.getUserId());
                    CommunityPost post = postMap.get(comment.getPostId());
                    return ReceivedInteractionVO.CommentItem.builder()
                            .userId(comment.getUserId())
                            .userName(formatUserName(user))
                            .commentContent(comment.getContent())
                            .postId(comment.getPostId())
                            .postContent(post != null ? truncate(post.getContent(), 80) : "已删除")
                            .postCategory(post != null ? post.getCategory() : null)
                            .createTime(comment.getCreateTime())
                            .build();
                })
                .toList();

        // 9. 组装回复条目
        List<ReceivedInteractionVO.ReplyItem> replyItems = pageReplies.stream()
                .map(reply -> {
                    SysUser user = userMap.get(reply.getUserId());
                    CommunityPost post = postMap.get(reply.getPostId());
                    return ReceivedInteractionVO.ReplyItem.builder()
                            .userId(reply.getUserId())
                            .userName(formatUserName(user))
                            .replyContent(reply.getContent())
                            .parentCommentContent(parentCommentContentMap.get(reply.getParentCommentId()))
                            .postId(reply.getPostId())
                            .postContent(post != null ? truncate(post.getContent(), 80) : "已删除")
                            .postCategory(post != null ? post.getCategory() : null)
                            .createTime(reply.getCreateTime())
                            .build();
                })
                .toList();

        // 10. 组装收藏条目
        List<ReceivedInteractionVO.FavoriteItem> favItems = pageFavorites.stream()
                .map(fav -> {
                    SysUser user = userMap.get(fav.getUserId());
                    CommunityPost post = postMap.get(fav.getPostId());
                    return ReceivedInteractionVO.FavoriteItem.builder()
                            .userId(fav.getUserId())
                            .userName(formatUserName(user))
                            .postId(fav.getPostId())
                            .postContent(post != null ? truncate(post.getContent(), 80) : "已删除")
                            .postCategory(post != null ? post.getCategory() : null)
                            .createTime(fav.getCreateTime())
                            .build();
                })
                .toList();

        return ReceivedInteractionVO.builder()
                .likes(likeItems)
                .totalLikes(totalLikes)
                .comments(commentItems)
                .totalComments(totalComments)
                .replies(replyItems)
                .totalReplies(totalReplies)
                .favorites(favItems)
                .totalFavorites(totalFavorites)
                .build();
    }

    /**
     * 获取未读互动数量（收到的点赞 + 评论 + 回复，时间在since之后）
     *
     * @param userId 当前用户ID
     * @param since  上次查看时间
     * @return 未读数量
     */
    public int getUnreadInteractionCount(Long userId, LocalDateTime since) {
        // 查询我的帖子ID
        LambdaQueryWrapper<CommunityPost> myPostWrapper = new LambdaQueryWrapper<>();
        myPostWrapper.eq(CommunityPost::getUserId, userId)
                .select(CommunityPost::getId);
        List<CommunityPost> myPosts = postMapper.selectList(myPostWrapper);
        Set<Long> myPostIds = myPosts.stream().map(CommunityPost::getId).collect(Collectors.toSet());

        if (myPostIds.isEmpty()) {
            return 0;
        }

        int count = 0;

        // 未读点赞
        LambdaQueryWrapper<CommunityPostLike> likeWrapper = new LambdaQueryWrapper<>();
        likeWrapper.in(CommunityPostLike::getPostId, myPostIds)
                .ne(CommunityPostLike::getUserId, userId)
                .gt(CommunityPostLike::getCreateTime, since);
        count += likeMapper.selectCount(likeWrapper).intValue();

        // 未读评论（顶级评论）
        LambdaQueryWrapper<CommunityComment> commentWrapper = new LambdaQueryWrapper<>();
        commentWrapper.in(CommunityComment::getPostId, myPostIds)
                .ne(CommunityComment::getUserId, userId)
                .isNull(CommunityComment::getParentCommentId)
                .gt(CommunityComment::getCreateTime, since);
        count += commentMapper.selectCount(commentWrapper).intValue();

        // 未读回复（别人回复了我的评论）
        LambdaQueryWrapper<CommunityComment> myCommentIdWrapper = new LambdaQueryWrapper<>();
        myCommentIdWrapper.eq(CommunityComment::getUserId, userId)
                .select(CommunityComment::getId);
        List<CommunityComment> myCommentEntities = commentMapper.selectList(myCommentIdWrapper);
        Set<Long> myCommentIds = myCommentEntities.stream()
                .map(CommunityComment::getId)
                .collect(Collectors.toSet());

        if (!myCommentIds.isEmpty()) {
            LambdaQueryWrapper<CommunityComment> replyWrapper = new LambdaQueryWrapper<>();
            replyWrapper.in(CommunityComment::getParentCommentId, myCommentIds)
                    .ne(CommunityComment::getUserId, userId)
                    .gt(CommunityComment::getCreateTime, since);
            count += commentMapper.selectCount(replyWrapper).intValue();
        }

        // 未读收藏
        LambdaQueryWrapper<CommunityPostFavorite> favWrapper = new LambdaQueryWrapper<>();
        favWrapper.in(CommunityPostFavorite::getPostId, myPostIds)
                .ne(CommunityPostFavorite::getUserId, userId)
                .gt(CommunityPostFavorite::getCreateTime, since);
        count += favoriteMapper.selectCount(favWrapper).intValue();

        return count;
    }

    private String formatUserName(SysUser user) {
        if (user == null) return "匿名用户";
        return user.getNickname() != null ? user.getNickname() : user.getUsername();
    }

    private String truncate(String text, int maxLen) {
        if (text == null) return "";
        return text.length() > maxLen ? text.substring(0, maxLen) + "..." : text;
    }

    // ==================== 内部辅助方法 ====================

    /**
     * 批量查询用户信息
     */
    private Map<Long, SysUser> batchGetUsers(Set<Long> userIds) {
        if (userIds.isEmpty()) {
            return Collections.emptyMap();
        }
        LambdaQueryWrapper<SysUser> wrapper = new LambdaQueryWrapper<>();
        wrapper.in(SysUser::getId, userIds);
        List<SysUser> users = userMapper.selectList(wrapper);
        return users.stream().collect(Collectors.toMap(SysUser::getId, u -> u));
    }

    /**
     * 批量查询当前用户对一组帖子的点赞状态
     */
    private Set<Long> batchCheckLiked(Long userId, List<CommunityPost> posts) {
        if (userId == null || posts.isEmpty()) {
            return Collections.emptySet();
        }
        Set<Long> postIds = posts.stream().map(CommunityPost::getId).collect(Collectors.toSet());
        LambdaQueryWrapper<CommunityPostLike> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(CommunityPostLike::getUserId, userId)
                .in(CommunityPostLike::getPostId, postIds);
        return likeMapper.selectList(wrapper).stream()
                .map(CommunityPostLike::getPostId)
                .collect(Collectors.toSet());
    }

    /**
     * 检查用户是否已点赞某帖子
     */
    private boolean checkLiked(Long userId, Long postId) {
        if (userId == null) {
            return false;
        }
        LambdaQueryWrapper<CommunityPostLike> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(CommunityPostLike::getPostId, postId)
                .eq(CommunityPostLike::getUserId, userId);
        return likeMapper.selectCount(wrapper) > 0;
    }

    /**
     * 批量查询当前用户对一组帖子的收藏状态
     */
    private Set<Long> batchCheckFavorited(Long userId, List<CommunityPost> posts) {
        if (userId == null || posts.isEmpty()) {
            return Collections.emptySet();
        }
        Set<Long> postIds = posts.stream().map(CommunityPost::getId).collect(Collectors.toSet());
        LambdaQueryWrapper<CommunityPostFavorite> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(CommunityPostFavorite::getUserId, userId)
                .in(CommunityPostFavorite::getPostId, postIds);
        return favoriteMapper.selectList(wrapper).stream()
                .map(CommunityPostFavorite::getPostId)
                .collect(Collectors.toSet());
    }

    /**
     * 检查用户是否已收藏某帖子
     */
    private boolean checkFavorited(Long userId, Long postId) {
        if (userId == null) {
            return false;
        }
        LambdaQueryWrapper<CommunityPostFavorite> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(CommunityPostFavorite::getPostId, postId)
                .eq(CommunityPostFavorite::getUserId, userId);
        return favoriteMapper.selectCount(wrapper) > 0;
    }

    /**
     * 帖子点赞数+1
     */
    private void incrementLikeCount(Long postId) {
        LambdaUpdateWrapper<CommunityPost> wrapper = new LambdaUpdateWrapper<>();
        wrapper.eq(CommunityPost::getId, postId)
                .setSql("like_count = like_count + 1");
        postMapper.update(null, wrapper);
    }

    /**
     * 帖子点赞数-1（最小为0）
     */
    private void decrementLikeCount(Long postId) {
        LambdaUpdateWrapper<CommunityPost> wrapper = new LambdaUpdateWrapper<>();
        wrapper.eq(CommunityPost::getId, postId)
                .setSql("like_count = GREATEST(like_count - 1, 0)");
        postMapper.update(null, wrapper);
    }

    /**
     * 帖子评论数+1
     */
    private void incrementCommentCount(Long postId) {
        LambdaUpdateWrapper<CommunityPost> wrapper = new LambdaUpdateWrapper<>();
        wrapper.eq(CommunityPost::getId, postId)
                .setSql("comment_count = comment_count + 1");
        postMapper.update(null, wrapper);
    }

    /**
     * 帖子评论数-1（最小为0）
     */
    private void decrementCommentCount(Long postId) {
        LambdaUpdateWrapper<CommunityPost> wrapper = new LambdaUpdateWrapper<>();
        wrapper.eq(CommunityPost::getId, postId)
                .setSql("comment_count = GREATEST(comment_count - 1, 0)");
        postMapper.update(null, wrapper);
    }

    /**
     * 帖子实体转VO（列表场景，带批量用户Map）
     */
    private PostVO toPostVO(CommunityPost post, Map<Long, SysUser> userMap, Set<Long> likedPostIds, Set<Long> favoritedPostIds) {
        SysUser author = userMap.get(post.getUserId());
        return PostVO.builder()
                .id(post.getId())
                .userId(post.getUserId())
                .authorName(author != null ? (author.getNickname() != null ? author.getNickname() : author.getUsername()) : "匿名用户")
                .authorAvatar(null) // SysUser暂无avatar字段
                .category(post.getCategory())
                .content(post.getContent())
                .images(parseImages(post.getImages()))
                .likeCount(post.getLikeCount())
                .commentCount(post.getCommentCount())
                .liked(likedPostIds.contains(post.getId()))
                .favorited(favoritedPostIds.contains(post.getId()))
                .createTime(post.getCreateTime())
                .build();
    }

    /**
     * 帖子实体转VO（详情场景，单个用户对象）
     */
    private PostVO toPostVO(CommunityPost post, SysUser author, boolean liked, boolean favorited) {
        return PostVO.builder()
                .id(post.getId())
                .userId(post.getUserId())
                .authorName(author != null ? (author.getNickname() != null ? author.getNickname() : author.getUsername()) : "匿名用户")
                .authorAvatar(null) // SysUser暂无avatar字段
                .category(post.getCategory())
                .content(post.getContent())
                .images(parseImages(post.getImages()))
                .likeCount(post.getLikeCount())
                .commentCount(post.getCommentCount())
                .liked(liked)
                .favorited(favorited)
                .createTime(post.getCreateTime())
                .build();
    }

    /**
     * 评论实体转VO
     */
    private CommentVO toCommentVO(CommunityComment comment, Map<Long, SysUser> userMap,
                                  Long postAuthorId, Long currentUserId) {
        SysUser author = userMap.get(comment.getUserId());
        boolean isPostAuthor = postAuthorId != null && postAuthorId.equals(comment.getUserId());
        boolean deletable = comment.getUserId().equals(currentUserId)
                || (postAuthorId != null && postAuthorId.equals(currentUserId));
        return CommentVO.builder()
                .id(comment.getId())
                .postId(comment.getPostId())
                .userId(comment.getUserId())
                .authorName(author != null ? (author.getNickname() != null ? author.getNickname() : author.getUsername()) : "匿名用户")
                .authorAvatar(null)
                .content(comment.getContent())
                .createTime(comment.getCreateTime())
                .isPostAuthor(isPostAuthor)
                .deletable(deletable)
                .parentCommentId(comment.getParentCommentId())
                .replyCount(0)
                .build();
    }

    /**
     * 解析图片JSON字符串为List
     */
    private List<String> parseImages(String imagesJson) {
        if (imagesJson == null || imagesJson.isBlank()) {
            return Collections.emptyList();
        }
        try {
            return objectMapper.readValue(imagesJson, new TypeReference<>() {});
        } catch (JsonProcessingException e) {
            log.warn("图片JSON解析失败: {}", imagesJson, e);
            return Collections.emptyList();
        }
    }
}
