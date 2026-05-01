-- V1.2 回滚：删除 resume_polish_record 表中的 resume_document_json 和 template_code 字段
-- 执行前请确认已备份数据库

ALTER TABLE `resume_polish_record` DROP COLUMN `resume_document_json`;
ALTER TABLE `resume_polish_record` DROP COLUMN `template_code`;
