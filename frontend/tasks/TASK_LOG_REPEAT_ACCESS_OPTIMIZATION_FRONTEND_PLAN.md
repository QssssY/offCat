# 日志重复访问与数据库读降噪前端修复计划

## 当前任务所属模块

前端 API 短缓存、pending 请求复用、轮询停表和重复挂载保护。

本文档是前端侧修复计划，配套根目录计划：`tasks/task-日志异常与重复访问优化修复计划.md`。

## 当前问题定位

- `logs/ai-resume.log` 中 `Get latest version logs` 出现 12 次，入口为 `frontend/app/src/views/HomePageView.vue` 调用 `getLatestVersionLogs(3)`。
- `logs/ai-resume.log` 中 `查询引导状态` 出现 20 次，入口为 `frontend/app/src/layouts/MainLayout.vue` 调用 `getOnboardingStatus()`。
- `logs/ai-resume.log` 中 `[社区] 查询帖子详情` 和 `[社区] 查询评论列表` 各出现 11 次，当前 `frontend/app/src/api/community.js` 只有帖子列表使用 `cachedGet`，详情和评论未缓存。
- `logs/ai-resume.log` 中 `获取会话轻量状态` 出现 21 次，报告页轮询位于 `frontend/app/src/views/interview/InterviewReportView.vue`，面试开场白轮询位于 `frontend/app/src/views/interview/InterviewSessionView.vue`。

## 前端文件定位

- `frontend/app/src/utils/apiCache.js`
- `frontend/app/src/api/versionLog.js`
- `frontend/app/src/api/onboarding.js`
- `frontend/app/src/api/community.js`
- `frontend/app/src/views/interview/InterviewReportView.vue`
- `frontend/app/src/views/interview/InterviewSessionView.vue`
- `frontend/app/src/__tests__/utils/apiCache.test.js`
- `frontend/app/src/__tests__/api/versionLog.test.js`
- `frontend/app/src/__tests__/api/onboarding.test.js`
- `frontend/app/src/__tests__/api/community.test.js`
- `frontend/app/src/__tests__/views/InterviewReportView.test.js`
- `frontend/app/src/__tests__/views/InterviewSessionView.test.js`

## 后端文件定位

本前端计划不直接修改后端；后端日志级别、缓存兜底和敏感日志脱敏见根目录计划。

## 本轮修改文件清单

- 新增 `frontend/tasks/TASK_LOG_REPEAT_ACCESS_OPTIMIZATION_FRONTEND_PLAN.md`。
- 本计划不修改业务代码。

## 前端实现方案

### P1-1 扩展 API 短缓存类型

目标：

- 对稳定 GET 数据使用已有 `cachedGet`，复用 pending Promise，避免同一页面短时间重复请求。
- 写操作后按 prefix 主动清缓存，避免展示旧数据。

计划修改：

- 在 `frontend/app/src/utils/apiCache.js` 的 `API_CACHE_TTL` 增加：
  - `VERSION_LOGS: 60 * 1000`
  - `ONBOARDING_STATUS: 60 * 1000`
  - `COMMUNITY_DETAIL: 15 * 1000`
  - `COMMUNITY_COMMENTS: 15 * 1000`
- 保持现有 `cachedGet` 失败后删除缓存的行为，避免失败响应被缓存。
- 保持写操作后 `clearApiCacheByPrefix` 的清理方式，不引入全局状态库。

测试：

- 扩展 `apiCache.test.js`，验证新增 TTL 常量存在。
- 验证同 key pending 请求只调用一次 loader。
- 验证 loader 失败后缓存被清理，下一次会重新请求。

### P1-2 版本日志短缓存

目标：

- 首页短时间重复进入或组件重复挂载时，`getLatestVersionLogs(3)` 只发起一次网络请求。

计划修改：

- `frontend/app/src/api/versionLog.js` 引入 `cachedGet`、`buildCacheKey`、`API_CACHE_TTL`。
- 将 `getLatestVersionLogs(limit = 5)` 改为按 limit 构造 key，例如 `version:latest:limit=3`。
- 保持请求 URL、method、params 不变，不改后端契约。

参考实现形态：

```js
export function getLatestVersionLogs(limit = 5) {
  return cachedGet(buildCacheKey('version:latest', { limit }), API_CACHE_TTL.VERSION_LOGS, () =>
    request({
      url: '/api/version/logs/latest',
      method: 'get',
      params: { limit }
    })
  )
}
```

测试：

- 新增 `frontend/app/src/__tests__/api/versionLog.test.js`。
- mock `@/utils/request`，连续调用两次 `getLatestVersionLogs(3)`，断言 request 只调用一次。
- 调用 `getLatestVersionLogs(5)` 后应产生不同 key，断言 request 对不同 limit 分别调用。

### P1-3 引导状态短缓存与写后清理

目标：

- `MainLayout`、路由切换和重复挂载时不连续请求 `/api/user/onboarding/status`。
- 用户更新引导状态或完成任务后，下一次读取必须拿到新状态。

计划修改：

- `frontend/app/src/api/onboarding.js` 的 `getOnboardingStatus()` 改为 `cachedGet('onboarding:status', API_CACHE_TTL.ONBOARDING_STATUS, loader)`。
- `updateOnboardingStatus()`、`getOnboardingTasks()` 如读取任务列表仍保持实时；`completeOnboardingTask()` 成功后调用 `clearApiCacheByPrefix('onboarding')`。
- 如果 `updateOnboardingStatus()` 已存在成功回调，也在成功后调用 `clearApiCacheByPrefix('onboarding')`。

测试：

- 新增 `frontend/app/src/__tests__/api/onboarding.test.js`。
- 连续两次 `getOnboardingStatus()` 只触发一次 request。
- `updateOnboardingStatus()` 成功后再次 `getOnboardingStatus()` 会重新触发 request。
- `completeOnboardingTask()` 成功后再次 `getOnboardingStatus()` 会重新触发 request。

### P1-4 社区详情与评论短缓存

目标：

- 同一 postId 的详情和同一评论分页短时间重复进入时只请求一次。
- 点赞、收藏、评论、删除、审核隐藏等写操作后主动清理社区缓存。

计划修改：

- `frontend/app/src/api/community.js` 的 `getPostDetail(postId)` 改为：
  - key: `community:postDetail:postId=<postId>`
  - TTL: `API_CACHE_TTL.COMMUNITY_DETAIL`
- `getComments(postId, params)` 改为：
  - key: `community:comments:postId=<postId>&pageNum=<pageNum>&pageSize=<pageSize>`
  - TTL: `API_CACHE_TTL.COMMUNITY_COMMENTS`
- 保持 `createPost`、`togglePostLike`、`togglePostFavorite`、`createComment`、`deleteComment`、管理员隐藏等已有 `clearApiCacheByPrefix('community')` 语义。
- 若发现某个写操作当前没有清理社区缓存，应补齐成功后的 `clearApiCacheByPrefix('community')`，不改变请求参数和响应结构。

参考实现形态：

```js
export function getPostDetail(postId) {
  return cachedGet(buildCacheKey('community:postDetail', { postId }), API_CACHE_TTL.COMMUNITY_DETAIL, () =>
    request({
      url: `/api/community/posts/${postId}`,
      method: 'get'
    })
  )
}

export function getComments(postId, params) {
  return cachedGet(buildCacheKey('community:comments', { postId, ...params }), API_CACHE_TTL.COMMUNITY_COMMENTS, () =>
    request({
      url: `/api/community/posts/${postId}/comments`,
      method: 'get',
      params
    })
  )
}
```

测试：

- 新增 `frontend/app/src/__tests__/api/community.test.js`。
- 连续两次 `getPostDetail(1)` 只触发一次 request。
- `getPostDetail(1)` 和 `getPostDetail(2)` 使用不同缓存。
- 连续两次 `getComments(1, { pageNum: 1, pageSize: 20 })` 只触发一次 request。
- `createComment(1, data)` 成功后再次 `getComments(1, ...)` 会重新请求。

### P1-5 报告页轮询停表与不重叠

目标：

- `InterviewReportView.vue` 只轮询轻量状态接口。
- 单个 sessionId 同一时刻最多一个状态请求在途。
- 报告就绪、错误、离开页面或组件卸载后停止轮询。

计划修改：

- 在 `InterviewReportView.vue` 中增加 `statusRequestInFlight` 或复用现有请求锁。
- 轮询函数开始前判断：
  - 已有请求在途时跳过本轮。
  - `reportReady === true` 时停止定时器。
  - 当前 `sessionId` 变化时清理旧定时器和旧请求状态。
- 保持前几轮 3 秒轮询；超过固定轮数后调整为 6 秒，减少长时间报告生成期间的请求量。
- 报告就绪后只调用一次完整详情加载，不循环读取完整报告。

测试：

- 扩展 `InterviewReportView.test.js`。
- 使用 fake timers，验证多次 timer tick 在请求未完成时不会重复调用状态接口。
- mock 状态返回 `reportReady=true` 后，验证定时器停止且只调用一次完整详情加载。
- mock 路由 sessionId 切换，验证旧轮询被清理。

### P1-6 面试页开场白轮询防重复

目标：

- `InterviewSessionView.vue` 开场白生成轮询不因组件 keep-alive、路由回退或重复启动创建多个 timer。
- 开场白已生成或会话状态进入结束态后立即停止轮询。

计划修改：

- 为开场白状态轮询增加单一 timer 管理函数，例如 `startOpeningStatusPolling()` 和 `stopOpeningStatusPolling()`。
- 启动前先清理旧 timer。
- 请求在途时跳过下一轮 tick。
- 状态满足 `openingGenerated=true` 后停止轮询，并只更新一次本地开场白状态。

测试：

- 扩展 `InterviewSessionView.test.js`。
- 连续调用启动轮询两次，只保留一个定时器效果。
- 请求未完成时推进 timer，不重复调用状态接口。
- 返回 `openingGenerated=true` 后停止后续状态请求。

## 数据存储方案

前端计划不涉及数据库表、字段、索引或迁移脚本变更。

## stage 更新说明

本轮仅新增计划文档，不更新 `frontend/tasks/stage.md`。后续真正实施前端修复时，再按实际完成内容更新前端 stage。

## 编译结果

本轮未修改业务代码，未执行前端测试或编译。

## 构建结果

本轮未执行前端构建。

## 后续验证命令

实施前端修复后执行：

- `cd frontend/app && npm.cmd test -- --run src/__tests__/utils/apiCache.test.js src/__tests__/api/versionLog.test.js src/__tests__/api/onboarding.test.js src/__tests__/api/community.test.js src/__tests__/views/InterviewReportView.test.js src/__tests__/views/InterviewSessionView.test.js`
- `cd frontend/app && npm.cmd run build`

联动后端日志复查：

- `rg -n "Get latest version logs|查询引导状态|\\[社区\\] 查询帖子详情|\\[社区\\] 查询评论列表|获取会话轻量状态" logs/ai-resume.log`

验收标准：

- 首页同一次访问中版本日志请求不重复。
- 主布局同一次挂载周期内引导状态请求不重复，写后能刷新。
- 社区同一详情和同一评论分页短时间内只请求一次，写后能刷新。
- 报告页和面试页轮询无重叠请求，完成后停止。

## 停止，不继续下一功能

本轮只制定前端修复计划，不开始实现。后续需要单独确认执行 P1 前端降噪任务，完成后更新 `frontend/tasks/stage.md` 并停止等待验收。

## P1 前端降噪执行记录（2026-06-06）

### 本轮修改文件清单

- 修改 `frontend/app/src/utils/apiCache.js`。
- 修改 `frontend/app/src/api/versionLog.js`。
- 修改 `frontend/app/src/api/onboarding.js`。
- 修改 `frontend/app/src/api/community.js`。
- 修改 `frontend/app/src/views/interview/InterviewReportView.vue`。
- 修改 `frontend/app/src/views/interview/InterviewSessionView.vue`。
- 补充 `frontend/app/src/__tests__/utils/apiCache.test.js`。
- 补充 `frontend/app/src/__tests__/api/versionLog.test.js`。
- 新增 `frontend/app/src/__tests__/api/onboarding.test.js`。
- 新增 `frontend/app/src/__tests__/api/community.test.js`。
- 补充 `frontend/app/src/__tests__/views/InterviewReportView.test.js`。
- 补充 `frontend/app/src/__tests__/views/InterviewSessionView.test.js`。

### 已完成内容

- `API_CACHE_TTL` 已补充版本日志、引导状态、社区详情、社区评论短 TTL。
- `getLatestVersionLogs(limit)` 按 limit 缓存 60 秒，复用同 key pending/value，不改变 URL、method 或 params。
- `getOnboardingStatus()` 缓存 60 秒；`updateOnboardingStatus()` 与 `completeOnboardingTask()` 成功后清理 `onboarding` 前缀缓存。
- `getPostDetail(postId)` 与 `getComments(postId, params)` 分别按 postId / 分页参数缓存 15 秒；既有评论、点赞、收藏、删除、管理员隐藏等写操作继续清理 `community` 前缀。
- 报告页状态轮询改为递归 `setTimeout`：前 6 轮 3 秒，之后 6 秒；同一时刻只保留一个状态轮询请求，报告详情成功回填后停止。
- 面试页开场白轮询增加轮询代际与在途标记；重复启动或卸载会让旧轮询失效，`openingGenerated=true`、`openingPending=false` 或会话结束后停止后续轮询。

### 验证结果

- RED 验证：`npm.cmd test -- --run src/__tests__/utils/apiCache.test.js src/__tests__/api/versionLog.test.js src/__tests__/api/onboarding.test.js src/__tests__/api/community.test.js src/__tests__/views/InterviewReportView.test.js src/__tests__/views/InterviewSessionView.test.js` 在旧实现下失败，覆盖缺少 TTL、未缓存版本日志/引导状态/社区详情评论、报告轮询未退避、开场白 `openingGenerated` 未停止等问题。
- GREEN 验证：同一命令通过，6 个测试文件 / 78 个用例通过。
- 扩展验证：`npm.cmd test -- --run src/__tests__/api/performanceCache.test.js src/__tests__/layouts/MainLayout.test.js src/__tests__/views/HomePageView.test.js src/__tests__/views/community/PostDetailView.test.js src/__tests__/components/community/CommentSection.test.js` 中前 4 个相关测试文件通过；`CommentSection.test.js` 存在既有断言漂移，实际提示为 `单张图片不能超过2MB`、旧断言期望 `5MB`，与本轮缓存/轮询改动无关。
- 构建验证：`npm.cmd run build` 通过。
- 敏感日志复查：`rg -n 'OSSAccessKeyId=[^*&\\s]{8,}|Signature=[^*&\\s]{8,}|Authorization: Bearer|\"messages\"\\s*:\\s*\\[|apiKey\\s*[:=]\\s*[^*\\s,;}&\"]{8,}|accessKeySecret\\s*[:=]\\s*[^*\\s,;}&\"]{8,}|accessKeyId\\s*[:=]\\s*[^*\\s,;}&\"]{8,}' logs debug.txt` 无命中。
- 重复访问日志复查：`rg -n "Get latest version logs|查询引导状态|\\[社区\\] 查询帖子详情|\\[社区\\] 查询评论列表|获取会话轻量状态" logs/ai-resume.log` 当前仅命中历史样本中的 3 行；本轮未启动后端服务生成新访问日志。

### 数据存储方案

- 本轮不新增表、不修改字段、不新增索引，不涉及数据库 schema 或 migration。
- 本轮不修改后端业务代码。

### 停止说明

本轮只完成本计划 P1-1 至 P1-6 前端重复访问降噪与轮询稳定化；不继续推进新的前端功能、后端接口、数据库结构或其它优化项。

## P1-7 残留通知与版本分页短缓存执行记录（2026-06-06）

### 本轮修改文件清单

- 修改 `frontend/app/src/api/notification.js`。
- 修改 `frontend/app/src/api/publicVersionLog.js`。
- 修改 `frontend/app/src/utils/apiCache.js`。
- 修改 `frontend/app/src/__tests__/api/performanceCache.test.js`。
- 修改 `frontend/app/src/__tests__/api/versionLog.test.js`。
- 更新 `frontend/tasks/TASK_LOG_REPEAT_ACCESS_OPTIMIZATION_FRONTEND_PLAN.md` 与 `frontend/tasks/stage.md`。

### 已完成内容

- 通知列表 GET 接入 `cachedGet`，缓存 key 使用 `notification:list` + 分页/筛选参数，TTL 为 15 秒。
- 通知写操作继续通过 `clearApiCacheByPrefix('notification')` 清理未读数和通知列表缓存，避免写后展示旧列表。
- 公开版本日志分页 GET 接入 `cachedGet`，缓存 key 使用 `version:page` + page/size 参数，复用既有 `VERSION_LOGS` TTL。
- 本轮不缓存通知 SSE、写操作、面试状态轮询或其它实时接口。

### 验证结果

- RED 验证：`npm.cmd test -- --run src/__tests__/api/performanceCache.test.js src/__tests__/api/versionLog.test.js` 在旧实现下失败，失败点为通知列表和公开版本日志分页重复调用 request。
- GREEN 验证：同一命令通过，2 个测试文件 / 15 个用例。
- 前端构建验证：`npm.cmd run build` 通过。

### 数据存储方案

- 本轮为前端 API 封装层短缓存，不涉及数据库结构、后端接口或 migration。

### 停止说明

本轮只补齐通知列表与公开版本日志分页两个残留短缓存热点，不继续扩展其它页面缓存、后端接口或数据库优化。
