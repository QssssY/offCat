# TASK 51 管理端 AI 引擎连通测试前端

## 当前任务所属模块

管理端模块，子模块为 AI 引擎配置页面。

## 前端文件定位

- `frontend/app/src/views/admin/AdminAiEngineView.vue`
- `frontend/app/src/api/admin/aiEngines.js`
- `frontend/app/src/__tests__/api/admin.aiEngines.test.js`
- `frontend/app/src/__tests__/views/AdminAiEngineView.test.js`

## 后端文件定位

后端实现见 `tasks/TASK_51_ADMIN_AI_ENGINE_CONNECTIVITY_TEST_BACKEND.md`。

## 本轮修改文件清单

- `AdminAiEngineView.vue`：新增弹窗内“测试连通性”按钮、测试说明、成功/失败结果提示和配置变更后清空旧结果逻辑。
- `aiEngines.js`：新增 `testAdminAiEngineConnectivity` API 方法。
- 新增前端 API 测试和页面源码回归测试。

## 前端实现方案

新增和编辑弹窗复用同一个测试入口。点击“测试连通性”前先执行现有表单校验；新增态使用当前输入的 API Key，编辑态如果 API Key 留空则只提交配置 ID 和当前表单参数，由后端使用已保存真实密钥测试。测试结果在按钮附近以内联 `el-alert` 展示，包含成功/失败、耗时、返回摘要或失败原因，避免只依赖 toast。

## 后端实现方案

见后端 task 文件。前端调用 `POST /api/admin/ai-engines/connectivity-test`，不改变保存接口。

## 数据存储方案

不新增本地存储，不新增后端表字段；连通测试结果只保存在当前弹窗临时状态。

## stage 更新说明

已同步更新 `frontend/tasks/stage.md`。

## 编译结果

前端构建已通过。

## 构建结果

- `npm.cmd run build` 通过。

## 测试结果

- 前端目标测试：`npm.cmd test -- --run src/__tests__/api/admin.aiEngines.test.js src/__tests__/views/AdminAiEngineView.test.js` 通过，2 个测试通过。
- 前端完整测试：`npm.cmd test` 通过，27 个测试文件、156 个测试用例通过。

## 当前功能验收说明

- 管理端 AI 引擎新增弹窗中可以先测试当前表单配置是否可用。
- 管理端 AI 引擎编辑弹窗中可以不重新输入 API Key，直接测试已保存密钥对应配置。
- 配置字段变更后会清空旧测试结果，避免误读。
- 测试失败会在弹窗内明确展示失败原因。

## 停止说明

本轮只实现管理端 AI 引擎配置连通测试，不继续扩展供应商模板、测试历史、自动修复或批量测试能力。
