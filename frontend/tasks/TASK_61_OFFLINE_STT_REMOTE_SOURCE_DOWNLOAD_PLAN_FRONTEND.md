# 离线语音识别源站下载改造执行方案

## 追加修复：Emscripten 默认资源路径桥接（2026-05-31）
### 修复原因

用户重新下载远程源离线引擎资源后，进入模拟语音面试仍报 `WebAssembly.instantiateStreaming(): expected magic word 00 61 73 6d, found 3c 21 44 4f`。`3c 21 44 4f` 是 `<!DO`，说明 Emscripten 主运行时仍在某条默认加载路径上拿到了 SPA HTML，而不是真实 wasm。前一轮已注入 `Module.instantiateWasm`，但真实浏览器中仍可能出现主运行时绕过该钩子或继续按默认同源 `.wasm/.data` 路径读取资源的情况。

### 修复范围

- 修改：`frontend/app/public/voice-models/sherpa-onnx/zh-cn-streaming/runtime.js`
- 修改：`frontend/app/src/composables/useSpeechToText.js`
- 修改：`frontend/app/src/__tests__/utils/sherpaRuntimeAsset.test.js`
- 修改：`frontend/app/src/__tests__/composables/useSpeechToText.test.js`
- 修改：`frontend/tasks/stage.md`

### 实现说明

- `runtime.js` 在完成 `.wasm` magic word 校验并生成 Blob URL 后，导入 sherpa API wrapper 和 Emscripten 主运行时时临时桥接 `fetch` 与 `XMLHttpRequest`。
- 当 Emscripten 仍按 `/voice-models/sherpa-onnx/zh-cn-streaming/sherpa-onnx-wasm-main-asr.wasm` 或 `.data` 默认同源路径请求时，桥接层会改为读取对应的已校验 Blob URL，避免命中 SPA HTML fallback。
- 原有 `wasmBinary`、`instantiateWasm`、`onAbort` reject、MODULARIZE factory 兼容逻辑保留，桥接层只作为 Emscripten 默认资源路径的补充防护。
- `OFFLINE_STT_RUNTIME_VERSION` 升级为 `20260531-runtime-asset-bridge`，强制浏览器刷新 `public` 下不会自动带 Vite hash 的静态 runtime。

### 验证结果

- RED 阶段新增两个用例：模拟主运行时绕过 `instantiateWasm` 直接 fetch 默认 `.wasm`；模拟 `.data` 预加载通过 XHR 请求默认同源路径。旧实现分别会拿到 HTML 或同源路径，修复后均命中 Blob URL。
- `npm.cmd test -- --run src/__tests__/utils/sherpaRuntimeAsset.test.js src/__tests__/workers/sherpaSpeechWorker.test.js src/__tests__/composables/useSpeechToText.test.js src/__tests__/views/InterviewSessionView.test.js` 通过，4 个测试文件 / 89 个用例。
- `npm.cmd run build` 通过。
- `dist/voice-models/sherpa-onnx/zh-cn-streaming/` 仅包含 `manifest.json`、`runtime.js`、`runtime.js.gz`；`dist` 未发现超过 25MiB 的模型文件。

### 停止说明

本轮只修复离线 STT 前端 runtime 启动链路，不改后端代理、不改模型源 manifest、不新增离线 TTS、自有模型托管或其它语音能力。

## 追加修复：ModelScope 直连备用源恢复（2026-05-31）
### 修复原因

用户点击下载离线语音识别包后仍提示“当前模型源不可用”。复查源站发现：当前生产 manifest 只有同源 `/api/offline-stt/models/<fileName>` 与 HuggingFace 官方 Space raw 两类候选；HuggingFace 官方 raw 在当前环境对 `resolve/main/sherpa-onnx-asr.js` 返回 `401 Invalid username or password`，如果同源后端兜底未部署、未重启或后端出网失败，前端就没有可用候选源。

### 修复范围

- 修改：`frontend/app/public/voice-models/sherpa-onnx/zh-cn-streaming/manifest.json`
- 修改：`frontend/app/src/utils/offlineVoiceModelCache.js`
- 修改：`frontend/app/src/__tests__/utils/offlineVoiceModelCache.test.js`
- 修改：`frontend/tasks/artifacts/offline-stt-remote-source-check.md`
- 修改：`frontend/tasks/stage.md`

### 实现说明

- manifest 版本更新为 `sherpa-zh-en-remote-20260531-modelscope-direct-fallback`，强制旧失败状态重新读取新候选源。
- 每个模型文件的候选顺序调整为：`https://modelscope.cn/.../static/<fileName>`、`https://www.modelscope.cn/.../static/<fileName>`、`/api/offline-stt/models/<fileName>`、HuggingFace 官方 Space raw。
- 已验证 ModelScope 小文件带 `Origin` 请求返回 `200` 与 `access-control-allow-origin: *`；`.data` 范围请求返回 `206 Partial Content` 与 `Content-Range: bytes 0-0/199059238`。
- `offlineVoiceModelCache.js` 在候选源响应无效或请求失败时输出具体 `file/source/status/contentType/error`，便于后续确认到底是哪一个源失败。
- 已复查 `anyshu/sherpa-onnx-wasm-main-asr.data`，该仓库可访问但公开目录不是当前 runtime 可直接使用的完整四文件组合，本轮不加入生产 manifest，避免下载成功后运行时不兼容。

### 验证计划

- 前端：`npm.cmd test -- --run src/__tests__/utils/offlineVoiceModelCache.test.js src/__tests__/views/SettingsView.test.js` 通过，2 个测试文件 / 65 个用例。
- 前端构建：`npm.cmd run build` 通过。
- 构建产物检查：`dist` 未发现超过 25MiB 文件，`dist/voice-models/sherpa-onnx/zh-cn-streaming/manifest.json` 已包含新的 ModelScope 直连候选源。

### 停止说明

本轮只修复 TASK_61 离线 STT 模型源候选链路，不继续推进 R2/OSS/COS 托管、离线 TTS 或其它语音能力。

> **执行说明**：本文件是实施方案，不是已完成实现。后续执行时必须按 TDD 流程逐步落地：先写失败测试，再改实现，再构建验证。

## 目标

消除离线语音识别大模型资源存放在前端工程和 Cloudflare Pages 静态产物中的问题。生产环境只保留小体积的清单与运行时脚本，用户点击下载离线识别包时，浏览器直接从国内镜像源下载 sherpa-onnx 模型文件；国内镜像失败后，再回退到 HuggingFace 源站。下载完成后继续缓存到浏览器 Cache API，本项目的 2 核 2G 后端和 Cloudflare Pages 均不承载模型下载流量。

## 当前问题

当前本地模型目录 `frontend/app/voice-models-local/` 中包含大文件：

- `sherpa-onnx-wasm-main-asr.data`：约 281 MB
- `sherpa-onnx-wasm-main-asr.wasm`：约 11 MB

Cloudflare Pages 单文件上限为 25 MiB，因此 `.data` 文件不能部署到 Pages。当前 `frontend/app/public/voice-models/sherpa-onnx/zh-cn-streaming/` 只保留了小体积的 `manifest.json` 和 `runtime.js`，方向正确，但运行时仍默认从同源 `/voice-models/...` 拉取模型文件，不能满足“绕开我方服务，从源头下载”的目标。

## 架构方案

```text
Cloudflare Pages
  ├─ 前端应用
  ├─ 小体积 manifest.json
  └─ 小体积 runtime.js

用户浏览器
  ├─ 点击下载离线识别包
  ├─ 按 manifest 中的候选 URL 顺序下载
  ├─ 先试国内镜像源
  ├─ 失败后回退 HuggingFace 源站
  └─ 下载成功后写入 Cache API

模型文件源
  ├─ 国内镜像源 1：hf.qhduan.com
  ├─ 国内镜像源 2：hf-mirror.com
  └─ 源站兜底：huggingface.co
```

## 推荐模型源候选

优先使用 k2-fsa 官方 WebAssembly ASR Space 的资源路径。后续实施前必须用浏览器或脚本验证这些 URL 的 CORS、文件存在性和响应类型。

基础路径候选：

```text
https://hf.qhduan.com/spaces/k2-fsa/web-assembly-asr-sherpa-onnx-zh-en/resolve/main/
https://hf-mirror.com/spaces/k2-fsa/web-assembly-asr-sherpa-onnx-zh-en/resolve/main/
https://huggingface.co/spaces/k2-fsa/web-assembly-asr-sherpa-onnx-zh-en/resolve/main/
```

文件名候选：

```text
sherpa-onnx-asr.js
sherpa-onnx-wasm-main-asr.js
sherpa-onnx-wasm-main-asr.wasm
sherpa-onnx-wasm-main-asr.data
```

如果上述官方 Space 文件路径验证失败，则使用当前项目已有的 `voice-models-local/sherpa-onnx/zh-cn-streaming/` 文件名与版本，自行寻找可跨域直链镜像，不改业务调用方式。

## 文件改造范围

### 需要修改

- `frontend/app/public/voice-models/sherpa-onnx/zh-cn-streaming/manifest.json`
  - 从同源文件清单改为远程候选源清单。

- `frontend/app/src/utils/offlineVoiceModelCache.js`
  - 支持每个模型文件配置多个候选 URL。
  - 下载时按候选 URL 顺序尝试。
  - 缓存成功后记录实际命中的 URL，供运行时加载。

- `frontend/app/public/voice-models/sherpa-onnx/zh-cn-streaming/runtime.js`
  - 去掉固定同源模型地址依赖。
  - 从 Worker 注入的文件列表读取已命中的模型 URL。
  - 继续优先从 Cache API 读取，不命中时再 fetch 源站。

- `frontend/app/src/workers/sherpaSpeechWorker.js`
  - 在 `importScripts(runtimeUrl)` 前，把模型文件 URL 列表注入到 Worker 全局变量。

- `frontend/app/src/composables/useSpeechToText.js`
  - 保持下载入口不变。
  - 确保 `buildOfflineWorkerConfig()` 传入的是缓存状态里实际命中的文件 URL。

- `frontend/app/src/views/settings/SettingsView.vue`
  - 下载文案明确说明“约 300MB，直接从模型源下载，不占用本站服务器流量”。
  - 下载失败时提示“国内镜像和源站均不可用，请稍后重试”。

### 需要删除或排除

- `frontend/app/voice-models-local/kokoro/`
- `frontend/app/voice-models-local/sherpa-onnx/zh-cn-streaming/sherpa-onnx-wasm-main-asr.data`
- `frontend/app/voice-models-local/sherpa-onnx/zh-cn-streaming/sherpa-onnx-wasm-main-asr.wasm`
- `frontend/app/.tmp-sherpa-pack/`

删除前先确认这些路径未被构建脚本依赖。删除操作必须单独执行并确认 `npm run build` 后 `frontend/app/dist` 不包含大模型。

## 任务拆分

### Task 1：扩展模型清单格式

**文件：**

- 修改：`frontend/app/public/voice-models/sherpa-onnx/zh-cn-streaming/manifest.json`
- 测试：`frontend/app/src/__tests__/utils/offlineVoiceModelCache.test.js`

**目标清单格式：**

```json
{
  "version": "sherpa-zh-en-remote-20260531",
  "runtime": "runtime.js",
  "files": [
    {
      "path": "sherpa-onnx-asr.js",
      "size": 43606,
      "urls": [
        "https://hf.qhduan.com/spaces/k2-fsa/web-assembly-asr-sherpa-onnx-zh-en/resolve/main/sherpa-onnx-asr.js",
        "https://hf-mirror.com/spaces/k2-fsa/web-assembly-asr-sherpa-onnx-zh-en/resolve/main/sherpa-onnx-asr.js",
        "https://huggingface.co/spaces/k2-fsa/web-assembly-asr-sherpa-onnx-zh-en/resolve/main/sherpa-onnx-asr.js"
      ]
    },
    {
      "path": "sherpa-onnx-wasm-main-asr.js",
      "size": 100495,
      "urls": [
        "https://hf.qhduan.com/spaces/k2-fsa/web-assembly-asr-sherpa-onnx-zh-en/resolve/main/sherpa-onnx-wasm-main-asr.js",
        "https://hf-mirror.com/spaces/k2-fsa/web-assembly-asr-sherpa-onnx-zh-en/resolve/main/sherpa-onnx-wasm-main-asr.js",
        "https://huggingface.co/spaces/k2-fsa/web-assembly-asr-sherpa-onnx-zh-en/resolve/main/sherpa-onnx-wasm-main-asr.js"
      ]
    },
    {
      "path": "sherpa-onnx-wasm-main-asr.wasm",
      "size": 11861462,
      "urls": [
        "https://hf.qhduan.com/spaces/k2-fsa/web-assembly-asr-sherpa-onnx-zh-en/resolve/main/sherpa-onnx-wasm-main-asr.wasm",
        "https://hf-mirror.com/spaces/k2-fsa/web-assembly-asr-sherpa-onnx-zh-en/resolve/main/sherpa-onnx-wasm-main-asr.wasm",
        "https://huggingface.co/spaces/k2-fsa/web-assembly-asr-sherpa-onnx-zh-en/resolve/main/sherpa-onnx-wasm-main-asr.wasm"
      ]
    },
    {
      "path": "sherpa-onnx-wasm-main-asr.data",
      "size": 281105165,
      "urls": [
        "https://hf.qhduan.com/spaces/k2-fsa/web-assembly-asr-sherpa-onnx-zh-en/resolve/main/sherpa-onnx-wasm-main-asr.data",
        "https://hf-mirror.com/spaces/k2-fsa/web-assembly-asr-sherpa-onnx-zh-en/resolve/main/sherpa-onnx-wasm-main-asr.data",
        "https://huggingface.co/spaces/k2-fsa/web-assembly-asr-sherpa-onnx-zh-en/resolve/main/sherpa-onnx-wasm-main-asr.data"
      ]
    }
  ]
}
```

**测试要求：**

- 新增测试：`readModelManifest` 能解析 `urls` 数组。
- 新增测试：兼容旧格式 `url` 和同源 `path`。
- 新增测试：当 `urls` 为空且没有 `url` 时，回退到原来的同源路径拼接。

**执行命令：**

```bash
cd frontend/app
npm run test -- src/__tests__/utils/offlineVoiceModelCache.test.js
```

预期：先失败，提示 `urls` 字段未被保留；实现后通过。

### Task 2：实现候选源顺序下载与命中源持久化

**文件：**

- 修改：`frontend/app/src/utils/offlineVoiceModelCache.js`
- 测试：`frontend/app/src/__tests__/utils/offlineVoiceModelCache.test.js`

**核心实现要求：**

新增内部函数：

```js
const resolveCandidateUrls = (file) => {
  const candidates = Array.isArray(file.urls) ? file.urls : []
  const normalizedCandidates = candidates
    .filter((url) => typeof url === 'string' && url.trim())
    .map((url) => url.trim())
  if (typeof file.url === 'string' && file.url.trim()) {
    normalizedCandidates.push(file.url.trim())
  }
  return [...new Set(normalizedCandidates)]
}
```

新增内部函数：

```js
const fetchFirstAvailableModelFile = async (file) => {
  const candidates = resolveCandidateUrls(file)
  let lastError = null

  for (const candidateUrl of candidates) {
    try {
      const response = await fetch(candidateUrl)
      if (response.ok && !(await isHtmlResponse(response))) {
        return { response, selectedUrl: candidateUrl }
      }
      lastError = new Error(`模型源响应无效: ${candidateUrl}`)
    } catch (error) {
      lastError = error
    }
  }

  throw new Error(lastError?.message || `离线语音识别模型文件请求失败: ${file.path}`)
}
```

下载成功后，写入状态的 `files` 必须使用命中的 `selectedUrl`：

```js
downloadedFiles.push({
  ...file,
  url: selectedUrl
})
```

**测试要求：**

- 国内第一个源失败时，自动尝试第二个源。
- 国内镜像全部失败时，尝试 HuggingFace 源站。
- 所有候选源失败时，状态变为 `failed`。
- 成功后 `getOfflineVoiceModelStatus(...).files[n].url` 等于实际命中的 URL。
- `cache.put` 使用实际命中的 URL，保证 runtime 后续能从同一 key 读取缓存。

**执行命令：**

```bash
cd frontend/app
npm run test -- src/__tests__/utils/offlineVoiceModelCache.test.js
```

预期：新增测试全部通过，旧的同源 manifest 测试仍通过。

### Task 3：让 runtime 使用 Worker 注入的模型文件 URL

**文件：**

- 修改：`frontend/app/src/workers/sherpaSpeechWorker.js`
- 修改：`frontend/app/public/voice-models/sherpa-onnx/zh-cn-streaming/runtime.js`
- 测试：`frontend/app/src/__tests__/workers/sherpaSpeechWorker.test.js`
- 测试：`frontend/app/src/__tests__/utils/sherpaRuntimeAsset.test.js`

**Worker 注入要求：**

在 `importScripts(runtimeUrl)` 前注入：

```js
self.__AI_RESUME_SHERPA_MODEL_FILES = Array.isArray(config?.files)
  ? config.files.reduce((map, file) => {
    if (file?.path && file?.url) {
      map[file.path] = file.url
    }
    return map
  }, {})
  : {}
```

`loadRuntime` 调用改为：

```js
await loadRuntime(runtimeUrl, config)
```

**runtime 读取要求：**

将原固定同源文件映射：

```js
const MODEL_BASE_URL = '/voice-models/sherpa-onnx/zh-cn-streaming/'
```

改为：

```js
const DEFAULT_MODEL_BASE_URL = '/voice-models/sherpa-onnx/zh-cn-streaming/'
const REMOTE_MODEL_FILES = self.__AI_RESUME_SHERPA_MODEL_FILES || {}
const MODEL_FILES = Object.freeze({
  'sherpa-onnx-asr.js': REMOTE_MODEL_FILES['sherpa-onnx-asr.js'] || `${DEFAULT_MODEL_BASE_URL}sherpa-onnx-asr.js`,
  'sherpa-onnx-wasm-main-asr.js': REMOTE_MODEL_FILES['sherpa-onnx-wasm-main-asr.js'] || `${DEFAULT_MODEL_BASE_URL}sherpa-onnx-wasm-main-asr.js`,
  'sherpa-onnx-wasm-main-asr.wasm': REMOTE_MODEL_FILES['sherpa-onnx-wasm-main-asr.wasm'] || `${DEFAULT_MODEL_BASE_URL}sherpa-onnx-wasm-main-asr.wasm`,
  'sherpa-onnx-wasm-main-asr.data': REMOTE_MODEL_FILES['sherpa-onnx-wasm-main-asr.data'] || `${DEFAULT_MODEL_BASE_URL}sherpa-onnx-wasm-main-asr.data`
})
```

**测试要求：**

- Worker 初始化时把 `config.files` 注入全局变量。
- runtime 仍按 `sherpa-onnx-asr.js` 先于 `sherpa-onnx-wasm-main-asr.js` 的顺序加载。
- runtime 使用远程命中 URL 查 Cache API。
- 当状态里没有远程 URL 时，runtime 仍回退同源路径，保证开发环境可用。

**执行命令：**

```bash
cd frontend/app
npm run test -- src/__tests__/workers/sherpaSpeechWorker.test.js src/__tests__/utils/sherpaRuntimeAsset.test.js
```

预期：全部通过。

### Task 4：调整设置页下载提示与失败文案

**文件：**

- 修改：`frontend/app/src/views/settings/SettingsView.vue`
- 测试：`frontend/app/src/__tests__/views/SettingsView.test.js`

**文案要求：**

离线识别包说明改为：

```text
离线语音识别包约 300MB，将由浏览器直接从模型镜像源下载并缓存到本机，不占用本站服务器流量。
```

下载失败文案改为：

```text
离线语音识别包下载失败：国内镜像和源站均不可用，请稍后重试或切换网络。
```

**测试要求：**

- 设置页能显示“约 300MB”。
- 设置页能显示“模型镜像源”。
- 下载失败时展示新的失败文案。
- 下载成功后仍保存 `voiceRecognitionEngine: 'offline_sherpa'`。

**执行命令：**

```bash
cd frontend/app
npm run test -- src/__tests__/views/SettingsView.test.js
```

预期：全部通过。

### Task 5：清理前端本地大模型资源

**文件：**

- 删除：`frontend/app/voice-models-local/kokoro/`
- 删除：`frontend/app/voice-models-local/sherpa-onnx/zh-cn-streaming/sherpa-onnx-wasm-main-asr.data`
- 删除：`frontend/app/voice-models-local/sherpa-onnx/zh-cn-streaming/sherpa-onnx-wasm-main-asr.wasm`
- 删除：`frontend/app/.tmp-sherpa-pack/`

**保留：**

- `frontend/app/public/voice-models/sherpa-onnx/zh-cn-streaming/manifest.json`
- `frontend/app/public/voice-models/sherpa-onnx/zh-cn-streaming/runtime.js`
- `frontend/app/public/audio-worklets/offline-stt-processor.js`

**验证命令：**

```bash
cd frontend/app
npm run build
```

构建后检查最大文件：

```powershell
Get-ChildItem -Recurse F:\Code\ai-resume\frontend\app\dist |
  Sort-Object Length -Descending |
  Select-Object -First 20 Length,FullName
```

预期：

- `dist` 中不存在 25 MiB 以上文件。
- `dist/voice-models/sherpa-onnx/zh-cn-streaming/` 只包含小体积 `manifest.json` 和 `runtime.js`。
- `dist` 中不存在 `kokoro` 离线音色目录。

### Task 6：源站可用性与 CORS 验证

**文件：**

- 新增：`frontend/tasks/artifacts/offline-stt-remote-source-check.md`

**验证方式：**

在浏览器控制台或 Playwright 环境执行：

```js
await fetch('https://hf.qhduan.com/spaces/k2-fsa/web-assembly-asr-sherpa-onnx-zh-en/resolve/main/sherpa-onnx-wasm-main-asr.data', {
  method: 'GET',
  cache: 'no-cache'
}).then((response) => ({
  ok: response.ok,
  status: response.status,
  type: response.type,
  contentType: response.headers.get('content-type'),
  cors: response.headers.get('access-control-allow-origin')
}))
```

依次验证：

```text
https://hf.qhduan.com/spaces/k2-fsa/web-assembly-asr-sherpa-onnx-zh-en/resolve/main/
https://hf-mirror.com/spaces/k2-fsa/web-assembly-asr-sherpa-onnx-zh-en/resolve/main/
https://huggingface.co/spaces/k2-fsa/web-assembly-asr-sherpa-onnx-zh-en/resolve/main/
```

验收标准：

- 至少一个国内镜像源 `GET` 成功。
- HuggingFace 源站作为兜底 `GET` 成功。
- `.wasm` 响应能被浏览器 fetch。
- `.data` 响应不是 HTML。
- fetch 不因 CORS 被浏览器拦截。

如果国内镜像源 CORS 不可用，则保留 HuggingFace 源站兜底，并在设置页提示“国内镜像不可用时可能下载较慢”。

## 全量验证

执行：

```bash
cd frontend/app
npm run test
npm run build
```

构建体积检查：

```powershell
Get-ChildItem -Recurse F:\Code\ai-resume\frontend\app\dist |
  Where-Object { $_.Length -gt 25MB } |
  Select-Object Length,FullName
```

预期无输出。

手动验证：

1. 部署前端到 Cloudflare Pages。
2. 打开设置页。
3. 点击下载离线语音识别包。
4. 浏览器 Network 面板显示模型请求直接访问镜像源或 HuggingFace。
5. 后端服务日志无模型下载请求。
6. 下载完成后刷新页面，离线识别状态仍为 ready。
7. 断网后进行离线语音识别，确认能使用已缓存模型。

## 风险与处理

| 风险 | 处理 |
|---|---|
| 国内镜像 CORS 不允许 fetch | 保留 HuggingFace 源站兜底；后续再切换到 R2/OSS |
| 镜像源文件版本和当前 runtime 不匹配 | manifest 中 `version` 每次变更时强制重新下载；不要混用不同来源的 runtime 和 `.data` |
| 用户首次下载 300MB 体验慢 | 设置页明确提示体积；不自动下载，只允许用户手动触发 |
| Cache API 空间不足 | 下载失败时提示清理浏览器缓存；保留清理离线模型按钮 |
| 源站删除文件 | manifest 增加新候选源；不需要改后端 |

## 回滚方案

如果远程源稳定性无法接受：

1. 保留当前 `system_local` 浏览器识别作为默认方案。
2. 临时隐藏离线 sherpa 下载入口。
3. 不把大模型恢复到 Pages。
4. 后续改用 R2、阿里云 OSS 或腾讯云 COS 承载模型文件。

## 验收结论

完成后应满足：

- Cloudflare Pages 只部署小体积前端文件。
- 前端仓库不再保留 Kokoro 离线音色资源。
- 前端构建产物不包含超过 25 MiB 的模型文件。
- 用户下载离线识别包时直接访问模型源站。
- 国内镜像失败时自动回退到 HuggingFace 源站。
- 2 核 2G 后端不承担任何模型下载流量。

## 执行结果（2026-05-31）

### 当前任务所属模块

前端设置中心离线增强 / sherpa-onnx 离线语音识别下载与 Worker 运行时加载链路。

### 前端文件定位

- `frontend/app/public/voice-models/sherpa-onnx/zh-cn-streaming/manifest.json`
- `frontend/app/public/voice-models/sherpa-onnx/zh-cn-streaming/runtime.js`
- `frontend/app/src/utils/offlineVoiceModelCache.js`
- `frontend/app/src/workers/sherpaSpeechWorker.js`
- `frontend/app/src/composables/useSpeechToText.js`
- `frontend/app/src/views/settings/SettingsView.vue`

### 后端文件定位

本轮无后端改动；模型下载不经过 Java 后端。

### 本轮修改文件清单

- 修改：`frontend/app/public/voice-models/sherpa-onnx/zh-cn-streaming/manifest.json`
- 修改：`frontend/app/public/voice-models/sherpa-onnx/zh-cn-streaming/runtime.js`
- 修改：`frontend/app/src/utils/offlineVoiceModelCache.js`
- 修改：`frontend/app/src/workers/sherpaSpeechWorker.js`
- 修改：`frontend/app/src/views/settings/SettingsView.vue`
- 修改：`frontend/app/src/__tests__/utils/offlineVoiceModelCache.test.js`
- 修改：`frontend/app/src/__tests__/utils/sherpaRuntimeAsset.test.js`
- 修改：`frontend/app/src/__tests__/workers/sherpaSpeechWorker.test.js`
- 修改：`frontend/app/src/__tests__/views/SettingsView.test.js`
- 新增：`frontend/tasks/artifacts/offline-stt-remote-source-check.md`
- 删除本地忽略资源：`frontend/app/voice-models-local/sherpa-onnx/zh-cn-streaming/sherpa-onnx-wasm-main-asr.data`
- 删除本地忽略资源：`frontend/app/voice-models-local/sherpa-onnx/zh-cn-streaming/sherpa-onnx-wasm-main-asr.wasm`
- 删除本地忽略目录：`frontend/app/.tmp-sherpa-pack/`

### 前端实现方案

- `manifest.json` 已改为远程候选源清单，保留 `runtime.js` 为同源小文件。
- `offlineVoiceModelCache.js` 已支持 `urls` 候选源数组，按候选源顺序下载；成功后把实际命中的 `selectedUrl` 写入状态并作为 Cache API key。
- `sherpaSpeechWorker.js` 在 `importScripts(runtimeUrl)` 前将状态中的 `config.files` 转为 `self.__AI_RESUME_SHERPA_MODEL_FILES`，让 runtime 能读取实际命中的远程 URL。
- `runtime.js` 改为优先使用 Worker 注入的远程 URL；没有命中 URL 时仍回退同源路径，保留开发兼容性。
- `SettingsView.vue` 已提示离线识别包约 300MB、由浏览器直接从模型镜像源下载、不占用本站服务器流量，并补充国内镜像不可用时可能较慢；下载失败统一提示国内镜像和源站均不可用。

### 后端实现方案

无后端实现。本轮明确保持后端不承载模型下载流量。

### 数据存储方案

无数据库结构变更。浏览器端仍使用 `localStorage` 保存轻量状态，使用 Cache API 保存模型文件；状态中的 `files[n].url` 记录实际命中的远程源 URL。

### stage 更新说明

已在 `frontend/tasks/stage.md` 顶部补充本轮离线 STT 远程源下载改造状态、验证结果和停止说明。

### 编译结果

本轮为前端改动，无后端编译。

### 构建结果

- `npm.cmd run build` 通过。
- `dist/voice-models/sherpa-onnx/zh-cn-streaming/` 仅包含 `manifest.json` 和 `runtime.js`。
- `Get-ChildItem -Recurse F:\Code\ai-resume\frontend\app\dist | Where-Object { $_.Length -gt 25MB } | Select-Object Length,FullName` 无输出。
- `dist` 中未发现 `kokoro`、`sherpa-onnx-wasm-main-asr.data` 或 `sherpa-onnx-wasm-main-asr.wasm`。

### 测试结果

- RED 阶段：新增测试先失败于 `urls` 未保留、候选源不回退、Worker 未注入模型 URL、runtime 固定同源、设置页缺少新文案。
- GREEN 阶段：`npm.cmd run test -- --run src/__tests__/utils/offlineVoiceModelCache.test.js src/__tests__/workers/sherpaSpeechWorker.test.js src/__tests__/utils/sherpaRuntimeAsset.test.js src/__tests__/views/SettingsView.test.js` 通过，4 个测试文件 / 71 个用例。
- 全量测试：`npm.cmd run test` 通过，72 个测试文件 / 511 个用例。

### 源站验证结果

- 已新增验证记录：`frontend/tasks/artifacts/offline-stt-remote-source-check.md`。
- `hf.qhduan.com` 当前返回 `401`。
- `hf-mirror.com` 当前返回 `404`。
- HuggingFace 官方 Space 页面可访问并列出目标文件；当前本机 Node fetch 到 `huggingface.co` 超时，仍需上线前用真实浏览器 Network 面板做 CORS 与大文件下载人工验证。

### 当前功能验收说明

- Cloudflare Pages 构建产物只保留小体积 manifest 与 runtime。
- 下载逻辑会按候选源顺序尝试，并把成功命中的 URL 持久化到状态与 Cache API。
- Worker/runtime 使用同一命中 URL 查缓存，避免下载和运行时读取使用不同 key。
- 前端设置页明确说明下载流量不经过本站服务器。
- 本轮不新增后端代理、不新增云存储、不恢复 Kokoro 或离线 TTS。

### 停止，不继续下一个功能

本轮只完成离线 STT 远程源下载改造与本地大模型清理，等待验收；不继续推进 R2/OSS/COS 托管、后端代理或其它语音能力。

## 追加修复：移除已知不可用镜像并补充官方备用源（2026-05-31）

### 修复原因

源站验证发现 `hf.qhduan.com` 当前返回 `401`，`hf-mirror.com` 当前返回 `404`。这两个源继续保留在生产 manifest 中会导致每个模型文件先经历确定失败的请求，增加用户首次下载耗时，也会让设置页“国内镜像”提示和真实风险不一致。

### 修复范围

- 修改：`frontend/app/public/voice-models/sherpa-onnx/zh-cn-streaming/manifest.json`
- 修改：`frontend/app/src/views/settings/SettingsView.vue`
- 修改：`frontend/app/src/__tests__/utils/offlineVoiceModelCache.test.js`
- 修改：`frontend/app/src/__tests__/views/SettingsView.test.js`
- 修改：`frontend/tasks/artifacts/offline-stt-remote-source-check.md`
- 修改：`frontend/tasks/stage.md`

### 实现说明

- `manifest.json` 已移除当前明确失败的 `hf.qhduan.com` 和 `hf-mirror.com`。
- `manifest.json` 当前候选源顺序调整为：
  - `https://www.modelscope.cn/api/v1/studio/k2-fsa/web-assembly-asr-sherpa-onnx-zh-en/static/`
  - `https://huggingface.co/spaces/k2-fsa/web-assembly-asr-sherpa-onnx-zh-en/resolve/main/`
- ModelScope Studio 来源于 k2-fsa 官方文档对 HuggingFace 不可访问时镜像的说明；当前工具环境对 ModelScope 静态文件 HEAD 验证超时，仍需上线前真实浏览器验证 CORS 和大文件下载。
- 设置页文案改为“已配置模型源”和“官方源较慢时可尝试切换网络”，下载失败提示改为“当前模型源不可用”，不再承诺国内镜像可用。

### 测试结果

- RED 阶段：`npm.cmd run test -- --run src/__tests__/utils/offlineVoiceModelCache.test.js src/__tests__/views/SettingsView.test.js` 失败于生产 manifest 仍包含 `hf.qhduan.com` / `hf-mirror.com`、设置页仍提示“模型镜像源”和旧失败文案。
- GREEN 阶段：同命令通过，2 个测试文件 / 63 个用例通过。
- 相关回归：`npm.cmd run test -- --run src/__tests__/utils/offlineVoiceModelCache.test.js src/__tests__/workers/sherpaSpeechWorker.test.js src/__tests__/utils/sherpaRuntimeAsset.test.js src/__tests__/views/SettingsView.test.js` 通过，4 个测试文件 / 72 个用例通过。
- 构建验证：`npm.cmd run build` 通过；`dist` 中无超过 25MiB 文件，`dist/voice-models/sherpa-onnx/zh-cn-streaming/` 只包含 `manifest.json` 和 `runtime.js`。

### 当前功能验收说明

本轮只修复离线 STT 模型源候选风险；不新增后端代理、不新增云存储、不恢复离线 TTS。上线前仍必须在目标浏览器 Network 面板验证 ModelScope 与 HuggingFace 的 `.js`、`.wasm`、`.data` 下载、CORS 和 Cache API 写入。

### 停止，不继续下一个功能

本轮只处理已知失败源站移除与官方备用源补充，等待验收；不继续推进 R2/OSS/COS 托管、后端代理或其它语音能力。
## 追加修复：浏览器 CORS 失败后增加同源兜底模型源（2026-05-31）

### 修复原因

用户点击下载离线语音识别包后仍提示“离线语音识别包下载失败：当前模型源不可用，请稍后重试或切换网络。”复查发现 ModelScope 静态文件在服务端/Node 环境可以读取，但响应未返回 `access-control-allow-origin`，浏览器直接 `fetch` 会被 CORS 拦截；HuggingFace 官方源在当前网络下又可能超时。因此只继续增加第三方直链仍无法保证真实浏览器可下载。

### 修复范围

- 修改：`frontend/app/public/voice-models/sherpa-onnx/zh-cn-streaming/manifest.json`
- 修改：`frontend/app/src/views/settings/SettingsView.vue`
- 修改：`frontend/app/src/__tests__/utils/offlineVoiceModelCache.test.js`
- 修改：`frontend/app/src/__tests__/views/SettingsView.test.js`
- 修改：`frontend/app/vite.config.js`
- 修改：`frontend/tasks/stage.md`
- 修改：`frontend/tasks/artifacts/offline-stt-remote-source-check.md`
- 新增后端兜底记录：`tasks/TASK_61_OFFLINE_STT_MODEL_PROXY_BACKEND.md`

### 前端实现说明

- `manifest.json` 版本更新为 `sherpa-zh-en-remote-20260531-api-fallback`。
- 生产 manifest 的候选源顺序改为同源 `/api/offline-stt/models/<fileName>` 兜底地址加 HuggingFace 官方 Space 地址；移除浏览器已确认会被 CORS 拦截的 ModelScope 直连静态地址。
- manifest 文件大小更新为当前 ModelScope/HuggingFace 官方 Space 文件大小：`53867`、`93043`、`11186321`、`199059238`。
- 设置页文案改为约 `200MB`，并明确外部源不可用时会使用本站同源兜底通道，不再声明“不占用本站服务器流量”。
- Vite 开发代理 `/api` 超时时间从 120 秒调整为 600 秒，避免本地开发时 200MB 流式模型下载被代理提前断开。

### 后端配套说明

新增 `GET /api/offline-stt/models/{fileName}` 只读流式兜底接口，后端仅代理四个白名单 sherpa-onnx 文件名，不接受任意 URL 或任意路径。详见 `tasks/TASK_61_OFFLINE_STT_MODEL_PROXY_BACKEND.md`。

### 测试结果

- 前端目标回归：`npm.cmd test -- --run src/__tests__/utils/offlineVoiceModelCache.test.js src/__tests__/views/SettingsView.test.js` 通过，2 个测试文件 / 64 个用例。
- 前端相关回归：`npm.cmd test -- --run src/__tests__/utils/offlineVoiceModelCache.test.js src/__tests__/workers/sherpaSpeechWorker.test.js src/__tests__/utils/sherpaRuntimeAsset.test.js src/__tests__/views/SettingsView.test.js src/__tests__/composables/useSpeechToText.test.js` 通过，5 个测试文件 / 113 个用例。
- 前端构建：`npm.cmd run build` 通过，`dist/voice-models/sherpa-onnx/zh-cn-streaming/` 仅包含 `manifest.json` 与 `runtime.js`，未发现超过 25MiB 的构建产物。
- 后端目标回归：`mvn.cmd test "-Dtest=OfflineSttModelProxyServiceTest,OfflineSttModelProxyControllerTest,SecurityConfigTest"` 通过，11 个用例。

### 当前功能验收说明

点击下载时，浏览器会优先尝试同源兜底通道，规避 ModelScope CORS 问题；如果后端同源兜底不可用，仍会继续尝试 HuggingFace 官方源。该修复只处理离线 STT 下载源不可用问题，不恢复离线 TTS，不新增云存储托管，不开放任意文件代理。

### 停止，不继续下一个功能

本轮只处理 TASK_61 下载失败兜底源修复，等待验收；不继续推进 R2/OSS/COS 托管或其它语音能力。

## 追加修复：避免大模型响应被整包按文本读取（2026-05-31）

### 修复原因

用户再次反馈点击下载仍提示“离线语音识别包下载失败：当前模型源不可用，请稍后重试或切换网络。”且控制台无报错。复查前端下载器发现 `isHtmlResponse()` 为了识别 SPA HTML fallback，会在非 `text/html` 响应上调用 `response.clone().text()`。对 `sherpa-onnx-wasm-main-asr.data` 这类约 200MB 的 `application/octet-stream` 二进制模型文件，该逻辑会把整包按文本解码，可能导致浏览器内存、解码或流读取失败，最终被设置页统一 catch 成“当前模型源不可用”。

### 修复范围

- 修改：`frontend/app/src/utils/offlineVoiceModelCache.js`
- 修改：`frontend/app/src/views/settings/SettingsView.vue`
- 修改：`frontend/app/src/__tests__/utils/offlineVoiceModelCache.test.js`
- 修改：`frontend/app/src/__tests__/views/SettingsView.test.js`
- 修改：`frontend/tasks/stage.md`

### 前端实现方案

- `isHtmlResponse()` 增加模型二进制 Content-Type 白名单：`application/octet-stream`、`application/wasm`、`application/javascript`、`text/javascript` 直接视为非 HTML，不再读取响应体。
- 对未知小响应只嗅探开头 `1KB`，继续保留缺失静态文件时的 SPA HTML fallback 检测。
- 对 `content-length` 超过 `1MB` 的未知响应直接跳过文本嗅探，避免再次整包读取大文件。
- 设置页下载失败 catch 中增加 `console.warn('离线语音识别包下载失败', err)`，后续如果 `/api` 未部署、后端返回 401/404/502 或外部源超时，浏览器控制台能看到真实错误对象，不再完全静默。

### 后端实现方案

本轮无后端代码改动。后端同源兜底接口仍沿用 `GET /api/offline-stt/models/{fileName}` 的白名单流式转发方案。

### 数据存储方案

无数据库结构变更。浏览器仍通过 Cache API 保存模型文件，通过 localStorage 保存轻量状态。

### 编译结果

- 后端兜底接口相关测试：`mvn.cmd test "-Dtest=OfflineSttModelProxyServiceTest,OfflineSttModelProxyControllerTest,SecurityConfigTest"` 通过，11 个用例。

### 构建结果

- `npm.cmd run build` 通过。
- `dist` 中无超过 25MiB 文件。
- `dist/voice-models/sherpa-onnx/zh-cn-streaming/` 仅包含 `manifest.json` 和 `runtime.js`。

### 测试结果

- RED 阶段：新增 `does not read a large binary model response as text while validating sources` 用例，当前实现失败于 `large binary text read`，证明下载器会误读大二进制响应。
- GREEN 阶段：`npm.cmd test -- --run src/__tests__/utils/offlineVoiceModelCache.test.js` 通过，23 个用例。
- 设置页 RED 阶段：新增控制台错误可见性断言，当前实现失败于 `console.warn` 未调用。
- 设置页 GREEN 阶段：`npm.cmd test -- --run src/__tests__/views/SettingsView.test.js` 通过，42 个用例。
- 相关回归：`npm.cmd test -- --run src/__tests__/utils/offlineVoiceModelCache.test.js src/__tests__/workers/sherpaSpeechWorker.test.js src/__tests__/utils/sherpaRuntimeAsset.test.js src/__tests__/views/SettingsView.test.js src/__tests__/composables/useSpeechToText.test.js` 通过，5 个测试文件 / 114 个用例。

### 当前功能验收说明

本轮修复的是浏览器下载阶段的隐藏失败点：模型源返回真实二进制文件时，前端不会再为了 HTML 检测把 `.data` 大文件整包按文本读取。若下载仍失败，设置页会保留用户友好提示，同时控制台会输出真实错误，便于确认是 `/api` 路由未部署、后端未重启、后端出网失败，还是 HuggingFace 兜底不可达。

### 停止，不继续下一个功能

本轮只处理 TASK_61 离线 STT 模型包下载失败的前端大文件校验问题，不继续推进 R2/OSS/COS 托管、离线 TTS 或其它语音能力。

## 追加修复：支持显式配置模型源覆盖（可选）（2026-05-31）

### 修复原因

用户明确反馈模型文件太重，因此不能把 `sherpa-onnx` 大文件放进仓库或前端构建产物，需要继续保留“从网上拉取并写入浏览器 Cache API”的方案。复查现有实现发现下载器已有 ModelScope、后端代理和 HuggingFace 候选源；为应对未来公共第三方源受网络、CORS、限流和出网环境影响的场景，保留一个显式配置模型源的可选覆盖入口，但默认仍走生产 manifest 中的公共镜像候选。

### 修复范围

- 修改：`frontend/app/src/utils/offlineVoiceModelCache.js`
- 修改：`frontend/app/src/__tests__/utils/offlineVoiceModelCache.test.js`
- 修改：`frontend/tasks/TASK_61_OFFLINE_STT_REMOTE_SOURCE_DOWNLOAD_PLAN_FRONTEND.md`
- 修改：`frontend/tasks/stage.md`

### 前端实现方案

- `offlineVoiceModelCache.js` 新增远程模型根地址归一化逻辑，支持通过 `VITE_OFFLINE_STT_MODEL_BASE_URL` 显式覆盖默认源。
- 同时支持运行时全局变量 `window.__AI_RESUME_OFFLINE_STT_MODEL_BASE_URL__`，便于静态部署场景在 `index.html` 或外部配置脚本中覆盖模型源。
- 读取 manifest 后，只有显式配置该根地址时，才会把模型文件 `path` 拼接成第一候选 URL；未配置时保持 ModelScope、`/api/offline-stt/models/**` 和 HuggingFace 的公共候选顺序。
- 该方案不把 `.wasm` / `.data` 大模型写入仓库，不增加构建产物体积。如未来确实需要覆盖公共源，可把四个模型文件放到同一个线上目录，例如：

```text
https://cdn.example.com/ai-resume/sherpa/sherpa-onnx-asr.js
https://cdn.example.com/ai-resume/sherpa/sherpa-onnx-wasm-main-asr.js
https://cdn.example.com/ai-resume/sherpa/sherpa-onnx-wasm-main-asr.wasm
https://cdn.example.com/ai-resume/sherpa/sherpa-onnx-wasm-main-asr.data
```

并配置：

```text
VITE_OFFLINE_STT_MODEL_BASE_URL=https://cdn.example.com/ai-resume/sherpa/
```

### 后端实现方案

本轮无后端代码改动。后端同源兜底接口仍作为公共源不可用时的兜底通道。

### 数据存储方案

无数据库结构变更。命中的线上 URL 会作为 Cache API key 写入浏览器缓存，状态仍记录在 localStorage。

### 测试结果

- RED 阶段：新增 `prepends configured remote model base url before public fallback sources` 用例，旧实现失败于第一候选源仍为 ModelScope。
- GREEN 阶段：`npm.cmd test -- --run src/__tests__/utils/offlineVoiceModelCache.test.js` 通过，24 个用例。
- 相关回归：`npm.cmd test -- --run src/__tests__/utils/offlineVoiceModelCache.test.js src/__tests__/workers/sherpaSpeechWorker.test.js src/__tests__/utils/sherpaRuntimeAsset.test.js src/__tests__/composables/useSpeechToText.test.js` 通过，4 个测试文件 / 73 个用例。

### 构建结果

- `npm.cmd run build` 通过。
- `dist/voice-models/sherpa-onnx/zh-cn-streaming/` 仍只包含 `manifest.json` 和 `runtime.js`。
- `dist` 中未发现超过 25MiB 的文件。

### 当前功能验收说明

本轮实现的是“轻量构建 + 线上拉取”的可选覆盖能力。默认情况下用户不需要部署自有模型源；前端会继续尝试 manifest 中的 ModelScope 公共镜像、同源兜底和 HuggingFace 官方源。只有未来需要主动覆盖公共候选时，才配置 `VITE_OFFLINE_STT_MODEL_BASE_URL`。

### 停止，不继续下一个功能

本轮只处理离线 STT 显式配置模型源覆盖能力，不继续推进模型托管落地、离线 TTS 或其它语音能力。

## 追加修复：恢复公共镜像源作为默认下载认知（2026-05-31）

### 修复原因

用户明确说明目标不是自行部署模型文件，而是“白嫖直接从网上已经有的镜像源拉取下载”。上一轮新增的 `VITE_OFFLINE_STT_MODEL_BASE_URL` 只是可选覆盖能力，但文档和设置页文案容易被理解为必须配置自有 OSS/R2/COS/CDN 模型源，因此需要修正当前交付口径。

### 修复范围

- 修改：`frontend/app/src/views/settings/SettingsView.vue`
- 修改：`frontend/app/src/utils/offlineVoiceModelCache.js`
- 修改：`frontend/app/src/__tests__/views/SettingsView.test.js`
- 修改：`frontend/tasks/stage.md`

### 当前公共源结论

- `hf.qhduan.com` 当前返回 `401 Unauthorized`，不写入默认生产候选。
- `hf-mirror.com` 对 HuggingFace Space 形式的 `/spaces/k2-fsa/.../resolve/main/...` 当前返回 `404 RepoNotFound`，不写入默认生产候选。
- ModelScope 公共 Studio 静态源当前 1 字节范围请求返回 `206 Partial Content`，并带 `Access-Control-Allow-Origin: *`，仍作为默认公共镜像第一候选。
- HuggingFace 官方 Space 保留为末位公共官方源；同源 `/api/offline-stt/models/**` 仍是可选兜底通道，不要求用户部署自有模型目录。

### 前端实现方案

- 设置页离线增强说明改为：离线语音识别包约 200MB，默认由浏览器从 ModelScope 公共模型镜像源下载并缓存到本机，无需自建模型源。
- `offlineVoiceModelCache.js` 注释修正为：显式配置模型源只是覆盖入口；未配置时默认使用 `manifest.json` 中的公共镜像候选。
- 保留 `VITE_OFFLINE_STT_MODEL_BASE_URL` 和 `window.__AI_RESUME_OFFLINE_STT_MODEL_BASE_URL__`，但仅作为高级可选覆盖，不作为当前方案的必要前提。

### 测试结果

- RED 阶段：新增设置页断言，旧文案失败于未包含 `ModelScope 公共模型镜像源` 和 `无需自建模型源`。
- GREEN 阶段：`npm.cmd test -- --run src/__tests__/views/SettingsView.test.js` 通过，42 个用例。

### 当前功能验收说明

默认下载路径仍由生产 `manifest.json` 决定：先尝试 ModelScope 公共镜像源，再按清单继续尝试后续候选。用户不需要自行部署模型源；如果公共源因限流、地区网络或服务波动不可用，应用会按候选链路继续兜底，但无法承诺任何免费第三方镜像永久稳定。

### 停止，不继续下一个功能

本轮只处理公共镜像默认下载口径修正，不继续推进自有模型托管、离线 TTS 或其它语音能力。

## 追加修复：公共源响应体缓存失败后继续尝试下一个候选源（2026-05-31）

### 修复原因

用户反馈公共镜像源方案仍下载失败，并追问是否必须部署在本地。复查下载链路发现：`fetch()` 只要拿到响应头就会返回 `Response`，旧逻辑会立刻把该源视为命中；但 200MB `.data` 文件真正下载和写入发生在 `cache.put(response.clone())`。如果该阶段网络流中断，异常发生在候选源循环之外，前端不会再尝试第二个 ModelScope 域名、同源兜底或 HuggingFace。

### 修复范围

- 修改：`frontend/app/src/utils/offlineVoiceModelCache.js`
- 修改：`frontend/app/src/__tests__/utils/offlineVoiceModelCache.test.js`
- 修改：`frontend/tasks/stage.md`

### 前端实现方案

- 将 `fetch + 响应类型校验 + cache.put` 合并到同一个候选源循环中。
- 只有模型文件完整写入 Cache API 后，才把该候选 URL 记录为命中源并进入后续状态保存。
- 如果某个公共源响应头正常但缓存写入失败，会记录 `console.warn('离线语音识别模型源请求失败', ...)`，并继续尝试下一个候选源。
- 该修复不要求本地部署模型文件，也不把大模型写入前端构建产物。

### 当前公共源复测

- ModelScope `.data` 大文件 1 字节范围请求返回 `206 Partial Content`。
- 响应包含 `Content-Range: bytes 0-0/199059238`。
- 响应包含 `Access-Control-Allow-Origin: *`。
- 因此当前并非必须本地部署；但免费公共源仍可能因网络波动、浏览器缓存空间、限流或地区链路中断导致失败。

### 测试结果

- RED 阶段：新增 `tries the next mirror when caching the first large model response fails` 用例，旧实现失败于 `stream interrupted`，证明缓存写入失败不会继续换源。
- GREEN 阶段：`npm.cmd test -- --run src/__tests__/utils/offlineVoiceModelCache.test.js` 通过，25 个用例。
- 相关回归：`npm.cmd test -- --run src/__tests__/utils/offlineVoiceModelCache.test.js src/__tests__/workers/sherpaSpeechWorker.test.js src/__tests__/utils/sherpaRuntimeAsset.test.js src/__tests__/composables/useSpeechToText.test.js src/__tests__/views/SettingsView.test.js` 通过，5 个测试文件 / 116 个用例。
- 构建验证：`npm.cmd run build` 通过；`dist/voice-models/sherpa-onnx/zh-cn-streaming/` 仅包含 `manifest.json` 和 `runtime.js`，`dist` 中无超过 25MiB 文件。

### 当前功能验收说明

下载器现在会把“完整缓存成功”作为模型源命中标准。公共源不需要部署到本地；如果第一个公共镜像在大文件传输中断，前端会自动换到后续候选源继续尝试。

### 停止，不继续下一个功能

本轮只处理离线 STT 公共源缓存失败后的换源逻辑，不继续推进自有模型托管、离线 TTS 或其它语音能力。
## 追加修复：旧 manifest 缺少 urls 时仍走公共镜像（2026-05-31）
### 修复原因

用户反馈当前仍显示“离线语音识别包下载失败”，并且 Network 面板无外网请求，控制台只出现 `/voice-models/sherpa-onnx/zh-cn-streaming/sherpa-onnx-wasm-main-asr.wasm` 返回 `200 text/html` 后被判定为“模型源响应无效”。复查下载链路后确认：当运行时读到旧版或缓存的 sherpa manifest，文件项只有 `path`、没有 `urls` 时，下载器会退回同源 `/voice-models/...` 大文件路径；轻量部署中这些大文件不存在，所以前端站点返回 SPA HTML fallback，导致下载瞬间失败且没有外网镜像请求。

### 修复范围

- 修改：`frontend/app/src/utils/offlineVoiceModelCache.js`
- 修改：`frontend/app/src/__tests__/utils/offlineVoiceModelCache.test.js`
- 修改：`frontend/tasks/stage.md`

### 前端实现方案

- 新增 sherpa-onnx 固定文件名白名单，仅覆盖 `sherpa-onnx-asr.js`、`sherpa-onnx-wasm-main-asr.js`、`sherpa-onnx-wasm-main-asr.wasm`、`sherpa-onnx-wasm-main-asr.data`。
- 当 manifest 所在目录是 `/voice-models/sherpa-onnx/zh-cn-streaming/` 且文件项没有 `urls` 时，自动补齐 `modelscope.cn`、`www.modelscope.cn`、`/api/offline-stt/models/`、HuggingFace 官方源四个候选地址。
- 已经带有 `urls` 的新 manifest 不被覆盖，继续尊重清单中的候选源顺序；普通非 sherpa 文件仍按原同源 fallback 行为处理，避免扩大影响范围。

### 测试结果

- RED 阶段：新增 `adds public mirror candidates when an old production sherpa manifest has no urls` 与 `downloads from the public mirror when an old production sherpa manifest has no urls`，旧实现失败于仍解析/请求 `/voice-models/...wasm`。
- GREEN 阶段：`npm.cmd test -- --run src/__tests__/utils/offlineVoiceModelCache.test.js` 通过，26 个用例全部通过。

### 当前功能验收说明

即使浏览器、CDN 或部署环境仍拿到缺少 `urls` 的旧 sherpa manifest，下载器也会先尝试公共镜像源，不会直接命中同源 `/voice-models/...` 大文件路径并被 SPA HTML fallback 瞬间失败。

### 停止，不继续下一个功能

本轮只处理 TASK_61 离线 STT 旧 manifest 缺少远程候选源的兼容修复，不继续推进自有模型托管、离线 TTS 或其它语音能力。

## 追加修复：已缓存但运行时加载到 HTML WASM 的防护（2026-05-31）

### 修复原因

用户反馈离线语音识别包已显示“已缓存”，但进入语音通话后卡在通话准备阶段，并出现 `WebAssembly.instantiate(): expected magic word 00 61 73 6d, found 3c 21 44 4f`。其中 `3c 21 44 4f` 对应 `<!DO`，说明运行时实际拿到的是 HTML 兜底页而不是 `.wasm` 二进制。复查链路后确认：下载状态和运行时 Cache API 读取都需要继续做内容级校验，不能只以 HTTP 状态、Content-Type 或 cache 命中作为“已缓存成功”依据。

### 修复范围

- 修改：`frontend/app/src/utils/offlineVoiceModelCache.js`
- 修改：`frontend/app/public/voice-models/sherpa-onnx/zh-cn-streaming/runtime.js`
- 修改：`frontend/app/src/__tests__/utils/offlineVoiceModelCache.test.js`
- 修改：`frontend/app/src/__tests__/utils/sherpaRuntimeAsset.test.js`
- 修改：`frontend/tasks/stage.md`

### 前端实现方案

- 下载阶段新增 `.wasm` 文件头校验：WASM 前 4 字节必须是 `00 61 73 6d`，否则即使响应头是 `application/wasm` 也视为无效模型源。
- `isModelCached()` 不再只检查 Cache API 是否命中；命中后会再次校验 HTML 兜底和 WASM magic word，发现旧坏缓存会立即删除并返回未命中。
- `runtime.js` 在把缓存文件转成 Blob URL 前再次校验响应内容；发现历史 HTML 坏缓存会删除并重新请求，重新请求仍无效时抛出明确的“运行时文件内容无效”，避免继续进入 WebAssembly 编译阶段才报底层异常。
- 该修复不新增本地模型部署要求，也不把大模型文件加入前端构建产物。

### 测试结果

- RED 阶段：新增 `rejects a wasm model file when the body is html even if the content type says wasm`，旧实现会把 HTML 响应标记为 ready。
- RED 阶段：新增 `treats cached html wasm entries as invalid and deletes them before reporting cache readiness`，旧实现会把缓存中的 HTML `.wasm` 当作可用缓存。
- RED 阶段：新增 `rejects cached html wasm before importing the sherpa runtime`，旧 runtime 会继续 import 并进入 WASM 编译错误。
- GREEN 阶段：`npm.cmd test -- --run src/__tests__/utils/offlineVoiceModelCache.test.js` 通过，28 个用例。
- GREEN 阶段：`npm.cmd test -- --run src/__tests__/utils/sherpaRuntimeAsset.test.js` 通过，6 个用例。
- 相关回归：`npm.cmd test -- --run src/__tests__/utils/offlineVoiceModelCache.test.js src/__tests__/utils/sherpaRuntimeAsset.test.js` 通过，34 个用例。
- 相关回归：`npm.cmd test -- --run src/__tests__/workers/sherpaSpeechWorker.test.js` 通过，4 个用例。

### 当前功能验收说明

“已缓存”现在会更接近真实可用状态：至少能确认缓存文件不是 SPA HTML fallback，并且 `.wasm` 文件头符合 WebAssembly 标准。已存在于浏览器中的旧坏缓存需要用户删除资源包后重新下载，或由新版运行时首次加载时发现并清理。

### 停止，不继续下一个功能

本轮只处理离线 STT 已缓存但运行时拿到 HTML WASM 的防护和自清理，不继续推进自有模型托管、离线 TTS 或其它语音能力。

## 追加修复：强制刷新离线 STT runtime.js 缓存版本（2026-05-31）

### 修复原因

用户反馈在进入模拟语音面试时仍然直接出现 `WebAssembly.instantiateStreaming(): expected magic word 00 61 73 6d, found 3c 21 44 4f`。复查后确认：上一轮已经更新 `runtime.js` 的坏缓存校验，但 `useSpeechToText.js` 拼接 runtime URL 的前端适配器版本仍停留在 `20260530-persistent-worker-stream-reset`。由于 `runtime.js` 位于 `public/voice-models/...` 静态目录，不会像 Vite 打包 chunk 一样自动带内容 hash，浏览器可能继续使用旧的 `/runtime.js?v=...20260530...` 缓存，从而绕过新加的 WASM 内容校验。

### 修复范围

- 修改：`frontend/app/src/composables/useSpeechToText.js`
- 修改：`frontend/app/src/__tests__/composables/useSpeechToText.test.js`
- 修改：`frontend/tasks/stage.md`

### 前端实现方案

- 将 `OFFLINE_STT_RUNTIME_VERSION` 从 `20260530-persistent-worker-stream-reset` 升级为 `20260531-runtime-asset-validation`。
- 保留原有 `manifest.version + runtime adapter version` 组合查询参数策略；即使用户已经显示“已缓存”，新版前端也会生成新的 runtime URL，强制浏览器重新拉取包含 WASM/HTML 校验逻辑的 `runtime.js`。
- 新增中文注释说明：`runtime.js` 不进入 Vite hash 产物，运行时脚本变更必须显式升级查询参数，避免浏览器执行旧加载器。

### 测试结果

- RED 阶段：更新 `appends the runtime asset validation version even when the cached model has its own version`，旧实现失败于 runtime URL 仍包含 `20260530-persistent-worker-stream-reset`。
- GREEN 阶段：`npm.cmd test -- --run src/__tests__/composables/useSpeechToText.test.js` 通过，40 个用例。
- 相关回归：`npm.cmd test -- --run src/__tests__/utils/offlineVoiceModelCache.test.js src/__tests__/utils/sherpaRuntimeAsset.test.js src/__tests__/workers/sherpaSpeechWorker.test.js src/__tests__/composables/useSpeechToText.test.js src/__tests__/views/SettingsView.test.js` 通过，5 个测试文件 / 120 个用例。
- 构建验证：`npm.cmd run build` 通过；`dist/voice-models/sherpa-onnx/zh-cn-streaming/` 仅包含 `manifest.json` 和 `runtime.js`；`dist` 中无超过 25MiB 文件。

### 当前功能验收说明

上线该前端后，模拟语音面试会使用新的 `/voice-models/sherpa-onnx/zh-cn-streaming/runtime.js?v=...20260531-runtime-asset-validation`。如果缓存里仍有 HTML 伪 WASM，新 runtime 会先删除坏缓存并重新请求，不能再无提示地进入 WebAssembly 底层编译错误。

### 停止，不继续下一个功能

本轮只处理离线 STT runtime 静态脚本的浏览器缓存破坏问题，不继续推进自有模型托管、离线 TTS 或其它语音能力。

## 追加修复：运行时预加载 WASM 二进制并清理历史同源坏缓存（2026-05-31）

### 修复原因

用户删除离线语音包并重新下载后，浏览器 Cache Storage 中仍能看到 `/voice-models/sherpa-onnx/zh-cn-streaming/sherpa-onnx-wasm-main-asr.wasm` 类型为 `text/html`、长度约 1512 的历史坏缓存；同时新下载的 ModelScope 远程 `.wasm` 和 `.data` 已经存在。进入模拟语音面试后仍报 `expected magic word 00 61 73 6d, found 3c 21 44 4f`，说明 Emscripten 主运行时仍可能按默认 wasm 文件路径取到同源 HTML fallback，而不是使用我们已经缓存的远程 wasm。

### 修复范围

- 修改：`frontend/app/public/voice-models/sherpa-onnx/zh-cn-streaming/runtime.js`
- 修改：`frontend/app/src/utils/offlineVoiceModelCache.js`
- 修改：`frontend/app/src/composables/useSpeechToText.js`
- 修改：`frontend/app/src/__tests__/utils/offlineVoiceModelCache.test.js`
- 修改：`frontend/app/src/__tests__/utils/sherpaRuntimeAsset.test.js`
- 修改：`frontend/app/src/__tests__/composables/useSpeechToText.test.js`
- 修改：`frontend/tasks/stage.md`

### 前端实现方案

- `runtime.js` 读取 `.wasm` 时不再只生成 Blob URL，而是先把真实 wasm 响应转成 `ArrayBuffer` 并注入 `self.Module.wasmBinary`，让 Emscripten 直接使用已校验的 wasm 二进制，避免再次按默认同源路径请求假 wasm。
- `runtime.js` 仍保留 `locateFile()` 和 wasm Blob URL，兼容依赖路径解析的加载分支。
- `clearModelCache()` 在删除状态中记录的远程 URL 之外，会按 manifest 所在的 `/voice-models/sherpa-onnx/zh-cn-streaming/` 前缀同步清理历史同源缓存项，避免用户删除资源包后仍残留 `text/html` 的同源 `.wasm/.js`。
- `OFFLINE_STT_RUNTIME_VERSION` 升级为 `20260531-wasm-binary-preload`，强制浏览器重新拉取本次改动后的静态 runtime。

### 测试结果

- RED 阶段：新增 `clears stale same-origin sherpa cache entries when the ready package points at remote mirror urls`，旧实现只删除远程命中 URL，不删除同源坏缓存。
- RED 阶段：新增 `preloads the wasm binary into Module before importing the Emscripten runtime`，旧 runtime 导入 Emscripten 主运行时时 `Module.wasmBinary` 为空。
- RED 阶段：更新 `appends the wasm binary preload runtime version even when the cached model has its own version`，旧实现仍生成 `20260531-runtime-asset-validation`。
- GREEN 阶段：`npm.cmd test -- --run src/__tests__/utils/offlineVoiceModelCache.test.js src/__tests__/utils/sherpaRuntimeAsset.test.js` 通过，2 个测试文件 / 36 个用例。
- GREEN 阶段：`npm.cmd test -- --run src/__tests__/composables/useSpeechToText.test.js -t "wasm binary preload"` 通过。
- 相关回归：`npm.cmd test -- --run src/__tests__/utils/offlineVoiceModelCache.test.js src/__tests__/utils/sherpaRuntimeAsset.test.js src/__tests__/workers/sherpaSpeechWorker.test.js src/__tests__/composables/useSpeechToText.test.js src/__tests__/views/SettingsView.test.js` 通过，5 个测试文件 / 122 个用例。
- 构建验证：`npm.cmd run build` 通过；`dist/voice-models/sherpa-onnx/zh-cn-streaming/` 仅包含 `manifest.json` 和 `runtime.js`；`dist` 中无超过 25MiB 文件；构建产物包含 `20260531-wasm-binary-preload` 和 `wasmBinary`。

### 当前功能验收说明

上线该前端后，模拟语音面试会使用 `/voice-models/sherpa-onnx/zh-cn-streaming/runtime.js?v=...20260531-wasm-binary-preload`。用户再次点“删除资源包”时，会同时清掉历史同源 `/voice-models/sherpa-onnx/zh-cn-streaming/` 下的坏缓存；重新下载后运行时会把已校验的远程 wasm 直接注入给 Emscripten，不应再把 1512 字节的 HTML 当作 wasm 编译。

### 停止，不继续下一个功能

本轮只处理离线 STT 运行时继续命中 HTML 伪 WASM 的根因，不继续推进自有模型托管、离线 TTS 或其它语音能力。

## 追加修复：Emscripten instantiateWasm 运行时启动防悬挂（2026-05-31）

### 修复原因

用户反馈离线引擎下载后进入语音通话仍一直卡在“通话准备中”，前端控制台继续出现 `expected magic word 00 61 73 6d, found 3c 21 44 4f`。复查当前启动链路后确认：上一轮已把真实 `.wasm` 读取成 `wasmBinary`，但 Emscripten 主运行时仍可能不使用该二进制，转而按默认同源 `/voice-models/sherpa-onnx/zh-cn-streaming/sherpa-onnx-wasm-main-asr.wasm` 再次 fetch；轻量前端部署会把该路径回退成 SPA HTML，最终把 `<!DO` 当作 wasm 编译。同时 WASM abort 没有稳定 reject，导致 `self.__sherpaRuntimeReady` 可能一直悬挂。

### 修复范围

- 修改：`frontend/app/public/voice-models/sherpa-onnx/zh-cn-streaming/runtime.js`
- 修改：`frontend/app/src/composables/useSpeechToText.js`
- 修改：`frontend/app/src/__tests__/utils/sherpaRuntimeAsset.test.js`
- 修改：`frontend/app/src/__tests__/composables/useSpeechToText.test.js`
- 修改：`frontend/tasks/stage.md`

### 前端实现方案

- `runtime.js` 在 `Module` 上新增 `instantiateWasm(imports, successCallback)`，直接调用 `WebAssembly.instantiate(wasmBinary, imports)`，强制 Emscripten 使用已经校验过的真实 wasm 二进制。
- `locateFile()` 保留用于 `.data`、脚本和其它资源定位，继续优先返回已下载命中的缓存 URL / blob URL，不再承担主 wasm 实例化入口。
- 新增 `onAbort(reason)`、`try-catch` 和统一 `rejectRuntimeReady()`，确保 wasm 编译、导入或 runtime abort 失败时 `self.__sherpaRuntimeReady` 会 reject，不再让界面悬挂在准备中。
- 兼容 classic 和 MODULARIZE 两类 sherpa wasm runtime：如果导入后 `self.Module` 变成 factory，则恢复同一个注入过 `wasmBinary`、`locateFile`、`instantiateWasm` 的 module 对象并显式调用 factory。
- `OFFLINE_STT_RUNTIME_VERSION` 升级为 `20260531-instantiate-wasm-preload`，强制浏览器拉取新版 `runtime.js`。
- Worker 初始化失败仍走 `offline-error`，会清理录音状态并展示错误文案，不停留在 loading/ready 的中间态。

### 后端实现方案

本轮无后端代码改动，不改后端代理、不改模型源 manifest、不新增模型托管链路。

### 数据存储方案

无数据库结构变更。浏览器 Cache API 和 localStorage 状态格式不变，本轮只约束 runtime 如何使用已缓存的 wasm 二进制。

### 测试结果

- RED 阶段：新增模拟真实 Emscripten 主运行时的用例，旧实现会继续 fetch 默认同源 `.wasm` 并拿到 HTML fallback。
- RED 阶段：新增 `onAbort` 用例，旧实现会让 `self.__sherpaRuntimeReady` 悬挂。
- RED 阶段：新增 MODULARIZE factory 用例，旧实现没有保证 factory 收到同一个注入了 `wasmBinary/locateFile/instantiateWasm` 的 module 对象。
- RED 阶段：新增 Worker 初始化失败回归，要求 `engineStatus` 为 `offline-error`、`isRecording` 为 `false` 且错误文案可见。
- 目标回归：`npm.cmd test -- --run src/__tests__/utils/sherpaRuntimeAsset.test.js src/__tests__/workers/sherpaSpeechWorker.test.js src/__tests__/composables/useSpeechToText.test.js src/__tests__/views/InterviewSessionView.test.js` 通过，4 个测试文件 / 87 个用例。

### 构建结果

- `npm.cmd run build` 通过。
- 本轮未把 `.wasm`、`.data` 大模型文件加入前端构建产物。

### 当前功能验收说明

上线后，离线 STT Worker 会使用新版 `/voice-models/sherpa-onnx/zh-cn-streaming/runtime.js?v=...20260531-instantiate-wasm-preload`。Emscripten 主运行时应直接实例化已校验的远程 wasm 二进制，不再回退请求同源 HTML 伪 WASM；如果 WASM 初始化仍失败，也会进入 `offline-error` 并显示错误，而不是一直显示“通话准备中”。

### 停止，不继续下一个功能

本轮只修复离线 STT 启动阶段继续命中 HTML 伪 WASM 和异常悬挂的根因，不继续推进后端代理、模型源 manifest、自有模型托管、离线 TTS 或其它语音能力。

## 追加修复：Emscripten 主运行时 Blob 显式注入 Module（2026-05-31）

### 修复原因

用户重启电脑、切换浏览器并重新下载远程源离线引擎后，仍然出现 `WebAssembly.instantiateStreaming(): expected magic word 00 61 73 6d, found 3c 21 44 4f`。继续核对真实 ModelScope 主运行时后确认：`sherpa-onnx-wasm-main-asr.js` 开头会声明 `var Module = typeof Module != "undefined" ? Module : {}`，随后用 `Module.locateFile()`、`fetch()`、`XMLHttpRequest` 加载 `.data/.wasm`。在 Blob 脚本和部分浏览器执行形态下，主运行时的局部 `var Module` 仍可能没有稳定拿到 wrapper 预先设置的 `self.Module`，于是 `wasmBinary/instantiateWasm/locateFile` 都失效，继续走默认同源 wasm 路径并命中 SPA HTML。

### 修复范围

- 修改：`frontend/app/public/voice-models/sherpa-onnx/zh-cn-streaming/runtime.js`
- 修改：`frontend/app/src/composables/useSpeechToText.js`
- 修改：`frontend/app/src/__tests__/utils/sherpaRuntimeAsset.test.js`
- 修改：`frontend/app/src/__tests__/composables/useSpeechToText.test.js`
- 修改：`frontend/tasks/stage.md`

### 前端实现方案

- `runtime.js` 不再把 Emscripten 主运行时 JS 原样转成 Blob；生成 `sherpa-onnx-wasm-main-asr.js` Blob 前，会在脚本文本前注入：
  - `var Module = self.Module || {};`
  - `var fetch = typeof self.fetch === "function" ? self.fetch.bind(self) : undefined;`
  - `var XMLHttpRequest = self.XMLHttpRequest;`
- 这段 bootstrap 会让主运行时内部的局部 `Module/fetch/XMLHttpRequest` 明确使用 wrapper 已注入、已桥接的对象，避免它回到默认同源 `/voice-models/...wasm` 路径。
- `runtime.js` 仍保留上一轮的 `wasmBinary`、`instantiateWasm`、`locateFile`、`onAbort` 和 fetch/XHR bridge 防护。
- `OFFLINE_STT_RUNTIME_VERSION` 升级为 `20260531-runtime-main-bootstrap`，强制浏览器重新拉取本次修改后的静态 runtime。

### 后端实现方案

本轮无后端代码改动，不改后端代理、不改模型源 manifest、不新增模型托管链路。

### 数据存储方案

无数据库结构变更。浏览器 Cache API 和 localStorage 状态格式不变，本轮只改变运行时 Blob 的包装方式。

### 测试结果

- RED 阶段：新增 `injects the prepared Module into the Emscripten main runtime blob`，旧实现失败于主运行时 Blob 中的局部 `Module` 没有拿到 `wasmBinary/instantiateWasm/locateFile`。
- GREEN 阶段：`npm.cmd test -- --run src/__tests__/utils/sherpaRuntimeAsset.test.js` 通过，13 个用例。
- 目标回归：`npm.cmd test -- --run src/__tests__/utils/sherpaRuntimeAsset.test.js src/__tests__/workers/sherpaSpeechWorker.test.js src/__tests__/composables/useSpeechToText.test.js src/__tests__/views/InterviewSessionView.test.js` 通过，4 个测试文件 / 90 个用例。

### 构建结果

- `npm.cmd run build` 通过。
- 产物检查：`dist/voice-models/sherpa-onnx/zh-cn-streaming/` 包含 `manifest.json`、`runtime.js`、`runtime.js.gz`。
- 产物检查：`dist` 中无超过 25MiB 文件；构建产物包含 `20260531-runtime-main-bootstrap`、`var Module = self.Module || {}`、`var fetch = typeof self.fetch === "function" ? self.fetch.bind(self) : undefined`。

### 当前功能验收说明

上线该前端后，语音面试会使用 `/voice-models/sherpa-onnx/zh-cn-streaming/runtime.js?v=...20260531-runtime-main-bootstrap`。远程源下载模式下，Emscripten 主运行时应该明确使用已校验的远程 wasm 和 data Blob，不再把同源 SPA HTML fallback 当作 wasm 编译。

### 停止，不继续下一个功能

本轮只修复离线 STT Emscripten 主运行时 Blob 没有稳定继承注入对象的问题，不继续推进后端代理、模型源 manifest、自有模型托管、离线 TTS 或其它语音能力。
## 追加修复：Emscripten 主运行时源码硬化（2026-06-01）

### 修复原因

用户确认 Network 中存在 `blob:http://localhost:3000/...` 请求且状态为 200，但控制台仍报 `expected magic word 00 61 73 6d, found 3c 21 44 4f`。这说明主运行时 JS Blob 已成功加载，真正失败点是该 JS 内部继续按默认路径加载 `.wasm/.data`，并命中 SPA HTML。上一轮仅依赖 bootstrap、`instantiateWasm` 和 fetch/XHR bridge，仍不足以彻底切断 Emscripten classic runtime 内部的默认路径分支。

### 修复范围

- 修改：`frontend/app/public/voice-models/sherpa-onnx/zh-cn-streaming/runtime.js`
- 修改：`frontend/app/src/workers/sherpaSpeechWorker.js`
- 修改：`frontend/app/src/composables/useSpeechToText.js`
- 修改：`frontend/app/src/__tests__/utils/sherpaRuntimeAsset.test.js`
- 修改：`frontend/app/src/__tests__/workers/sherpaSpeechWorker.test.js`
- 修改：`frontend/app/src/__tests__/composables/useSpeechToText.test.js`
- 修改：`frontend/tasks/stage.md`

### 前端实现方案

- `runtime.js` 改为先读取并校验 wasm/data，再生成 Emscripten 主运行时 Blob；生成主运行时 Blob 前，对 `sherpa-onnx-wasm-main-asr.js` 源码做定向 patch。
- patch 内容包括：将 classic runtime 的 `var Module=typeof Module!="undefined"?Module:{}` 绑定到 wrapper 注入的同一个 module；将 `wasmBinaryFile="sherpa-onnx-wasm-main-asr.wasm"` 改为已校验的 wasm Blob URL；将 `.data` 的 `REMOTE_PACKAGE_NAME` 改为已校验的 data Blob URL。
- `runtimeModule.locateFile()` 收紧为：`.wasm/.data` 只能返回已生成的 Blob URL；如果 Blob 映射缺失，直接 reject，阻止继续请求 `/voice-models/...` 默认同源路径。
- Worker 新增运行时诊断：记录 runtime URL、版本号、注入模型文件数量和源码 patch 状态；初始化失败时随 `error` 消息返回，便于区分“下载失败”和“Emscripten 默认路径回退”。
- `OFFLINE_STT_RUNTIME_VERSION` 升级为 `20260601-runtime-source-patch`，强制刷新新版静态 runtime。

### 测试结果

- RED 阶段新增 classic Emscripten 最小主运行时用例，旧实现仍保留默认 `.wasm/.data` 字面路径，无法证明源码已硬化。
- RED 阶段新增未知 Emscripten runtime 结构用例，要求提前 reject，而不是继续执行到 HTML-as-wasm。
- RED 阶段新增 Worker 初始化失败诊断用例，旧实现仅返回 error，缺少 runtime version 和模型文件诊断。
- 目标回归：`npm.cmd test -- --run src/__tests__/utils/sherpaRuntimeAsset.test.js src/__tests__/workers/sherpaSpeechWorker.test.js src/__tests__/composables/useSpeechToText.test.js src/__tests__/views/InterviewSessionView.test.js` 通过，4 个测试文件 / 93 个用例。
- 构建验证：`npm.cmd run build` 通过；`dist/voice-models/sherpa-onnx/zh-cn-streaming/` 仅包含 `manifest.json`、`runtime.js`、`runtime.js.gz`；`dist` 中无超过 25MiB 文件；构建产物包含 `20260601-runtime-source-patch` 和 `__aiResumeSherpaRuntimeBlobUrls`。

### 当前功能验收说明

上线后，离线 STT Worker 应加载 `/voice-models/sherpa-onnx/zh-cn-streaming/runtime.js?v=...20260601-runtime-source-patch`。Network 中允许出现 `blob:http://localhost:3000/...`；但不应再看到 `/voice-models/sherpa-onnx/zh-cn-streaming/sherpa-onnx-wasm-main-asr.wasm` 返回 HTML，也不应再出现 `found 3c 21 44 4f`。如上游主运行时结构变化，前端会提前进入 `offline-error` 并返回诊断信息。

### 停止，不继续下一个功能

本轮只修复离线 STT 网络源启动链路，不改后端代理、不改模型源 manifest、不新增离线 TTS、自有模型托管或其它语音能力。
