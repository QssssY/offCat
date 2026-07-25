-- 社区内容审核字段与索引增量迁移
-- 说明：历史帖子和评论默认视为已通过，避免升级后现有社区内容全部消失。

SET NAMES utf8mb4;

DELIMITER $$

DROP PROCEDURE IF EXISTS add_column_if_missing $$
CREATE PROCEDURE add_column_if_missing(
  IN p_table_name VARCHAR(64) CHARACTER SET utf8mb4,
  IN p_column_name VARCHAR(64) CHARACTER SET utf8mb4,
  IN p_column_definition TEXT CHARACTER SET utf8mb4
)
BEGIN
  IF NOT EXISTS (
    SELECT 1 FROM information_schema.COLUMNS
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
  IN p_table_name VARCHAR(64) CHARACTER SET utf8mb4,
  IN p_index_name VARCHAR(64) CHARACTER SET utf8mb4,
  IN p_index_definition TEXT CHARACTER SET utf8mb4
)
BEGIN
  IF NOT EXISTS (
    SELECT 1 FROM information_schema.STATISTICS
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

CALL add_column_if_missing('community_post', 'review_status', '`review_status` VARCHAR(16) NOT NULL DEFAULT ''approved'' COMMENT ''审核状态：pending-待审，approved-通过，rejected-拒绝，hidden-隐藏'' AFTER `comment_count`');
CALL add_column_if_missing('community_post', 'review_reason', '`review_reason` VARCHAR(255) NULL COMMENT ''审核原因'' AFTER `review_status`');
CALL add_column_if_missing('community_post', 'reviewed_by', '`reviewed_by` BIGINT NULL COMMENT ''审核管理员用户ID'' AFTER `review_reason`');
CALL add_column_if_missing('community_post', 'reviewed_time', '`reviewed_time` DATETIME NULL COMMENT ''审核时间'' AFTER `reviewed_by`');
CALL add_index_if_missing('community_post', 'idx_community_post_review_time', 'INDEX `idx_community_post_review_time` (`review_status`, `create_time`)');
CALL add_index_if_missing('community_post', 'idx_community_post_public_review_time', 'INDEX `idx_community_post_public_review_time` (`is_deleted`, `review_status`, `category`, `create_time`)');

CALL add_column_if_missing('community_comment', 'review_status', '`review_status` VARCHAR(16) NOT NULL DEFAULT ''approved'' COMMENT ''审核状态：pending-待审，approved-通过，rejected-拒绝，hidden-隐藏'' AFTER `images`');
CALL add_column_if_missing('community_comment', 'review_reason', '`review_reason` VARCHAR(255) NULL COMMENT ''审核原因'' AFTER `review_status`');
CALL add_column_if_missing('community_comment', 'reviewed_by', '`reviewed_by` BIGINT NULL COMMENT ''审核管理员用户ID'' AFTER `review_reason`');
CALL add_column_if_missing('community_comment', 'reviewed_time', '`reviewed_time` DATETIME NULL COMMENT ''审核时间'' AFTER `reviewed_by`');
CALL add_index_if_missing('community_comment', 'idx_community_comment_review_time', 'INDEX `idx_community_comment_review_time` (`review_status`, `create_time`)');
CALL add_index_if_missing('community_comment', 'idx_community_comment_post_review_time', 'INDEX `idx_community_comment_post_review_time` (`post_id`, `review_status`, `create_time`)');

DROP PROCEDURE IF EXISTS add_column_if_missing;
DROP PROCEDURE IF EXISTS add_index_if_missing;
