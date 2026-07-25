# TASK_06A_ADMIN

## 目标
补齐管理端最小闭环接口，修复交付前发现的管理能力缺口。

## 当前背景
项目已完成用户侧核心链路，并已执行第一轮交付整理任务 `TASK_06_DELIVERY`。
当前发现管理端接口尚未完整落地，因此需要在最终交付前补齐管理端最小能力。

## 本轮范围
仅允许实现以下内容：
1. Prompt 列表查询接口
2. Prompt 新增接口
3. Prompt 修改接口
4. Prompt 启用/禁用接口
5. 用户列表查询接口
6. 用户状态修改接口（封禁/解封）
7. 用户额度查询接口
8. 用户额度调整接口
9. 所有新增代码必须添加必要中文注释
10. 所有新增代码必须添加必要日志
11. 如新增或修改前端需要访问的 HTTP 接口，必须同步生成接口文档

## 本轮涉及表
- sys_prompt
- sys_user
- user_quota

## 本轮涉及上下文
- runtime/RULES.md
- runtime/STACK.md
- runtime/STATE.md
- runtime/API_DOC_RULES.md
- runtime/LOG_RULES.md
- runtime/TASK_FLOW_RULES.md
- runtime/PROJECT_BLUEPRINT_MIN.md
- db/DB_AUTH.md
- tasks/TASK_06A_ADMIN.md

## 本轮禁止
1. 不实现复杂数据看板
2. 不实现 AI 配置中心
3. 不实现复杂监控功能
4. 不修改用户侧既有业务逻辑
5. 不修改无关模块

## 输出要求
1. 先列影响文件
2. 再逐文件输出完整代码
3. 所有新增代码必须包含必要中文注释
4. 所有新增代码必须包含必要日志
5. 如果新增或修改了前端需要访问的 HTTP 接口，必须同步生成 `docs/api/TASK_06A_ADMIN_API.md`
6. 检查所有 Controller 中 Result.success / Result.error 的调用是否与当前 Result 工具类签名一致
7. 最后输出自检结果
8. 输出任务完成后的 STATE.md 更新内容
9. 输出任务归档建议
10. 输出蓝图进度对照报告

## 验收标准
1. 支持 Prompt 管理基础接口
2. 支持用户列表与状态管理
3. 支持用户额度查看与调整
4. 代码具备中文注释
5. 代码具备必要日志
6. 若新增前端需要访问的 HTTP 接口，则接口文档符合 `runtime/API_DOC_RULES.md`