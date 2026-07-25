-- Add nickname field to sys_user table
-- 功能：支持用户自定义昵称，显示优先于用户名
-- 生成默认昵称规则：CONCAT('用户_', LPAD(FLOOR(RAND() * 26 + 65), 1, CHAR(65)), LPAD(FLOOR(RAND() * 26 + 65), 1, CHAR(65)), LPAD(FLOOR(RAND() * 26 + 65), 1, CHAR(65)), LPAD(FLOOR(RAND() * 26 + 65), 1, CHAR(65)), LPAD(FLOOR(RAND() * 10 + 48), 1, CHAR(48)), LPAD(FLOOR(RAND() * 10 + 48), 1, CHAR(48)))

ALTER TABLE `sys_user`
ADD COLUMN `nickname` VARCHAR(50) NULL DEFAULT NULL COMMENT 'User nickname' AFTER `username`;

CREATE INDEX `idx_sys_user_nickname` ON `sys_user` (`nickname`);