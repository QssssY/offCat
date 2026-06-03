SET NAMES utf8mb4;
SET FOREIGN_KEY_CHECKS = 0;

DROP TABLE IF EXISTS `community_comment`;
DROP TABLE IF EXISTS `community_post_favorite`;
DROP TABLE IF EXISTS `community_post_like`;
DROP TABLE IF EXISTS `community_post`;
DROP TABLE IF EXISTS `user_notification`;
DROP TABLE IF EXISTS `user_feedback`;
DROP TABLE IF EXISTS `user_onboarding_task`;
DROP TABLE IF EXISTS `user_onboarding_state`;
DROP TABLE IF EXISTS `user_settings`;
DROP TABLE IF EXISTS `membership_order`;
DROP TABLE IF EXISTS `membership_plan`;
DROP TABLE IF EXISTS `interview_chat_log`;
DROP TABLE IF EXISTS `mock_interview_job_target_record`;
DROP TABLE IF EXISTS `interview_session`;
DROP TABLE IF EXISTS `resume_polish_record`;
DROP TABLE IF EXISTS `resume_job_match_record`;
DROP TABLE IF EXISTS `resume_diagnosis_task`;
DROP TABLE IF EXISTS `user_rights_change_log`;
DROP TABLE IF EXISTS `sys_ai_engine_config`;
DROP TABLE IF EXISTS `sys_growth_config`;
DROP TABLE IF EXISTS `sys_job_role`;
DROP TABLE IF EXISTS `sys_prompt`;
DROP TABLE IF EXISTS `sys_version_log`;
DROP TABLE IF EXISTS `sys_admin_notification`;
DROP TABLE IF EXISTS `user_ai_usage_detail`;
DROP TABLE IF EXISTS `user_ai_daily_usage`;
DROP TABLE IF EXISTS `user_ai_config`;
DROP TABLE IF EXISTS `sys_config`;
DROP TABLE IF EXISTS `user_quota`;
DROP TABLE IF EXISTS `sys_user`;

CREATE TABLE `sys_user` (
  `id` BIGINT NOT NULL COMMENT 'Primary key',
  `username` VARCHAR(50) NOT NULL COMMENT 'Username',
  `nickname` VARCHAR(50) NULL DEFAULT NULL COMMENT 'User nickname',
  `password` VARCHAR(255) NOT NULL COMMENT 'Encrypted password',
  `role` TINYINT NOT NULL COMMENT '0-normal, 1-vip, 9-admin',
  `status` TINYINT NOT NULL DEFAULT 1 COMMENT '1-active, 0-disabled',
  `membership_plan_code` VARCHAR(32) NULL DEFAULT NULL COMMENT 'Current membership plan code',
  `vip_expire_time` DATETIME NULL DEFAULT NULL COMMENT 'VIP expire time',
  `ban_reason` VARCHAR(255) NULL DEFAULT NULL COMMENT 'Account ban reason',
  `banned_until` DATETIME NULL DEFAULT NULL COMMENT 'Account ban expiry time, null means permanent',
  `banned_by` BIGINT NULL DEFAULT NULL COMMENT 'Admin user id who banned the account',
  `banned_time` DATETIME NULL DEFAULT NULL COMMENT 'Account ban time',
  `security_question` VARCHAR(200) NULL DEFAULT NULL COMMENT 'Security question for password recovery',
  `security_answer` VARCHAR(255) NULL DEFAULT NULL COMMENT 'Security answer (BCrypt encrypted)',
  `create_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT 'Create time',
  `update_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT 'Update time',
  `is_deleted` TINYINT NOT NULL DEFAULT 0 COMMENT 'Logical delete flag',
  PRIMARY KEY (`id`),
  UNIQUE INDEX `uk_sys_user_username` (`username`),
  INDEX `idx_sys_user_role` (`role`),
  INDEX `idx_sys_user_status` (`status`),
  INDEX `idx_sys_user_membership_plan_code` (`membership_plan_code`),
  INDEX `idx_sys_user_vip_expire_time` (`vip_expire_time`),
  INDEX `idx_sys_user_banned_until` (`status`, `banned_until`),
  INDEX `idx_sys_user_create_time` (`create_time`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='User table';

CREATE TABLE `user_quota` (
  `id` BIGINT NOT NULL COMMENT 'Primary key',
  `user_id` BIGINT NOT NULL COMMENT 'User id',
  `total_interview_used` INT NOT NULL DEFAULT 0 COMMENT 'Total interview used',
  `total_resume_used` INT NOT NULL DEFAULT 0 COMMENT 'Total resume used',
  `interview_quota` INT NOT NULL DEFAULT 0 COMMENT 'Remaining interview quota',
  `resume_quota` INT NOT NULL DEFAULT 0 COMMENT 'Remaining resume quota',
  `daily_interview_used` INT NOT NULL DEFAULT 0 COMMENT 'Daily interview used',
  `daily_resume_used` INT NOT NULL DEFAULT 0 COMMENT 'Daily resume used',
  `daily_polish_used` INT NOT NULL DEFAULT 0 COMMENT '今日AI润色使用次数',
  `daily_jd_match_used` INT NOT NULL DEFAULT 0 COMMENT '今日JD匹配使用次数',
  `daily_template_used` INT NOT NULL DEFAULT 0 COMMENT '今日模板使用次数',
  `daily_offer_used` INT NOT NULL DEFAULT 0 COMMENT '今日Offer辅助使用次数',
  `cycle_resume_used` INT NOT NULL DEFAULT 0 COMMENT '周期内简历诊断已用',
  `cycle_interview_used` INT NOT NULL DEFAULT 0 COMMENT '周期内面试已用',
  `cycle_polish_used` INT NOT NULL DEFAULT 0 COMMENT '周期内润色已用',
  `cycle_jd_match_used` INT NOT NULL DEFAULT 0 COMMENT '周期内JD匹配已用',
  `cycle_template_used` INT NOT NULL DEFAULT 0 COMMENT '周期内模板已用',
  `cycle_offer_used` INT NOT NULL DEFAULT 0 COMMENT '周期内Offer已用',
  `cycle_start_time` DATETIME NULL COMMENT '当前周期开始时间',
  `free_polish_left` INT NOT NULL DEFAULT 1 COMMENT '非会员免费润色剩余',
  `free_jd_match_left` INT NOT NULL DEFAULT 1 COMMENT '非会员免费JD匹配剩余',
  `free_template_left` INT NOT NULL DEFAULT 2 COMMENT '非会员免费模板剩余',
  `free_offer_left` INT NOT NULL DEFAULT 1 COMMENT '非会员免费Offer剩余',
  `last_refresh_date` DATE NOT NULL COMMENT 'Last refresh date',
  `create_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT 'Create time',
  `update_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT 'Update time',
  `is_deleted` TINYINT NOT NULL DEFAULT 0 COMMENT 'Logical delete flag',
  PRIMARY KEY (`id`),
  UNIQUE INDEX `uk_user_quota_user_id` (`user_id`),
  INDEX `idx_user_quota_last_refresh_date` (`last_refresh_date`),
  INDEX `idx_user_quota_create_time` (`create_time`),
  CONSTRAINT `fk_user_quota_user_id` FOREIGN KEY (`user_id`) REFERENCES `sys_user` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='User quota table';

CREATE TABLE `user_ai_config` (
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
  `tts_endpoint_path` VARCHAR(128) DEFAULT '/audio/speech' COMMENT 'TTS 合成端点路径，由发现接口自动探测',
  `tts_provider` VARCHAR(32) DEFAULT NULL COMMENT 'TTS 提供商标识：openai/mimo，NULL 或空按 OpenAI 兜底',
  `create_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `update_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  `is_deleted` TINYINT NOT NULL DEFAULT 0,
  PRIMARY KEY (`id`),
  UNIQUE INDEX `uk_user_config_type` (`user_id`, `config_type`, `is_deleted`),
  INDEX `idx_user_ai_config_user_enabled` (`user_id`, `is_enabled`, `config_type`),
  CONSTRAINT `fk_user_ai_config_user_id` FOREIGN KEY (`user_id`) REFERENCES `sys_user` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='用户自定义AI配置';

CREATE TABLE `user_ai_daily_usage` (
  `id` BIGINT NOT NULL COMMENT '雪花ID',
  `user_id` BIGINT NOT NULL COMMENT '用户ID',
  `usage_date` DATE NOT NULL COMMENT '使用日期',
  `call_count` INT NOT NULL DEFAULT 0 COMMENT '当日已调用次数',
  `create_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `update_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  `is_deleted` TINYINT NOT NULL DEFAULT 0,
  PRIMARY KEY (`id`),
  UNIQUE INDEX `uk_user_date` (`user_id`, `usage_date`),
  INDEX `idx_user_ai_usage_date` (`usage_date`),
  CONSTRAINT `fk_user_ai_daily_usage_user_id` FOREIGN KEY (`user_id`) REFERENCES `sys_user` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='用户自定义AI每日使用量';

CREATE TABLE `user_ai_usage_detail` (
  `id` BIGINT NOT NULL COMMENT '雪花ID',
  `user_id` BIGINT NOT NULL COMMENT '用户ID',
  `usage_date` DATE NOT NULL COMMENT '使用日期',
  `usage_type` VARCHAR(64) NOT NULL COMMENT '功能统计类型',
  `call_count` INT NOT NULL DEFAULT 0 COMMENT '当日该功能已调用次数',
  `create_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `update_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  `is_deleted` TINYINT NOT NULL DEFAULT 0,
  PRIMARY KEY (`id`),
  UNIQUE INDEX `uk_user_ai_usage_detail_user_date_type` (`user_id`, `usage_date`, `usage_type`),
  INDEX `idx_user_ai_usage_detail_date_type` (`usage_date`, `usage_type`),
  INDEX `idx_user_ai_usage_detail_user_date` (`user_id`, `usage_date`),
  CONSTRAINT `fk_user_ai_usage_detail_user_id` FOREIGN KEY (`user_id`) REFERENCES `sys_user` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='用户自定义AI按功能每日使用明细';

CREATE TABLE `sys_config` (
  `id` BIGINT NOT NULL COMMENT '雪花ID',
  `config_key` VARCHAR(128) NOT NULL COMMENT '配置键',
  `config_value` VARCHAR(512) NOT NULL COMMENT '配置值',
  `description` VARCHAR(256) NULL COMMENT '配置说明',
  `create_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `update_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  `is_deleted` TINYINT NOT NULL DEFAULT 0,
  PRIMARY KEY (`id`),
  UNIQUE INDEX `uk_config_key` (`config_key`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='系统配置';

CREATE TABLE `user_settings` (
  `id` BIGINT NOT NULL COMMENT 'Primary key',
  `user_id` BIGINT NOT NULL COMMENT 'User id',
  `interview_retention_days` INT NOT NULL DEFAULT 0 COMMENT 'Interview history retention days, 0 means disabled',
  `resume_retention_days` INT NOT NULL DEFAULT 0 COMMENT 'Resume diagnosis retention days, 0 means disabled',
  `create_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT 'Create time',
  `update_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT 'Update time',
  `is_deleted` TINYINT NOT NULL DEFAULT 0 COMMENT 'Logical delete flag',
  PRIMARY KEY (`id`),
  UNIQUE INDEX `uk_user_settings_user_id` (`user_id`),
  INDEX `idx_user_settings_interview_retention` (`interview_retention_days`, `is_deleted`),
  INDEX `idx_user_settings_resume_retention` (`resume_retention_days`, `is_deleted`),
  CONSTRAINT `fk_user_settings_user_id` FOREIGN KEY (`user_id`) REFERENCES `sys_user` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='User settings table';

CREATE TABLE `membership_plan` (
  `id` BIGINT NOT NULL COMMENT 'Primary key',
  `plan_code` VARCHAR(32) NOT NULL COMMENT 'Plan code',
  `plan_name` VARCHAR(64) NOT NULL COMMENT 'Plan name',
  `description` VARCHAR(255) NULL DEFAULT NULL COMMENT 'Plan description',
  `price_amount` DECIMAL(10,2) NOT NULL COMMENT 'Price amount',
  `duration_days` INT NOT NULL COMMENT 'Duration days',
  `resume_quota` INT NOT NULL DEFAULT 0 COMMENT 'Granted resume quota',
  `interview_quota` INT NOT NULL DEFAULT 0 COMMENT 'Granted interview quota',
  `daily_polish_limit` INT NOT NULL DEFAULT 1 COMMENT '每日AI润色次数',
  `daily_jd_match_limit` INT NOT NULL DEFAULT 3 COMMENT '每日JD匹配次数',
  `daily_template_limit` INT NOT NULL DEFAULT 5 COMMENT '每日模板使用次数',
  `daily_offer_limit` INT NOT NULL DEFAULT 3 COMMENT '每日Offer辅助次数',
  `total_resume_quota` INT NOT NULL DEFAULT 0 COMMENT '套餐周期内简历诊断总额度（0=不限）',
  `total_interview_quota` INT NOT NULL DEFAULT 0 COMMENT '套餐周期内面试总额度（0=不限）',
  `total_polish_quota` INT NOT NULL DEFAULT 0 COMMENT '套餐周期内AI润色总额度（0=不限）',
  `total_jd_match_quota` INT NOT NULL DEFAULT 0 COMMENT '套餐周期内JD匹配总额度（0=不限）',
  `total_template_quota` INT NOT NULL DEFAULT 0 COMMENT '套餐周期内模板总额度（0=不限）',
  `total_offer_quota` INT NOT NULL DEFAULT 0 COMMENT '套餐周期内Offer总额度（0=不限）',
  `bonus_resume_quota` INT NOT NULL DEFAULT 0 COMMENT '购买赠送简历诊断额度',
  `bonus_interview_quota` INT NOT NULL DEFAULT 0 COMMENT '购买赠送面试额度',
  `benefits` JSON NULL COMMENT '套餐权益描述列表（前端展示）',
  `status` TINYINT NOT NULL DEFAULT 1 COMMENT '1-enabled, 0-disabled',
  `sort` INT NOT NULL DEFAULT 0 COMMENT 'Sort order',
  `create_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT 'Create time',
  `update_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT 'Update time',
  `is_deleted` TINYINT NOT NULL DEFAULT 0 COMMENT 'Logical delete flag',
  PRIMARY KEY (`id`),
  UNIQUE INDEX `uk_membership_plan_code` (`plan_code`),
  INDEX `idx_membership_plan_status_sort` (`status`, `sort`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='Membership plan table';

CREATE TABLE `membership_order` (
  `id` BIGINT NOT NULL COMMENT 'Primary key',
  `order_no` VARCHAR(64) NOT NULL COMMENT 'Order no',
  `user_id` BIGINT NOT NULL COMMENT 'User id',
  `plan_id` BIGINT NOT NULL COMMENT 'Plan id',
  `plan_code` VARCHAR(32) NOT NULL COMMENT 'Plan code snapshot',
  `plan_name` VARCHAR(64) NOT NULL COMMENT 'Plan name snapshot',
  `order_status` VARCHAR(20) NOT NULL COMMENT 'CREATED/PAID',
  `pay_channel` VARCHAR(20) NOT NULL COMMENT 'MOCK',
  `order_amount` DECIMAL(10,2) NOT NULL COMMENT 'Order amount',
  `duration_days` INT NOT NULL COMMENT 'Duration days',
  `granted_resume_quota` INT NOT NULL DEFAULT 0 COMMENT 'Granted resume quota',
  `granted_interview_quota` INT NOT NULL DEFAULT 0 COMMENT 'Granted interview quota',
  `expire_time_before` DATETIME NULL DEFAULT NULL COMMENT 'Expire time before upgrade',
  `expire_time_after` DATETIME NOT NULL COMMENT 'Expire time after upgrade',
  `paid_at` DATETIME NOT NULL COMMENT 'Paid time',
  `create_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT 'Create time',
  `update_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT 'Update time',
  `is_deleted` TINYINT NOT NULL DEFAULT 0 COMMENT 'Logical delete flag',
  PRIMARY KEY (`id`),
  UNIQUE INDEX `uk_membership_order_no` (`order_no`),
  INDEX `idx_membership_order_user_id` (`user_id`),
  INDEX `idx_membership_order_plan_id` (`plan_id`),
  INDEX `idx_membership_order_status` (`order_status`),
  INDEX `idx_membership_order_create_time` (`create_time`),
  CONSTRAINT `fk_membership_order_user_id` FOREIGN KEY (`user_id`) REFERENCES `sys_user` (`id`),
  CONSTRAINT `fk_membership_order_plan_id` FOREIGN KEY (`plan_id`) REFERENCES `membership_plan` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='Membership order table';

CREATE TABLE `sys_prompt` (
  `id` BIGINT NOT NULL COMMENT 'Primary key',
  `scenario_type` TINYINT NOT NULL COMMENT '1-interview, 2-resume',
  `job_role_code` VARCHAR(64) NULL DEFAULT NULL COMMENT 'Configured job role code',
  `job_role` VARCHAR(50) NOT NULL COMMENT 'Job role',
  `difficulty` TINYINT NOT NULL DEFAULT 1 COMMENT '1-primary, 2-intermediate, 3-advanced',
  `prompt_content` TEXT NOT NULL COMMENT 'Prompt content',
  `is_active` TINYINT NOT NULL DEFAULT 1 COMMENT '1-active, 0-inactive',
  `create_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT 'Create time',
  `update_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT 'Update time',
  `is_deleted` TINYINT NOT NULL DEFAULT 0 COMMENT 'Logical delete flag',
  PRIMARY KEY (`id`),
  INDEX `idx_sys_prompt_scenario_type` (`scenario_type`),
  INDEX `idx_sys_prompt_job_role_code` (`job_role_code`),
  INDEX `idx_sys_prompt_job_role` (`job_role`),
  INDEX `idx_sys_prompt_difficulty` (`difficulty`),
  INDEX `idx_sys_prompt_is_active` (`is_active`),
  INDEX `idx_sys_prompt_query` (`scenario_type`, `job_role`, `difficulty`, `is_active`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='Prompt table';

CREATE TABLE `sys_job_role` (
  `id` BIGINT NOT NULL COMMENT 'Primary key',
  `role_code` VARCHAR(64) NOT NULL COMMENT 'Stable role code for admin management',
  `role_name` VARCHAR(64) NOT NULL COMMENT 'Displayed interview job role name',
  `interview_tag` VARCHAR(32) NULL DEFAULT NULL COMMENT 'Optional tag shown in interview selector',
  `tag_type` VARCHAR(32) NULL DEFAULT NULL COMMENT 'Tag type used by frontend style mapping',
  `is_active` TINYINT NOT NULL DEFAULT 1 COMMENT '1-enabled, 0-disabled',
  `sort` INT NOT NULL DEFAULT 0 COMMENT 'Sort order',
  `create_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT 'Create time',
  `update_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT 'Update time',
  `is_deleted` TINYINT NOT NULL DEFAULT 0 COMMENT 'Logical delete flag',
  PRIMARY KEY (`id`),
  UNIQUE INDEX `uk_sys_job_role_code` (`role_code`),
  UNIQUE INDEX `uk_sys_job_role_name` (`role_name`),
  INDEX `idx_sys_job_role_active_sort` (`is_active`, `sort`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='Admin configurable interview job roles';

CREATE TABLE `sys_ai_engine_config` (
  `id` BIGINT NOT NULL COMMENT 'Primary key',
  `engine_code` VARCHAR(64) NOT NULL COMMENT 'Stable engine code for admin management',
  `engine_name` VARCHAR(64) NOT NULL COMMENT 'Displayed engine name',
  `provider_type` VARCHAR(32) NOT NULL COMMENT 'AI provider type such as openai/doubao/mock',
  `business_type` VARCHAR(32) NOT NULL COMMENT 'Business type: interview/resume',
  `model_name` VARCHAR(128) NOT NULL COMMENT 'Model name used by current config',
  `base_url` VARCHAR(255) NOT NULL COMMENT 'Base URL for provider API',
  `api_key` VARCHAR(255) NOT NULL COMMENT 'Provider API key',
  `supports_multimodal` TINYINT NOT NULL DEFAULT 0 COMMENT '1-supported, 0-unsupported',
  `thinking_mode` VARCHAR(16) NOT NULL DEFAULT 'none' COMMENT 'Thinking mode: enabled/disabled/none',
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

CREATE TABLE `user_rights_change_log` (
  `id` BIGINT NOT NULL COMMENT 'Primary key',
  `user_id` BIGINT NOT NULL COMMENT 'Target user id',
  `operator_user_id` BIGINT NOT NULL COMMENT 'Admin operator user id',
  `before_role` TINYINT NULL DEFAULT NULL COMMENT 'Role before change',
  `after_role` TINYINT NULL DEFAULT NULL COMMENT 'Role after change',
  `before_membership_plan_code` VARCHAR(32) NULL DEFAULT NULL COMMENT 'Membership plan code before change',
  `after_membership_plan_code` VARCHAR(32) NULL DEFAULT NULL COMMENT 'Membership plan code after change',
  `before_vip_expire_time` DATETIME NULL DEFAULT NULL COMMENT 'VIP expire time before change',
  `after_vip_expire_time` DATETIME NULL DEFAULT NULL COMMENT 'VIP expire time after change',
  `remark` VARCHAR(255) NULL DEFAULT NULL COMMENT 'Admin change remark',
  `create_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT 'Create time',
  `update_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT 'Update time',
  `is_deleted` TINYINT NOT NULL DEFAULT 0 COMMENT 'Logical delete flag',
  PRIMARY KEY (`id`),
  INDEX `idx_user_rights_change_log_user_id` (`user_id`),
  INDEX `idx_user_rights_change_log_operator_user_id` (`operator_user_id`),
  INDEX `idx_user_rights_change_log_create_time` (`create_time`),
  CONSTRAINT `fk_user_rights_change_log_user_id` FOREIGN KEY (`user_id`) REFERENCES `sys_user` (`id`),
  CONSTRAINT `fk_user_rights_change_log_operator_user_id` FOREIGN KEY (`operator_user_id`) REFERENCES `sys_user` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='Admin user rights change log';

CREATE TABLE `resume_diagnosis_task` (
  `id` BIGINT NOT NULL COMMENT 'Primary key',
  `user_id` BIGINT NOT NULL COMMENT 'User id',
  `file_url` VARCHAR(255) NOT NULL COMMENT 'Uploaded file url',
  `status` TINYINT NOT NULL DEFAULT 0 COMMENT '0-pending, 1-processing, 2-completed, 3-failed',
  `stage` VARCHAR(32) NULL DEFAULT NULL COMMENT '诊断子阶段: extracting/ai_analyzing/enhancing',
  `diagnosis_result` JSON NULL COMMENT 'Diagnosis result',
  `error_msg` VARCHAR(255) NULL DEFAULT NULL COMMENT 'Error message',
  `failed_at` DATETIME NULL DEFAULT NULL COMMENT '任务进入失败状态的时间，用于24小时重试窗口',
  `resume_text` MEDIUMTEXT NULL COMMENT '简历提取的文本内容，用于缓存PDF解析结果',
  `parse_mode` VARCHAR(16) NULL DEFAULT NULL COMMENT 'Resume parse mode: TEXT/MULTIMODAL/OCR/MIXED',
  `parse_message` VARCHAR(255) NULL DEFAULT NULL COMMENT 'Resume parse hint message',
  `ai_billing_source` VARCHAR(32) NOT NULL DEFAULT 'platform' COMMENT 'AI计费来源: platform/user_custom',
  `fallback_to_platform` TINYINT NOT NULL DEFAULT 0 COMMENT '是否显式回退平台AI',
  `create_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT 'Create time',
  `update_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT 'Update time',
  `is_deleted` TINYINT NOT NULL DEFAULT 0 COMMENT 'Logical delete flag',
  PRIMARY KEY (`id`),
  INDEX `idx_resume_task_user_id` (`user_id`),
  INDEX `idx_resume_task_status` (`status`),
  INDEX `idx_resume_task_create_time` (`create_time`),
  INDEX `idx_resume_task_user_status` (`user_id`, `status`),
  INDEX `idx_resume_task_retention_cleanup` (`user_id`, `status`, `is_deleted`, `create_time`),
  INDEX `idx_resume_task_status_failed_at` (`status`, `failed_at`),
  INDEX `idx_resume_task_user_status_time` (`user_id`, `status`, `create_time`),
  CONSTRAINT `fk_resume_task_user_id` FOREIGN KEY (`user_id`) REFERENCES `sys_user` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='Resume diagnosis task table';

CREATE TABLE `resume_job_match_record` (
  `id` BIGINT NOT NULL COMMENT 'Primary key',
  `user_id` BIGINT NOT NULL COMMENT 'User id',
  `resume_task_id` BIGINT NOT NULL COMMENT 'Resume diagnosis task id',
  `resume_text` MEDIUMTEXT NOT NULL COMMENT 'Resume text snapshot',
  `jd_text` MEDIUMTEXT NOT NULL COMMENT 'Job description text snapshot',
  `match_score` INT NOT NULL DEFAULT 0 COMMENT 'Match score',
  `analysis_result` JSON NULL COMMENT 'Structured analysis result',
  `create_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT 'Create time',
  `update_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT 'Update time',
  `is_deleted` TINYINT NOT NULL DEFAULT 0 COMMENT 'Logical delete flag',
  PRIMARY KEY (`id`),
  INDEX `idx_resume_job_match_user_id` (`user_id`),
  INDEX `idx_resume_job_match_resume_task_id` (`resume_task_id`),
  INDEX `idx_resume_job_match_create_time` (`create_time`),
  CONSTRAINT `fk_resume_job_match_user_id` FOREIGN KEY (`user_id`) REFERENCES `sys_user` (`id`),
  CONSTRAINT `fk_resume_job_match_resume_task_id` FOREIGN KEY (`resume_task_id`) REFERENCES `resume_diagnosis_task` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='Resume job match analysis record table';

CREATE TABLE `resume_polish_record` (
  `id` BIGINT NOT NULL COMMENT 'Primary key',
  `user_id` BIGINT NOT NULL COMMENT 'User id',
  `resume_task_id` BIGINT NOT NULL COMMENT 'Resume diagnosis task id',
  `source_resume_text` MEDIUMTEXT NOT NULL COMMENT 'Source resume text snapshot',
  `jd_text` MEDIUMTEXT NULL COMMENT 'Job description text snapshot',
  `polished_resume_text` MEDIUMTEXT NOT NULL COMMENT 'Polished resume text',
  `document_json` LONGTEXT NULL DEFAULT NULL COMMENT '编辑后的简历文档JSON',
  `edited_plain_text` TEXT NULL DEFAULT NULL COMMENT '编辑后的简历纯文本',
  `modification_notes` JSON NULL COMMENT 'Modification notes',
  `source_type` VARCHAR(32) NOT NULL COMMENT 'Polish source type',
  `create_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT 'Create time',
  `update_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT 'Update time',
  `is_deleted` TINYINT NOT NULL DEFAULT 0 COMMENT 'Logical delete flag',
  PRIMARY KEY (`id`),
  INDEX `idx_resume_polish_user_id` (`user_id`),
  INDEX `idx_resume_polish_resume_task_id` (`resume_task_id`),
  INDEX `idx_resume_polish_create_time` (`create_time`),
  CONSTRAINT `fk_resume_polish_user_id` FOREIGN KEY (`user_id`) REFERENCES `sys_user` (`id`),
  CONSTRAINT `fk_resume_polish_resume_task_id` FOREIGN KEY (`resume_task_id`) REFERENCES `resume_diagnosis_task` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='Resume polish record table';

CREATE TABLE `interview_session` (
  `id` BIGINT NOT NULL COMMENT 'Primary key',
  `session_id` VARCHAR(64) NOT NULL COMMENT 'Session id',
  `user_id` BIGINT NOT NULL COMMENT 'User id',
  `job_role` VARCHAR(50) NOT NULL COMMENT 'Job role',
  `job_role_code` VARCHAR(64) NULL COMMENT 'Job role code (for prompt linking)',
  `difficulty` TINYINT NOT NULL COMMENT 'Difficulty',
  `interview_mode` VARCHAR(20) NOT NULL DEFAULT 'normal' COMMENT 'normal/stress/job_targeted/big_company_hr/tech_leader/foreign_interviewer',
  `status` TINYINT NOT NULL DEFAULT 0 COMMENT '0-running, 1-finished',
  `comprehensive_score` INT NULL DEFAULT NULL COMMENT 'Score',
  `evaluation_report` JSON NULL COMMENT 'Evaluation report',
  `opening_generated` INT(1) NOT NULL DEFAULT 0 COMMENT 'Opening generated: 0-no, 1-yes',
  `feedback_mode` VARCHAR(20) DEFAULT NULL COMMENT 'Feedback mode: immediate-每题反馈, after_interview-面完复盘, NULL-默认面完复盘',
  `interaction_type` TINYINT NOT NULL DEFAULT 0 COMMENT '交互方式：0-文字面试，1-语音面试',
  `ai_billing_source` VARCHAR(32) NOT NULL DEFAULT 'platform' COMMENT 'AI billing source: platform/user_custom/platform_fallback',
  `create_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT 'Create time',
  `update_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT 'Update time',
  `is_deleted` TINYINT NOT NULL DEFAULT 0 COMMENT 'Logical delete flag',
  PRIMARY KEY (`id`),
  UNIQUE INDEX `uk_interview_session_session_id` (`session_id`),
  INDEX `idx_interview_session_user_id` (`user_id`),
  INDEX `idx_interview_session_status` (`status`),
  INDEX `idx_interview_session_job_role` (`job_role`),
  INDEX `idx_interview_session_job_role_code` (`job_role_code`),
  INDEX `idx_interview_session_interview_mode` (`interview_mode`),
  INDEX `idx_interview_session_create_time` (`create_time`),
  INDEX `idx_interview_session_user_status` (`user_id`, `status`),
  INDEX `idx_interview_session_retention_cleanup` (`user_id`, `status`, `is_deleted`, `create_time`),
  INDEX `idx_interview_session_user_status_time` (`user_id`, `status`, `create_time`),
  CONSTRAINT `fk_interview_session_user_id` FOREIGN KEY (`user_id`) REFERENCES `sys_user` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='Interview session table';

CREATE TABLE `mock_interview_job_target_record` (
  `id` BIGINT NOT NULL COMMENT 'Primary key',
  `user_id` BIGINT NOT NULL COMMENT 'User id',
  `session_id` VARCHAR(64) NOT NULL COMMENT 'Interview session id',
  `resume_task_id` BIGINT NULL DEFAULT NULL COMMENT 'Related resume diagnosis task id',
  `jd_text` MEDIUMTEXT NOT NULL COMMENT 'Job description text snapshot',
  `job_match_record_id` BIGINT NULL DEFAULT NULL COMMENT 'Related resume job match record id',
  `generated_questions` MEDIUMTEXT NULL COMMENT 'Generated opening question snapshot',
  `job_targeted_feedback` JSON NULL COMMENT 'Structured job targeted feedback',
  `source_type` VARCHAR(32) NOT NULL COMMENT 'Context source type',
  `create_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT 'Create time',
  `update_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT 'Update time',
  `is_deleted` TINYINT NOT NULL DEFAULT 0 COMMENT 'Logical delete flag',
  PRIMARY KEY (`id`),
  INDEX `idx_mock_interview_job_target_user_id` (`user_id`),
  INDEX `idx_mock_interview_job_target_session_id` (`session_id`),
  INDEX `idx_mock_interview_job_target_resume_task_id` (`resume_task_id`),
  INDEX `idx_mock_interview_job_target_job_match_record_id` (`job_match_record_id`),
  INDEX `idx_mock_interview_job_target_create_time` (`create_time`),
  CONSTRAINT `fk_mock_interview_job_target_user_id` FOREIGN KEY (`user_id`) REFERENCES `sys_user` (`id`),
  CONSTRAINT `fk_mock_interview_job_target_session_id` FOREIGN KEY (`session_id`) REFERENCES `interview_session` (`session_id`),
  CONSTRAINT `fk_mock_interview_job_target_resume_task_id` FOREIGN KEY (`resume_task_id`) REFERENCES `resume_diagnosis_task` (`id`),
  CONSTRAINT `fk_mock_interview_job_target_job_match_record_id` FOREIGN KEY (`job_match_record_id`) REFERENCES `resume_job_match_record` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='Job targeted mock interview record table';

CREATE TABLE `interview_chat_log` (
  `id` BIGINT NOT NULL COMMENT 'Primary key',
  `session_id` VARCHAR(64) NOT NULL COMMENT 'Session id',
  `message_role` VARCHAR(20) NOT NULL COMMENT 'user/assistant/system',
  `content` TEXT NOT NULL COMMENT 'Message content',
  `create_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT 'Create time',
  `update_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT 'Update time',
  `is_deleted` TINYINT NOT NULL DEFAULT 0 COMMENT 'Logical delete flag',
  PRIMARY KEY (`id`),
  INDEX `idx_chat_log_session_id` (`session_id`),
  INDEX `idx_chat_log_message_role` (`message_role`),
  INDEX `idx_chat_log_create_time` (`create_time`),
  INDEX `idx_chat_log_session_create_time` (`session_id`, `create_time`),
  CONSTRAINT `fk_chat_log_session_id` FOREIGN KEY (`session_id`) REFERENCES `interview_session` (`session_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='Interview chat log table';

CREATE TABLE `user_onboarding_state` (
  `id` BIGINT NOT NULL COMMENT '主键',
  `user_id` BIGINT NOT NULL COMMENT '用户ID',
  `guide_key` VARCHAR(64) NOT NULL COMMENT '引导版本标识，如 v1_2_main_onboarding',
  `status` VARCHAR(20) NOT NULL DEFAULT 'not_started' COMMENT '状态：not_started/in_progress/completed/skipped',
  `current_step` INT NOT NULL DEFAULT 0 COMMENT '当前步骤索引（从0开始）',
  `completed_time` DATETIME NULL DEFAULT NULL COMMENT '完成时间',
  `skip_time` DATETIME NULL DEFAULT NULL COMMENT '跳过时间',
  `create_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `is_deleted` TINYINT NOT NULL DEFAULT 0 COMMENT '逻辑删除标志',
  PRIMARY KEY (`id`),
  UNIQUE INDEX `uk_onboarding_user_guide` (`user_id`, `guide_key`),
  INDEX `idx_onboarding_user_id` (`user_id`),
  INDEX `idx_onboarding_status` (`status`),
  CONSTRAINT `fk_onboarding_user_id` FOREIGN KEY (`user_id`) REFERENCES `sys_user` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='用户新手引导状态表';

CREATE TABLE `user_onboarding_task` (
  `id`             BIGINT       NOT NULL COMMENT '主键',
  `user_id`        BIGINT       NOT NULL COMMENT '用户ID',
  `task_key`       VARCHAR(32)  NOT NULL COMMENT '任务标识: resume_uploaded/report_viewed/jd_compared/interview_completed',
  `completed`      TINYINT      NOT NULL DEFAULT 0 COMMENT '是否完成 0-否 1-是',
  `completed_time` DATETIME     NULL     COMMENT '完成时间',
  `create_time`    DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time`    DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `is_deleted`     TINYINT      NOT NULL DEFAULT 0 COMMENT '逻辑删除标志',
  PRIMARY KEY (`id`),
  UNIQUE INDEX `uk_user_task` (`user_id`, `task_key`),
  INDEX `idx_task_user` (`user_id`, `is_deleted`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='用户新手任务表';

CREATE TABLE `user_notification` (
  `id` BIGINT NOT NULL COMMENT '主键',
  `user_id` BIGINT NOT NULL COMMENT '所属用户ID',
  `type` VARCHAR(32) NOT NULL COMMENT '通知类型: resume/polish/interview/quota/system',
  `title` VARCHAR(200) NOT NULL COMMENT '通知标题',
  `content` TEXT COMMENT '通知内容',
  `biz_type` VARCHAR(64) COMMENT '关联业务类型: resume_diagnosis/resume_polish/mock_interview/quota',
  `biz_id` VARCHAR(64) COMMENT '关联业务ID',
  `broadcast_id` BIGINT NULL DEFAULT NULL COMMENT '关联系统公告ID',
  `read_status` TINYINT NOT NULL DEFAULT 0 COMMENT '已读状态: 0未读 1已读',
  `read_time` DATETIME DEFAULT NULL COMMENT '已读时间',
  `create_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `is_deleted` TINYINT NOT NULL DEFAULT 0 COMMENT '逻辑删除标志',
  PRIMARY KEY (`id`),
  INDEX `idx_notification_user_id` (`user_id`),
  INDEX `idx_notification_user_read` (`user_id`, `read_status`),
  INDEX `idx_notification_user_read_time` (`user_id`, `read_status`, `create_time`),
  INDEX `idx_notification_user_type` (`user_id`, `type`),
  INDEX `idx_notification_broadcast_id` (`broadcast_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='用户站内通知表';

-- ========================================
-- 管理端功能扩展表（V3.0）
-- ========================================

CREATE TABLE `user_feedback` (
  `id` BIGINT NOT NULL COMMENT '主键',
  `user_id` BIGINT NOT NULL COMMENT '提交用户ID',
  `type` VARCHAR(32) NOT NULL COMMENT '反馈类型: bug/suggestion/experience/other',
  `title` VARCHAR(100) NOT NULL COMMENT '反馈标题',
  `content` TEXT NOT NULL COMMENT '反馈内容',
  `contact` VARCHAR(100) NULL DEFAULT NULL COMMENT '用户联系方式',
  `status` TINYINT NOT NULL DEFAULT 0 COMMENT '处理状态: 0待处理 1处理中 2已处理 3已关闭',
  `admin_remark` VARCHAR(1000) NULL DEFAULT NULL COMMENT '管理端处理备注',
  `handled_by` BIGINT NULL DEFAULT NULL COMMENT '处理管理员ID',
  `handled_at` DATETIME NULL DEFAULT NULL COMMENT '处理时间',
  `create_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `is_deleted` TINYINT NOT NULL DEFAULT 0 COMMENT '逻辑删除标志',
  PRIMARY KEY (`id`),
  INDEX `idx_feedback_user_id` (`user_id`),
  INDEX `idx_feedback_type` (`type`),
  INDEX `idx_feedback_status` (`status`),
  INDEX `idx_feedback_create_time` (`create_time`),
  CONSTRAINT `fk_feedback_user_id` FOREIGN KEY (`user_id`) REFERENCES `sys_user` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='用户问题反馈与建议表';

CREATE TABLE `sys_admin_notification` (
  `id` BIGINT NOT NULL COMMENT '主键',
  `title` VARCHAR(200) NOT NULL COMMENT '公告标题',
  `content` TEXT NOT NULL COMMENT '公告内容',
  `type` VARCHAR(32) NOT NULL DEFAULT 'system' COMMENT '公告类型: system/activity/update/maintenance',
  `target_type` VARCHAR(32) NOT NULL DEFAULT 'all' COMMENT '目标用户: all/vip/normal',
  `status` TINYINT NOT NULL DEFAULT 0 COMMENT '0-draft, 1-published',
  `published_at` DATETIME NULL DEFAULT NULL COMMENT '发布时间',
  `create_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `is_deleted` TINYINT NOT NULL DEFAULT 0 COMMENT '逻辑删除标志',
  PRIMARY KEY (`id`),
  INDEX `idx_admin_notification_status` (`status`),
  INDEX `idx_admin_notification_type` (`type`),
  INDEX `idx_admin_notification_create_time` (`create_time`),
  INDEX `idx_admin_notification_filter_time` (`target_type`, `status`, `type`, `create_time`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='系统公告表';

CREATE TABLE `sys_version_log` (
  `id` BIGINT NOT NULL COMMENT '主键',
  `version` VARCHAR(32) NOT NULL COMMENT '版本号',
  `title` VARCHAR(200) NOT NULL COMMENT '版本标题',
  `content` TEXT NOT NULL COMMENT '更新内容（Markdown格式）',
  `type` VARCHAR(16) NOT NULL DEFAULT 'minor' COMMENT '版本类型: major/minor/patch',
  `status` TINYINT NOT NULL DEFAULT 0 COMMENT '0-draft, 1-published',
  `published_at` DATETIME NULL DEFAULT NULL COMMENT '发布时间',
  `create_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `is_deleted` TINYINT NOT NULL DEFAULT 0 COMMENT '逻辑删除标志',
  PRIMARY KEY (`id`),
  UNIQUE INDEX `uk_version_log_version` (`version`),
  INDEX `idx_version_log_status` (`status`),
  INDEX `idx_version_log_filter_time` (`status`, `type`, `create_time`),
  INDEX `idx_version_log_published_at` (`published_at`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='版本更新日志表';

CREATE TABLE `sys_growth_config` (
  `id` BIGINT NOT NULL COMMENT '主键',
  `config_key` VARCHAR(64) NOT NULL COMMENT '配置键',
  `config_value` TEXT NOT NULL COMMENT '配置值',
  `description` VARCHAR(255) NULL DEFAULT NULL COMMENT '配置说明',
  `group_name` VARCHAR(64) NOT NULL DEFAULT 'default' COMMENT '配置分组',
  `sort` INT NOT NULL DEFAULT 0 COMMENT '排序',
  `create_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `is_deleted` TINYINT NOT NULL DEFAULT 0 COMMENT '逻辑删除标志',
  PRIMARY KEY (`id`),
  UNIQUE INDEX `uk_growth_config_key` (`config_key`),
  INDEX `idx_growth_config_group` (`group_name`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='成长中心配置表';

-- ============================================================
-- 社区模块表
-- ============================================================

CREATE TABLE `community_post` (
  `id` BIGINT NOT NULL COMMENT '主键',
  `user_id` BIGINT NOT NULL COMMENT '发布者用户ID',
  `category` VARCHAR(32) NOT NULL COMMENT '帖子板块：interview_exp-面试经验分享，referral-内推广场',
  `title` VARCHAR(120) NOT NULL DEFAULT '未命名帖子' COMMENT '帖子标题',
  `content` TEXT NOT NULL COMMENT '帖子内容',
  `shared_interview_session_id` VARCHAR(64) NULL COMMENT '分享的面试报告会话ID',
  `images` JSON NULL COMMENT '图片URL列表JSON数组',
  `like_count` INT NOT NULL DEFAULT 0 COMMENT '点赞数',
  `comment_count` INT NOT NULL DEFAULT 0 COMMENT '评论数',
  `review_status` VARCHAR(16) NOT NULL DEFAULT 'approved' COMMENT '审核状态：pending-待审，approved-通过，rejected-拒绝，hidden-隐藏',
  `review_reason` VARCHAR(255) NULL COMMENT '审核原因',
  `reviewed_by` BIGINT NULL COMMENT '审核管理员用户ID',
  `reviewed_time` DATETIME NULL COMMENT '审核时间',
  `create_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `is_deleted` TINYINT NOT NULL DEFAULT 0 COMMENT '逻辑删除标志',
  PRIMARY KEY (`id`),
  INDEX `idx_community_post_user_id` (`user_id`),
  INDEX `idx_community_post_category` (`category`),
  INDEX `idx_community_post_shared_interview_session_id` (`shared_interview_session_id`),
  INDEX `idx_community_post_create_time` (`create_time`),
  INDEX `idx_community_post_category_time` (`category`, `create_time`),
  INDEX `idx_community_post_deleted_category_time` (`is_deleted`, `category`, `create_time`),
  INDEX `idx_community_post_review_time` (`review_status`, `create_time`),
  INDEX `idx_community_post_public_review_time` (`is_deleted`, `review_status`, `category`, `create_time`),
  INDEX `idx_community_post_category_like` (`category`, `like_count`),
  CONSTRAINT `fk_community_post_user_id` FOREIGN KEY (`user_id`) REFERENCES `sys_user` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='社区帖子表';

CREATE TABLE `community_comment` (
  `id` BIGINT NOT NULL COMMENT '主键',
  `post_id` BIGINT NOT NULL COMMENT '所属帖子ID',
  `user_id` BIGINT NOT NULL COMMENT '评论者用户ID',
  `parent_comment_id` BIGINT NULL DEFAULT NULL COMMENT '父评论ID，NULL表示顶级评论',
  `reply_to_user_id` BIGINT NULL DEFAULT NULL COMMENT '被回复用户ID',
  `content` TEXT NOT NULL COMMENT '评论内容',
  `images` JSON NULL COMMENT '评论图片URL列表JSON数组',
  `review_status` VARCHAR(16) NOT NULL DEFAULT 'approved' COMMENT '审核状态：pending-待审，approved-通过，rejected-拒绝，hidden-隐藏',
  `review_reason` VARCHAR(255) NULL COMMENT '审核原因',
  `reviewed_by` BIGINT NULL COMMENT '审核管理员用户ID',
  `reviewed_time` DATETIME NULL COMMENT '审核时间',
  `create_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `is_deleted` TINYINT NOT NULL DEFAULT 0 COMMENT '逻辑删除标志',
  PRIMARY KEY (`id`),
  INDEX `idx_community_comment_post_id` (`post_id`),
  INDEX `idx_community_comment_user_id` (`user_id`),
  INDEX `idx_community_comment_create_time` (`create_time`),
  INDEX `idx_community_comment_post_time` (`post_id`, `create_time`),
  INDEX `idx_community_comment_parent_id` (`parent_comment_id`),
  INDEX `idx_community_comment_parent_time` (`parent_comment_id`, `create_time`),
  INDEX `idx_community_comment_reply_user` (`reply_to_user_id`, `create_time`),
  INDEX `idx_community_comment_reply_user_actor_time` (`reply_to_user_id`, `user_id`, `create_time`),
  INDEX `idx_community_comment_review_time` (`review_status`, `create_time`),
  INDEX `idx_community_comment_post_review_time` (`post_id`, `review_status`, `create_time`),
  CONSTRAINT `fk_community_comment_post_id` FOREIGN KEY (`post_id`) REFERENCES `community_post` (`id`),
  CONSTRAINT `fk_community_comment_user_id` FOREIGN KEY (`user_id`) REFERENCES `sys_user` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='社区评论表';

CREATE TABLE `community_post_like` (
  `id` BIGINT NOT NULL COMMENT '主键',
  `post_id` BIGINT NOT NULL COMMENT '帖子ID',
  `user_id` BIGINT NOT NULL COMMENT '点赞用户ID',
  `create_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  PRIMARY KEY (`id`),
  UNIQUE INDEX `uk_post_like_user` (`post_id`, `user_id`),
  INDEX `idx_community_post_like_user_id` (`user_id`),
  INDEX `idx_community_post_like_create_time` (`create_time`),
  CONSTRAINT `fk_community_post_like_post_id` FOREIGN KEY (`post_id`) REFERENCES `community_post` (`id`),
  CONSTRAINT `fk_community_post_like_user_id` FOREIGN KEY (`user_id`) REFERENCES `sys_user` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='社区帖子点赞表';

CREATE TABLE `community_post_favorite` (
  `id` BIGINT NOT NULL COMMENT '主键',
  `post_id` BIGINT NOT NULL COMMENT '帖子ID',
  `user_id` BIGINT NOT NULL COMMENT '收藏用户ID',
  `create_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  PRIMARY KEY (`id`),
  UNIQUE INDEX `uk_post_favorite_user` (`post_id`, `user_id`),
  INDEX `idx_community_post_favorite_user_id` (`user_id`),
  INDEX `idx_community_post_favorite_create_time` (`create_time`),
  CONSTRAINT `fk_community_post_favorite_post_id` FOREIGN KEY (`post_id`) REFERENCES `community_post` (`id`),
  CONSTRAINT `fk_community_post_favorite_user_id` FOREIGN KEY (`user_id`) REFERENCES `sys_user` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='社区帖子收藏表';

CREATE TABLE IF NOT EXISTS `interview_dimension_score` (
  `id`             BIGINT       NOT NULL COMMENT '主键（雪花ID）',
  `user_id`        BIGINT       NOT NULL COMMENT '用户ID',
  `session_id`     VARCHAR(64)  NOT NULL COMMENT '面试会话ID',
  `dimension_key`  VARCHAR(32)  NOT NULL COMMENT '维度标识: technicalDepth/projectExpression/communication/problemSolving/pressureResistance/jobMatch',
  `score`          INT          NOT NULL COMMENT '维度分数 0-100',
  `comment`        TEXT         NULL     COMMENT '维度评价说明',
  `strengths`      JSON         NULL     COMMENT '加分项列表',
  `weaknesses`     JSON         NULL     COMMENT '扣分项列表',
  `create_time`    DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time`    DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `is_deleted`     TINYINT      NOT NULL DEFAULT 0 COMMENT '逻辑删除标志',
  PRIMARY KEY (`id`),
  INDEX `idx_dim_user` (`user_id`, `is_deleted`),
  INDEX `idx_dim_session` (`session_id`),
  -- 唯一约束不包含 is_deleted：历史记录逻辑删除后不自动复写，重复写入由业务幂等保护处理。
  UNIQUE INDEX `uk_session_dimension` (`session_id`, `dimension_key`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='面试维度评分表';

INSERT INTO `membership_plan` (`id`, `plan_code`, `plan_name`, `description`, `price_amount`, `duration_days`, `resume_quota`, `interview_quota`, `daily_polish_limit`, `daily_jd_match_limit`, `daily_template_limit`, `daily_offer_limit`, `total_resume_quota`, `total_interview_quota`, `total_polish_quota`, `total_jd_match_quota`, `total_template_quota`, `total_offer_quota`, `bonus_resume_quota`, `bonus_interview_quota`, `benefits`, `status`, `sort`)
VALUES
  (2001, 'vip_month', 'Monthly VIP', '30 days VIP with 10 resume diagnoses and 10 mock interviews', 29.90, 30, 10, 10, 1, 3, 5, 3, 0, 0, 0, 0, 0, 0, 0, 0, CONVERT(0x5B22414920E7AE80E58E86E6B6A6E889B2EFBC88E6AF8FE4BBBDE7AE80E58E86203120E6ACA1EFBC89222C224A4420E5B297E4BD8DE58CB9E9858DE58886E69E90EFBC88E6AF8FE697A5203320E6ACA1EFBC89222C22E7AE80E58E86E6A8A1E69DBFE5BA93EFBC88E6AF8FE697A5203520E6ACA1E4BDBFE794A8EFBC89222C224F6666657220E896AAE8B584E8B088E588A4E8BE85E58AA9EFBC88E6AF8FE697A5203320E6ACA1EFBC89222C22E6A8A1E68B9FE99DA2E8AF95EFBC88E6AF8FE697A520313020E6ACA1EFBC89222C22E7AE80E58E86E8AF8AE696ADEFBC88E6AF8FE697A5203520E6ACA1EFBC89225D USING utf8mb4), 1, 1),
  (2002, 'vip_quarter', 'Quarterly VIP', '90 days VIP with 35 resume diagnoses and 35 mock interviews', 79.90, 90, 35, 35, 1, 3, 5, 3, 0, 0, 0, 0, 0, 0, 0, 0, CONVERT(0x5B22414920E7AE80E58E86E6B6A6E889B2EFBC88E6AF8FE4BBBDE7AE80E58E86203120E6ACA1EFBC89222C224A4420E5B297E4BD8DE58CB9E9858DE58886E69E90EFBC88E6AF8FE697A5203320E6ACA1EFBC89222C22E7AE80E58E86E6A8A1E69DBFE5BA93EFBC88E6AF8FE697A5203520E6ACA1E4BDBFE794A8EFBC89222C224F6666657220E896AAE8B584E8B088E588A4E8BE85E58AA9EFBC88E6AF8FE697A5203320E6ACA1EFBC89222C22E6A8A1E68B9FE99DA2E8AF95EFBC88E6AF8FE697A520313020E6ACA1EFBC89222C22E7AE80E58E86E8AF8AE696ADEFBC88E6AF8FE697A5203520E6ACA1EFBC89225D USING utf8mb4), 1, 2),
  (2003, 'vip_year', 'Yearly VIP', '365 days VIP with 150 resume diagnoses and 150 mock interviews', 299.00, 365, 150, 150, 1, 3, 5, 3, 0, 0, 0, 0, 0, 0, 0, 0, CONVERT(0x5B22414920E7AE80E58E86E6B6A6E889B2EFBC88E6AF8FE4BBBDE7AE80E58E86203120E6ACA1EFBC89222C224A4420E5B297E4BD8DE58CB9E9858DE58886E69E90EFBC88E6AF8FE697A5203320E6ACA1EFBC89222C22E7AE80E58E86E6A8A1E69DBFE5BA93EFBC88E6AF8FE697A5203520E6ACA1E4BDBFE794A8EFBC89222C224F6666657220E896AAE8B584E8B088E588A4E8BE85E58AA9EFBC88E6AF8FE697A5203320E6ACA1EFBC89222C22E6A8A1E68B9FE99DA2E8AF95EFBC88E6AF8FE697A520313020E6ACA1EFBC89222C22E7AE80E58E86E8AF8AE696ADEFBC88E6AF8FE697A5203520E6ACA1EFBC89225D USING utf8mb4), 1, 3);

INSERT INTO `sys_job_role` (`id`, `role_code`, `role_name`, `interview_tag`, `tag_type`, `is_active`, `sort`)
VALUES
  (3001, 'frontend_engineer', '前端开发工程师', '热门', 'hot', 1, 10),
  (3002, 'backend_engineer', '后端开发工程师', '热门', 'hot', 1, 20),
  (3003, 'java_engineer', 'Java开发工程师', '热门', 'hot', 1, 30),
  (3004, 'product_manager', '产品经理', '常见', 'common', 1, 40),
  (3005, 'algorithm_engineer', '算法工程师', '高竞争', 'competitive', 1, 50),
  (3006, 'operations_specialist', '运营', '常规', 'normal', 1, 60),
  (3007, 'sales_marketing', '市场/销售', '常规', 'normal', 1, 70);

INSERT INTO `sys_ai_engine_config`
(`id`, `engine_code`, `engine_name`, `provider_type`, `business_type`, `model_name`, `base_url`, `api_key`, `supports_multimodal`, `thinking_mode`, `temperature`, `max_tokens`, `timeout_ms`, `is_active`, `sort`, `remark`)
VALUES
  (4001, 'interview_mock_engine', 'Interview Mock Engine', 'mock', 'interview', 'mock-interview-model', 'https://mock.example.com/interview', 'sk-interview-demo-key', 0, 'none', 0.70, 4096, 30000, 1, 10, 'Seed config for interview business'),
  (4002, 'resume_mock_engine', 'Resume Mock Engine', 'mock', 'resume', 'mock-resume-model', 'https://mock.example.com/resume', 'sk-resume-demo-key', 0, 'none', 0.50, 4096, 30000, 1, 20, 'Seed config for resume business');

INSERT INTO `sys_config` (`id`, `config_key`, `config_value`, `description`)
VALUES (1, 'custom_ai_daily_limit', '50', '用户自定义API Key每日调用上限');

SET FOREIGN_KEY_CHECKS = 1;
