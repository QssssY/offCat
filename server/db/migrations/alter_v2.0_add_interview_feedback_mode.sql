-- V2.0: interview_session 表新增反馈模式字段（每题即时反馈 / 面完统一复盘）
ALTER TABLE `interview_session`
  ADD COLUMN `feedback_mode` VARCHAR(20) DEFAULT NULL COMMENT 'Feedback mode: immediate-每题反馈, after_interview-面完复盘, NULL-默认面完复盘' AFTER `opening_generated`;
