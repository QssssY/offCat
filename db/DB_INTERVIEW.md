# 模拟面试模块数据库摘要

## interview_session
- id
- session_id（唯一）
- user_id
- job_role
- difficulty
- status（0进行中 1结束）
- comprehensive_score
- evaluation_report（JSON）

## interview_chat_log
- id
- session_id
- message_role
- content

用途：
- 会话管理
- 聊天记录存储
- 面试历史回溯
