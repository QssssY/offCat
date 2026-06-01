SET NAMES utf8mb4;

CREATE TABLE IF NOT EXISTS `user_ai_config` (
  `id` BIGINT NOT NULL COMMENT '雪花ID',
  `user_id` BIGINT NOT NULL COMMENT '用户ID',
  `config_type` VARCHAR(32) NOT NULL DEFAULT 'default' COMMENT '配置类型: default=通用, resume=简历, interview=面试',
  `provider_name` VARCHAR(64) NULL COMMENT '用户自定义名称',
  `base_url` VARCHAR(512) NOT NULL COMMENT 'OpenAI兼容API基础地址',
  `api_key` VARCHAR(1024) NOT NULL COMMENT 'API Key密文',
  `model` VARCHAR(128) NOT NULL COMMENT '模型标识',
  `is_enabled` TINYINT NOT NULL DEFAULT 1 COMMENT '是否启用: 1=启用, 0=禁用',
  `supports_multimodal` TINYINT NOT NULL DEFAULT 0 COMMENT '是否支持多模态',
  `last_verified_at` DATETIME NULL COMMENT '最后一次连通测试通过时间',
  `verification_status` VARCHAR(32) NOT NULL DEFAULT 'pending' COMMENT '连通状态: pending/verified/failed',
  `tts_base_url` VARCHAR(512) NULL COMMENT '预留: TTS服务地址',
  `tts_api_key` VARCHAR(1024) NULL COMMENT '预留: TTS API Key密文',
  `tts_model` VARCHAR(128) NULL COMMENT '预留: TTS模型',
  `tts_voice_id` VARCHAR(128) NULL COMMENT '预留: TTS音色ID',
  `create_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `update_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  `is_deleted` TINYINT NOT NULL DEFAULT 0,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_user_config_type` (`user_id`, `config_type`, `is_deleted`),
  KEY `idx_user_ai_config_user_enabled` (`user_id`, `is_enabled`, `config_type`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='用户自定义AI配置';

CREATE TABLE IF NOT EXISTS `user_ai_daily_usage` (
  `id` BIGINT NOT NULL COMMENT '雪花ID',
  `user_id` BIGINT NOT NULL COMMENT '用户ID',
  `usage_date` DATE NOT NULL COMMENT '使用日期',
  `call_count` INT NOT NULL DEFAULT 0 COMMENT '当日已调用次数',
  `create_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `update_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  `is_deleted` TINYINT NOT NULL DEFAULT 0,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_user_date` (`user_id`, `usage_date`),
  KEY `idx_user_ai_usage_date` (`usage_date`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='用户自定义AI每日使用量';

CREATE TABLE IF NOT EXISTS `sys_config` (
  `id` BIGINT NOT NULL COMMENT '雪花ID',
  `config_key` VARCHAR(128) NOT NULL COMMENT '配置键',
  `config_value` VARCHAR(512) NOT NULL COMMENT '配置值',
  `description` VARCHAR(256) NULL COMMENT '配置说明',
  `create_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `update_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  `is_deleted` TINYINT NOT NULL DEFAULT 0,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_config_key` (`config_key`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='系统配置';

INSERT INTO `sys_config` (`id`, `config_key`, `config_value`, `description`, `create_time`, `update_time`, `is_deleted`)
VALUES (1, 'custom_ai_daily_limit', '50', '用户自定义API Key每日调用上限', NOW(), NOW(), 0)
ON DUPLICATE KEY UPDATE
  `config_value` = IF(`config_value` IS NULL OR `config_value` = '', VALUES(`config_value`), `config_value`),
  `description` = VALUES(`description`),
  `update_time` = NOW(),
  `is_deleted` = 0;

DROP PROCEDURE IF EXISTS add_column_if_missing_task68;
DELIMITER $$
CREATE PROCEDURE add_column_if_missing_task68(
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
END$$
DELIMITER ;

CALL add_column_if_missing_task68('resume_diagnosis_task', 'ai_billing_source',
  '`ai_billing_source` VARCHAR(32) NOT NULL DEFAULT ''platform'' COMMENT ''AI计费来源: platform/user_custom'' AFTER `parse_message`');
CALL add_column_if_missing_task68('resume_diagnosis_task', 'fallback_to_platform',
  '`fallback_to_platform` TINYINT NOT NULL DEFAULT 0 COMMENT ''是否显式回退平台AI'' AFTER `ai_billing_source`');

DROP PROCEDURE IF EXISTS add_column_if_missing_task68;
