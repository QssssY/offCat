-- TASK_58: 后端性能优化补充索引
-- 本脚本只新增低风险二级索引，不删除线上既有索引。
-- 可重复执行：通过 information_schema.STATISTICS 判断索引是否已存在。

SET NAMES utf8mb4;

DELIMITER $$

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

CALL add_index_if_missing(
  'community_comment',
  'idx_community_comment_parent_time',
  'INDEX `idx_community_comment_parent_time` (`parent_comment_id`, `create_time`)'
);

CALL add_index_if_missing(
  'resume_diagnosis_task',
  'idx_resume_task_status_failed_at',
  'INDEX `idx_resume_task_status_failed_at` (`status`, `failed_at`)'
);

DROP PROCEDURE IF EXISTS add_index_if_missing;
