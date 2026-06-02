SET NAMES utf8mb4;

-- 模拟面试自定义 AI 会话手动切平台后按整场会话只扣一次平台额度。
-- 该字段用于锁定会话计费来源，避免同一 session 重复点击或并发 fallback 重复扣费。
DROP PROCEDURE IF EXISTS add_column_if_missing_task74;

DELIMITER $$
CREATE PROCEDURE add_column_if_missing_task74(
  IN p_table_name VARCHAR(128),
  IN p_column_name VARCHAR(128),
  IN p_column_definition TEXT CHARACTER SET utf8mb4
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
END$$
DELIMITER ;

CALL add_column_if_missing_task74(
  'interview_session',
  'ai_billing_source',
  '`ai_billing_source` VARCHAR(32) NOT NULL DEFAULT ''platform'' COMMENT ''AI billing source: platform/user_custom/platform_fallback'' AFTER `interaction_type`'
);

DROP PROCEDURE IF EXISTS add_column_if_missing_task74;
