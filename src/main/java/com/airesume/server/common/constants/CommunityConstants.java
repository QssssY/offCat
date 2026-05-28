package com.airesume.server.common.constants;

/**
 * 社区模块常量定义
 */
public class CommunityConstants {

    private CommunityConstants() {
        // 防止实例化
    }

    /** 帖子板块：面试经验分享 */
    public static final String CATEGORY_INTERVIEW_EXP = "interview_exp";

    /** 帖子板块：内推广场 */
    public static final String CATEGORY_REFERRAL = "referral";

    /** 排序方式：最新 */
    public static final String SORT_LATEST = "latest";

    /** 排序方式：最热（按点赞数） */
    public static final String SORT_HOT = "hot";

    /** 帖子内容最大长度 */
    public static final int MAX_CONTENT_LENGTH = 2000;

    /** 帖子标题最大长度 */
    public static final int MAX_TITLE_LENGTH = 120;

    /** 评论内容最大长度 */
    public static final int MAX_COMMENT_LENGTH = 500;

    /** 最大图片数量 */
    public static final int MAX_IMAGE_COUNT = 9;

    /** 单张图片最大大小：5MB */
    public static final long MAX_IMAGE_SIZE = 5 * 1024 * 1024;

    /** 默认分页大小 */
    public static final int DEFAULT_PAGE_SIZE = 15;

    /** 最大分页大小 */
    public static final int MAX_PAGE_SIZE = 50;
}
