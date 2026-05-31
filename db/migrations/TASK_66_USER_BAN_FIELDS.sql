-- TASK 66 user ban metadata fields
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

CALL add_column_if_missing('sys_user', 'ban_reason', '`ban_reason` VARCHAR(255) NULL DEFAULT NULL COMMENT ''Account ban reason'' AFTER `vip_expire_time`');
CALL add_column_if_missing('sys_user', 'banned_until', '`banned_until` DATETIME NULL DEFAULT NULL COMMENT ''Account ban expiry time, null means permanent'' AFTER `ban_reason`');
CALL add_column_if_missing('sys_user', 'banned_by', '`banned_by` BIGINT NULL DEFAULT NULL COMMENT ''Admin user id who banned the account'' AFTER `banned_until`');
CALL add_column_if_missing('sys_user', 'banned_time', '`banned_time` DATETIME NULL DEFAULT NULL COMMENT ''Account ban time'' AFTER `banned_by`');
CALL add_index_if_missing('sys_user', 'idx_sys_user_banned_until', 'INDEX `idx_sys_user_banned_until` (`status`, `banned_until`)');

DROP PROCEDURE IF EXISTS add_column_if_missing;
DROP PROCEDURE IF EXISTS add_index_if_missing;
