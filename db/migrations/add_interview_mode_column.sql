-- ==========================================
-- 数据库迁移脚本：添加面试模式字段
-- 执行日期：2026-04-09
-- 功能：新增 interview_mode 列支持普通面试/压力面试
-- ==========================================

-- 1. 给 interview_session 表添加 interview_mode 列
-- 注意：先判断列是否存在，避免重复执行报错
-- MySQL 8.0+ 可以使用 ALTER TABLE ... ADD COLUMN IF NOT EXISTS
-- 低版本 MySQL 需要手动判断或使用存储过程

ALTER TABLE `interview_session`
ADD COLUMN `interview_mode` VARCHAR(20) NOT NULL DEFAULT 'normal'
COMMENT '面试模式：normal-普通面试，stress-压力面试'
AFTER `difficulty`;

-- 2. 给新增列添加索引（可选，用于按模式查询统计）
CREATE INDEX `idx_interview_session_interview_mode`
ON `interview_session` (`interview_mode`);

-- 3. 验证列是否添加成功
-- 执行以下查询验证：
-- SHOW COLUMNS FROM `interview_session` LIKE 'interview_mode';

-- 4. 兼容旧数据说明：
-- - 旧数据的 interview_mode 会自动设置为 'normal'
-- - 无需额外的 UPDATE 语句
-- - 查询时会自动处理 null 值（代码层有兜底逻辑）

-- ==========================================
-- 回滚脚本（如需撤销）：
-- ==========================================
-- ALTER TABLE `interview_session` DROP COLUMN `interview_mode`;
-- DROP INDEX `idx_interview_session_interview_mode` ON `interview_session`;
