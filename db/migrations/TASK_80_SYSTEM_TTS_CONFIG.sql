SET NAMES utf8mb4;

CREATE TABLE IF NOT EXISTS `sys_tts_config` (
  `id` BIGINT NOT NULL COMMENT '雪花ID',
  `singleton_key` TINYINT NOT NULL DEFAULT 1 COMMENT '系统TTS单例键，固定为1',
  `tts_provider` VARCHAR(32) NOT NULL DEFAULT 'openai' COMMENT 'TTS提供商标识: openai/mimo等',
  `base_url` VARCHAR(512) NULL COMMENT 'TTS服务基础地址',
  `api_key` VARCHAR(1024) NULL COMMENT 'TTS API Key密文',
  `model` VARCHAR(128) NULL COMMENT 'TTS模型标识',
  `voice_id` VARCHAR(128) NULL COMMENT 'TTS音色ID',
  `endpoint_path` VARCHAR(128) NOT NULL DEFAULT '/audio/speech' COMMENT 'TTS合成端点路径',
  `enabled` TINYINT NOT NULL DEFAULT 0 COMMENT '是否启用系统TTS: 1=启用, 0=禁用',
  `create_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `is_deleted` TINYINT NOT NULL DEFAULT 0 COMMENT '逻辑删除标记',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_sys_tts_config_singleton` (`singleton_key`, `is_deleted`),
  KEY `idx_sys_tts_config_enabled` (`enabled`, `is_deleted`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='系统级TTS配置';
