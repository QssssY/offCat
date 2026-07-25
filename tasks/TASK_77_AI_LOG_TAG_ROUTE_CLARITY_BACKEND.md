# AI 调用日志标签与路由展示统一修复后端

## 当前任务所属模块

- 后端模块：简历 AI、模拟面试 AI、轻量 AI 调用客户端、用户自定义 AI 日志可观测性。
- 触发原因：用户实测发现自定义简历 AI 配置名称为 Mimo、实际 `baseUrl/model` 也是 Mimo，但日志标签显示 `[OPENAI]`；同时历史日志中还出现过 `[DEEPSEEK]` 与 `USER_CUSTOM/openai-compatible` 混排，容易误判真实调用来源。

## 前端文件定位

- 本轮不涉及前端页面、接口协议或展示字段变更。

## 后端文件定位

- `server/src/main/java/com/airesume/server/service/impl/ResumeAiServiceImpl.java`
- `server/src/main/java/com/airesume/server/service/impl/InterviewAiServiceImpl.java`
- `server/src/main/java/com/airesume/server/service/AiChatClient.java`
- `server/src/test/java/com/airesume/server/service/impl/ResumeAiServiceImplTest.java`
- `server/src/test/java/com/airesume/server/service/impl/InterviewAiServiceImplTest.java`
- `server/src/test/java/com/airesume/server/service/AiChatClientTest.java`

## 本轮修改文件清单

- `AiChatClient`：平台运行时日志标签由裸 provider 改为 `PLATFORM/<provider>`；用户自定义仍保持 `USER_CUSTOM/openai-compatible`，调用日志继续输出 `source/baseUrl/endpoint/model/configType`。
- `InterviewAiServiceImpl`：启动日志和平台运行时日志统一使用 `PLATFORM/<provider>`；用户自定义请求继续使用运行时自定义标签。
- `ResumeAiServiceImpl`：简历诊断、图片识别、JD 匹配、简历润色等运行时日志保持 `USER_CUSTOM/openai-compatible`，平台和启动日志统一使用 `PLATFORM/<provider>`。
- `AiChatClientTest`、`InterviewAiServiceImplTest`、`ResumeAiServiceImplTest`：补充平台标签前缀和自定义标签不回退测试。

## 前端实现方案

- 无前端改动。

## 后端实现方案

- 保留用户自定义 AI 的 OpenAI-compatible 调用协议，不改 `provider=openai` 的真实路由含义。
- 日志标签不再直接展示协议 provider：用户自定义显示 `USER_CUSTOM/openai-compatible`，平台配置显示 `PLATFORM/<provider>`。
- 真实排查以日志里的 `source/baseUrl/endpoint/model/configType` 为准；`providerName` 继续只作为用户配置展示名，不参与路由。
- 未新增数据库字段，未改用户配置解析优先级，未改计费和失败回滚逻辑。

## 数据存储方案

- 无数据库结构变更。
- 无迁移脚本变更。

## stage 更新说明

- 已更新 `tasks/stage.md`，记录本轮简历/面试/轻量 AI 调用日志标签统一结果。

## 编译结果

- `mvn.cmd -q -DskipTests compile` 通过。

## 构建结果

- 本轮仅后端改动，无前端构建。

## 测试结果

- RED 验证：`mvn.cmd -q "-Dtest=AiChatClientTest#shouldUsePlatformPrefixForPlatformRuntimeLogTag" test` 在旧实现下失败，失败点为期望 `PLATFORM/MIMO` 但实际为 `MIMO`。
- GREEN 验证：`mvn.cmd -q "-Dtest=AiChatClientTest,ResumeAiServiceImplTest,InterviewAiServiceImplTest" test` 通过。
- 代码扫描：`rg "String tag = this\\.provider\\.toUpperCase\\(|runtimeConfig\\.provider\\(\\)\\.toUpperCase\\(|provider\\.toUpperCase\\(|\\[OPENAI\\]|\\[DEEPSEEK\\]|\\[MIMO\\]" server/src/main/java/com/airesume/server` 无命中。

## 当前功能验收说明

- 用户自定义 Mimo 简历 AI 的真实调用仍使用其配置的 `baseUrl/model/apiKey`；日志标签不再显示为 `[OPENAI]`。
- 平台 AI 日志不再显示裸 `[DEEPSEEK]`、`[MIMO]`，改为 `PLATFORM/DEEPSEEK`、`PLATFORM/MIMO`，避免和用户自定义请求混淆。
- 自定义 AI 请求日志应显示 `USER_CUSTOM/openai-compatible`，并带 `source=user_custom`、真实 `baseUrl`、真实 `model`、`configType`。

## 停止，不继续下一个功能

本轮仅修复 AI 调用日志标签和路由展示口径，不继续推进新的 AI Provider 协议、前端 UI、计费规则、数据库结构或摘要/报告策略。
