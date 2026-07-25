-- TASK_60: 全量性能优化复合索引补齐
-- 本脚本只新增低风险二级索引，不修改业务字段；可重复执行。
SET NAMES utf8mb4;

DELIMITER $$

DROP PROCEDURE IF EXISTS add_index_if_missing $$
CREATE PROCEDURE add_index_if_missing(
  IN p_table_name VARCHAR(64),
  IN p_index_name VARCHAR(64),
  IN p_index_definition TEXT
)
BEGIN
  IF NOT EXISTS (
    SELECT 1
    FROM information_schema.STATISTICS
    WHERE TABLE_SCHEMA = DATABASE()
      AND TABLE_NAME = p_table_name
      AND INDEX_NAME = p_index_name
  ) THEN
    SET @ddl = CONCAT('ALTER TABLE `', p_table_name, '` ADD ', p_index_definition);
    PREPARE stmt FROM @ddl;
    EXECUTE stmt;
    DEALLOCATE PREPARE stmt;
  END IF;
END $$

DELIMITER ;

CALL add_index_if_missing(
  'resume_diagnosis_task',
  'idx_resume_task_user_status_time',
  'INDEX `idx_resume_task_user_status_time` (`user_id`, `status`, `create_time`)'
);

CALL add_index_if_missing(
  'interview_session',
  'idx_interview_session_user_status_time',
  'INDEX `idx_interview_session_user_status_time` (`user_id`, `status`, `create_time`)'
);

CALL add_index_if_missing(
  'user_notification',
  'idx_notification_user_read_time',
  'INDEX `idx_notification_user_read_time` (`user_id`, `read_status`, `create_time`)'
);

CALL add_index_if_missing(
  'community_post',
  'idx_community_post_deleted_category_time',
  'INDEX `idx_community_post_deleted_category_time` (`is_deleted`, `category`, `create_time`)'
);

CALL add_index_if_missing(
  'community_comment',
  'idx_community_comment_reply_user_actor_time',
  'INDEX `idx_community_comment_reply_user_actor_time` (`reply_to_user_id`, `user_id`, `create_time`)'
);

DROP PROCEDURE IF EXISTS add_index_if_missing;