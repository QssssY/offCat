package com.airesume.server.service;

import com.airesume.server.common.constants.CommunityConstants;
import com.airesume.server.common.constants.UserRoleConstants;
import com.airesume.server.common.exception.BusinessException;
import com.airesume.server.common.result.PageResult;
import com.airesume.server.dto.community.*;
import com.airesume.server.entity.CommunityComment;
import com.airesume.server.entity.CommunityPost;
import com.airesume.server.entity.CommunityPostFavorite;
import com.airesume.server.entity.CommunityPostLike;
import com.airesume.server.entity.InterviewSession;
import com.airesume.server.entity.SysUser;
import com.airesume.server.mapper.CommunityCommentMapper;
import com.airesume.server.mapper.CommunityPostFavoriteMapper;
import com.airesume.server.mapper.CommunityPostLikeMapper;
import com.airesume.server.mapper.CommunityPostMapper;
import com.airesume.server.mapper.InterviewSessionMapper;
import com.airesume.server.util.ImageValidator;
import com.airesume.server.service.impl.OssServiceImpl;
import com.airesume.server.mapper.SysUserMapper;
import org.springframework.data.redis.core.StringRedisTemplate;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * 社区模块业务服务
 * 提供帖子CRUD、点赞/取消点赞、评论、图片上传等能力
 */
@Slf4j
@Service
public class CommunityService {

    private final CommunityPostMapper postMapper;
    private final CommunityCommentMapper commentMapper;
    private final CommunityPostLikeMapper likeMapper;
    private final CommunityPostFavoriteMapper favoriteMapper;
    private final SysUserMapper userMapper;
    private final InterviewSessionMapper interviewSessionMapper;
    private final ObjectMapper objectMapper;
    private final CommunityTextModerationService moderationService;
    private final NotificationService notificationService;
    private final OssService ossService;

    /**
     * 兼容既有单元测试的构造路径；生产环境使用包含 OSS 服务的完整构造器。
     */
    public CommunityService(CommunityPostMapper postMapper,
                            CommunityCommentMapper commentMapper,
                            CommunityPostLikeMapper likeMapper,
                            CommunityPostFavoriteMapper favoriteMapper,
                            SysUserMapper userMapper,
                            InterviewSessionMapper interviewSessionMapper,
                            ObjectMapper objectMapper,
                            CommunityTextModerationService moderationService,
                            NotificationService notificationService) {
        this(postMapper, commentMapper, likeMapper, favoriteMapper, userMapper,
                interviewSessionMapper, objectMapper, moderationService, notificationService, new OssServiceImpl(null));
    }

    /**
     * 生产环境完整依赖注入构造器。
     * 作用：当前类保留了单元测试兼容构造器，必须显式标记 Spring 注入入口，避免多构造器场景回退到无参构造器导致启动失败。
     */
    @Autowired
    public CommunityService(CommunityPostMapper postMapper,
                            CommunityCommentMapper commentMapper,
                            CommunityPostLikeMapper likeMapper,
                            CommunityPostFavoriteMapper favoriteMapper,
                            SysUserMapper userMapper,
                            InterviewSessionMapper interviewSessionMapper,
                            ObjectMapper objectMapper,
                            CommunityTextModerationService moderationService,
                            NotificationService notificationService,
                            OssService ossService) {
        this.postMapper = postMapper;
        this.commentMapper = commentMapper;
        this.likeMapper = likeMapper;
        this.favoriteMapper = favoriteMapper;
        this.userMapper = userMapper;
        this.interviewSessionMapper = interviewSessionMapper;
        this.objectMapper = objectMapper;
        this.moderationService = moderationService;
        this.notificationService = notificationService;
        this.ossService = ossService;
    }

    /**
     * 兼容既有单元测试的构造路径；生产环境使用包含通知服务的完整构造器。
     */
    public CommunityService(CommunityPostMapper postMapper,
                            CommunityCommentMapper commentMapper,
                            CommunityPostLikeMapper likeMapper,
                            CommunityPostFavoriteMapper favoriteMapper,
                            SysUserMapper userMapper,
                            InterviewSessionMapper interviewSessionMapper,
                            ObjectMapper objectMapper,
                            CommunityTextModerationService moderationService) {
        this(postMapper, commentMapper, likeMapper, favoriteMapper, userMapper,
                interviewSessionMapper, objectMapper, moderationService, null, new OssServiceImpl(null));
    }

    /**
     * 兼容既有单元测试的构造路径，生产环境仍通过 Spring 注入完整依赖。
     */
    public CommunityService(CommunityPostMapper postMapper,
                            CommunityCommentMapper commentMapper,
                            CommunityPostLikeMapper likeMapper,
                            CommunityPostFavoriteMapper favoriteMapper,
                            SysUserMapper userMapper,
                            InterviewSessionMapper interviewSessionMapper,
                            ObjectMapper objectMapper) {
        this(postMapper, commentMapper, likeMapper, favoriteMapper, userMapper,
                interviewSessionMapper, objectMapper, new CommunityTextModerationService(), null, new OssServiceImpl(null));
    }

    /**
     * 社区图片对象存储占位访问地址。
     * 当前阿里云 OCS/OSS 尚未配置密钥，上传链路先保留图片校验并返回公网占位图，避免帖子保存本机静态路径导致其他用户无法访问。
     */
    @Value("${app.upload.community-placeholder-url:https://ts3.tc.mm.bing.net/th/id/OIP-C.TmvkuikpStxy5wKWiziR1AHaE7?rs=1&pid=ImgDetMain&o=7&rm=3}")
    private String communityPlaceholderImageUrl;

    /** 最大文件大小（字节），默认5MB */
    @Value("${app.upload.community-max-size:5242880}")
    private long maxFileSize;

    /** 每用户每日上传图片上限，默认30张 */
    @Value("${app.upload.community-daily-upload-limit:30}")
    private int dailyUploadLimit;

    /** Redis 模板（可选依赖，测试环境可能不可用） */
    @Autowired(required = false)
    private StringRedisTemplate stringRedisTemplate;

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
        wrapper.eq(CommunityPost::getReviewStatus, CommunityConstants.REVIEW_STATUS_APPROVED);
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
        List<CommunityPost> approvedPosts = resultPage.getRecords().stream()
                .filter(this::isApprovedPost)
                .toList();

        List<PostVO> voList = approvedPosts.stream()
                .map(post -> toPostVO(post, userMap, likedPostIds, favoritedPostIds))
                .toList();

        return PageResult.of(voList, voList.size(), pageNum, safeSize);
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
        if (!isApprovedPost(post) && !post.getUserId().equals(userId)) {
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
    public CreateCommunityContentResponse createPost(Long userId, CreatePostRequest request) {
        // 校验板块类型
        if (!CommunityConstants.CATEGORY_INTERVIEW_EXP.equals(request.getCategory())
                && !CommunityConstants.CATEGORY_REFERRAL.equals(request.getCategory())) {
            throw new BusinessException("无效的帖子板块类型");
        }

        // 校验内容长度
        if (request.getContent().length() > CommunityConstants.MAX_CONTENT_LENGTH) {
            throw new BusinessException("帖子内容不能超过" + CommunityConstants.MAX_CONTENT_LENGTH + "字");
        }

        String title = request.getTitle() == null ? "" : request.getTitle().trim();
        if (title.isBlank()) {
            throw new BusinessException("帖子标题不能为空");
        }
        if (title.length() > CommunityConstants.MAX_TITLE_LENGTH) {
            throw new BusinessException("帖子标题不能超过" + CommunityConstants.MAX_TITLE_LENGTH + "字");
        }

        // 校验图片数量
        if (request.getImages() != null && request.getImages().size() > CommunityConstants.MAX_IMAGE_COUNT) {
            throw new BusinessException("图片数量不能超过" + CommunityConstants.MAX_IMAGE_COUNT + "张");
        }

        boolean hasImages = request.getImages() != null && !request.getImages().isEmpty();
        CommunityModerationDecision moderationDecision = moderationService.reviewPost(title, request.getContent(), hasImages);
        if (moderationDecision.isRejected()) {
            throw new BusinessException(moderationDecision.getRejectMessage());
        }

        String sharedInterviewSessionId = normalizeSharedInterviewSessionId(request.getSharedInterviewSessionId());
        validateSharedInterviewSessionOwnership(userId, sharedInterviewSessionId);

        CommunityPost post = new CommunityPost();
        post.setUserId(userId);
        post.setCategory(request.getCategory());
        post.setTitle(title);
        post.setContent(request.getContent().trim());
        post.setSharedInterviewSessionId(sharedInterviewSessionId);
        post.setLikeCount(0);
        post.setCommentCount(0);
        post.setIsDeleted(0);
        // 新发内容必须先进入审核池，避免未审核内容直接公开展示。
        // 自动审核分流：低风险纯文本可直接公开，图片或疑似风险内容继续进入人工审核池。
        post.setReviewStatus(moderationDecision.getReviewStatus());
        post.setReviewReason(moderationDecision.getReviewReason());
        if (CommunityConstants.REVIEW_STATUS_APPROVED.equals(moderationDecision.getReviewStatus())) {
            post.setReviewedTime(LocalDateTime.now());
        }

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
        return CreateCommunityContentResponse.builder()
                .id(post.getId())
                .reviewStatus(post.getReviewStatus())
                .build();
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
        LambdaQueryWrapper<CommunityPostLike> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(CommunityPostLike::getUserId, userId)
                .orderByDesc(CommunityPostLike::getCreateTime);
        return listInteractedPosts(userId, pageNum, pageSize,
                (page) -> likeMapper.selectPage(page, wrapper),
                CommunityPostLike::getPostId);
    }

    /**
     * 分页查询用户收藏过的帖子
     */
    public PageResult<PostVO> listFavoritedPosts(Long userId, Integer pageNum, Integer pageSize) {
        LambdaQueryWrapper<CommunityPostFavorite> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(CommunityPostFavorite::getUserId, userId)
                .orderByDesc(CommunityPostFavorite::getCreateTime);
        return listInteractedPosts(userId, pageNum, pageSize,
                (page) -> favoriteMapper.selectPage(page, wrapper),
                CommunityPostFavorite::getPostId);
    }

    private <T> PageResult<PostVO> listInteractedPosts(
            Long userId, Integer pageNum, Integer pageSize,
            Function<Page<T>, IPage<T>> selectPageFn,
            Function<T, Long> getPostIdFn) {
        int safeSize = Math.min(pageSize, CommunityConstants.MAX_PAGE_SIZE);
        Page<T> pageParam = new Page<>(pageNum, safeSize);
        IPage<T> pageResult = selectPageFn.apply(pageParam);

        if (pageResult.getRecords().isEmpty()) {
            return PageResult.of(Collections.emptyList(), 0, pageNum, safeSize);
        }

        Set<Long> postIds = pageResult.getRecords().stream()
                .map(getPostIdFn)
                .collect(Collectors.toSet());
        LambdaQueryWrapper<CommunityPost> postWrapper = new LambdaQueryWrapper<>();
        postWrapper.in(CommunityPost::getId, postIds);
        Map<Long, CommunityPost> postMap = postMapper.selectList(postWrapper).stream()
                .collect(Collectors.toMap(CommunityPost::getId, p -> p));

        List<CommunityPost> posts = pageResult.getRecords().stream()
                .map(entity -> postMap.get(getPostIdFn.apply(entity)))
                .filter(Objects::nonNull)
                .filter(this::isApprovedPost)
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

        boolean hasMore = pageResult.getRecords().size() == safeSize;
        long total = hasMore ? pageResult.getTotal() : voList.size();
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
        int safePageNum = normalizePageNum(pageNum);
        int safeSize = normalizePageSize(pageSize);

        // 评论过的帖子使用 SQL 去重并分页，避免把用户全部评论加载到内存中再分页。
        IPage<Long> postIdPage = commentMapper.selectDistinctPostIdsByUserId(new Page<>(safePageNum, safeSize), userId);
        List<Long> pagePostIds = postIdPage.getRecords();
        if (pagePostIds.isEmpty()) {
            return PageResult.of(Collections.emptyList(), 0, safePageNum, safeSize);
        }

        // 批量查询当前页有效帖子；已删除帖子由 MyBatis-Plus 逻辑删除条件自动过滤。
        LambdaQueryWrapper<CommunityPost> postWrapper = new LambdaQueryWrapper<>();
        postWrapper.in(CommunityPost::getId, pagePostIds)
                .eq(CommunityPost::getReviewStatus, CommunityConstants.REVIEW_STATUS_APPROVED);
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

        return PageResult.of(voList, postIdPage.getTotal(), safePageNum, safeSize);
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
                    MyCommentVO vo = new MyCommentVO();
                    vo.setCommentId(comment.getId());
                    vo.setCommentContent(comment.getContent());
                    vo.setCommentTime(comment.getCreateTime());
                    vo.setParentCommentId(comment.getParentCommentId());
                    vo.setPostId(comment.getPostId());
                    vo.setPostCategory(!deleted ? post.getCategory() : null);
                    vo.setPostTitle(!deleted ? post.getTitle() : null);
                    vo.setPostContent(!deleted ? post.getContent() : null);
                    vo.setPostImages(!deleted ? post.getImages() : null);
                    vo.setPostAuthorName(authorName);
                    vo.setPostDeleted(deleted);
                    return vo;
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
    @Transactional(rollbackFor = Exception.class)
    public void deletePost(Long userId, Long postId) {
        CommunityPost post = postMapper.selectById(postId);
        if (post == null) {
            throw new BusinessException("帖子不存在");
        }
        if (!post.getUserId().equals(userId)) {
            throw new BusinessException("只能删除自己发布的帖子");
        }
        // 先逻辑删除帖子本身，再清理附属互动；任一后续清理失败都会向上抛出并触发事务回滚。
        postMapper.deleteById(postId);

        // 级联删除该帖子的所有评论
        LambdaQueryWrapper<CommunityComment> commentWrapper = new LambdaQueryWrapper<>();
        commentWrapper.eq(CommunityComment::getPostId, postId);
        commentMapper.delete(commentWrapper);

        // 级联删除该帖子的所有点赞
        LambdaQueryWrapper<CommunityPostLike> likeWrapper = new LambdaQueryWrapper<>();
        likeWrapper.eq(CommunityPostLike::getPostId, postId);
        likeMapper.delete(likeWrapper);

        // 级联删除该帖子的所有收藏记录
        LambdaQueryWrapper<CommunityPostFavorite> favWrapper = new LambdaQueryWrapper<>();
        favWrapper.eq(CommunityPostFavorite::getPostId, postId);
        favoriteMapper.delete(favWrapper);

        log.info("帖子删除成功, postId: {}, userId: {}（含级联删除评论、点赞、收藏记录）", postId, userId);
    }

    /**
     * 管理员在用户端社区下架帖子。
     * 说明：这里不做物理删除，只改为 hidden 并记录原因，保留证据链，前台公开查询会自动过滤非 approved 内容。
     */
    @Transactional(rollbackFor = Exception.class)
    public void adminHidePost(Long adminUserId, Long postId, String reason) {
        SysUser adminUser = userMapper.selectById(adminUserId);
        if (adminUser == null || adminUser.getRole() == null || adminUser.getRole() != UserRoleConstants.ROLE_ADMIN) {
            throw new BusinessException("无权限执行社区管理操作");
        }
        String normalizedReason = reason == null ? "" : reason.trim();
        if (normalizedReason.isBlank()) {
            throw new BusinessException("下架原因不能为空");
        }
        if (normalizedReason.length() > CommunityConstants.MAX_ADMIN_HIDE_REASON_LENGTH) {
            throw new BusinessException("下架原因不能超过200字");
        }

        CommunityPost post = postMapper.selectById(postId);
        if (post == null) {
            throw new BusinessException("帖子不存在");
        }

        post.setReviewStatus(CommunityConstants.REVIEW_STATUS_HIDDEN);
        post.setReviewReason(normalizedReason);
        post.setReviewedBy(adminUserId);
        post.setReviewedTime(LocalDateTime.now());
        postMapper.updateById(post);

        if (notificationService != null) {
            notificationService.createNotification(
                    post.getUserId(),
                    "system",
                    "社区帖子已下架",
                    "你的社区帖子《" + safePostTitle(post) + "》已被管理员下架，原因：" + normalizedReason,
                    "community_post",
                    String.valueOf(postId)
            );
        }
    }

    /**
     * 管理员在用户端下架评论。顶级评论会连同直接回复一起隐藏，避免违规讨论串继续公开展示。
     */
    @Transactional(rollbackFor = Exception.class)
    public void adminHideComment(Long adminUserId, Long postId, Long commentId, String reason) {
        SysUser adminUser = userMapper.selectById(adminUserId);
        if (adminUser == null || adminUser.getRole() == null || adminUser.getRole() != UserRoleConstants.ROLE_ADMIN) {
            throw new BusinessException("无权限执行社区管理操作");
        }
        String normalizedReason = reason == null ? "" : reason.trim();
        if (normalizedReason.isBlank()) {
            throw new BusinessException("下架原因不能为空");
        }
        if (normalizedReason.length() > CommunityConstants.MAX_ADMIN_HIDE_REASON_LENGTH) {
            throw new BusinessException("下架原因不能超过200字");
        }

        CommunityComment targetComment = commentMapper.selectById(commentId);
        if (targetComment == null || !Objects.equals(targetComment.getPostId(), postId)) {
            throw new BusinessException("评论不存在");
        }

        List<CommunityComment> commentsToHide = new ArrayList<>();
        commentsToHide.add(targetComment);
        if (targetComment.getParentCommentId() == null) {
            LambdaQueryWrapper<CommunityComment> replyWrapper = new LambdaQueryWrapper<>();
            replyWrapper.eq(CommunityComment::getPostId, postId)
                    .eq(CommunityComment::getParentCommentId, commentId)
                    .eq(CommunityComment::getIsDeleted, 0);
            commentsToHide.addAll(commentMapper.selectList(replyWrapper));
        }

        LocalDateTime reviewedTime = LocalDateTime.now();
        int approvedHiddenCount = 0;
        for (CommunityComment comment : commentsToHide) {
            if (CommunityConstants.REVIEW_STATUS_APPROVED.equals(comment.getReviewStatus())) {
                approvedHiddenCount++;
            }
            comment.setReviewStatus(CommunityConstants.REVIEW_STATUS_HIDDEN);
            comment.setReviewReason(normalizedReason);
            comment.setReviewedBy(adminUserId);
            comment.setReviewedTime(reviewedTime);
            commentMapper.updateById(comment);
            notifyCommentHidden(comment, normalizedReason);
        }

        if (approvedHiddenCount > 0) {
            decrementCommentCount(postId, approvedHiddenCount);
        }
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
            try {
                likeMapper.insert(like);
                // 帖子点赞数+1
                incrementLikeCount(postId);
                log.info("点赞成功, userId: {}, postId: {}", userId, postId);
            } catch (DuplicateKeyException e) {
                // 并发插入导致唯一索引冲突，视为已点赞，做幂等处理
                log.info("并发点赞，唯一索引冲突，视为已点赞, userId: {}, postId: {}", userId, postId);
            }
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
            try {
                favoriteMapper.insert(fav);
                log.info("收藏成功, userId: {}, postId: {}", userId, postId);
            } catch (DuplicateKeyException e) {
                // 并发插入导致唯一索引冲突，视为已收藏，做幂等处理
                log.info("并发收藏，唯一索引冲突，视为已收藏, userId: {}, postId: {}", userId, postId);
            }
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
    public CommentVO getCommentDetail(Long postId, Long commentId, Long currentUserId) {
        CommunityComment comment = commentMapper.selectById(commentId);
        if (comment == null || !comment.getPostId().equals(postId) || !isApprovedComment(comment)) {
            throw new BusinessException("评论不存在");
        }
        CommunityPost post = postMapper.selectById(postId);
        Long postAuthorId = post != null ? post.getUserId() : null;
        if (post == null || !isApprovedPost(post)) {
            throw new BusinessException("帖子不存在");
        }
        SysUser user = userMapper.selectById(comment.getUserId());

        return CommentVO.builder()
                .id(comment.getId())
                .postId(comment.getPostId())
                .userId(comment.getUserId())
                .authorName(formatUserName(user))
                .authorAvatar(null)
                .content(comment.getContent())
                .images(parseImages(comment.getImages()))
                .createTime(comment.getCreateTime())
                .isPostAuthor(comment.getUserId().equals(postAuthorId))
                .deletable(currentUserId != null && (comment.getUserId().equals(currentUserId)
                        || (postAuthorId != null && postAuthorId.equals(currentUserId))))
                .parentCommentId(comment.getParentCommentId())
                .replyCount(comment.getParentCommentId() == null ? batchGetReplyCounts(Set.of(commentId)).getOrDefault(commentId, 0L).intValue() : null)
                .build();
    }

    public PageResult<CommentVO> listComments(Long postId, Long currentUserId, Integer pageNum, Integer pageSize) {
        int safeSize = Math.min(pageSize, CommunityConstants.MAX_PAGE_SIZE);

        // 查询帖子作者ID
        CommunityPost post = postMapper.selectById(postId);
        Long postAuthorId = post != null ? post.getUserId() : null;
        if (post == null || !isApprovedPost(post)) {
            throw new BusinessException("帖子不存在");
        }

        // 只查询顶级评论（parentCommentId IS NULL）
        LambdaQueryWrapper<CommunityComment> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(CommunityComment::getPostId, postId)
                .isNull(CommunityComment::getParentCommentId)
                .eq(CommunityComment::getReviewStatus, CommunityConstants.REVIEW_STATUS_APPROVED)
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
                .eq(CommunityComment::getReviewStatus, CommunityConstants.REVIEW_STATUS_APPROVED)
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
        List<Map<String, Object>> rows = commentMapper.countByParentIds(commentIds);
        Map<Long, Long> result = new HashMap<>();
        for (Map<String, Object> row : rows) {
            Long parentId = ((Number) row.get("parent_comment_id")).longValue();
            Long cnt = ((Number) row.get("cnt")).longValue();
            result.put(parentId, cnt);
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
    public CreateCommunityContentResponse createComment(Long userId, Long postId, CreateCommentRequest request) {
        // 校验帖子是否存在
        CommunityPost post = postMapper.selectById(postId);
        if (post == null) {
            throw new BusinessException("帖子不存在");
        }
        if (!isApprovedPost(post)) {
            throw new BusinessException("帖子不存在");
        }

        // 校验内容和图片至少有一个
        boolean hasContent = request.getContent() != null && !request.getContent().isBlank();
        boolean hasImages = request.getImages() != null && !request.getImages().isEmpty();
        if (!hasContent && !hasImages) {
            throw new BusinessException("评论内容不能为空");
        }

        // 校验内容长度
        if (hasContent && request.getContent().length() > CommunityConstants.MAX_COMMENT_LENGTH) {
            throw new BusinessException("评论内容不能超过" + CommunityConstants.MAX_COMMENT_LENGTH + "字");
        }
        CommunityModerationDecision moderationDecision = moderationService.reviewComment(request.getContent(), hasImages);
        if (moderationDecision.isRejected()) {
            throw new BusinessException(moderationDecision.getRejectMessage());
        }

        CommunityComment comment = new CommunityComment();
        comment.setPostId(postId);
        comment.setUserId(userId);
        comment.setContent(hasContent ? request.getContent().trim() : "");
        // 评论同样先进入审核池，通过后才公开展示并计入帖子评论数。
        // 评论自动审核分流：通过后才计入公开评论数，待审评论不影响前台计数。
        comment.setReviewStatus(moderationDecision.getReviewStatus());
        comment.setReviewReason(moderationDecision.getReviewReason());
        if (CommunityConstants.REVIEW_STATUS_APPROVED.equals(moderationDecision.getReviewStatus())) {
            comment.setReviewedTime(LocalDateTime.now());
        }

        // 图片列表转JSON存储
        if (request.getImages() != null && !request.getImages().isEmpty()) {
            if (request.getImages().size() > CommunityConstants.MAX_IMAGE_COUNT) {
                throw new BusinessException("图片数量不能超过" + CommunityConstants.MAX_IMAGE_COUNT + "张");
            }
            try {
                comment.setImages(objectMapper.writeValueAsString(request.getImages()));
            } catch (JsonProcessingException e) {
                log.error("评论图片列表序列化失败", e);
                throw new BusinessException("图片数据处理失败");
            }
        }

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

        if (CommunityConstants.REVIEW_STATUS_APPROVED.equals(moderationDecision.getReviewStatus())) {
            incrementCommentCount(postId);
        }

        log.info("评论创建成功, commentId: {}, postId: {}, userId: {}, parentId: {}", comment.getId(), postId, userId, parentCommentId);
        return CreateCommunityContentResponse.builder()
                .id(comment.getId())
                .reviewStatus(comment.getReviewStatus())
                .build();
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

        // 查询并级联删除子回复
        LambdaQueryWrapper<CommunityComment> childWrapper = new LambdaQueryWrapper<>();
        childWrapper.eq(CommunityComment::getParentCommentId, commentId);
        long childCount = commentMapper.selectCount(childWrapper);
        if (childCount > 0) {
            commentMapper.delete(childWrapper);
        }

        commentMapper.deleteById(commentId);
        // 帖子评论数减去实际删除数量（评论 + 子回复）
        long totalToDelete = 1 + childCount;
        LambdaUpdateWrapper<CommunityPost> updateWrapper = new LambdaUpdateWrapper<>();
        updateWrapper.eq(CommunityPost::getId, postId)
                .setSql("comment_count = GREATEST(comment_count - " + totalToDelete + ", 0)");
        postMapper.update(null, updateWrapper);
        log.info("评论删除成功, commentId: {}, postId: {}, userId: {}, 级联删除子回复: {}", commentId, postId, userId, childCount);
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
        if (originalFilename == null || originalFilename.isBlank()) {
            throw new BusinessException("文件名不能为空");
        }
        String lowerName = originalFilename.toLowerCase();
        if (!lowerName.endsWith(".jpg") && !lowerName.endsWith(".jpeg")
                && !lowerName.endsWith(".png") && !lowerName.endsWith(".gif")
                && !lowerName.endsWith(".webp")) {
            throw new BusinessException("仅支持 JPG、PNG、GIF、WebP 格式图片");
        }

        // 校验文件魔术字节，防止伪造扩展名
        String extWithoutDot = lowerName.substring(lowerName.lastIndexOf(".") + 1);
        try {
            if (!ImageValidator.validateMagicBytes(file, extWithoutDot)) {
                throw new BusinessException("文件内容与声明格式不符，请上传真实的图片文件");
            }
        } catch (IOException e) {
            log.error("读取文件魔术字节失败, userId: {}, fileName: {}", userId, originalFilename, e);
            throw new BusinessException("文件校验失败，请重新上传");
        }

        // 校验每用户每日上传限额（防止图床滥用）
        checkDailyUploadLimit(userId);

        // OSS 已配置：上传到阿里云，返回稳定的代理 URL 供前端存入帖子/评论数据
        if (ossService.isEnabled()) {
            String objectKey = ossService.upload(file, userId, extWithoutDot);
            String proxyUrl = "/api/community/images/" + objectKey;
            log.info("社区图片上传至OSS成功, userId: {}, objectKey: {}", userId, objectKey);
            return proxyUrl;
        }

        // OSS 未配置：保留占位图逻辑，兼容开发环境
        log.info("社区图片上传校验通过(OSS未启用), 暂用占位图, userId: {}, fileName: {}, fileSize: {}",
                userId, originalFilename, file.getSize());
        return communityPlaceholderImageUrl;
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
        int safePageNum = normalizePageNum(pageNum);
        int safeSize = normalizePageSize(pageSize);

        String myPostSubQuery = "SELECT id FROM community_post WHERE user_id = " + userId + " AND is_deleted = 0";

        Page<CommunityPostLike> likePageParam = new Page<>(safePageNum, safeSize);
        Page<CommunityComment> commentPageParam = new Page<>(safePageNum, safeSize);
        Page<CommunityComment> replyPageParam = new Page<>(safePageNum, safeSize);
        Page<CommunityPostFavorite> favPageParam = new Page<>(safePageNum, safeSize);

        // 2. 查询收到的点赞（排除自己的点赞，数据库侧过滤并分页，避免加载全部帖子ID）
        LambdaQueryWrapper<CommunityPostLike> likeWrapper = new LambdaQueryWrapper<>();
        likeWrapper.inSql(CommunityPostLike::getPostId, myPostSubQuery)
                .ne(CommunityPostLike::getUserId, userId)
                .orderByDesc(CommunityPostLike::getCreateTime);
        Page<CommunityPostLike> likePageResult = likeMapper.selectPage(likePageParam, likeWrapper);
        long totalLikes = likePageResult.getTotal();
        List<CommunityPostLike> pageLikes = likePageResult.getRecords();

        // 3. 查询收到的评论（排除自己的评论，只查顶级评论，数据库侧过滤并分页）
        LambdaQueryWrapper<CommunityComment> commentWrapper = new LambdaQueryWrapper<>();
        commentWrapper.inSql(CommunityComment::getPostId, myPostSubQuery)
                .ne(CommunityComment::getUserId, userId)
                .isNull(CommunityComment::getParentCommentId)
                .orderByDesc(CommunityComment::getCreateTime);
        Page<CommunityComment> commentPageResult = commentMapper.selectPage(commentPageParam, commentWrapper);
        long totalComments = commentPageResult.getTotal();
        List<CommunityComment> pageComments = commentPageResult.getRecords();

        // 4. 查询收到的回复（别人回复了我，通过 replyToUserId 判断，数据库侧分页）
        LambdaQueryWrapper<CommunityComment> replyWrapper = new LambdaQueryWrapper<>();
        replyWrapper.eq(CommunityComment::getReplyToUserId, userId)
                .ne(CommunityComment::getUserId, userId)
                .orderByDesc(CommunityComment::getCreateTime);
        Page<CommunityComment> replyPageResult = commentMapper.selectPage(replyPageParam, replyWrapper);
        long totalReplies = replyPageResult.getTotal();
        List<CommunityComment> pageReplies = replyPageResult.getRecords();

        // 4.5. 查询收到的收藏（别人收藏了我的帖子，数据库侧过滤并分页）
        LambdaQueryWrapper<CommunityPostFavorite> favWrapper = new LambdaQueryWrapper<>();
        favWrapper.inSql(CommunityPostFavorite::getPostId, myPostSubQuery)
                .ne(CommunityPostFavorite::getUserId, userId)
                .orderByDesc(CommunityPostFavorite::getCreateTime);
        Page<CommunityPostFavorite> favPageResult = favoriteMapper.selectPage(favPageParam, favWrapper);
        long totalFavorites = favPageResult.getTotal();
        List<CommunityPostFavorite> pageFavorites = favPageResult.getRecords();

        // 5. 批量查询帖子信息（用于显示帖子摘要）
        Set<Long> relatedPostIds = new HashSet<>();
        pageLikes.forEach(l -> relatedPostIds.add(l.getPostId()));
        pageComments.forEach(c -> relatedPostIds.add(c.getPostId()));
        pageReplies.forEach(r -> relatedPostIds.add(r.getPostId()));
        pageFavorites.forEach(f -> relatedPostIds.add(f.getPostId()));
        Map<Long, CommunityPost> queriedPostMap = Collections.emptyMap();
        if (!relatedPostIds.isEmpty()) {
            LambdaQueryWrapper<CommunityPost> postWrapper = new LambdaQueryWrapper<>();
            postWrapper.in(CommunityPost::getId, relatedPostIds);
            queriedPostMap = postMapper.selectList(postWrapper).stream()
                    .collect(Collectors.toMap(CommunityPost::getId, p -> p));
        }
        Map<Long, CommunityPost> postMap = queriedPostMap;

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
                            .postTitle(post != null ? post.getTitle() : null)
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
                            .commentId(comment.getId())
                            .userId(comment.getUserId())
                            .userName(formatUserName(user))
                            .commentContent(comment.getContent())
                            .postId(comment.getPostId())
                            .postTitle(post != null ? post.getTitle() : null)
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
                            .replyId(reply.getId())
                            .userId(reply.getUserId())
                            .userName(formatUserName(user))
                            .replyContent(reply.getContent())
                            .parentCommentContent(parentCommentContentMap.get(reply.getParentCommentId()))
                            .parentCommentId(reply.getParentCommentId())
                            .postId(reply.getPostId())
                            .postTitle(post != null ? post.getTitle() : null)
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
                            .postTitle(post != null ? post.getTitle() : null)
                            .postContent(post != null ? truncate(post.getContent(), 80) : "已删除")
                            .postCategory(post != null ? post.getCategory() : null)
                            .createTime(fav.getCreateTime())
                            .build();
                })
                .toList();

        boolean hasMore = (totalLikes > (long) safePageNum * safeSize)
                || (totalComments > (long) safePageNum * safeSize)
                || (totalReplies > (long) safePageNum * safeSize)
                || (totalFavorites > (long) safePageNum * safeSize);

        return ReceivedInteractionVO.builder()
                .likes(likeItems)
                .totalLikes((int) totalLikes)
                .comments(commentItems)
                .totalComments((int) totalComments)
                .replies(replyItems)
                .totalReplies((int) totalReplies)
                .favorites(favItems)
                .totalFavorites((int) totalFavorites)
                .hasMore(hasMore)
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
        // 未读数使用聚合 SQL 统计，避免把用户全部帖子或评论 ID 拉到应用层再拼 IN 条件。
        return commentMapper.countUnreadInteractions(userId, since);
    }

    /**
     * 服务层兜底页码，避免内部调用传入非法参数导致下标越界。
     */
    private int normalizePageNum(Integer pageNum) {
        return pageNum == null || pageNum < 1 ? 1 : pageNum;
    }

    /**
     * 服务层兜底分页大小，避免空值或非正数影响手写分页。
     */
    private int normalizePageSize(Integer pageSize) {
        if (pageSize == null || pageSize < 1) {
            return CommunityConstants.DEFAULT_PAGE_SIZE;
        }
        return Math.min(pageSize, CommunityConstants.MAX_PAGE_SIZE);
    }

    private String formatUserName(SysUser user) {
        if (user == null) return "匿名用户";
        return user.getNickname() != null ? user.getNickname() : user.getUsername();
    }

    private String truncate(String text, int maxLen) {
        if (text == null) return "";
        return text.length() > maxLen ? text.substring(0, maxLen) + "..." : text;
    }

    /**
     * 下架通知标题兜底，避免旧数据标题为空时通知内容不完整。
     */
    private String safePostTitle(CommunityPost post) {
        String title = post.getTitle() == null ? "" : post.getTitle().trim();
        if (title.isBlank()) {
            return "未命名帖子";
        }
        // 通知列表只需要摘要标题，完整标题仍保留在帖子本身，避免超长标题撑开通知 UI。
        if (title.length() > CommunityConstants.ADMIN_HIDE_NOTIFICATION_TITLE_MAX_LENGTH) {
            return title.substring(0, CommunityConstants.ADMIN_HIDE_NOTIFICATION_TITLE_MAX_LENGTH) + "...";
        }
        return title;
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
     * 按实际隐藏的公开评论数量回退帖子评论数，避免顶级评论连同回复下架后计数仍然虚高。
     */
    private void decrementCommentCount(Long postId, int count) {
        if (count <= 0) {
            return;
        }
        LambdaUpdateWrapper<CommunityPost> wrapper = new LambdaUpdateWrapper<>();
        wrapper.eq(CommunityPost::getId, postId)
                .setSql("comment_count = GREATEST(comment_count - " + count + ", 0)");
        postMapper.update(null, wrapper);
    }

    private void notifyCommentHidden(CommunityComment comment, String reason) {
        if (notificationService == null) {
            return;
        }
        notificationService.createNotification(
                comment.getUserId(),
                "system",
                "社区评论已下架",
                "你的社区评论已被管理员下架，原因：" + reason,
                "community_comment",
                String.valueOf(comment.getId())
        );
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
                .title(post.getTitle())
                .content(post.getContent())
                .sharedInterviewSessionId(post.getSharedInterviewSessionId())
                .images(parseImages(post.getImages()))
                .likeCount(post.getLikeCount())
                .commentCount(post.getCommentCount())
                .liked(likedPostIds.contains(post.getId()))
                .favorited(favoritedPostIds.contains(post.getId()))
                .reviewStatus(post.getReviewStatus())
                .reviewReason(post.getReviewReason())
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
                .title(post.getTitle())
                .content(post.getContent())
                .sharedInterviewSessionId(post.getSharedInterviewSessionId())
                .images(parseImages(post.getImages()))
                .likeCount(post.getLikeCount())
                .commentCount(post.getCommentCount())
                .liked(liked)
                .favorited(favorited)
                .reviewStatus(post.getReviewStatus())
                .reviewReason(post.getReviewReason())
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
                .images(parseImages(comment.getImages()))
                .createTime(comment.getCreateTime())
                .isPostAuthor(isPostAuthor)
                .deletable(deletable)
                .parentCommentId(comment.getParentCommentId())
                .replyCount(0)
                .reviewStatus(comment.getReviewStatus())
                .reviewReason(comment.getReviewReason())
                .build();
    }

    /**
     * 社区公开内容判断。
     * 作用：所有用户端公开列表和详情都只能展示审核通过内容，避免待审或违规内容被直接曝光。
     */
    private boolean isApprovedPost(CommunityPost post) {
        return post != null && CommunityConstants.REVIEW_STATUS_APPROVED.equals(post.getReviewStatus());
    }

    /**
     * 评论公开判断，与帖子审核状态保持同一语义。
     */
    private boolean isApprovedComment(CommunityComment comment) {
        return comment != null && CommunityConstants.REVIEW_STATUS_APPROVED.equals(comment.getReviewStatus());
    }

    /**
     * 最小规则审核：先拦截明确违规文本，剩余正常内容进入人工审核队列。
     * 说明：当前不接外部审核服务，先用规则把政治、色情、辱骂、广告等高风险内容挡在入库前。
     */
    private void validateCommunityTextOrThrow(String text) {
        String normalized = normalizeModerationText(text);
        if (normalized.isBlank()) {
            return;
        }
        List<String> severeWords = List.of(
                "色情", "成人视频", "约炮", "裸聊", "成人视频资源",
                "政治敏感", "反动", "颠覆", "台独", "港独",
                "傻逼", "去死", "诈骗", "加微信", "博彩"
        );
        boolean matched = severeWords.stream().anyMatch(normalized::contains);
        if (matched) {
            throw new BusinessException("内容包含违规信息，请修改后再发布");
        }
    }

    /**
     * 审核文本归一化，减少空格、大小写和简单符号绕过。
     */
    private String normalizeModerationText(String text) {
        if (text == null) {
            return "";
        }
        return text.toLowerCase(Locale.ROOT)
                .replaceAll("\\s+", "")
                .replaceAll("[\\p{Punct}，。！？、；：“”‘’（）【】《》]", "");
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

    /**
     * 报告分享帖只保存会话ID本身，前端根据该ID生成站内跳转链接。
     */
    private String normalizeSharedInterviewSessionId(String sessionId) {
        if (sessionId == null || sessionId.isBlank()) {
            return null;
        }
        return sessionId.trim();
    }

    /**
     * 报告分享只能绑定发布者自己的面试会话，避免用户构造他人 sessionId 后公开他人报告。
     */
    private void validateSharedInterviewSessionOwnership(Long userId, String sessionId) {
        if (sessionId == null) {
            return;
        }
        Long count = interviewSessionMapper.selectCount(new LambdaQueryWrapper<InterviewSession>()
                .eq(InterviewSession::getSessionId, sessionId)
                .eq(InterviewSession::getUserId, userId)
                .eq(InterviewSession::getIsDeleted, 0));
        if (count == null || count == 0) {
            throw new BusinessException("只能分享自己的面试报告");
        }
    }

    /**
     * 校验每用户每日上传图片限额，防止 OSS 被当作免费图床滥用。
     * 使用 Redis INCR + TTL 实现滚动窗口计数，key 格式：community:upload:count:{userId}:{yyyyMMdd}
     * Redis 不可用时跳过计数（降级策略，不阻塞正常上传）
     */
    private void checkDailyUploadLimit(Long userId) {
        if (stringRedisTemplate == null || dailyUploadLimit <= 0) {
            return;
        }
        String today = java.time.LocalDate.now().format(java.time.format.DateTimeFormatter.BASIC_ISO_DATE);
        String redisKey = "community:upload:count:" + userId + ":" + today;
        try {
            Long count = stringRedisTemplate.opsForValue().increment(redisKey);
            // 首次写入时设置过期时间为第二天凌晨（确保 key 自动清理）
            if (count != null && count == 1) {
                stringRedisTemplate.expire(redisKey, java.time.Duration.ofDays(2));
            }
            if (count != null && count > dailyUploadLimit) {
                log.warn("用户每日上传图片超过限额, userId: {}, count: {}, limit: {}",
                        userId, count, dailyUploadLimit);
                throw new BusinessException("今日图片上传次数已达上限（" + dailyUploadLimit + "张）");
            }
        } catch (BusinessException e) {
            throw e;
        } catch (Exception e) {
            // Redis 异常时降级：记录警告但不阻塞上传
            log.warn("Redis每日上传计数异常, 降级放行, userId: {}", userId, e);
        }
    }
}
