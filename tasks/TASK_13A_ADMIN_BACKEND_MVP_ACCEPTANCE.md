# TASK_13A_ADMIN_BACKEND_MVP_ACCEPTANCE

## 所属模块
- 管理端模块
- 子模块：后端接口最小可用验收测试

## 目标
在进入管理端前端开发前，对已完成的管理端后端能力做最小验收，重点覆盖：
- 权限
- 参数校验
- 空数据场景
- 兼容性

## 本轮测试 Task 拆分
1. 启动本地服务并准备管理员/普通用户 token
2. 验证 `/api/admin/**` 权限边界（管理员、普通用户、未登录）
3. 验证用户权益管理接口
4. 验证 AI 引擎配置接口
5. 验证岗位与 Prompt 接口
6. 验证数据看板接口（含参数校验、空数据、旧参数兼容）
7. 生成可复跑测试脚本与结果制品
8. 输出验收结论并更新 stage 状态

## 测试实现
- 脚本：`tasks/scripts/task13a_admin_backend_acceptance.ps1`
- 结果制品：`tasks/artifacts/TASK_13A_ADMIN_BACKEND_MVP_ACCEPTANCE_RESULT.json`
- 执行命令：
  - `powershell -ExecutionPolicy Bypass -File tasks/scripts/task13a_admin_backend_acceptance.ps1`

## 测试清单（请求 / 预期结果 / 失败判定 / 实际）

| Case | 请求 | 预期结果 | 失败判定 | 实际 |
|------|------|----------|----------|------|
| P1 | `GET /api/admin/ai-engines`（管理员） | 可访问 | 非 200 | 通过 |
| P2 | `GET /api/admin/ai-engines`（普通用户） | 不可访问 | 返回 200 | 通过 |
| P3 | `GET /api/admin/ai-engines`（未登录） | 不可访问 | 返回 200 | 通过 |
| U1 | `GET /api/admin/users/{userId}/rights` | 返回完整权益字段 | 非 200 或关键字段缺失 | 通过 |
| U2 | `PUT /api/admin/users/{userId}/rights`（no-op） | 可成功提交 | 非 200 | 通过 |
| A1 | `GET /api/admin/ai-engines` | 可返回并脱敏 `apiKey` | 非 200 或 `apiKey` 非脱敏 | 通过 |
| A2 | `PUT /api/admin/ai-engines/{id}/active` | 可成功切换（本次保持原值） | 非 200 | 通过 |
| J1 | `GET /api/admin/job-roles` | 可返回岗位列表 | 非 200 或空列表 | 通过 |
| J2 | `POST /api/admin/prompts`（合法 `jobRoleCode`） | 成功创建并返回 ID | 非 200 或无 ID | 通过 |
| D1 | `GET /api/admin/dashboard/overview`（无参数） | 兼容旧调用 | 非 200 | 通过 |
| D2 | `GET /api/admin/dashboard/trends`（无参数） | 兼容旧调用并返回趋势数据 | 非 200 或空趋势 | 通过 |
| D3 | `GET /api/admin/dashboard/hot-job-roles`（无参数） | 兼容旧调用 | 非 200 | 通过 |
| D4 | `GET /api/admin/dashboard/business-distribution`（无参数） | 默认最近 7 天可用 | 非 200 | 通过 |
| V1 | `GET /api/admin/dashboard/overview?startDate> endDate` | 拦截非法区间 | 未返回业务异常 | 通过 |
| V2 | `GET /api/admin/dashboard/trends`（超过 90 天） | 拦截超范围 | 未返回业务异常 | 通过 |
| V3 | `GET /api/admin/dashboard/hot-job-roles?limit=0` | 拦截非法 limit | 未返回业务异常 | 通过 |
| E1 | `GET /api/admin/dashboard/hot-job-roles`（历史无数据区间） | 返回 `data=[]` | 非 200 或非空数组 | 通过 |
| E2 | `GET /api/admin/dashboard/overview`（历史无数据区间） | 统计计数返回 0 | 非 200 或计数非 0 | 通过 |
| E3 | `GET /api/admin/dashboard/business-distribution`（历史无数据区间） | `totalCount=0` | 非 200 或总数非 0 | 通过 |

## 执行结果
- 总用例：19
- 通过：19
- 失败：0
- 结论：`allPassed = true`

## 发现与说明
1. 项目当前采用统一响应体表达业务失败（`HTTP 200 + code=500`），而非 HTTP 4xx/5xx。
2. 该返回契约本轮可工作，但前端接入时必须统一按响应体 `code` 判断成功失败。

## 验收结论
管理端后端接口通过本轮最小可用验收，可以进入管理端前端开发对接阶段。
