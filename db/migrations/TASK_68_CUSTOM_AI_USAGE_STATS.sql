SET NAMES utf8mb4;

CREATE TABLE IF NOT EXISTS `user_ai_usage_detail` (
  `id` BIGINT NOT NULL COMMENT '雪花ID',
  `user_id` BIGINT NOT NULL COMMENT '用户ID',
  `usage_date` DATE NOT NULL COMMENT '使用日期',
  `usage_type` VARCHAR(64) NOT NULL COMMENT '功能统计类型',
  `call_count` INT NOT NULL DEFAULT 0 COMMENT '当日该功能已调用次数',
  `create_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `update_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  `is_deleted` TINYINT NOT NULL DEFAULT 0,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_user_ai_usage_detail_user_date_type` (`user_id`, `usage_date`, `usage_type`),
  KEY `idx_user_ai_usage_detail_date_type` (`usage_date`, `usage_type`),
  KEY `idx_user_ai_usage_detail_user_date` (`user_id`, `usage_date`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='用户自定义AI按功能每日使用明细';
