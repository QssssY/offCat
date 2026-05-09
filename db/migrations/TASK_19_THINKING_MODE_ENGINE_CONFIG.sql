SET NAMES utf8mb4;
SET FOREIGN_KEY_CHECKS = 0;

ALTER TABLE `sys_ai_engine_config`
    ADD COLUMN `thinking_mode` VARCHAR(16) NOT NULL DEFAULT 'none' COMMENT 'Thinking mode: enabled/disabled/none' AFTER `supports_multimodal`;

SET FOREIGN_KEY_CHECKS = 1;
