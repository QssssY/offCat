-- TASK_55: 新手任务式引导 — 用户新手任务表
CREATE TABLE IF NOT EXISTS `user_onboarding_task` (
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
