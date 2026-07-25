# STAGE_TASK_CORE_BUGFIX_ROUND_9

## 当前阶段结论

- 已补上关键高成本接口的最小限流兜底
- 已补上流式面试连接断开后的订阅取消
- 已收口 `InterviewSessionService` 旧写链路，只保留查询职责
- 已清理 `application-dev.yml` 中残留的 `JWT_SECRET` fallback
- 已将 Spring Boot 升级到 `3.2.11`
- 本轮做到这里停止，等待验收

## 本轮关键事实

### 修复前
- 剩余严重问题中，全局限流仍不存在
- 流式面试连接超时或断开后，订阅不会被主动取消
- 旧 `InterviewSessionServiceImpl` 仍保留完整写链路，虽然当前主控制器未调用，但存在误用风险
- `application-dev.yml` 与上一轮既定策略不一致，仍保留 `JWT_SECRET` fallback
- Spring Boot 仍是 `3.2.3`

### 修复后
- 注册、简历上传、面试写接口都经过统一过滤器限流
- `ResponseBodyEmitter` 生命周期与 AI 订阅取消联动
- 旧面试服务接口只保留读能力，写能力统一收口到 `InterviewService`
- dev 环境也必须显式配置 `JWT_SECRET`
- Spring Boot 已升级至 `3.2.11`

## 代码落点

- `server/src/main/java/com/airesume/server/infrastructure/security/CriticalEndpointRateLimitFilter.java`
- `server/src/main/java/com/airesume/server/config/SecurityConfig.java`
- `server/src/main/java/com/airesume/server/controller/InterviewController.java`
- `server/src/main/java/com/airesume/server/service/InterviewService.java`
- `server/src/main/java/com/airesume/server/service/InterviewSessionService.java`
- `server/src/main/java/com/airesume/server/service/impl/InterviewSessionServiceImpl.java`
- `server/src/main/java/com/airesume/server/common/result/ResultCode.java`
- `server/src/main/resources/application-dev.yml`
- `server/pom.xml`

## 验证结果

- 后端编译：`mvn -q -DskipTests compile` 通过
- 后端测试：`mvn -q test` 通过
- 前端构建回归：`npm.cmd run build` 通过

## 风险备注

- 本轮限流先采用进程内实现，优先止血，暂不处理分布式实例间共享计数
- `InterviewService.java` 原文件仍包含部分历史中文乱码，本轮只对严重问题相关块做必要修改
- 本轮未继续推进高优先级和中优先级问题
