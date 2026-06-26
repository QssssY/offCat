-- TASK-52 修复：使用独立失败时间计算 24 小时重试窗口，避免 update_time 被维护更新延长。

ALTER TABLE `resume_diagnosis_task`
    ADD COLUMN `failed_at` DATETIME NULL DEFAULT NULL
        COMMENT '任务进入失败状态的时间，用于24小时重试窗口'
        AFTER `error_msg`;
