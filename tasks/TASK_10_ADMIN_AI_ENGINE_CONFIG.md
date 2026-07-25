# TASK_10_ADMIN_AI_ENGINE_CONFIG

## 所属模块
- 管理端模块
- 子模块：AI 引擎配置模块

## 背景
上一轮 `TASK_09_ADMIN_PROMPT_JOB_ROLE_LINK` 已经完成 Prompt 与岗位配置联动，但 AI 模型配置仍然停留在配置文件层，没有进入管理端统一管理。

当前缺口：
- 不同业务类型还不能通过后台维护各自的模型配置
- 缺少统一的 AI 引擎配置表承载 provider / model / baseUrl / apiKey 等字段
- 缺少“同一 businessType 仅一个启用配置”的后台规则约束
- API 文档与 stage 进度也尚未覆盖该模块

## 本轮目标
1. 新增 `sys_ai_engine_config` 表
2. 支持 `interview` / `resume` 两类业务配置不同模型
3. 提供管理端查询、新增、修改、启用切换接口
4. 后端保证同一 `businessType` 同时只能有一个启用配置
5. 列表接口中的 `apiKey` 必须脱敏返回
6. 预置两条测试配置，方便联调
7. 同步更新 task、API 文档和 stage 进度文档

## 本轮 task 拆分
1. 数据层：新增 AI 引擎配置表与迁移脚本，并插入测试数据
2. 模型层：补实体、Mapper、Service、DTO
3. 业务规则：实现引擎编码唯一校验、businessType 合法性校验、单业务单启用切换
4. 控制层：在 `AdminController` 增加 4 个管理端接口
5. 文档资产：更新 API 文档、索引和 stage 状态
6. 验证：执行 `mvn.cmd -q -DskipTests compile`

## 实现清单
- `db/schema.sql`
- `db/migrations/TASK_10_ADMIN_AI_ENGINE_CONFIG.sql`
- `server/src/main/java/com/airesume/server/common/constants/AiEngineConstants.java`
- `server/src/main/java/com/airesume/server/entity/SysAiEngineConfig.java`
- `server/src/main/java/com/airesume/server/mapper/SysAiEngineConfigMapper.java`
- `server/src/main/java/com/airesume/server/service/SysAiEngineConfigService.java`
- `server/src/main/java/com/airesume/server/service/impl/SysAiEngineConfigServiceImpl.java`
- `server/src/main/java/com/airesume/server/dto/admin/AiEngineConfigCreateRequest.java`
- `server/src/main/java/com/airesume/server/dto/admin/AiEngineConfigUpdateRequest.java`
- `server/src/main/java/com/airesume/server/dto/admin/AiEngineConfigResponse.java`
- `server/src/main/java/com/airesume/server/controller/AdminController.java`
- `docs/api/TASK_06A_ADMIN_API.md`
- `docs/api/API_INDEX.md`
- `runtime/STATE.md`

## 验收标准
- 管理端可以查询 AI 引擎配置列表
- 管理端可以新增和修改 AI 引擎配置
- 管理端可以切换配置启用状态
- 同一 `businessType` 不会同时出现两个启用配置
- `/api/admin/ai-engines` 列表返回中的 `apiKey` 为脱敏值
- `interview`、`resume` 两类业务都有预置测试配置
- task 文档、API 文档、stage 进度文档均已同步更新
