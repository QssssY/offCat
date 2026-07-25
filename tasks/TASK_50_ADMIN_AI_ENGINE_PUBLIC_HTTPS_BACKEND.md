# TASK 50 管理端 AI 引擎公网 HTTPS 地址校验修复

## 当前任务所属模块

管理端 AI 引擎配置与模拟面试 AI 调用链路。

## 前端文件定位

本轮不修改前端文件。管理端原有 AI 引擎配置表单继续提交 `baseUrl`。

## 后端文件定位

- `server/src/main/java/com/airesume/server/controller/AdminController.java`
- `server/src/main/java/com/airesume/server/service/impl/InterviewAiServiceImpl.java`
- `server/src/main/java/com/airesume/server/common/util/PublicHttpsUrlValidator.java`
- `server/src/test/java/com/airesume/server/common/util/PublicHttpsUrlValidatorTest.java`
- `server/src/test/java/com/airesume/server/service/impl/InterviewAiServiceImplTest.java`

## 本轮修改文件清单

- 新增 `PublicHttpsUrlValidator` 公共工具，统一校验 AI baseUrl 必须是公网 HTTPS 地址。
- 移除 `InterviewAiServiceImpl` 中硬编码的 AI 服务商 host 白名单。
- 面试 AI 运行时允许 Mimo 等新公网 HTTPS 模型服务商地址，不再因为域名不在白名单而回退默认地址。
- 非法 baseUrl 改为直接抛错，避免静默回退导致排障困难。
- 管理端保存 AI 引擎配置时复用同一套公网 HTTPS 校验规则。
- 新增单元测试覆盖公网 HTTPS、HTTP、本机、内网、云元数据地址和 IPv4 映射 IPv6 内网地址。

## 后端实现方案

运行时 `resolveBaseUrl` 只在未配置 baseUrl 时使用 provider 默认地址；只要管理端或配置文件显式提供 baseUrl，就调用 `PublicHttpsUrlValidator.validate` 校验。校验通过时原样使用 trim 后的配置地址，校验失败时立即抛出异常。

校验逻辑不再维护服务商白名单，只做安全边界检查：要求 `https`、存在 host、禁止 userInfo，并拦截 localhost、内网 IPv4、链路本地地址、云元数据地址、常见本地 IPv6 字面量和 IPv4 映射 IPv6 内网地址。

## 数据存储方案

不新增数据库表、字段或迁移脚本。继续使用现有 `sys_ai_engine_config.base_url` 字段。

## stage 更新说明

已同步更新 `runtime/STATE.md`，记录本轮后端修复范围、验证结果和停止边界。

## 编译结果

目标测试执行时已完成后端编译阶段并通过。

## 构建/测试结果

- `mvn test "-Dtest=PublicHttpsUrlValidatorTest,InterviewAiServiceImplTest"` 通过，38 个测试通过。
- `mvn test` 通过，394 个测试通过。

## 当前功能验收说明

- Mimo 等新模型服务商只要使用公网 HTTPS baseUrl，即可在管理端配置并被面试 AI 运行时使用。
- `http://`、`localhost`、`127.0.0.1`、`10.*`、`172.16-31.*`、`192.168.*`、`169.254.169.254` 等地址会被拒绝。
- 显式配置非法 baseUrl 时会直接报错，不会静默回退到豆包默认地址。

## 停止说明

本轮只修复 AI 引擎 baseUrl 白名单导致 Mimo 无法连通的问题，不新增模型供应商枚举、不改前端交互、不扩展连通性诊断接口。
