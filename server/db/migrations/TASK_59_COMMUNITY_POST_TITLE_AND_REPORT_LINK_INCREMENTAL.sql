-- TASK_59: 社区帖子标题与面试报告链接分享增量迁移
-- 适用场景：目标库已执行过旧版 TASK_56，社区表已存在，但缺少本轮新增字段和索引。
-- 执行方式：在目标数据库执行本文件一次即可；脚本按字段名和索引名幂等补齐，不删除任何业务数据。

SET NAMES utf8mb4;

DELIMITER $$

DROP PROCEDURE IF EXISTS add_community_column_if_missing $$
CREATE PROCEDURE add_community_column_if_missing(
  IN p_table_name VARCHAR(64),
  IN p_column_name VARCHAR(64),
  IN p_column_definition TEXT
)
BEGIN
  IF NOT EXISTS (
    SELECT 1
    FROM information_schema.TABLES
    WHERE TABLE_SCHEMA = DATABASE()
      AND TABLE_NAME = p_table_name
  ) THEN
    SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'community_post table does not exist; run TASK_56 first';
  END IF;

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

DROP PROCEDURE IF EXISTS add_community_index_if_missing $$
CREATE PROCEDURE add_community_index_if_missing(
  IN p_table_name VARCHAR(64),
  IN p_index_name VARCHAR(64),
  IN p_index_definition TEXT
)
BEGIN
  IF NOT EXISTS (
    SELECT 1
    FROM information_schema.TABLES
    WHERE TABLE_SCHEMA = DATABASE()
      AND TABLE_NAME = p_table_name
  ) THEN
    SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'community_post table does not exist; run TASK_56 first';
  END IF;

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

-- 旧社区表补齐帖子标题。默认值用于兼容线上已有历史帖子。
CALL add_community_column_if_missing(
  'community_post',
  'title',
  '`title` VARCHAR(120) NOT NULL DEFAULT ''未命名帖子'' COMMENT ''帖子标题'' AFTER `category`'
);

-- 报告分享帖只保存面试会话 ID，前端据此生成站内报告跳转链接。
CALL add_community_column_if_missing(
  'community_post',
  'shared_interview_session_id',
  '`shared_interview_session_id` VARCHAR(64) NULL COMMENT ''分享的面试报告会话ID'' AFTER `content`'
);

-- 社区报告访问授权会按分享会话 ID 查询未删除帖子，需要补充索引。
CALL add_community_index_if_missing(
  'community_post',
  'idx_community_post_shared_interview_session_id',
  'INDEX `idx_community_post_shared_interview_session_id` (`shared_interview_session_id`)'
);

DROP PROCEDURE IF EXISTS add_community_column_if_missing;
DROP PROCEDURE IF EXISTS add_community_index_if_missing;
