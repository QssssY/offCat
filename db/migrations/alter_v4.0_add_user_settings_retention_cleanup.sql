-- V4.0: 用户设置与数据保留期自动清理
CREATE TABLE IF NOT EXISTS `user_settings` (
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

ALTER TABLE `interview_session`
  ADD INDEX `idx_interview_session_retention_cleanup` (`user_id`, `status`, `is_deleted`, `create_time`);

ALTER TABLE `resume_diagnosis_task`
  ADD INDEX `idx_resume_task_retention_cleanup` (`user_id`, `status`, `is_deleted`, `create_time`);
