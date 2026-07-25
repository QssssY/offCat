-- 管理端版本日志筛选性能索引
-- 说明：可重复执行；用于支撑 status/type 组合筛选后按 create_time 倒序分页。
SET @index_exists := (
  SELECT COUNT(1)
  FROM information_schema.STATISTICS
  WHERE TABLE_SCHEMA = DATABASE()
    AND TABLE_NAME = 'sys_version_log'
    AND INDEX_NAME = 'idx_version_log_filter_time'
);

SET @ddl := IF(
  @index_exists = 0,
  'ALTER TABLE `sys_version_log` ADD INDEX `idx_version_log_filter_time` (`status`, `type`, `create_time`)',
  'SELECT 1'
);

PREPARE stmt FROM @ddl;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;
