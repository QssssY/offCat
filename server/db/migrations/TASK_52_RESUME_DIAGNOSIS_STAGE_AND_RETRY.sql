-- TASK-52: 简历诊断进度可视化 + 失败重试
-- 新增 stage 字段，跟踪诊断处理中的子阶段进度

ALTER TABLE `resume_diagnosis_task`
    ADD COLUMN `stage` VARCHAR(32) NULL DEFAULT NULL
        COMMENT '诊断子阶段: extracting/ai_analyzing/enhancing'
        AFTER `status`;
