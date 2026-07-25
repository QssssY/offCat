# TASK_61 离线 STT 模型同源兜底源后端修复

## 追加修复：后端兜底源补充非 www ModelScope 域名（2026-05-31）
### 修复原因

前端离线 STT 下载仍可能失败时，后端 `/api/offline-stt/models/<fileName>` 需要尽量提高 ModelScope 兜底命中率。复测发现 `https://modelscope.cn/api/v1/studio/.../static/` 与 `https://www.modelscope.cn/api/v1/studio/.../static/` 小文件均可返回 `200`，因此后端固定源列表补充非 `www` 域名并放在首位。

### 修复范围

- 修改：`server/src/main/java/com/airesume/server/service/OfflineSttModelProxyService.java`
- 修改：`server/src/test/java/com/airesume/server/service/OfflineSttModelProxyServiceTest.java`

### 实现说明

- 后端白名单文件名保持不变，仍只允许四个 sherpa-onnx 文件。
- `sourceUrls(fileName)` 顺序调整为 `modelscope.cn`、`www.modelscope.cn`、HuggingFace 官方 Space raw。
- 仍不开放任意 URL 代理，不落盘模型文件，不新增数据库结构。

### 验证计划

- `mvn.cmd -DskipTests compile` 通过。
- `mvn.cmd test "-Dtest=OfflineSttModelProxyServiceTest,OfflineSttModelProxyControllerTest,SecurityConfigTest"` 未能执行到目标用例，原因是当前仓库已有 `CommunityServiceModerationTest` 构造器参数与 `CommunityService` 不匹配导致 testCompile 失败，和本轮离线 STT 后端源列表改动无关。

### 停止说明

本轮只补充 TASK_61 后端模型源兜底域名，不继续推进 R2/OSS/COS 托管、后端语音识别或其它语音能力。

## 当前任务所属模块

后端离线语音识别模型下载兜底通道，仅服务前端 `TASK_61_OFFLINE_STT_REMOTE_SOURCE_DOWNLOAD_PLAN_FRONTEND.md` 中的 sherpa-onnx 离线 STT 下载。

## 前端文件定位

- `frontend/app/public/voice-models/sherpa-onnx/zh-cn-streaming/manifest.json`
- `frontend/app/src/utils/offlineVoiceModelCache.js`
- `frontend/app/src/views/settings/SettingsView.vue`

## 后端文件定位

- `server/src/main/java/com/airesume/server/controller/OfflineSttModelProxyController.java`
- `server/src/main/java/com/airesume/server/service/OfflineSttModelProxyService.java`
- `server/src/main/java/com/airesume/server/config/SecurityConfig.java`
- `server/src/test/java/com/airesume/server/controller/OfflineSttModelProxyControllerTest.java`
- `server/src/test/java/com/airesume/server/service/OfflineSttModelProxyServiceTest.java`
- `server/src/test/java/com/airesume/server/config/SecurityConfigTest.java`

## 本轮修改文件清单

- 新增：`server/src/main/java/com/airesume/server/controller/OfflineSttModelProxyController.java`
- 新增：`server/src/main/java/com/airesume/server/service/OfflineSttModelProxyService.java`
- 修改：`server/src/main/java/com/airesume/server/config/SecurityConfig.java`
- 新增：`server/src/test/java/com/airesume/server/controller/OfflineSttModelProxyControllerTest.java`
- 新增：`server/src/test/java/com/airesume/server/service/OfflineSttModelProxyServiceTest.java`
- 修改：`server/src/test/java/com/airesume/server/config/SecurityConfigTest.java`

## 修复原因

浏览器点击下载离线语音识别包时仍提示“当前模型源不可用”。复查发现 ModelScope 静态文件可由服务端请求获取，但响应没有 `access-control-allow-origin`，浏览器直接 `fetch` 会被 CORS 拦截；HuggingFace 官方源在当前网络下又可能超时。继续只增加第三方直链不能保证用户侧下载成功。

## 后端实现方案

- 新增 `GET /api/offline-stt/models/{fileName}`，作为前端 manifest 的同源兜底候选源。
- 后端只允许四个固定文件名：`sherpa-onnx-asr.js`、`sherpa-onnx-wasm-main-asr.js`、`sherpa-onnx-wasm-main-asr.wasm`、`sherpa-onnx-wasm-main-asr.data`，拒绝路径穿越和任意 URL 转发。
- 后端按固定源顺序流式读取 ModelScope Studio 静态文件，失败后再尝试 HuggingFace 官方 Space 文件。
- 响应保持流式写出，不把 200MB 模型文件落盘到后端，也不纳入前端构建产物。
- 安全配置放行 `GET /api/offline-stt/models/**`，因为浏览器 Cache API 的原生 `fetch` 不会走项目 Axios 鉴权拦截器；接口本身通过文件名白名单限制风险边界。

## 数据存储方案

无数据库结构变更。模型文件仍缓存到用户浏览器 Cache API；后端只做兜底流式转发，不持久化文件。

## 编译结果

目标后端测试已通过：

```bash
mvn.cmd test "-Dtest=OfflineSttModelProxyServiceTest,OfflineSttModelProxyControllerTest,SecurityConfigTest"
```

结果：11 个用例通过。

## 当前功能验收说明

前端直连源不可用时，manifest 会继续尝试 `/api/offline-stt/models/<fileName>` 同源兜底地址，避免浏览器 CORS 直接阻断下载。该修复只处理 sherpa-onnx 离线 STT 下载链路，不新增离线 TTS、云存储托管或任意文件代理能力。

## 停止，不继续下一个功能

本轮只修复 TASK_61 的离线 STT 模型源不可用问题，等待验收；不继续推进 R2/OSS/COS 托管、后端语音识别或其它语音能力。
