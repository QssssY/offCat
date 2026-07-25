# STAGE_TASK_CORE_BUGFIX_ROUND_8

## 当前阶段结论

- 已完成文件读取路径边界校验
- 已完成简历诊断任务原子抢占与重复处理阻断
- 已完成 `resume:task` 缓存失效补齐
- 已完成 JWT 弱默认值移除与启动期强校验
- 已完成 Redis 故障下的登录限流兜底
- 已完成密码找回统一失败口径，防止用户名枚举
- 已完成前端 token 读取链路统一
- 本轮到此停止，等待验收

## 本轮关键事实

### 修复前
- `PdfTextExtractor` 可基于伪造路径读取项目目录外文件
- 简历诊断任务存在先查后改，多个消费者可重复处理同一任务
- `resume:task` 存在查缓存但无状态变更失效
- `JWT_SECRET` 存在弱默认占位值
- Redis 故障时登录暴力破解防护完全失效
- 密码找回对不同失败原因返回不同错误
- 前端仍有页面读取旧键名 `token`

### 修复后
- `PdfTextExtractor` 仅允许访问 `uploads/resumes` 目录内文件
- 简历诊断任务只能从 `PENDING` 原子切换到 `PROCESSING`
- 任务状态写入和解析结果写入均触发 `resume:task` 缓存失效
- JWT 启动时必须显式提供合法强密钥
- Redis 读写异常时自动切换到进程内限流兜底
- 密码找回失败统一返回“用户名或安全问题答案不正确”
- `App.vue` 与 `MainLayout.vue` 均只读取 `ai_resume_token`

## 代码落点

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
- 前端
  - `frontend/app/src/App.vue`
  - `frontend/app/src/layouts/MainLayout.vue`

## 验证结果

- 后端编译：`mvn -q -DskipTests compile` 通过
- 前端构建：`npm.cmd run build` 通过
- 单元测试文件已补：
  - `AuthServiceImplTest`
  - `ResumeDiagnosisProcessorTest`

## 风险备注

- `resume:task` 本轮使用 `allEntries = true`，优先保证正确性
- `InterviewService.java` 与 `NotificationService.java` 为此前已有未提交修改，本轮未继续扩展其范围
- 本轮未处理全局限流、依赖升级与 SSE 超时取消
