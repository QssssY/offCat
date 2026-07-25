-- TASK_56: 社区功能整合迁移
-- 来源：本次 pull 的社区发帖、评论、回复、图片、点赞、收藏相关数据库变更。
-- 使用方式：在目标数据库中执行本文件一次即可；脚本尽量保持幂等，不删除现有业务数据。

SET NAMES utf8mb4;

DELIMITER $$

DROP PROCEDURE IF EXISTS add_column_if_missing $$
CREATE PROCEDURE add_column_if_missing(
  IN p_table_name VARCHAR(64),
  IN p_column_name VARCHAR(64),
  IN p_column_definition TEXT
)
BEGIN
  IF NOT EXISTS (
    SELECT 1
    FROM information_schema.COLUMNS
    WHERE TABLE_SCHEMA = DATABASE()
      AND TABLE_NAME = p_table_name
      AND COLUMN_NAME = p_column_name
  ) THEN
    SET @ddl = CONCAT('ALTER TABLE `', p_table_name, '` ADD COLUMN ', p_column_definition);
    PREPARE stmt FROM @ddl;
    EXECUTE stmt;
    DEALLOCATE PREPARE stmt;
  END IF;
END $$

DROP PROCEDURE IF EXISTS add_index_if_missing $$
CREATE PROCEDURE add_index_if_missing(
  IN p_table_name VARCHAR(64),
  IN p_index_name VARCHAR(64),
  IN p_index_definition TEXT
)
BEGIN
  IF NOT EXISTS (
    SELECT 1
    FROM information_schema.STATISTICS
    WHERE TABLE_SCHEMA = DATABASE()
      AND TABLE_NAME = p_table_name
      AND INDEX_NAME = p_index_name
  ) THEN
    SET @ddl = CONCAT('ALTER TABLE `', p_table_name, '` ADD ', p_index_definition);
    PREPARE stmt FROM @ddl;
    EXECUTE stmt;
    DEALLOCATE PREPARE stmt;
  END IF;
END $$

DELIMITER ;

CREATE TABLE IF NOT EXISTS `community_post` (
  `id` BIGINT NOT NULL COMMENT '主键',
  `user_id` BIGINT NOT NULL COMMENT '发布者用户ID',
  `category` VARCHAR(32) NOT NULL COMMENT '帖子板块：interview_exp-面试经验分享，referral-内推广场',
  `title` VARCHAR(120) NOT NULL DEFAULT '未命名帖子' COMMENT '帖子标题',
  `content` TEXT NOT NULL COMMENT '帖子内容',
  `shared_interview_session_id` VARCHAR(64) NULL COMMENT '分享的面试报告会话ID',
  `images` JSON NULL COMMENT '图片URL列表JSON数组',
  `like_count` INT NOT NULL DEFAULT 0 COMMENT '点赞数',
  `comment_count` INT NOT NULL DEFAULT 0 COMMENT '评论数',
  `create_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `is_deleted` TINYINT NOT NULL DEFAULT 0 COMMENT '逻辑删除标志',
  PRIMARY KEY (`id`),
  INDEX `idx_community_post_user_id` (`user_id`),
  INDEX `idx_community_post_category` (`category`),
  INDEX `idx_community_post_shared_interview_session_id` (`shared_interview_session_id`),
  INDEX `idx_community_post_create_time` (`create_time`),
  INDEX `idx_community_post_category_time` (`category`, `create_time`),
  INDEX `idx_community_post_category_like` (`category`, `like_count`),
  CONSTRAINT `fk_community_post_user_id` FOREIGN KEY (`user_id`) REFERENCES `sys_user` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='社区帖子表';

CREATE TABLE IF NOT EXISTS `community_comment` (
  `id` BIGINT NOT NULL COMMENT '主键',
  `post_id` BIGINT NOT NULL COMMENT '所属帖子ID',
  `user_id` BIGINT NOT NULL COMMENT '评论者用户ID',
  `parent_comment_id` BIGINT NULL DEFAULT NULL COMMENT '父评论ID，NULL表示顶级评论',
  `reply_to_user_id` BIGINT NULL DEFAULT NULL COMMENT '被回复用户ID',
  `content` TEXT NOT NULL COMMENT '评论内容',
  `images` JSON NULL COMMENT '评论图片URL列表JSON数组',
  `create_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `is_deleted` TINYINT NOT NULL DEFAULT 0 COMMENT '逻辑删除标志',
  PRIMARY KEY (`id`),
  INDEX `idx_community_comment_post_id` (`post_id`),
  INDEX `idx_community_comment_user_id` (`user_id`),
  INDEX `idx_community_comment_create_time` (`create_time`),
  INDEX `idx_community_comment_post_time` (`post_id`, `create_time`),
  INDEX `idx_community_comment_parent_id` (`parent_comment_id`),
  INDEX `idx_community_comment_reply_user` (`reply_to_user_id`, `create_time`),
  CONSTRAINT `fk_community_comment_post_id` FOREIGN KEY (`post_id`) REFERENCES `community_post` (`id`),
  CONSTRAINT `fk_community_comment_user_id` FOREIGN KEY (`user_id`) REFERENCES `sys_user` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='社区评论表';

CREATE TABLE IF NOT EXISTS `community_post_like` (
  `id` BIGINT NOT NULL COMMENT '主键',
  `post_id` BIGINT NOT NULL COMMENT '帖子ID',
  `user_id` BIGINT NOT NULL COMMENT '点赞用户ID',
  `create_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  PRIMARY KEY (`id`),
  UNIQUE INDEX `uk_post_like_user` (`post_id`, `user_id`),
  INDEX `idx_community_post_like_user_id` (`user_id`),
  INDEX `idx_community_post_like_create_time` (`create_time`),
  CONSTRAINT `fk_community_post_like_post_id` FOREIGN KEY (`post_id`) REFERENCES `community_post` (`id`),
  CONSTRAINT `fk_community_post_like_user_id` FOREIGN KEY (`user_id`) REFERENCES `sys_user` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='社区帖子点赞表';

CREATE TABLE IF NOT EXISTS `community_post_favorite` (
  `id` BIGINT NOT NULL COMMENT '主键',
  `post_id` BIGINT NOT NULL COMMENT '帖子ID',
  `user_id` BIGINT NOT NULL COMMENT '收藏用户ID',
  `create_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  PRIMARY KEY (`id`),
  UNIQUE INDEX `uk_post_favorite_user` (`post_id`, `user_id`),
  INDEX `idx_community_post_favorite_user_id` (`user_id`),
  INDEX `idx_community_post_favorite_create_time` (`create_time`),
  CONSTRAINT `fk_community_post_favorite_post_id` FOREIGN KEY (`post_id`) REFERENCES `community_post` (`id`),
  CONSTRAINT `fk_community_post_favorite_user_id` FOREIGN KEY (`user_id`) REFERENCES `sys_user` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='社区帖子收藏表';

CALL add_column_if_missing('community_post', 'title', '`title` VARCHAR(120) NOT NULL DEFAULT ''未命名帖子'' COMMENT ''帖子标题'' AFTER `category`');
CALL add_column_if_missing('community_post', 'shared_interview_session_id', '`shared_interview_session_id` VARCHAR(64) NULL COMMENT ''分享的面试报告会话ID'' AFTER `content`');
CALL add_column_if_missing('community_post', 'images', '`images` JSON NULL COMMENT ''图片URL列表JSON数组'' AFTER `shared_interview_session_id`');
CALL add_column_if_missing('community_post', 'like_count', '`like_count` INT NOT NULL DEFAULT 0 COMMENT ''点赞数'' AFTER `images`');
CALL add_column_if_missing('community_post', 'comment_count', '`comment_count` INT NOT NULL DEFAULT 0 COMMENT ''评论数'' AFTER `like_count`');

CALL add_column_if_missing('community_comment', 'parent_comment_id', '`parent_comment_id` BIGINT NULL DEFAULT NULL COMMENT ''父评论ID，NULL表示顶级评论'' AFTER `user_id`');
CALL add_column_if_missing('community_comment', 'reply_to_user_id', '`reply_to_user_id` BIGINT NULL DEFAULT NULL COMMENT ''被回复用户ID'' AFTER `parent_comment_id`');
CALL add_column_if_missing('community_comment', 'images', '`images` JSON NULL COMMENT ''评论图片URL列表JSON数组'' AFTER `content`');

CALL add_index_if_missing('community_post', 'idx_community_post_user_id', 'INDEX `idx_community_post_user_id` (`user_id`)');
CALL add_index_if_missing('community_post', 'idx_community_post_category', 'INDEX `idx_community_post_category` (`category`)');
CALL add_index_if_missing('community_post', 'idx_community_post_shared_interview_session_id', 'INDEX `idx_community_post_shared_interview_session_id` (`shared_interview_session_id`)');
CALL add_index_if_missing('community_post', 'idx_community_post_create_time', 'INDEX `idx_community_post_create_time` (`create_time`)');
CALL add_index_if_missing('community_post', 'idx_community_post_category_time', 'INDEX `idx_community_post_category_time` (`category`, `create_time`)');
CALL add_index_if_missing('community_post', 'idx_community_post_category_like', 'INDEX `idx_community_post_category_like` (`category`, `like_count`)');

CALL add_index_if_missing('community_comment', 'idx_community_comment_post_id', 'INDEX `idx_community_comment_post_id` (`post_id`)');
CALL add_index_if_missing('community_comment', 'idx_community_comment_user_id', 'INDEX `idx_community_comment_user_id` (`user_id`)');
CALL add_index_if_missing('community_comment', 'idx_community_comment_create_time', 'INDEX `idx_community_comment_create_time` (`create_time`)');
CALL add_index_if_missing('community_comment', 'idx_community_comment_post_time', 'INDEX `idx_community_comment_post_time` (`post_id`, `create_time`)');
CALL add_index_if_missing('community_comment', 'idx_community_comment_parent_id', 'INDEX `idx_community_comment_parent_id` (`parent_comment_id`)');
CALL add_index_if_missing('community_comment', 'idx_community_comment_reply_user', 'INDEX `idx_community_comment_reply_user` (`reply_to_user_id`, `create_time`)');

CALL add_index_if_missing('community_post_like', 'uk_post_like_user', 'UNIQUE INDEX `uk_post_like_user` (`post_id`, `user_id`)');
CALL add_index_if_missing('community_post_like', 'idx_community_post_like_user_id', 'INDEX `idx_community_post_like_user_id` (`user_id`)');
CALL add_index_if_missing('community_post_like', 'idx_community_post_like_create_time', 'INDEX `idx_community_post_like_create_time` (`create_time`)');

CALL add_index_if_missing('community_post_favorite', 'uk_post_favorite_user', 'UNIQUE INDEX `uk_post_favorite_user` (`post_id`, `user_id`)');
CALL add_index_if_missing('community_post_favorite', 'idx_community_post_favorite_user_id', 'INDEX `idx_community_post_favorite_user_id` (`user_id`)');
CALL add_index_if_missing('community_post_favorite', 'idx_community_post_favorite_create_time', 'INDEX `idx_community_post_favorite_create_time` (`create_time`)');

DROP PROCEDURE IF EXISTS add_column_if_missing;
DROP PROCEDURE IF EXISTS add_index_if_missing;
