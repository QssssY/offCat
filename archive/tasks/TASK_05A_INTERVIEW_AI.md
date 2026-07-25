# TASK_05_INTERVIEW

## 目标
实现模拟面试模块最小闭环，优先打通会话创建、消息记录、结束面试、结果生成与历史查询链路；本轮允许使用 mock/stub 方式替代真实大模型回复与评分。

## 当前背景
认证模块、额度模块、简历诊断基础链路已完成。
当前尚未配置真实大模型 API Key，因此本轮以“面试链路先跑通”为优先目标。

## 本轮范围
仅允许实现以下内容：
1. 创建模拟面试会话
2. 保存面试会话基础信息（岗位、难度、用户）
3. 初始化面试上下文（可先使用简化实现或占位实现）
4. 用户发送消息接口
5. 保存聊天记录
6. 使用 mock/stub 方式生成面试官回复
7. 结束面试接口
8. 使用 mock/stub 方式生成综合评分和评价报告
9. 查询面试历史列表接口
10. 查询面试详情接口
11. 所有新增代码必须添加必要中文注释
12. 所有新增代码必须添加必要日志
13. 如果本轮新增或修改了前端需要访问的 HTTP 接口，必须同步生成接口文档

## 本轮涉及表
- interview_session
- interview_chat_log
- user_quota
- sys_user

## 本轮涉及上下文
- runtime/RULES.md
- runtime/STACK.md
- runtime/STATE.md
- runtime/API_DOC_RULES.md
- runtime/LOG_RULES.md
- runtime/TASK_FLOW_RULES.md
- runtime/PROJECT_BLUEPRINT_MIN.md
- db/DB_INTERVIEW.md
- db/DB_AUTH.md
- tasks/TASK_05_INTERVIEW.md

## 本轮禁止
1. 不强制接入真实大模型 API
2. 不强制实现真实 SSE 流式推送
3. 不实现前端页面
4. 不实现无关管理端能力
5. 不修改认证模块、额度模块、简历模块既有行为
6. 不修改无关模块

## 输出要求
1. 先列影响文件
2. 再逐文件输出完整代码
3. 所有新增代码必须包含必要中文注释
4. 所有新增代码必须包含必要日志
5. 如果新增或修改了前端需要访问的 HTTP 接口，必须同步生成 `docs/api/TASK_05_INTERVIEW_API.md`
6. 检查所有 Controller 中 Result.success / Result.error 调用是否与当前 Result 工具类签名一致
7. 最后输出自检结果
8. 输出任务完成后的 STATE.md 更新内容
9. 输出任务归档建议
10. 输出蓝图进度对照报告

## 验收标准
1. 支持创建面试会话
2. 支持消息发送与聊天记录保存
3. 支持使用 mock 回复模拟面试官响应
4. 支持结束面试
5. 支持生成 mock 综合评分和评价报告
6. 支持查询历史列表与详情
7. 代码具备中文注释
8. 代码具备必要日志
9. 若新增前端需要访问的 HTTP 接口，则接口文档符合 `runtime/API_DOC_RULES.md`