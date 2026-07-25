# 请求噪音与误报修复后端（2026-06-04）

## 当前任务所属模块

后端全局异常处理 / 用户自定义 TTS 音色发现。

## 前端文件定位

本轮不涉及前端文件修改；简历诊断轮询和管理端 dashboard 请求合并留到后续独立轮次。

## 后端文件定位

- `server/src/main/java/com/airesume/server/common/exception/GlobalExceptionHandler.java`
- `server/src/main/java/com/airesume/server/service/impl/TtsDiscoveryServiceImpl.java`
- `server/src/test/java/com/airesume/server/common/exception/GlobalExceptionHandlerTest.java`
- `server/src/test/java/com/airesume/server/service/impl/TtsDiscoveryServiceImplTest.java`
- `server/src/test/java/com/airesume/server/service/impl/UserTtsSpeechServiceImplTest.java`

## 本轮修改文件清单

- `GlobalExceptionHandler` 新增 `HttpRequestMethodNotSupportedException` 专用处理，HTTP 状态返回 405，响应体沿用参数错误码和固定提示。
- `TtsDiscoveryServiceImpl` 新增本地短 TTL 负向缓存，仅缓存同一 `provider + normalizedBaseUrl` 下两个音色发现端点均明确 404 的结果。
- `GlobalExceptionHandlerTest` 补充 405 handler 的响应体和 `@ResponseStatus(HttpStatus.METHOD_NOT_ALLOWED)` 断言。
- `TtsDiscoveryServiceImplTest` 调整为验证第二次发现仍执行 API Key 验证和 `/models`，但不重复请求音色端点。
- `UserTtsSpeechServiceImplTest` 将系统 TTS 兜底 mock 对齐当前生产实现的 `resolveEnabledConfig()` 调用，属于验证链路最小修正，不扩展功能。

## 前端实现方案

本轮无前端实现；未调整 processing 轮询间隔、未新增 SSE/长轮询、未修改 dashboard 页面请求方式。

## 后端实现方案

- 405 请求方法不支持属于客户端请求错误，单独使用 `warn` 日志记录 method 和 supported methods，不打印堆栈，避免落入通用 500 系统异常。
- 405 响应 HTTP 状态为 `METHOD_NOT_ALLOWED`，业务响应体使用 `ResultCode.PARAM_ERROR`，message 为 `请求方法不支持`，保持现有响应包络风格。
- TTS 音色端点缓存 key 仅由 providerId 和归一化 baseUrl 组成，不包含也不保存 API Key。
- TTS 发现仍每次执行 API Key 验证和 `/models` 模型发现，避免缓存掩盖凭据变化或模型变化。
- 仅当 `/audio/voices` 与 `/v1/audio/voices` 均返回明确 404 时写入 5 分钟负向缓存；401、403、网络异常、超时或其它非 404 错误不缓存。
- 缓存命中时只跳过已确认不可用的音色端点探测，仍回落到 Provider 预设音色，保持原有可用性。

## 数据存储方案

本轮无数据库表、字段、索引、迁移脚本或连接池配置变更；当前排查没有证据支持数据库层健康问题。

## stage 更新说明

根目录 `tasks/stage.md` 已置顶补充“请求噪音与误报修复后端”阶段记录，说明本轮只做后端最小修复，并明确后续轮询和 dashboard 优化未开始。

## 编译结果

- RED：`mvn.cmd -q "-Dtest=GlobalExceptionHandlerTest,TtsDiscoveryServiceImplTest" test` 在旧实现下因缺少 405 handler 编译失败，确认测试覆盖到缺失处理方法。
- GREEN：`mvn.cmd -q "-Dtest=GlobalExceptionHandlerTest,TtsDiscoveryServiceImplTest,ServerApplicationTests,UserTtsSpeechServiceImplTest" test` 通过。
- 编译：`mvn.cmd -q -DskipTests compile` 通过。
- 全量回归：`mvn.cmd -q test` 通过。

## 构建结果

本轮无前端修改，未执行前端构建；后端编译已通过。

## 当前功能验收说明

- GET 等错误方法请求命中不支持 GET 的接口时，不再按系统异常返回 500，而是返回 HTTP 405 和 `请求方法不支持`。
- 日志中 405 只按客户端错误记录 warn，不打印堆栈，降低生产错误日志误报。
- 同一 MiMo 等 Provider/baseUrl 的音色端点若两个候选端点均 404，5 分钟内后续发现不再重复探测这两个外部 404 端点。
- API Key 验证和 `/models` 模型发现没有被负向缓存跳过，认证失败和模型变化仍可被当前请求感知。
- 本轮没有数据库变更、没有前端变更、没有继续推进轮询或 dashboard 优化。

## 停止，不继续下一个功能

本轮仅完成请求噪音与误报修复计划的第一轮后端最小修复，等待验收，不继续推进简历轮询和管理端 dashboard 请求量优化。
