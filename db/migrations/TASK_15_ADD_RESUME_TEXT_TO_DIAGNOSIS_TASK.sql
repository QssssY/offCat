-- 迁移脚本：为 resume_diagnosis_task 表添加 resume_text 字段
-- 用途：缓存PDF解析结果，避免每次查询都重新解析
-- 日期：2026-04-28

SET NAMES utf8mb4;
SET FOREIGN_KEY_CHECKS = 0;

-- 为 resume_diagnosis_task 表添加 resume_text 字段
ALTER TABLE `resume_diagnosis_task`
    ADD COLUMN `resume_text` MEDIUMTEXT NULL COMMENT '简历提取的文本内容，用于缓存PDF解析结果' AFTER `error_msg`;

SET FOREIGN_KEY_CHECKS = 1;
