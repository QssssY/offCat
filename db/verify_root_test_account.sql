-- ============================================
-- 验证: root 测试账号配置结果
-- 文件: db/verify_root_test_account.sql
-- ============================================

-- 1. 验证用户权限
SELECT
    id,
    username,
    role,
    status,
    is_deleted,
    vip_expire_time,
    LEFT(password, 20) as password_prefix
FROM sys_user
WHERE username = 'root';

-- 预期结果:
-- role = 9 (管理员)
-- status = 1 (正常)
-- is_deleted = 0
-- vip_expire_time = 2099-12-31 23:59:59
-- password 保持不变

-- 2. 验证额度配置
SELECT
    q.id as quota_id,
    q.user_id,
    q.total_interview_used,
    q.total_resume_used,
    q.daily_interview_used,
    q.daily_resume_used,
    q.last_refresh_date
FROM user_quota q
JOIN sys_user u ON q.user_id = u.id
WHERE u.username = 'root';

-- 预期结果:
-- total_interview_used = -999999
-- total_resume_used = -999999
-- daily_interview_used = -999999
-- daily_resume_used = -999999

-- 3. 完整信息汇总
SELECT
    u.id as user_id,
    u.username,
    CASE u.role
        WHEN 0 THEN '普通用户'
        WHEN 1 THEN '会员'
        WHEN 9 THEN '管理员'
        ELSE '未知'
    END as role_name,
    CASE u.status
        WHEN 1 THEN '正常'
        WHEN 0 THEN '封禁'
        ELSE '未知'
    END as status_name,
    u.vip_expire_time,
    q.total_interview_used,
    q.total_resume_used,
    q.daily_interview_used,
    q.daily_resume_used
FROM sys_user u
LEFT JOIN user_quota q ON u.id = q.user_id
WHERE u.username = 'root';
