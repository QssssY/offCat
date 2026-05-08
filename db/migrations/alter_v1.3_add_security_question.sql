-- V1.3: sys_user 表新增安全问题字段（忘记密码功能）
ALTER TABLE `sys_user`
  ADD COLUMN `security_question` VARCHAR(200) NULL DEFAULT NULL COMMENT 'Security question for password recovery' AFTER `vip_expire_time`,
  ADD COLUMN `security_answer`   VARCHAR(255) NULL DEFAULT NULL COMMENT 'Security answer (BCrypt encrypted)' AFTER `security_question`;
