# TASK_42_SETTINGS_DATA_MANAGEMENT_BACKEND

## 1. 当前任务所属模块
- 用户侧设置中心
- 用户账号与历史数据管理

## 2. 前端文件定位
- `frontend/app/src/views/settings/SettingsView.vue`
- `frontend/app/src/api/auth.js`
- `frontend/app/src/api/interview.js`
- `frontend/app/src/api/resume.js`
- `frontend/app/src/__tests__/views/SettingsView.test.js`

## 3. 后端文件定位
- `server/src/main/java/com/airesume/server/controller/UserAccountController.java`
- `server/src/main/java/com/airesume/server/controller/InterviewController.java`
- `server/src/main/java/com/airesume/server/controller/ResumeDiagnosisController.java`
- `server/src/main/java/com/airesume/server/service/UserAccountService.java`
- `server/src/main/java/com/airesume/server/service/impl/UserAccountServiceImpl.java`
- `server/src/main/java/com/airesume/server/service/InterviewService.java`
- `server/src/main/java/com/airesume/server/service/ResumeDiagnosisTaskService.java`
- `server/src/main/java/com/airesume/server/service/impl/ResumeDiagnosisTaskServiceImpl.java`
- `server/src/main/java/com/airesume/server/infrastructure/security/JwtAuthenticationFilter.java`
- `server/src/main/java/com/airesume/server/mapper/`
- `server/src/main/java/com/airesume/server/repository/`

## 4. 本轮修改文件清单
- 新增 `server/src/main/java/com/airesume/server/controller/UserAccountController.java`
- 新增 `server/src/main/java/com/airesume/server/service/UserAccountService.java`
- 新增 `server/src/main/java/com/airesume/server/service/impl/UserAccountServiceImpl.java`
- 新增 `server/src/main/java/com/airesume/server/dto/user/AccountDeleteRequest.java`
- 新增 `server/src/main/java/com/airesume/server/dto/user/DataCleanupResponse.java`
- 修改 `server/src/main/java/com/airesume/server/controller/InterviewController.java`
- 修改 `server/src/main/java/com/airesume/server/controller/ResumeDiagnosisController.java`
- 修改 `server/src/main/java/com/airesume/server/service/InterviewService.java`
- 修改 `server/src/main/java/com/airesume/server/service/ResumeDiagnosisTaskService.java`
- 修改 `server/src/main/java/com/airesume/server/service/impl/ResumeDiagnosisTaskServiceImpl.java`
- 修改 `server/src/main/java/com/airesume/server/infrastructure/security/JwtAuthenticationFilter.java`
- 修改用户、面试、简历诊断、通知、额度、新手引导相关 Mapper 与 Repository
- 新增和补充账号注销、历史清理、JWT 有效性校验相关后端单元测试
- 新增 `tasks/TASK_42_SETTINGS_DATA_MANAGEMENT_BACKEND.md`
- 更新 `runtime/STATE.md`

## 5. 前端实现方案
- 前端实现说明见 `frontend/tasks/TASK_42_SETTINGS_DATA_MANAGEMENT_FRONTEND.md`
- 设置中心三个危险操作从“待后端接入”禁用态改为可点击操作
- 账号注销要求输入当前密码，成功后清除用户登录态并跳转登录页
- 面试记录清理和简历诊断清理使用二次确认，成功后展示清理数量并刷新账号数据概览

## 6. 后端实现方案
- 新增 `POST /api/user/account/delete`，请求体为 `{ oldPassword: string }`，必须登录后才能调用。
- 账号注销先校验当前密码；密码错误时返回业务错误，不清理登录态或业务数据。
- 密码校验通过后，统一清理当前用户关联业务数据，并将用户主表逻辑删除和敏感字段匿名化。
- 用户名改为 `deleted_{userId}`，昵称改为“已注销用户”，密码替换为随机不可用 hash，安全问题与答案、会员字段等敏感或权益字段清空，账号状态置为禁用。
- 新增 `DELETE /api/interview/history`，逻辑删除当前用户全部面试会话，并同步逻辑删除对应聊天记录和岗位定向上下文。
- 新增 `DELETE /api/resume/history`，逻辑删除当前用户全部简历诊断任务，并同步逻辑删除 JD 匹配记录和简历润色记录。
- 简历诊断清理会在安全路径校验通过后删除对应上传文件；文件已不存在时不阻断清理；路径越界时返回业务错误。
- JWT 过滤器补充账号有效性校验，已注销或禁用账号的旧 token 不再进入认证上下文。
- 面试历史、详情、聊天记录、统计和异步报告回写相关查询补齐 `isDeleted = 0` 条件，避免已清理数据继续回显或被异步任务回写。
- 本轮不接入“面试记录保留天数”的服务端自动清理任务。

## 7. 数据存储方案
- 不新增数据库表。
- 不新增数据库字段。
- 账号注销采用逻辑删除 + 敏感字段匿名化，保留主表行以避免破坏现有外键与审计链路。
- 面试记录、聊天记录、岗位定向上下文、简历诊断任务、JD 匹配记录、简历润色记录、通知、额度、新手引导状态均沿用现有 `is_deleted` 逻辑删除字段。
- 清理接口只处理当前登录用户自己的数据，不支持跨用户清理。

## 8. stage 更新说明
- 前端阶段记录已更新到 `frontend/tasks/stage.md`，记录设置中心危险操作后端接入已完成并等待验收。
- 后端阶段记录已更新到 `runtime/STATE.md`，记录新增接口、数据清理策略、验证结果和停止范围。

## 9. 编译结果
- `mvn.cmd test` 通过，348 个后端测试通过。

## 10. 构建结果
- 后端本轮以 Maven 测试完成编译校验。
- 前端构建结果见 `frontend/tasks/TASK_42_SETTINGS_DATA_MANAGEMENT_FRONTEND.md`。

## 11. 测试结果
- `mvn.cmd test` 通过，348 个测试通过。
- 覆盖账号注销密码错误拒绝、密码正确时清理关联数据和匿名化账号、已删除或禁用账号 token 不再认证。
- 覆盖面试记录清理只处理当前用户数据，并同步清理聊天记录和岗位定向上下文。
- 覆盖简历诊断清理只处理当前用户任务及衍生记录，文件缺失不阻断，路径越界会拒绝。
- 覆盖新增 Controller 从认证上下文读取当前用户并调用对应 Service。

## 12. 当前功能验收说明
- 设置中心账号注销已接入后端真实能力，需要当前密码二次确认。
- 账号注销成功后，当前账号会被逻辑删除和匿名化，旧 token 后续请求不再通过认证。
- 面试记录清理会批量逻辑删除当前用户全部历史面试会话及关联记录，并返回清理数量。
- 简历诊断清理会批量逻辑删除当前用户全部历史诊断任务及衍生记录，并返回清理数量。
- 本轮没有实现服务端自动保留天数清理，也没有新增按日期或勾选项清理。

## 13. 停止，不继续下一个功能
- 本轮只接入账号注销、面试记录清理、简历诊断清理三个显式危险操作。
- 不新增数据库表。
- 不实现自动保留天数清理任务。
- 不扩展账号恢复、数据导出、分项选择删除或管理端代删能力。
