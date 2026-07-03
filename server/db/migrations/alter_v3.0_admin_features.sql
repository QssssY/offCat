-- V3.0: 管理端功能扩展 - 新增系统公告、版本日志、成长配置表
-- 使用说明：在已有数据库上执行此文件即可完成迁移

-- 1. 系统公告/通知广播表
CREATE TABLE IF NOT EXISTS `sys_admin_notification` (
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
  INDEX `idx_admin_notification_create_time` (`create_time`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='系统公告表';

-- 2. 版本更新日志表
CREATE TABLE IF NOT EXISTS `sys_version_log` (
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
  INDEX `idx_version_log_published_at` (`published_at`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='版本更新日志表';

-- 3. 成长中心配置表
CREATE TABLE IF NOT EXISTS `sys_growth_config` (
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

-- 4. 用户通知表增加公告关联字段（ALTER 是幂等的，多次执行不会出错）
ALTER TABLE `user_notification`
  ADD COLUMN `broadcast_id` BIGINT NULL DEFAULT NULL COMMENT '关联系统公告ID' AFTER `biz_id`,
  ADD INDEX `idx_notification_broadcast_id` (`broadcast_id`);
