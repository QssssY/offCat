package com.airesume.server.controller;

import com.airesume.server.common.constants.CommunityConstants;
import com.airesume.server.common.result.PageResult;
import com.airesume.server.common.result.Result;
import com.airesume.server.dto.community.*;
import com.airesume.server.service.CommunityService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.MediaType;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.time.LocalDateTime;

/**
 * 社区模块控制器
 * 提供帖子列表/详情、发帖、点赞、评论、图片上传等接口
 */
@Slf4j
@RestController
@RequestMapping("/api/community")
@RequiredArgsConstructor
public class CommunityController {

    private final CommunityService communityService;

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
            @RequestParam(defaultValue = "1") Integer pageNum,
            @RequestParam(defaultValue = "15") Integer pageSize) {
        Long userId = (Long) authentication.getPrincipal();
        PageResult<PostVO> result;

        if ("liked".equals(filter)) {
            log.info("[社区] 查询点赞过的帖子, userId: {}, pageNum: {}", userId, pageNum);
            result = communityService.listLikedPosts(userId, pageNum, pageSize);
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
    public Result<String> createPost(
            Authentication authentication,
            @Valid @RequestBody CreatePostRequest request) {
        Long userId = (Long) authentication.getPrincipal();
        log.info("[社区] 创建帖子, userId: {}, category: {}", userId, request.getCategory());
        Long postId = communityService.createPost(userId, request);
        return Result.success(String.valueOf(postId));
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
     * 获取当前用户发布的评论列表（个人动态中心）
     */
    @GetMapping("/my/comments")
    public Result<PageResult<MyCommentVO>> listMyComments(
            Authentication authentication,
            @RequestParam(defaultValue = "1") Integer pageNum,
            @RequestParam(defaultValue = "15") Integer pageSize) {
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
            @RequestParam(defaultValue = "1") Integer pageNum,
            @RequestParam(defaultValue = "20") Integer pageSize) {
        Long userId = (Long) authentication.getPrincipal();
        log.info("[社区] 查询评论列表, userId: {}, postId: {}, pageNum: {}", userId, postId, pageNum);
        PageResult<CommentVO> result = communityService.listComments(postId, userId, pageNum, pageSize);
        return Result.success(result);
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
    public Result<String> createComment(
            Authentication authentication,
            @PathVariable Long postId,
            @Valid @RequestBody CreateCommentRequest request) {
        Long userId = (Long) authentication.getPrincipal();
        log.info("[社区] 创建评论, userId: {}, postId: {}", userId, postId);
        Long commentId = communityService.createComment(userId, postId, request);
        return Result.success(String.valueOf(commentId));
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
     * 获取评论的回复列表（分页）
     */
    @GetMapping("/posts/{postId}/comments/{commentId}/replies")
    public Result<PageResult<CommentVO>> listReplies(
            Authentication authentication,
            @PathVariable Long postId,
            @PathVariable Long commentId,
            @RequestParam(defaultValue = "1") Integer pageNum,
            @RequestParam(defaultValue = "20") Integer pageSize) {
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
            @RequestParam(defaultValue = "1") Integer pageNum,
            @RequestParam(defaultValue = "20") Integer pageSize) {
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
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime since) {
        Long userId = (Long) authentication.getPrincipal();
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
}
