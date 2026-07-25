# TASK_CORE_BUGFIX_ROUND_9_REMAINING_CRITICAL_ISSUES

## 当前任务所属模块
- 后端安全限流与资源保护
- 模拟面试流式链路取消控制
- 面试旧写链路收口
- 运行时安全配置修正

## 前端文件定位
- 本轮无前端代码改动

## 后端文件定位
- `server/src/main/java/com/airesume/server/infrastructure/security/CriticalEndpointRateLimitFilter.java`
- `server/src/main/java/com/airesume/server/config/SecurityConfig.java`
- `server/src/main/java/com/airesume/server/controller/InterviewController.java`
- `server/src/main/java/com/airesume/server/service/InterviewService.java`
- `server/src/main/java/com/airesume/server/service/InterviewSessionService.java`
- `server/src/main/java/com/airesume/server/service/impl/InterviewSessionServiceImpl.java`
- `server/src/main/java/com/airesume/server/common/result/ResultCode.java`
- `server/src/main/resources/application-dev.yml`
- `server/pom.xml`

## 本轮修改文件清单
- `server/src/main/java/com/airesume/server/infrastructure/security/CriticalEndpointRateLimitFilter.java`
- `server/src/main/java/com/airesume/server/config/SecurityConfig.java`
- `server/src/main/java/com/airesume/server/controller/InterviewController.java`
- `server/src/main/java/com/airesume/server/service/InterviewService.java`
- `server/src/main/java/com/airesume/server/service/InterviewSessionService.java`
- `server/src/main/java/com/airesume/server/service/impl/InterviewSessionServiceImpl.java`
- `server/src/main/java/com/airesume/server/common/result/ResultCode.java`
- `server/src/main/resources/application-dev.yml`
- `server/pom.xml`
- `server/src/test/java/com/airesume/server/infrastructure/security/CriticalEndpointRateLimitFilterTest.java`
- `server/src/test/java/com/airesume/server/service/InterviewServiceTest.java`

## 后端实现方案

### 1. 关键高成本接口最小限流兜底
- 新增 `CriticalEndpointRateLimitFilter`
- 先只覆盖审计报告点名的高风险写接口：
  - `POST /api/auth/register`
  - `POST /api/resume/upload`
  - `POST /api/interview/session`
  - `POST /api/interview/session/**`
- 注册接口按来源 IP 限流
- 已登录的简历上传和面试写接口优先按用户 ID 限流，未登录时回退到 IP
- 本轮采用进程内窗口计数，不新增 Redis 依赖，不改业务接口签名
- 超限统一返回 `429` 和标准 `ResultCode.TOO_MANY_REQUESTS`

### 2. SSE 超时/断开后取消 AI 流
- `InterviewController` 在建立 `ResponseBodyEmitter` 后立即注册超时、完成、异常回调
- 连接已经关闭时，后台线程在继续读历史、落用户消息、调用 AI 之前都会再次检查关闭标记
- `InterviewService.subscribeAndWriteStream` 在 `onTimeout/onCompletion/onError` 时取消 `Subscription`
- 这样可以阻断“前端已经断开，后端 AI 流还在继续跑”的资源泄漏

### 3. 收口旧的双会话写链路
- `InterviewSessionService` 接口删掉 `createSession/sendMessage/endInterview`
- `InterviewSessionServiceImpl` 改为只保留查询与历史聚合能力
- 当前主链路统一走 `InterviewService`
- 当前 `AdminDashboardServiceImpl` 仍通过 `InterviewSessionService` 做统计查询，不受影响

### 4. 运行时安全配置修正
- `application-dev.yml` 去掉 `JWT_SECRET` fallback，保持与上一轮“必须显式配置”决策一致
- `pom.xml` 将 Spring Boot 从 `3.2.3` 升级到 `3.2.11`
- 本轮只做安全补丁升级，不扩展到其他依赖整理

## 数据存储影响说明
- 不新增数据库表
- 不修改现有表结构
- 不新增缓存键结构
- 进程内限流状态只存在于应用内存，重启后自然清空

## stage 更新说明
- 新增 `runtime/STAGE_TASK_CORE_BUGFIX_ROUND_9.md`
- 本轮只记录剩余严重问题的真实修复状态，不扩展到中高优先级项

## 编译结果
- 后端编译：`mvn -q -DskipTests compile` 通过
- 后端测试：`mvn -q test` 通过
- 前端构建回归：`npm.cmd run build` 通过

## 当前验收说明
1. 注册、简历上传、面试写接口在短时间重复请求时会返回 `429`
2. 面试流式连接超时或断开后，后端会主动取消 `Publisher` 订阅
3. 仓库中不再保留可直接误用的旧面试写接口
4. 开发环境也必须显式提供 `JWT_SECRET`
5. Spring Boot 已切换到包含审计建议补丁的 `3.2.11`
6. Spring 上下文测试已同步显式注入测试用 `jwt.secret`，不再被启动期校验阻断

## 停止点
- 本轮只处理剩余严重问题
- 不继续扩展到 `SecurityConfig` 的 `ASYNC`、CORS 校验、XSS、Vite 升级等高优先级项
- 完成验证后停止，等待验收
