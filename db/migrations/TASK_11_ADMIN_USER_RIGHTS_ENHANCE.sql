SET NAMES utf8mb4;

/*
  作用：
  为管理端用户与权益增强模块补充用户权益变更日志表。
  当前阶段先支持后台手工调整会员角色、套餐和到期时间，并为后续审计查询预留留痕结构。
*/

CREATE TABLE IF NOT EXISTS `user_rights_change_log` (
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
