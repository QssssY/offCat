# 离线 STT 远程源验证记录

## 追加验证：ModelScope 直连源恢复与备用源复查（2026-05-31）
- 复测发现 ModelScope 静态文件 `GET` 已返回 `access-control-allow-origin: *`，`sherpa-onnx-asr.js` 与 `sherpa-onnx-wasm-main-asr.js` 带 `Origin: http://localhost:3000` 请求均返回 `200`，可作为浏览器直连候选源。
- `sherpa-onnx-wasm-main-asr.data` 使用范围请求 `curl.exe -L -r 0-0 -H "Origin: http://localhost:3000"` 返回 `206 Partial Content`、`Content-Range: bytes 0-0/199059238`、`access-control-allow-origin: *`，说明大文件可开始跨域传输。
- HuggingFace 官方 Space 的 `resolve/main/sherpa-onnx-asr.js` 在当前环境 `GET` 范围请求返回 `401 Invalid username or password`，继续仅作为最后备用源，不再依赖它兜底。
- `https://modelscope.cn/.../static/` 与 `https://www.modelscope.cn/.../static/` 两个域名均验证小文件 `GET 200`，生产 manifest 已将二者放在 `/api/offline-stt/models/<fileName>` 之前，避免后端未部署或未重启时直接失败。
- 复查 `anyshu/sherpa-onnx-wasm-main-asr.data` 仓库：可访问，但公开目录只提供部分模型包文件或 zip，不能直接与当前四文件 runtime 混用，因此本轮不加入生产 manifest，避免下载成功后运行时不兼容。

验证时间：2026-05-31

## 验证对象

- `https://hf.qhduan.com/spaces/k2-fsa/web-assembly-asr-sherpa-onnx-zh-en/resolve/main/`
- `https://hf-mirror.com/spaces/k2-fsa/web-assembly-asr-sherpa-onnx-zh-en/resolve/main/`
- `https://www.modelscope.cn/api/v1/studio/k2-fsa/web-assembly-asr-sherpa-onnx-zh-en/static/`
- `https://huggingface.co/spaces/k2-fsa/web-assembly-asr-sherpa-onnx-zh-en/resolve/main/`

验证文件：

- `sherpa-onnx-asr.js`
- `sherpa-onnx-wasm-main-asr.js`
- `sherpa-onnx-wasm-main-asr.wasm`
- `sherpa-onnx-wasm-main-asr.data`

## 轻量 HEAD 验证结果

| 源 | 结果 | 说明 |
|---|---|---|
| `hf.qhduan.com` | 失败 | 四个文件均返回 `401`，响应为 `Invalid username or password.`，`access-control-allow-origin` 为 `https://huggingface.co`。 |
| `hf-mirror.com` | 失败 | 四个 `resolve/main` 文件均返回 `404 Repository not found`，`access-control-allow-origin` 为 `https://hf-mirror.com`。 |
| `modelscope.cn` | 当前工具环境 HEAD 超时 | k2-fsa 官方文档将 ModelScope Studio 标为 HuggingFace 不可访问时的镜像。本机 `curl.exe -I` 与 `Invoke-WebRequest -Method Head` 对静态文件 URL 均超时，仍需真实浏览器验证 CORS 和大文件下载。 |
| `huggingface.co` | 本机 Node fetch 超时 | 当前执行环境访问 `huggingface.co:443` 出现 `ConnectTimeoutError`；通过 HuggingFace 页面验证官方 Space 文件列表存在目标文件。 |

HuggingFace 官方文件页：

- `https://huggingface.co/spaces/k2-fsa/web-assembly-asr-sherpa-onnx-zh-en/tree/main`

k2-fsa 官方镜像说明：

- `https://k2-fsa.github.io/sherpa/onnx/wasm/hf-spaces.html`
- `https://modelscope.cn/studios/k2-fsa/web-assembly-asr-sherpa-onnx-zh-en/summary`

## 结论

- 已从生产 manifest 移除当前明确返回 `401` / `404` 的 `hf.qhduan.com` 与 `hf-mirror.com`。
- 生产 manifest 当前保留 ModelScope Studio 镜像和 HuggingFace 官方源两个候选家族，运行时仍会按候选源顺序失败后继续尝试后续源。
- 设置页已改为“已配置模型源”和“官方源较慢时可尝试切换网络”，不再承诺国内镜像可用。
- 本轮没有使用本站后端或 Cloudflare Pages 承载模型文件。

## 未完成的人工验证

当前工具环境无法完成真实浏览器 CORS 下载验证。上线前仍需在目标浏览器 Network 面板中人工确认：

- `.wasm` 和 `.data` 能被浏览器 `fetch`。
- `.data` 响应不是 HTML。
- 下载请求直接访问候选模型源，不访问本站后端。
## 追加验证：浏览器 CORS 与同源兜底修复（2026-05-31）

- ModelScope 静态文件在 Node 环境可读取，例如 `sherpa-onnx-asr.js` 返回 `200`，但响应头没有 `access-control-allow-origin`，真实浏览器跨域 `fetch` 会被 CORS 拦截。
- ModelScope 当前文件大小已确认：`sherpa-onnx-asr.js` 为 `53867`，`sherpa-onnx-wasm-main-asr.js` 为 `93043`，`sherpa-onnx-wasm-main-asr.wasm` 为 `11186321`，`sherpa-onnx-wasm-main-asr.data` 为 `199059238`。
- HuggingFace 官方 Space 文件仍保留为备用源，但当前环境访问 `huggingface.co` 可能超时，不能作为唯一兜底。
- 已从生产 manifest 移除 ModelScope 直连静态地址，改为 `/api/offline-stt/models/<fileName>` 同源兜底地址，由后端流式读取 ModelScope/HuggingFace，规避浏览器 CORS 限制。
- 当前仍建议上线后在真实浏览器 Network 面板确认：`/api/offline-stt/models/sherpa-onnx-wasm-main-asr.data` 能返回 `200`、内容不是 HTML、Cache API 写入成功。
