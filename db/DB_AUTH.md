# 认证模块数据库摘要

涉及表：

## sys_user
字段：
- id
- username（唯一）
- password
- role（0普通 1会员 9管理员）
- status（1正常 0封禁）
- vip_expire_time

用途：
- 登录认证
- 角色判断
- 会员判定

## user_quota
字段：
- user_id（唯一）
- total_interview_used
- total_resume_used
- daily_interview_used
- daily_resume_used
- last_refresh_date

用途：
- 次数控制
- AI调用前校验