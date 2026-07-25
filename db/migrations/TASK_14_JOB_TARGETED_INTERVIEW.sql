SET NAMES utf8mb4;

CREATE TABLE IF NOT EXISTS `mock_interview_job_target_record` (
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
