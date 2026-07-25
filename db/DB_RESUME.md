# 简历诊断模块数据库摘要

## resume_diagnosis_task

字段：
- id
- user_id
- file_url
- status：0 排队中，1 处理中，2 已完成，3 已失败
- diagnosis_result
- error_msg

用途：
- 异步简历诊断任务跟踪
- 诊断结果持久化

## resume_job_match_record

字段：
- id
- user_id
- resume_task_id
- resume_text
- jd_text
- match_score
- analysis_result

用途：
- 记录岗位 JD 对比分析结果
- 为结果页回显最近一次岗位匹配分析提供数据来源

## resume_polish_record

字段：
- id
- user_id
- resume_task_id
- source_resume_text
- jd_text
- polished_resume_text
- modification_notes
- source_type

用途：
- 记录最近一次 AI 简历润色结果
- 支持结果页直接回显和复制
