# 移除离线 STT 后端兜底路径验证

## 当前任务所属模块

后端安全配置与旧离线 STT 模型兜底路径清理验证。

## 前端文件定位

前端删除范围见 `frontend/tasks/TASK_67_REMOVE_OFFLINE_STT_FRONTEND.md`。

## 后端文件定位

- `server/src/test/java/com/airesume/server/config/SecurityConfigTest.java`
- `server/src/main/java/com/airesume/server/config/SecurityConfig.java`

## 本轮修改文件清单

- 修改 `SecurityConfigTest`，增加断言确认安全配置源码中不再公开 `/api/offline-stt`，也不存在 `supportsPublicOfflineSttModelPath`。
- 本轮未新增后端生产接口，当前后端源码扫描未发现 `OfflineStt`、`offline-stt`、`sherpa` 或 `voice-models` 生产链路。

## 前端实现方案

前端已完全移除 sherpa-onnx 离线 STT 下载、缓存和 Worker 识别链路，详见前端任务文件。

## 后端实现方案

后端不再为离线 STT 提供模型代理或公开兜底路径。本轮仅补充安全配置回归断言，防止旧的 `/api/offline-stt/models/**` 公开下载路径被重新引入。

## 数据存储方案

无数据库结构变更，无新增迁移脚本。

## stage 更新说明

`tasks/stage.md` 顶部已记录本轮后端验证范围、测试结果和停止说明。

## 编译结果

- `mvn.cmd test "-Dtest=SecurityConfigTest"` 通过，5 个用例，0 失败，0 错误。

## 构建结果

本轮未执行完整后端打包；目标安全配置测试会触发后端主源码和测试源码编译，已通过。

## 当前功能验收说明

后端不再暴露离线 STT 模型下载兜底路径。前端语音识别改为浏览器能力后，不依赖该接口。

## 停止，不继续下一个功能

本轮只验证并锁定离线 STT 后端兜底路径不再存在，不新增云端 STT、后端语音识别、模型代理或其它语音服务。
