-- ============================================
-- 任务: 将 root 提升为联调测试账号
-- 文件: db/promote_root_to_test_account.sql
-- 目标: 管理员权限 + 大额度配额
-- 注意: 不修改密码
-- ============================================

-- 1. 确认 root 用户存在
SELECT id, username, role, status, password
FROM sys_user
WHERE username = 'root';

-- 2. 更新 root 为管理员权限
-- role: 0普通, 1会员, 9管理员
-- status: 1正常, 0封禁
UPDATE sys_user
SET
    role = 9,
    status = 1,
    is_deleted = 0,
    vip_expire_time = '2099-12-31 23:59:59',
    update_time = NOW()
WHERE username = 'root';

-- 3. 插入或更新 user_quota 大额度配置
-- 先尝试插入新记录
INSERT INTO user_quota (
    id,
    user_id,
    total_interview_used,
    total_resume_used,
    daily_interview_used,
    daily_resume_used,
    last_refresh_date,
    create_time,
    update_time,
    is_deleted
)
SELECT
    (SELECT CONCAT('999999', FLOOR(RAND() * 1000000000))),  -- 临时id，雪花算法风格
    id,
    -999999,
    -999999,
    -999999,
    -999999,
    CURDATE(),
    NOW(),
    NOW(),
    0
FROM sys_user
WHERE username = 'root'
ON DUPLICATE KEY UPDATE
    total_interview_used = -999999,
    total_resume_used = -999999,
    daily_interview_used = -999999,
    daily_resume_used = -999999,
    last_refresh_date = CURDATE(),
    update_time = NOW();
