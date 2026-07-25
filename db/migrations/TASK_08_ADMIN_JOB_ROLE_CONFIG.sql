SET NAMES utf8mb4;

/*
  作用：
  为现有数据库补充“岗位配置模块”所需的岗位表和测试岗位数据。

  为什么需要这份增量脚本：
  当前项目已经有运行中的数据库，单纯修改 db/schema.sql 只能影响全量初始化场景，
  无法覆盖已存在的本地/测试库。
  管理端岗位配置需要真实表结构承载，前端岗位选项也必须改为从后端读取，
  所以本轮任务必须同时提供可直接执行的增量 SQL。
*/

CREATE TABLE IF NOT EXISTS `sys_job_role` (
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

/*
  作用：
  先填充一组测试岗位，保证管理端和用户端联调时立即有可用数据。
  这里使用 INSERT ... ON DUPLICATE KEY UPDATE，便于重复执行脚本而不报唯一键冲突。
*/
INSERT INTO `sys_job_role` (`id`, `role_code`, `role_name`, `interview_tag`, `tag_type`, `is_active`, `sort`)
VALUES
  (3001, 'frontend_engineer', '前端开发工程师', '热门', 'hot', 1, 10),
  (3002, 'backend_engineer', '后端开发工程师', '热门', 'hot', 1, 20),
  (3003, 'java_engineer', 'Java开发工程师', '热门', 'hot', 1, 30),
  (3004, 'product_manager', '产品经理', '常见', 'common', 1, 40),
  (3005, 'algorithm_engineer', '算法工程师', '高竞争', 'competitive', 1, 50),
  (3006, 'operations_specialist', '运营', '常规', 'normal', 1, 60),
  (3007, 'sales_marketing', '市场/销售', '常规', 'normal', 1, 70)
ON DUPLICATE KEY UPDATE
  `role_name` = VALUES(`role_name`),
  `interview_tag` = VALUES(`interview_tag`),
  `tag_type` = VALUES(`tag_type`),
  `is_active` = VALUES(`is_active`),
  `sort` = VALUES(`sort`),
  `update_time` = CURRENT_TIMESTAMP;
