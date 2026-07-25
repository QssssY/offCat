# TASK_09_ADMIN_PROMPT_JOB_ROLE_LINK

## 所属模块
- 管理端模块
- 子模块：Prompt 管理模块 / 岗位配置联动

## 背景
上一轮 `TASK_08_ADMIN_JOB_ROLE_CONFIG` 已经完成岗位配置闭环，但 Prompt 管理仍然存在一个关键断点：

- Prompt 仍然直接使用 `job_role` 自由输入字符串
- Prompt 和 `sys_job_role` 之间没有稳定关联字段
- 管理端即使已经有岗位配置，也无法保证 Prompt 选择的是合法岗位

这会导致 Prompt 配置和岗位配置再次脱节。

## 本轮目标
1. 为 Prompt 增加稳定的岗位编码关联字段 `job_role_code`
2. 管理端保存 Prompt 时必须校验岗位来自 `sys_job_role`
3. Prompt 查询结果补充岗位编码和岗位名称
4. 保留兼容升级能力，避免旧数据直接失效
5. 同步更新 API 文档和 stage 进度文档

## 本轮 task 拆分
1. 数据层升级：给 `sys_prompt` 增加 `job_role_code` 字段并回填旧数据
2. DTO 与实体升级：Prompt 请求/响应补充岗位编码字段
3. 管理端联动：创建/编辑 Prompt 时走岗位配置校验
4. 查询结果增强：返回 `jobRoleCode + jobRoleName`
5. 文档资产同步：更新 task、API 文档、stage 文档

## 实现清单
- `db/schema.sql`
- `db/migrations/TASK_09_ADMIN_PROMPT_JOB_ROLE_LINK.sql`
- `server/src/main/java/com/airesume/server/entity/SysPrompt.java`
- `server/src/main/java/com/airesume/server/dto/admin/PromptCreateRequest.java`
- `server/src/main/java/com/airesume/server/dto/admin/PromptUpdateRequest.java`
- `server/src/main/java/com/airesume/server/dto/admin/PromptResponse.java`
- `server/src/main/java/com/airesume/server/service/SysJobRoleService.java`
- `server/src/main/java/com/airesume/server/service/impl/SysJobRoleServiceImpl.java`
- `server/src/main/java/com/airesume/server/controller/AdminController.java`
- `docs/api/TASK_06A_ADMIN_API.md`
- `runtime/STATE.md`

## 验收标准
- Prompt 创建/编辑不能再直接落自由输入岗位
- Prompt 保存时，岗位必须能在 `sys_job_role` 中解析成功
- Prompt 列表返回结果包含岗位编码和岗位名称
- 旧 Prompt 数据可以通过增量 SQL 按岗位名称回填编码
- API 文档、task 文档、stage 文档都已同步更新
