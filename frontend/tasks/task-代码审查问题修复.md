# 任务：代码审查问题修复（第二轮）

## 当前任务所属模块
前后端 - 代码质量与安全性修复

## 前端文件定位
- `frontend/app/src/utils/request.js` - 全局请求配置
- `frontend/app/src/views/interview/InterviewEntryView.vue` - 面试入口页
- `frontend/app/src/views/interview/InterviewSessionView.vue` - 面试会话页

## 后端文件定位
- `server/src/main/java/com/airesume/server/config/AsyncConfig.java` - 异步线程池配置（新增）
- `server/src/main/java/com/airesume/server/service/InterviewService.java` - 面试主服务
- `server/src/main/java/com/airesume/server/service/impl/InterviewAiServiceImpl.java` - AI 调用实现

## 本轮修改文件清单
1. `request.js` - 全局 timeout 从 60s 改回 30s
2. `InterviewEntryView.vue` - skipDefaultErrorHandler 增加 401 处理兜底
3. `InterviewSessionView.vue` - 开场白轮询增加最大次数限制（60次=3分钟）
4. `AsyncConfig.java` - 新增 AI 异步任务专用线程池配置
5. `InterviewService.java` - 注入 aiAsyncExecutor 替换 ForkJoinPool.commonPool；sendMessage/saveUserMessage 增加 openingGenerated 检查
6. `InterviewAiServiceImpl.java` - shouldFallbackToLocalMock 区分网络异常与业务异常；流式中断追加标记；getLatestResumeContext jobTargeted 逻辑修复

## 后端实现方案

### 1. 全局 timeout 回退
- `request.js` 默认 timeout 保持 30s，面试接口已单独配置 90s

### 2. 401 处理兜底
- InterviewEntryView.vue catch 块中增加 `err.response?.status === 401` 判断
- 命中时自动跳转登录页，避免用户看到模糊的"请求失败"

### 3. 开场白轮询超时上限
- 增加 `OPENING_POLL_MAX_ROUNDS = 60`（约 3 分钟）
- 超时后停止轮询，提示用户"开场白生成超时，请刷新页面重试"

### 4. 自定义线程池
- 新建 `AsyncConfig.java`，配置 `aiAsyncExecutor` Bean
- 核心 2 线程、最大 8 线程、队列容量 50
- 拒绝策略为 CallerRunsPolicy（队列满时由调用线程执行，避免丢失任务）
- InterviewService 注入 aiAsyncExecutor，两处 `CompletableFuture.runAsync` 均使用专用线程池

### 5. openingGenerated 竞态防护
- sendMessage 和 saveUserMessage 方法增加 `openingGenerated == 0` 检查
- 未生成开场白时抛出 BusinessException，前端可提示用户等待

### 6. 降级策略细化
- shouldFallbackToLocalMock 改为按异常类型判断：仅网络/连接/超时类异常才降级
- 业务异常（参数错误等）直接抛出，不降级到 Mock

### 7. 流式中断标记
- 流式回复中途网络异常且已输出部分内容时，追加 `…[网络中断，请重试]` 标记
- 避免前端只收到半句话

### 8. jobTargeted 逻辑修复
- getLatestResumeContext 中根据 `jdText` 是否存在判断是否为岗位定向面试
- 有 jdText → jobTargeted=true，无 jdText → jobTargeted=false

## 数据存储方案
- 无新增数据库变更

## stage 更新说明
- 当前为代码质量修复阶段（第二轮）
- 不属于版本功能开发，属于遗留问题修复

## 编译结果
- ✅ 后端编译通过（mvn compile -q 无错误输出）
- ✅ 前端构建通过（npm run build 无错误）

## 当前功能验收说明
1. 全局 timeout：保持 30s，面试接口单独 90s 不受影响
2. 401 处理：token 过期时自动跳转登录页
3. 开场白轮询：超过 3 分钟自动停止并提示用户刷新
4. 线程池：AI 异步任务使用专用线程池，不占用公共线程池
5. openingGenerated：开场白未生成完成时拒绝发送消息
6. 降级策略：仅网络异常降级到 Mock，业务异常直接报错
7. 流式中断：中途断开时追加中断标记，前端可感知
8. jobTargeted：普通面试不再被错误标记为岗位定向

## 停止，不继续下一个功能
- 本轮修复完成后将停止，等待验收
