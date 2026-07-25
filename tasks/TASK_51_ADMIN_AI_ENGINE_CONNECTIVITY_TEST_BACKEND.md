# TASK 51 管理端 AI 引擎连通测试后端

## 当前任务所属模块

管理端模块，子模块为 AI 引擎配置。

## 前端文件定位

前端实现见 `frontend/tasks/TASK_51_ADMIN_AI_ENGINE_CONNECTIVITY_TEST_FRONTEND.md`。

## 后端文件定位

- `server/src/main/java/com/airesume/server/controller/AdminController.java`
- `server/src/main/java/com/airesume/server/dto/admin/AiEngineConnectivityTestRequest.java`
- `server/src/main/java/com/airesume/server/dto/admin/AiEngineConnectivityTestResponse.java`
- `server/src/main/java/com/airesume/server/service/AiEngineConnectivityTestService.java`
- `server/src/main/java/com/airesume/server/service/impl/AiEngineConnectivityTestServiceImpl.java`
- `server/src/test/java/com/airesume/server/controller/AdminAiEngineConnectivityControllerTest.java`
- `server/src/test/java/com/airesume/server/service/impl/AiEngineConnectivityTestServiceImplTest.java`

## 本轮修改文件清单

- 新增 AI 引擎连通测试请求与响应 DTO。
- 新增 `AiEngineConnectivityTestService` 与实现类，使用当前表单配置发起一次最小 `chat/completions` 调用。
- `AdminController` 新增 `POST /api/admin/ai-engines/connectivity-test`。
- 连通测试显式复用公网 HTTPS 校验；非法 baseUrl 直接返回失败结果和具体原因，不发起外部请求。
- 新增后端单元测试覆盖连通成功、上游拒绝、Mock provider、非法内网地址、编辑态复用已保存密钥和新增态缺少密钥拒绝。

## 前端实现方案

见前端 task 文件。本轮后端为前端弹窗按钮提供接口，不改变列表、保存和启停接口契约。

## 后端实现方案

接口只做连通测试，不保存配置。新增态必须传完整 API Key；编辑态如果 API Key 为空或为脱敏格式，则读取数据库中已保存的真实密钥并解密后测试。连通测试沿用公网 HTTPS 地址校验，禁止内网和本机地址；如果 baseUrl 不合法，直接返回失败结果并在 `errorMessage` 中说明原因，不继续发起真实网络请求。非 `mock` provider 在校验通过后发送一次极小 token 的 OpenAI 兼容 `chat/completions` 请求，并返回成功状态、耗时、响应摘要或失败原因。`mock` provider 不发外部请求，直接返回格式有效。

## 数据存储方案

不新增表，不修改字段，不新增迁移脚本；接口不写入 `sys_ai_engine_config`。

## stage 更新说明

已同步更新 `runtime/STATE.md`。

## 编译结果

`mvn.cmd test` 已完成编译阶段并通过。

## 构建结果

后端无单独构建命令，本轮以后端测试编译作为验证。

## 测试结果

- 后端目标测试：`mvn.cmd test "-Dtest=AiEngineConnectivityTestServiceImplTest,AdminAiEngineConnectivityControllerTest,PublicHttpsUrlValidatorTest"` 通过，12 个测试通过。
- 后端完整测试：`mvn.cmd test` 通过，396 个测试通过。

## 当前功能验收说明

- 管理员可在新增/编辑 AI 引擎配置时测试当前配置是否真实可连通。
- 非法 baseUrl 会在连通测试结果中直接显示为失败，并返回“基础地址不合法”的具体原因；该场景不会请求上游模型。
- 新增态缺少 API Key 会明确报错。
- 编辑态未输入新 API Key 时会使用已保存真实密钥，不使用前端脱敏值。
- 上游 HTTP 401、解析失败、网络失败等会返回失败结果和原因，不会写入配置。

## 停止说明

本轮只实现 AI 引擎配置连通测试，不新增供应商管理、不自动修复配置、不保存测试历史、不扩展运行时模型路由。
