-- TASK_ADMIN_NOTIFICATION_FILTER_INDEXES: admin notification filter performance indexes
-- Repeatable migration: only adds secondary indexes when missing.
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
  'sys_admin_notification',
  'idx_admin_notification_filter_time',
  'INDEX `idx_admin_notification_filter_time` (`target_type`, `status`, `type`, `create_time`)'
);

