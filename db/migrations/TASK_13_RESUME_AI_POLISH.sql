SET NAMES utf8mb4;

-- V1.1 功能二：AI 简历润色
-- 用途：新增简历润色记录表，供结果页回显最近一次润色结果

CREATE TABLE IF NOT EXISTS `resume_polish_record` (
  `id` BIGINT NOT NULL COMMENT 'Primary key',
  `user_id` BIGINT NOT NULL COMMENT 'User id',
  `resume_task_id` BIGINT NOT NULL COMMENT 'Resume diagnosis task id',
  `source_resume_text` MEDIUMTEXT NOT NULL COMMENT 'Source resume text snapshot',
  `jd_text` MEDIUMTEXT NULL COMMENT 'Job description text snapshot',
  `polished_resume_text` MEDIUMTEXT NOT NULL COMMENT 'Polished resume text',
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

-- 回滚参考
-- DROP TABLE IF EXISTS `resume_polish_record`;
