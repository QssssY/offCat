SET NAMES utf8mb4;

-- V1.1 功能一：岗位 JD 对比分析
-- 用途：为简历诊断结果页的岗位 JD 对比分析增加独立存储表
-- 执行范围：增量更新，不影响现有 resume_diagnosis_task 主链路

CREATE TABLE IF NOT EXISTS `resume_job_match_record` (
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

-- 回滚参考（如需手动回滚再执行）
-- DROP TABLE IF EXISTS `resume_job_match_record`;
