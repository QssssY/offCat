# AI 基础地址 DNS 启动修复任务记录

## 当前任务所属模块

后端 AI 引擎基础地址校验与应用启动兼容性修复。

## 前端文件定位

本轮不涉及前端文件，不修改页面、交互或前端接口封装。

## 后端文件定位

- `server/src/main/java/com/airesume/server/common/util/PublicHttpsUrlValidator.java`
- `server/src/main/java/com/airesume/server/service/impl/ResumeAiServiceImpl.java`
- `server/src/main/java/com/airesume/server/service/impl/InterviewAiServiceImpl.java`
- `server/src/test/java/com/airesume/server/common/util/PublicHttpsUrlValidatorTest.java`
- `server/src/test/java/com/airesume/server/service/impl/ResumeAiServiceImplTest.java`
- `server/src/test/java/com/airesume/server/service/impl/InterviewAiServiceImplTest.java`

## 本轮修改文件清单

- 新增 `PublicHttpsUrlValidator.validateWithoutDnsResolution()`，用于启动期只校验 HTTPS、用户信息、本机/内网/云元数据字面量地址，不执行 DNS 解析。
- 保留原 `PublicHttpsUrlValidator.validate()` 的完整 DNS 解析校验，运行时 AI 出网调用仍会拦截解析到本机、内网或云元数据地址的域名，避免 SSRF 风险。
- `ResumeAiServiceImpl` 构造器改为启动期轻量校验 base-url，真正发起简历 AI 调用时仍走完整校验。
- `InterviewAiServiceImpl` 构造器同步采用启动期轻量校验，避免面试 AI 配置遇到同类 DNS 抖动后继续阻断启动。
- 新增回归测试覆盖：跳过 DNS 模式允许暂时不可解析的公网域名、简历 AI 构造器不依赖 DNS、面试 AI 构造器不依赖 DNS。

## 前端实现方案

本轮不涉及前端实现。

## 后端实现方案

- 根因：`ResumeAiServiceImpl` 构造器在 Bean 初始化期间调用 `PublicHttpsUrlValidator.validate()`，该方法会立即进行 DNS 解析；当 `app.ai.base-url` 域名暂时不可解析时，构造器抛出 `基础地址域名无法解析`，导致 `adminController -> resumeAiService` 依赖链启动失败。
- 修复：将启动期校验与运行时出网校验拆开。启动期只做不依赖网络的静态 URL 校验，确保服务可启动；运行时解析激活 AI 配置并发起请求前继续使用原完整校验，安全边界不下沉。
- 范围控制：未修改 AI 调用协议、模型配置、接口返回结构、数据库结构或前端逻辑。

## 数据存储方案

本轮不新增表、不修改字段、不新增索引，不涉及 `db/` 或 `server/db/` SQL 脚本变更。

## stage 更新说明

已更新根目录 `tasks/stage.md`，记录 AI 基础地址 DNS 启动修复、验证结果和停止边界。

## 编译结果

- `mvn -q -DskipTests compile` 通过。

## 构建结果

本轮为后端小修复，未执行打包构建；已执行定向测试、全量测试和启动验证。

## 当前功能验收说明

- RED 验证：`mvn -q "-Dtest=PublicHttpsUrlValidatorTest,ResumeAiServiceImplTest,InterviewAiServiceImplTest" test` 在生产代码修改前失败，失败点为缺少启动期跳过 DNS 的校验方法。
- GREEN 验证：`mvn -q "-Dtest=PublicHttpsUrlValidatorTest,ResumeAiServiceImplTest,InterviewAiServiceImplTest" test` 通过。
- 全量验证：`mvn -q test` 通过。
- 启动验证：使用 `--app.ai.base-url=https://startup-only.invalid/v1 --app.interview.base-url=https://startup-only.invalid/v1` 覆盖两个 AI 基础地址后，服务已启动到 `Started ServerApplication`；命令最终超时是因为 Spring Boot 服务为长驻进程。

## 停止，不继续下一个功能

本轮仅修复 AI 基础地址 DNS 解析失败导致后端启动中断的问题，不继续扩展 AI 配置管理、连通测试、模型路由或生产环境密钥配置。
