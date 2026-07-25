# TASK_CORE_BUGFIX_ROUND_8_SECURITY_AND_TOKEN_ALIGNMENT

## 当前任务所属模块
- 后端认证与安全防护
- 简历诊断任务处理链路
- 前端登录态恢复与引导页判断

## 前端文件定位
- `frontend/app/src/App.vue`
- `frontend/app/src/layouts/MainLayout.vue`

## 后端文件定位
- `server/src/main/java/com/airesume/server/service/PdfTextExtractor.java`
- `server/src/main/java/com/airesume/server/service/ResumeDiagnosisTaskService.java`
- `server/src/main/java/com/airesume/server/service/impl/ResumeDiagnosisTaskServiceImpl.java`
- `server/src/main/java/com/airesume/server/service/impl/ResumeDiagnosisProcessor.java`
- `server/src/main/java/com/airesume/server/mapper/ResumeDiagnosisTaskMapper.java`
- `server/src/main/java/com/airesume/server/infrastructure/security/JwtProperties.java`
- `server/src/main/java/com/airesume/server/service/impl/AuthServiceImpl.java`
- `server/src/main/resources/application.yml`
- `server/src/main/resources/application-dev.yml`

## 本轮修改文件清单
- 后端
  - `server/src/main/java/com/airesume/server/service/PdfTextExtractor.java`
  - `server/src/main/java/com/airesume/server/service/ResumeDiagnosisTaskService.java`
  - `server/src/main/java/com/airesume/server/service/impl/ResumeDiagnosisTaskServiceImpl.java`
  - `server/src/main/java/com/airesume/server/service/impl/ResumeDiagnosisProcessor.java`
  - `server/src/main/java/com/airesume/server/mapper/ResumeDiagnosisTaskMapper.java`
  - `server/src/main/java/com/airesume/server/infrastructure/security/JwtProperties.java`
  - `server/src/main/java/com/airesume/server/service/impl/AuthServiceImpl.java`
  - `server/src/main/resources/application.yml`
  - `server/src/main/resources/application-dev.yml`
  - `server/src/test/java/com/airesume/server/service/impl/AuthServiceImplTest.java`
  - `server/src/test/java/com/airesume/server/service/impl/ResumeDiagnosisProcessorTest.java`
- 前端
  - `frontend/app/src/App.vue`
  - `frontend/app/src/layouts/MainLayout.vue`

## 后端实现方案

### 1. 文件读取路径遍历修复
- `PdfTextExtractor.resolveAbsolutePath` 仅允许解析 `/uploads/resumes/` 前缀
- 将文件路径约束到项目 `uploads/resumes` 根目录内
- 对越界路径和空路径直接抛出异常

### 2. 简历诊断任务原子抢占
- `ResumeDiagnosisTaskMapper` 新增 `claimPendingTask`
- 仅当任务状态为 `PENDING` 时，原子更新为 `PROCESSING`
- `ResumeDiagnosisTaskService.updateStatusToProcessing` 改为返回 `boolean`
- `ResumeDiagnosisProcessor` 在抢占失败时立即跳过，不继续解析 PDF、调用 AI 或退款

### 3. `resume:task` 缓存失效
- `ResumeDiagnosisTaskServiceImpl` 在任务状态变更和解析结果写入方法上补 `@CacheEvict(allEntries = true)`
- 本轮以正确性优先，先接受全量失效

### 4. JWT 弱默认值修复
- `JwtProperties` 增加启动期校验
- 要求：
  - `JWT_SECRET` 不能为空
  - 不能使用默认占位符
  - 长度至少 32 位
- `application.yml` 与 `application-dev.yml` 改为显式读取 `${JWT_SECRET}`

### 5. 登录限流兜底
- `AuthServiceImpl` 在 Redis 不可用时使用进程内失败次数计数
- 继续维持 `5 次 / 15 分钟` 语义
- Redis 恢复时仍优先使用 Redis

### 6. 密码找回防枚举
- `resetPasswordBySecurityQuestion` 对“用户不存在 / 未设置安全问题 / 答案错误”统一返回：
  - `用户名或安全问题答案不正确`

## 前端实现方案
- `App.vue` 统一使用 `getToken()` / `removeToken()`
- `MainLayout.vue` 统一使用 `getToken()` 判断登录态
- 页面不再依赖旧的 `localStorage.getItem('token')`

## 数据存储方案
- 不新增数据库表或字段
- 不修改 `resume_diagnosis_task` 主表结构
- 前端仍使用现有本地存储键：
  - `ai_resume_token`
  - `ai_resume_token_type`

## stage 更新说明
- 新增 `runtime/STAGE_TASK_CORE_BUGFIX_ROUND_8.md`
- 更新 `frontend/tasks/stage.md`

## 编译结果
- 后端编译：`mvn -q -DskipTests compile` 通过
- 前端构建：`npm.cmd run build` 通过

## 当前功能验收说明
1. 伪造 `fileUrl` 越界读取时被拒绝
2. 同一简历诊断任务不会被多个消费者重复抢占
3. 任务状态变更后不再返回旧缓存
4. 未配置或弱配置 `JWT_SECRET` 时启动失败
5. Redis 不可用时登录失败次数仍会累计并锁定
6. 密码找回失败口径统一，避免用户名枚举
7. 仅存在 `ai_resume_token` 时刷新页面仍能恢复登录态

## 停止，不继续下一个功能
- 本轮只处理第一批真实存在的严重问题
- 不继续处理全局限流、Spring Boot 升级、Interview 超时取消链路
