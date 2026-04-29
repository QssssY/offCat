-- 为 interview_session 表添加 opening_generated 字段
-- 用于标记面试开场白是否已生成（0-未生成，1-已生成）
ALTER TABLE interview_session 
ADD COLUMN opening_generated INT(1) DEFAULT 0 COMMENT '开场白是否已生成：0-未生成，1-已生成';

-- 为已有数据设置默认值（已结束的会话默认为已生成）
UPDATE interview_session 
SET opening_generated = 1 
WHERE status = 1;

-- 进行中的会话需要根据是否有聊天记录来判断
-- 这里暂时设置为 0，让系统自动处理
UPDATE interview_session 
SET opening_generated = 0 
WHERE status = 0;
