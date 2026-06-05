package com.airesume.server.controller;

import com.airesume.server.common.constants.CommunityConstants;
import com.airesume.server.common.result.PageResult;
import com.airesume.server.common.result.Result;
import com.airesume.server.dto.community.*;
import com.airesume.server.service.CommunityService;
import com.airesume.server.service.OssService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.net.URI;
import java.time.LocalDateTime;

/**
 * 社区模块控制器
 * 提供帖子列表/详情、发帖、点赞、评论、图片上传等接口
 */
@Slf4j
@Validated
@RestController
@RequestMapping("/api/community")
@RequiredArgsConstructor
public class CommunityController {

    private final CommunityService communityService;
    private final OssService ossService;

    /** 生产默认拒绝缺失 Referer；开发环境可开启，方便本地直接调试图片。 */
    @Value("${app.community.image-access.allow-missing-referer:false}")
    private boolean allowMissingReferer;

    /**
     * 获取帖子列表（分页+筛选+排序）
     *
     * @param authentication 当前登录用户身份
     * @param category       板块筛选（可选）：interview_exp / referral
     * @param sort           排序方式：latest（默认）/ hot
     * @param mine           是否只看自己的帖子
     * @param pageNum        页码，默认1
     * @param pageSize       每页大小，默认15
     * @return 帖子分页列表
     */
    @GetMapping("/posts")
    public Result<PageResult<PostVO>> listPosts(
            Authentication authentication,
            @RequestParam(required = false) String category,
            @RequestParam(defaultValue = "latest") String sort,
            @RequestParam(defaultValue = "false") Boolean mine,
            @RequestParam(required = false) String filter,
            @RequestParam(defaultValue = "1") @Min(1) Integer pageNum,
            @RequestParam(defaultValue = "15") @Min(1) @Max(CommunityConstants.MAX_PAGE_SIZE) Integer pageSize) {
        Long userId = (Long) authentication.getPrincipal();
        PageResult<PostVO> result;

        if ("liked".equals(filter)) {
            log.info("[社区] 查询点赞过的帖子, userId: {}, pageNum: {}", userId, pageNum);
            result = communityService.listLikedPosts(userId, pageNum, pageSize);
        } else if ("favorited".equals(filter)) {
            log.info("[社区] 查询收藏的帖子, userId: {}, pageNum: {}", userId, pageNum);
            result = communityService.listFavoritedPosts(userId, pageNum, pageSize);
        } else if ("commented".equals(filter)) {
            log.info("[社区] 查询评论过的帖子, userId: {}, pageNum: {}", userId, pageNum);
            result = communityService.listCommentedPosts(userId, pageNum, pageSize);
        } else if (Boolean.TRUE.equals(mine)) {
            log.info("[社区] 查询我的帖子, userId: {}, pageNum: {}", userId, pageNum);
            result = communityService.listMyPosts(userId, pageNum, pageSize);
        } else {
            log.info("[社区] 查询帖子列表, userId: {}, category: {}, sort: {}, pageNum: {}", userId, category, sort, pageNum);
            result = communityService.listPosts(category, sort, pageNum, pageSize, userId);
        }

        return Result.success(result);
    }

    /**
     * 获取帖子详情
     *
     * @param authentication 当前登录用户身份
     * @param postId         帖子ID
     * @return 帖子详情
     */
    @GetMapping("/posts/{postId}")
    public Result<PostVO> getPostDetail(
            Authentication authentication,
            @PathVariable Long postId) {
        Long userId = (Long) authentication.getPrincipal();
        log.info("[社区] 查询帖子详情, userId: {}, postId: {}", userId, postId);
        PostVO post = communityService.getPostDetail(postId, userId);
        return Result.success(post);
    }

    /**
     * 创建帖子
     *
     * @param authentication 当前登录用户身份
     * @param request        创建帖子请求
     * @return 帖子ID
     */
    @PostMapping("/posts")
    public Result<CreateCommunityContentResponse> createPost(
            Authentication authentication,
            @Valid @RequestBody CreatePostRequest request) {
        Long userId = (Long) authentication.getPrincipal();
        log.info("[社区] 创建帖子, userId: {}, category: {}", userId, request.getCategory());
        CreateCommunityContentResponse response = communityService.createPost(userId, request);
        return Result.success(response);
    }

    /**
     * 删除帖子（仅限作者本人）
     */
    @DeleteMapping("/posts/{postId}")
    public Result<Void> deletePost(
            Authentication authentication,
            @PathVariable Long postId) {
        Long userId = (Long) authentication.getPrincipal();
        log.info("[社区] 删除帖子, userId: {}, postId: {}", userId, postId);
        communityService.deletePost(userId, postId);
        return Result.success();
    }

    /**
     * 管理员在用户端社区下架帖子。
     * 说明：接口仍走用户端 token，但服务层会再次校验管理员角色，避免只依赖前端按钮产生越权风险。
     */
    @PutMapping("/posts/{postId}/admin-hide")
    public Result<Void> adminHidePost(
            Authentication authentication,
            @PathVariable Long postId,
            @Valid @RequestBody AdminHidePostRequest request) {
        Long adminUserId = (Long) authentication.getPrincipal();
        log.info("[社区] 管理员下架帖子, adminUserId: {}, postId: {}", adminUserId, postId);
        communityService.adminHidePost(adminUserId, postId, request.getReason());
        return Result.success("帖子已下架", null);
    }

    /**
     * 点赞/取消点赞（幂等操作）
     *
     * @param authentication 当前登录用户身份
     * @param postId         帖子ID
     * @return 操作后的点赞状态
     */
    @PostMapping("/posts/{postId}/like")
    public Result<Boolean> toggleLike(
            Authentication authentication,
            @PathVariable Long postId) {
        Long userId = (Long) authentication.getPrincipal();
        log.info("[社区] 切换点赞, userId: {}, postId: {}", userId, postId);
        boolean liked = communityService.toggleLike(userId, postId);
        return Result.success(liked);
    }

    /**
     * 收藏/取消收藏（幂等操作）
     *
     * @param authentication 当前登录用户身份
     * @param postId         帖子ID
     * @return 操作后的收藏状态
     */
    @PostMapping("/posts/{postId}/favorite")
    public Result<Boolean> toggleFavorite(
            Authentication authentication,
            @PathVariable Long postId) {
        Long userId = (Long) authentication.getPrincipal();
        log.info("[社区] 切换收藏, userId: {}, postId: {}", userId, postId);
        boolean favorited = communityService.toggleFavorite(userId, postId);
        return Result.success(favorited);
    }

    /**
     * 获取当前用户发布的评论列表（个人动态中心）
     */
    @GetMapping("/my/comments")
    public Result<PageResult<MyCommentVO>> listMyComments(
            Authentication authentication,
            @RequestParam(defaultValue = "1") @Min(1) Integer pageNum,
            @RequestParam(defaultValue = "15") @Min(1) @Max(CommunityConstants.MAX_PAGE_SIZE) Integer pageSize) {
        Long userId = (Long) authentication.getPrincipal();
        log.info("[社区] 查询我的评论, userId: {}, pageNum: {}", userId, pageNum);
        PageResult<MyCommentVO> result = communityService.listMyComments(userId, pageNum, pageSize);
        return Result.success(result);
    }

    /**
     * 获取帖子评论列表（分页）
     *
     * @param authentication 当前登录用户身份
     * @param postId         帖子ID
     * @param pageNum        页码，默认1
     * @param pageSize       每页大小，默认20
     * @return 评论分页列表
     */
    @GetMapping("/posts/{postId}/comments")
    public Result<PageResult<CommentVO>> listComments(
            Authentication authentication,
            @PathVariable Long postId,
            @RequestParam(defaultValue = "1") @Min(1) Integer pageNum,
            @RequestParam(defaultValue = "20") @Min(1) @Max(CommunityConstants.MAX_PAGE_SIZE) Integer pageSize) {
        Long userId = (Long) authentication.getPrincipal();
        log.info("[社区] 查询评论列表, userId: {}, postId: {}, pageNum: {}", userId, postId, pageNum);
        PageResult<CommentVO> result = communityService.listComments(postId, userId, pageNum, pageSize);
        return Result.success(result);
    }

    /**
     * 获取单条评论详情
     */
    @GetMapping("/posts/{postId}/comments/{commentId}/detail")
    public Result<CommentVO> getCommentDetail(
            Authentication authentication,
            @PathVariable Long postId,
            @PathVariable Long commentId) {
        Long userId = authentication != null ? (Long) authentication.getPrincipal() : null;
        CommentVO vo = communityService.getCommentDetail(postId, commentId, userId);
        return Result.success(vo);
    }

    /**
     * 创建评论
     *
     * @param authentication 当前登录用户身份
     * @param postId         帖子ID
     * @param request        创建评论请求
     * @return 评论ID
     */
    @PostMapping("/posts/{postId}/comments")
    public Result<CreateCommunityContentResponse> createComment(
            Authentication authentication,
            @PathVariable Long postId,
            @Valid @RequestBody CreateCommentRequest request) {
        Long userId = (Long) authentication.getPrincipal();
        log.info("[社区] 创建评论, userId: {}, postId: {}", userId, postId);
        CreateCommunityContentResponse response = communityService.createComment(userId, postId, request);
        return Result.success(response);
    }

    /**
     * 删除评论
     * 规则：评论作者本人可删除，帖子作者也可删除其帖子下的任意评论
     */
    @DeleteMapping("/posts/{postId}/comments/{commentId}")
    public Result<Void> deleteComment(
            Authentication authentication,
            @PathVariable Long postId,
            @PathVariable Long commentId) {
        Long userId = (Long) authentication.getPrincipal();
        log.info("[社区] 删除评论, userId: {}, postId: {}, commentId: {}", userId, postId, commentId);
        communityService.deleteComment(userId, postId, commentId);
        return Result.success();
    }

    /**
     * 管理员在用户端下架评论或回复。
     */
    @PutMapping("/posts/{postId}/comments/{commentId}/admin-hide")
    public Result<Void> adminHideComment(
            Authentication authentication,
            @PathVariable Long postId,
            @PathVariable Long commentId,
            @Valid @RequestBody AdminHideCommentRequest request) {
        Long adminUserId = (Long) authentication.getPrincipal();
        log.info("[社区] 管理员下架评论, adminUserId: {}, postId: {}, commentId: {}", adminUserId, postId, commentId);
        communityService.adminHideComment(adminUserId, postId, commentId, request.getReason());
        return Result.success("评论已下架", null);
    }

    /**
     * 获取评论的回复列表（分页）
     */
    @GetMapping("/posts/{postId}/comments/{commentId}/replies")
    public Result<PageResult<CommentVO>> listReplies(
            Authentication authentication,
            @PathVariable Long postId,
            @PathVariable Long commentId,
            @RequestParam(defaultValue = "1") @Min(1) Integer pageNum,
            @RequestParam(defaultValue = "20") @Min(1) @Max(CommunityConstants.MAX_PAGE_SIZE) Integer pageSize) {
        Long userId = (Long) authentication.getPrincipal();
        log.info("[社区] 查询回复列表, userId: {}, postId: {}, commentId: {}", userId, postId, commentId);
        PageResult<CommentVO> result = communityService.listReplies(postId, commentId, userId, pageNum, pageSize);
        return Result.success(result);
    }

    /**
     * 获取当前用户收到的互动信息（别人对我帖子的点赞和评论）
     */
    @GetMapping("/my/interactions")
    public Result<ReceivedInteractionVO> listReceivedInteractions(
            Authentication authentication,
            @RequestParam(defaultValue = "1") @Min(1) Integer pageNum,
            @RequestParam(defaultValue = "20") @Min(1) @Max(CommunityConstants.MAX_PAGE_SIZE) Integer pageSize) {
        Long userId = (Long) authentication.getPrincipal();
        log.info("[社区] 查询收到的互动信息, userId: {}, pageNum: {}", userId, pageNum);
        ReceivedInteractionVO result = communityService.listReceivedInteractions(userId, pageNum, pageSize);
        return Result.success(result);
    }

    /**
     * 获取未读互动数量
     */
    @GetMapping("/my/interactions/unread-count")
    public Result<Integer> getUnreadInteractionCount(
            Authentication authentication,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime since) {
        Long userId = (Long) authentication.getPrincipal();
        if (since == null) {
            since = LocalDateTime.of(2000, 1, 1, 0, 0);
        }
        log.info("[社区] 查询未读互动数量, userId: {}, since: {}", userId, since);
        int count = communityService.getUnreadInteractionCount(userId, since);
        return Result.success(count);
    }

    /**
     * 上传社区图片
     *
     * @param authentication 当前登录用户身份
     * @param file           图片文件
     * @return 图片访问URL
     */
    @PostMapping(value = "/images/upload", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public Result<ImageUploadResponse> uploadImage(
            Authentication authentication,
            @RequestParam("file") MultipartFile file) {
        Long userId = (Long) authentication.getPrincipal();
        log.info("[社区] 上传图片, userId: {}, fileName: {}, fileSize: {}",
                userId, file.getOriginalFilename(), file.getSize());
        String url = communityService.uploadImage(file, userId);
        return Result.success(ImageUploadResponse.builder().url(url).build());
    }

    /**
     * 社区图片签名访问
     * 浏览器通过此端点获取 OSS 签名 URL 并 302 重定向到实际图片。
     * 路径格式：/api/community/images/community/{userId}/{date}/{uuid}.jpg
     * <p>
     * 该接口无需认证（img 标签不会携带 JWT），在 SecurityConfig 中放行。
     * 包含 Referer 盗链防护：只允许来自本站域名的请求访问图片。
     *
     * @param objectKey OSS 对象键路径（如 community/12345/20260604/uuid.jpg）
     * @param request   HTTP 请求（用于获取 Referer 头）
     * @return 302 重定向到 OSS 签名 URL
     */
    @GetMapping("/images/{*objectKey}")
    public ResponseEntity<Void> getImage(@PathVariable String objectKey,
                                         HttpServletRequest request) {
        // Spring 6 的 {*var} 语法可能包含前导 "/"，统一去除
        if (objectKey != null && objectKey.startsWith("/")) {
            objectKey = objectKey.substring(1);
        }
        // 安全校验：只允许 community/ 前缀的 object key，防止访问非社区资源
        if (objectKey == null || !objectKey.startsWith("community/")) {
            return ResponseEntity.notFound().build();
        }
        // 防御路径遍历尝试（OSS key 是扁平的，但做纵深防御）
        if (objectKey.contains("..") || objectKey.contains("//")) {
            return ResponseEntity.notFound().build();
        }
        // 校验 key 格式匹配上传模式：community/{userId}/{date}/{uuid}.{ext}
        if (!objectKey.matches("community/\\d+/\\d{8}/[0-9a-f]+\\.(jpg|jpeg|png|gif|webp)")) {
            return ResponseEntity.notFound().build();
        }

        // Referer 盗链防护：只允许来自本站域名的请求
        if (!isRefererAllowed(request)) {
            log.warn("图片盗链请求被拒绝, objectKey: {}, referer: {}, remoteAddr: {}",
                    objectKey, request.getHeader("Referer"), request.getRemoteAddr());
            return ResponseEntity.status(403).build();
        }

        if (!ossService.isEnabled()) {
            log.warn("OSS未启用, 无法访问图片: {}", objectKey);
            return ResponseEntity.notFound().build();
        }

        String signedUrl = ossService.generateSignedUrl(objectKey);
        log.info("社区图片签名访问, objectKey: {}, referer: {}",
                objectKey,
                request.getHeader("Referer"));
        return ResponseEntity.status(302)
                .header("Location", signedUrl)
                // 浏览器缓存重定向 1 小时，过期后重新请求获取新的签名 URL
                .header("Cache-Control", "private, max-age=3600")
                .build();
    }

    /**
     * 校验请求 Referer 是否来自允许的域名。
     * 允许规则：
     * 1. 无 Referer（直接访问/浏览器地址栏/部分隐私模式）→ 放行
     * 2. Referer 域名在 CORS_ALLOWED_ORIGINS 环境变量中 → 放行
     * 3. Referer 域名为 localhost（开发环境）→ 放行
     * 其他来源一律拒绝，防止外站将本站当作免费图床嵌入。
     */
    private boolean isRefererAllowed(HttpServletRequest request) {
        String referer = request.getHeader("Referer");
        // 无 Referer 放行（浏览器直接访问、部分隐私模式、部分移动浏览器）
        if (referer == null || referer.isBlank()) {
            return allowMissingReferer;
        }
        try {
            String host = new URI(referer).getHost();
            if (host == null) {
                return false;
            }
            // 开发环境始终允许 localhost
            if (allowMissingReferer && ("localhost".equals(host) || host.startsWith("127.0.0.1"))) {
                return true;
            }
            // 检查 CORS 允许的域名列表（复用 CORS_ALLOWED_ORIGINS 环境变量）
            String allowedOrigins = System.getenv("CORS_ALLOWED_ORIGINS");
            if (allowedOrigins != null && !allowedOrigins.isBlank()) {
                for (String origin : allowedOrigins.split(",")) {
                    try {
                        String allowedHost = new URI(origin.trim()).getHost();
                        if (host.equals(allowedHost)) {
                            return true;
                        }
                    } catch (Exception ignored) {
                        // 跳过无效的 origin 配置
                    }
                }
            }
            return false;
        } catch (Exception e) {
            // Referer 解析失败时放行，避免影响正常用户
            log.warn("Referer解析失败, 放行: {}", referer);
            return false;
        }
    }
}
