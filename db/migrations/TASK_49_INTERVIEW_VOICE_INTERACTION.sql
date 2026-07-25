-- 模拟面试语音通话功能：记录会话创建时选择的交互方式。
-- 0=文字面试，1=语音面试；默认 0 兼容历史会话。
ALTER TABLE `interview_session`
ADD COLUMN `interaction_type` TINYINT NOT NULL DEFAULT 0
COMMENT '交互方式：0-文字面试，1-语音面试'
AFTER `feedback_mode`;
