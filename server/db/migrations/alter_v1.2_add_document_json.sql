-- V1.2: 简历编辑器文档持久化字段
SET NAMES utf8mb4;
SET FOREIGN_KEY_CHECKS = 0;

ALTER TABLE `resume_polish_record`
    ADD COLUMN `document_json` LONGTEXT NULL DEFAULT NULL COMMENT '编辑后的简历文档JSON' AFTER `polished_resume_text`,
    ADD COLUMN `edited_plain_text` TEXT NULL DEFAULT NULL COMMENT '编辑后的简历纯文本' AFTER `document_json`;

SET FOREIGN_KEY_CHECKS = 1;
