-- V1.4: 新增社区模块数据表（帖子、评论、点赞）
-- 依赖：sys_user 表已存在

SET NAMES utf8mb4;
SET FOREIGN_KEY_CHECKS = 0;

-- 社区帖子表
CREATE TABLE IF NOT EXISTS `community_post` (
  `id` BIGINT NOT NULL COMMENT '主键',
  `user_id` BIGINT NOT NULL COMMENT '发布者用户ID',
  `category` VARCHAR(32) NOT NULL COMMENT '帖子板块：interview_exp-面试经验分享，referral-内推广场',
  `content` TEXT NOT NULL COMMENT '帖子内容',
  `images` JSON NULL COMMENT '图片URL列表JSON数组',
  `like_count` INT NOT NULL DEFAULT 0 COMMENT '点赞数',
  `comment_count` INT NOT NULL DEFAULT 0 COMMENT '评论数',
  `create_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `is_deleted` TINYINT NOT NULL DEFAULT 0 COMMENT '逻辑删除标志',
  PRIMARY KEY (`id`),
  INDEX `idx_community_post_user_id` (`user_id`),
  INDEX `idx_community_post_category` (`category`),
  INDEX `idx_community_post_create_time` (`create_time`),
  INDEX `idx_community_post_category_time` (`category`, `create_time`),
  INDEX `idx_community_post_category_like` (`category`, `like_count`),
  CONSTRAINT `fk_community_post_user_id` FOREIGN KEY (`user_id`) REFERENCES `sys_user` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='社区帖子表';

-- 社区评论表
CREATE TABLE IF NOT EXISTS `community_comment` (
  `id` BIGINT NOT NULL COMMENT '主键',
  `post_id` BIGINT NOT NULL COMMENT '所属帖子ID',
  `user_id` BIGINT NOT NULL COMMENT '评论者用户ID',
  `content` TEXT NOT NULL COMMENT '评论内容',
  `create_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `is_deleted` TINYINT NOT NULL DEFAULT 0 COMMENT '逻辑删除标志',
  PRIMARY KEY (`id`),
  INDEX `idx_community_comment_post_id` (`post_id`),
  INDEX `idx_community_comment_user_id` (`user_id`),
  INDEX `idx_community_comment_create_time` (`create_time`),
  INDEX `idx_community_comment_post_time` (`post_id`, `create_time`),
  CONSTRAINT `fk_community_comment_post_id` FOREIGN KEY (`post_id`) REFERENCES `community_post` (`id`),
  CONSTRAINT `fk_community_comment_user_id` FOREIGN KEY (`user_id`) REFERENCES `sys_user` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='社区评论表';

-- 社区帖子点赞表（幂等：同一用户对同一帖子只能点赞一次）
CREATE TABLE IF NOT EXISTS `community_post_like` (
  `id` BIGINT NOT NULL COMMENT '主键',
  `post_id` BIGINT NOT NULL COMMENT '帖子ID',
  `user_id` BIGINT NOT NULL COMMENT '点赞用户ID',
  `create_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `is_deleted` TINYINT NOT NULL DEFAULT 0 COMMENT '逻辑删除标志',
  PRIMARY KEY (`id`),
  UNIQUE INDEX `uk_post_like_user` (`post_id`, `user_id`),
  INDEX `idx_community_post_like_user_id` (`user_id`),
  INDEX `idx_community_post_like_create_time` (`create_time`),
  CONSTRAINT `fk_community_post_like_post_id` FOREIGN KEY (`post_id`) REFERENCES `community_post` (`id`),
  CONSTRAINT `fk_community_post_like_user_id` FOREIGN KEY (`user_id`) REFERENCES `sys_user` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='社区帖子点赞表';

SET FOREIGN_KEY_CHECKS = 1;
