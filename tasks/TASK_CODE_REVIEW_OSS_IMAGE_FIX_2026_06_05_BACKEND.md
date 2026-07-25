# 代码审查问题修复：社区图片 OSS 接入后端回归修复

## 当前任务所属模块

- 后端模块：社区图片上传与签名访问、全局安全放行配置、默认运行配置、社区服务构造器测试兼容。
- 前端模块：本轮未修改前端实现，仅复跑已存在的 PDF 分页目标测试。

## 本轮修改文件清单

- `server/src/main/resources/application.yml`
- `server/src/main/java/com/airesume/server/controller/CommunityController.java`
- `server/src/main/java/com/airesume/server/service/CommunityService.java`
- `server/src/test/java/com/airesume/server/config/RuntimeProtectionConfigTest.java`
- `server/src/test/java/com/airesume/server/controller/CommunityControllerImageAccessTest.java`
- `server/src/test/java/com/airesume/server/config/WebMvcConfigTest.java`
- `server/src/test/java/com/airesume/server/config/SecurityConfigTest.java`
- `server/src/test/java/com/airesume/server/service/CommunityServiceConstructorInjectionTest.java`

## 后端实现方案

- 将 `application.yml` 中 RabbitMQ 配置恢复到 `spring.rabbitmq` 命名空间，`server.shutdown` 保持独立顶层配置，避免 Spring Boot 忽略 MQ host/password/virtual-host。
- `CommunityController.getImage` 保留 302 跳转到 OSS 签名 URL，但日志只记录 `objectKey` 与 `referer`，不再记录签名 URL 或签名参数。
- `CommunityService` 补回兼容既有单元测试的 9 参数构造器，生产注入入口仍为带 `OssService` 的完整构造器并由 `@Autowired` 明确标记。
- 同步旧测试断言：公共图片路径迁移到 `/api/community/images/**`，生产构造器依赖数量更新为 10。

## 数据存储方案

- 本轮不修改数据库表、字段、索引、schema 或 migration。

## stage 更新说明

- 已在 `tasks/stage.md` 追加“社区图片 OSS 接入代码审查问题修复”记录，包含问题、修复、验证命令和停止边界。

## 编译与测试结果

- RED 验证：新增/同步回归测试后，旧实现下 `RuntimeProtectionConfigTest.shouldKeepRabbitMqConfigurationUnderSpringNamespace` 失败于缺少 `spring.rabbitmq`，`CommunityControllerImageAccessTest.shouldRedirectToSignedUrlWithoutLoggingCredentialUrl` 失败于日志包含签名参数。
- GREEN 目标回归：`mvn.cmd -q "-Dtest=RuntimeProtectionConfigTest,CommunityControllerImageAccessTest,WebMvcConfigTest,CommunityServicePostQueryDeleteTest,SecurityConfigTest,CommunityServiceConstructorInjectionTest" test` 通过。
- 后端编译：`mvn.cmd -q -DskipTests compile` 通过。
- 后端全量测试：`mvn.cmd -q test` 通过，771 个测试。
- 前端目标回归：`npm.cmd test -- src/__tests__/utils/resumePdfPagination.test.js` 通过，1 个测试文件 / 5 个用例。

## 当前功能验收说明

- RabbitMQ 默认配置重新由 Spring Boot 正确绑定。
- 社区图片签名访问仍返回签名 URL 给浏览器，但不会把临时访问凭证写入业务日志。
- OSS 接入后的社区服务构造器与旧单元测试兼容，完整后端测试不再在 testCompile 或旧断言处失败。

## 停止，不继续下一个功能

本轮仅修复本次未提交改动代码审查中指出的社区图片 OSS 接入相关回归问题，不继续扩展图片审核、对象存储清理、CDN、前端上传体验或其它社区功能。
