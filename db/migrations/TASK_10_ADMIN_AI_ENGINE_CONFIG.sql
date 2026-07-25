SET NAMES utf8mb4;

/*
  作用：
  为管理端新增 AI 引擎配置表，支持 interview / resume 两类业务分别维护模型配置。
  当前阶段先完成后台管理闭环，不改动运行时 AI 调用链路。
*/

CREATE TABLE IF NOT EXISTS `sys_ai_engine_config` (
  `id` BIGINT NOT NULL COMMENT 'Primary key',
  `engine_code` VARCHAR(64) NOT NULL COMMENT 'Stable engine code for admin management',
  `engine_name` VARCHAR(64) NOT NULL COMMENT 'Displayed engine name',
  `provider_type` VARCHAR(32) NOT NULL COMMENT 'AI provider type such as openai/doubao/mock',
  `business_type` VARCHAR(32) NOT NULL COMMENT 'Business type: interview/resume',
  `model_name` VARCHAR(128) NOT NULL COMMENT 'Model name used by current config',
  `base_url` VARCHAR(255) NOT NULL COMMENT 'Base URL for provider API',
  `api_key` VARCHAR(255) NOT NULL COMMENT 'Provider API key',
  `temperature` DECIMAL(4,2) NOT NULL DEFAULT 0.70 COMMENT 'Model temperature',
  `max_tokens` INT NOT NULL DEFAULT 4096 COMMENT 'Maximum tokens',
  `timeout_ms` INT NOT NULL DEFAULT 30000 COMMENT 'Request timeout in milliseconds',
  `is_active` TINYINT NOT NULL DEFAULT 0 COMMENT '1-enabled, 0-disabled',
  `sort` INT NOT NULL DEFAULT 0 COMMENT 'Sort order',
  `remark` VARCHAR(255) NULL DEFAULT NULL COMMENT 'Admin remark',
  `create_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT 'Create time',
  `update_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT 'Update time',
  `is_deleted` TINYINT NOT NULL DEFAULT 0 COMMENT 'Logical delete flag',
  PRIMARY KEY (`id`),
  UNIQUE INDEX `uk_sys_ai_engine_config_engine_code` (`engine_code`),
  INDEX `idx_sys_ai_engine_config_business_active` (`business_type`, `is_active`, `sort`),
  INDEX `idx_sys_ai_engine_config_provider` (`provider_type`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='Admin configurable AI engine configs';

/*
  作用：
  预置两条测试配置，便于管理端联调时直接验证 interview / resume 两类业务的配置切换。
  使用 ON DUPLICATE KEY UPDATE，保证脚本重复执行时仍可安全更新测试数据。
*/
INSERT INTO `sys_ai_engine_config`
(`id`, `engine_code`, `engine_name`, `provider_type`, `business_type`, `model_name`, `base_url`, `api_key`, `temperature`, `max_tokens`, `timeout_ms`, `is_active`, `sort`, `remark`)
VALUES
  (4001, 'interview_mock_engine', 'Interview Mock Engine', 'mock', 'interview', 'mock-interview-model', 'https://mock.example.com/interview', 'sk-interview-demo-key', 0.70, 4096, 30000, 1, 10, 'Seed config for interview business'),
  (4002, 'resume_mock_engine', 'Resume Mock Engine', 'mock', 'resume', 'mock-resume-model', 'https://mock.example.com/resume', 'sk-resume-demo-key', 0.50, 4096, 30000, 1, 20, 'Seed config for resume business')
ON DUPLICATE KEY UPDATE
  `engine_name` = VALUES(`engine_name`),
  `provider_type` = VALUES(`provider_type`),
  `business_type` = VALUES(`business_type`),
  `model_name` = VALUES(`model_name`),
  `base_url` = VALUES(`base_url`),
  `api_key` = VALUES(`api_key`),
  `temperature` = VALUES(`temperature`),
  `max_tokens` = VALUES(`max_tokens`),
  `timeout_ms` = VALUES(`timeout_ms`),
  `is_active` = VALUES(`is_active`),
  `sort` = VALUES(`sort`),
  `remark` = VALUES(`remark`),
  `update_time` = CURRENT_TIMESTAMP;
