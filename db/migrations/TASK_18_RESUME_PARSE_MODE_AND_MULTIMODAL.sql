SET NAMES utf8mb4;
SET FOREIGN_KEY_CHECKS = 0;

ALTER TABLE `resume_diagnosis_task`
    ADD COLUMN `parse_mode` VARCHAR(16) NULL DEFAULT NULL COMMENT 'Resume parse mode: TEXT/MULTIMODAL/OCR/MIXED' AFTER `resume_text`,
    ADD COLUMN `parse_message` VARCHAR(255) NULL DEFAULT NULL COMMENT 'Resume parse hint message' AFTER `parse_mode`;

ALTER TABLE `sys_ai_engine_config`
    ADD COLUMN `supports_multimodal` TINYINT NOT NULL DEFAULT 0 COMMENT '1-supported, 0-unsupported' AFTER `api_key`;

SET FOREIGN_KEY_CHECKS = 1;
