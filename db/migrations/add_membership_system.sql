ALTER TABLE `sys_user`
ADD COLUMN `membership_plan_code` VARCHAR(32) NULL DEFAULT NULL COMMENT 'Current membership plan code' AFTER `status`;

ALTER TABLE `user_quota`
ADD COLUMN `interview_quota` INT NOT NULL DEFAULT 0 COMMENT 'Remaining interview quota' AFTER `total_resume_used`,
ADD COLUMN `resume_quota` INT NOT NULL DEFAULT 0 COMMENT 'Remaining resume quota' AFTER `interview_quota`;

UPDATE `user_quota` uq
JOIN `sys_user` su ON su.id = uq.user_id
SET
  uq.interview_quota = CASE
    WHEN su.role = 1 AND su.vip_expire_time IS NOT NULL AND su.vip_expire_time > NOW()
      THEN GREATEST(0, 10 - uq.daily_interview_used)
    ELSE GREATEST(0, 3 - uq.total_interview_used)
  END,
  uq.resume_quota = CASE
    WHEN su.role = 1 AND su.vip_expire_time IS NOT NULL AND su.vip_expire_time > NOW()
      THEN GREATEST(0, 10 - uq.daily_resume_used)
    ELSE GREATEST(0, 3 - uq.total_resume_used)
  END;

CREATE TABLE `membership_plan` (
  `id` BIGINT NOT NULL COMMENT 'Primary key',
  `plan_code` VARCHAR(32) NOT NULL COMMENT 'Plan code',
  `plan_name` VARCHAR(64) NOT NULL COMMENT 'Plan name',
  `description` VARCHAR(255) NULL DEFAULT NULL COMMENT 'Plan description',
  `price_amount` DECIMAL(10,2) NOT NULL COMMENT 'Price amount',
  `duration_days` INT NOT NULL COMMENT 'Duration days',
  `resume_quota` INT NOT NULL DEFAULT 0 COMMENT 'Granted resume quota',
  `interview_quota` INT NOT NULL DEFAULT 0 COMMENT 'Granted interview quota',
  `status` TINYINT NOT NULL DEFAULT 1 COMMENT '1-enabled, 0-disabled',
  `sort` INT NOT NULL DEFAULT 0 COMMENT 'Sort order',
  `create_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT 'Create time',
  `update_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT 'Update time',
  `is_deleted` TINYINT NOT NULL DEFAULT 0 COMMENT 'Logical delete flag',
  PRIMARY KEY (`id`),
  UNIQUE INDEX `uk_membership_plan_code` (`plan_code`),
  INDEX `idx_membership_plan_status_sort` (`status`, `sort`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='Membership plan table';

CREATE TABLE `membership_order` (
  `id` BIGINT NOT NULL COMMENT 'Primary key',
  `order_no` VARCHAR(64) NOT NULL COMMENT 'Order no',
  `user_id` BIGINT NOT NULL COMMENT 'User id',
  `plan_id` BIGINT NOT NULL COMMENT 'Plan id',
  `plan_code` VARCHAR(32) NOT NULL COMMENT 'Plan code snapshot',
  `plan_name` VARCHAR(64) NOT NULL COMMENT 'Plan name snapshot',
  `order_status` VARCHAR(20) NOT NULL COMMENT 'CREATED/PAID',
  `pay_channel` VARCHAR(20) NOT NULL COMMENT 'MOCK',
  `order_amount` DECIMAL(10,2) NOT NULL COMMENT 'Order amount',
  `duration_days` INT NOT NULL COMMENT 'Duration days',
  `granted_resume_quota` INT NOT NULL DEFAULT 0 COMMENT 'Granted resume quota',
  `granted_interview_quota` INT NOT NULL DEFAULT 0 COMMENT 'Granted interview quota',
  `expire_time_before` DATETIME NULL DEFAULT NULL COMMENT 'Expire time before upgrade',
  `expire_time_after` DATETIME NOT NULL COMMENT 'Expire time after upgrade',
  `paid_at` DATETIME NOT NULL COMMENT 'Paid time',
  `create_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT 'Create time',
  `update_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT 'Update time',
  `is_deleted` TINYINT NOT NULL DEFAULT 0 COMMENT 'Logical delete flag',
  PRIMARY KEY (`id`),
  UNIQUE INDEX `uk_membership_order_no` (`order_no`),
  INDEX `idx_membership_order_user_id` (`user_id`),
  INDEX `idx_membership_order_plan_id` (`plan_id`),
  INDEX `idx_membership_order_status` (`order_status`),
  INDEX `idx_membership_order_create_time` (`create_time`),
  CONSTRAINT `fk_membership_order_user_id` FOREIGN KEY (`user_id`) REFERENCES `sys_user` (`id`),
  CONSTRAINT `fk_membership_order_plan_id` FOREIGN KEY (`plan_id`) REFERENCES `membership_plan` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='Membership order table';

INSERT INTO `membership_plan` (`id`, `plan_code`, `plan_name`, `description`, `price_amount`, `duration_days`, `resume_quota`, `interview_quota`, `status`, `sort`)
VALUES
  (2001, 'vip_month', 'Monthly VIP', '30 days VIP with 10 resume diagnoses and 10 mock interviews', 29.90, 30, 10, 10, 1, 1),
  (2002, 'vip_quarter', 'Quarterly VIP', '90 days VIP with 35 resume diagnoses and 35 mock interviews', 79.90, 90, 35, 35, 1, 2),
  (2003, 'vip_year', 'Yearly VIP', '365 days VIP with 150 resume diagnoses and 150 mock interviews', 299.00, 365, 150, 150, 1, 3);
