-- V1.2 功能一：新手引导状态表
-- 用于记录用户的新手引导进度，支持分步骤引导、跳过和完成状态

SET NAMES utf8mb4;
SET FOREIGN_KEY_CHECKS = 0;

CREATE TABLE IF NOT EXISTS `user_onboarding_state` (
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

SET FOREIGN_KEY_CHECKS = 1;
