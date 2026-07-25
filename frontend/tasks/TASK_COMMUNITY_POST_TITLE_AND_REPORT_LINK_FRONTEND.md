# 社区帖子标题、报告分享与展示体验前端修复

## 当前任务所属模块
- 前端社区模块。
- 页面：社区首页、帖子卡片、帖子详情、个人动态中心、面试报告分享弹窗。

## 前端文件定位
- `frontend/app/src/components/community/PostEditor.vue`
- `frontend/app/src/components/community/PostCard.vue`
- `frontend/app/src/components/community/ShareReportDialog.vue`
- `frontend/app/src/views/community/PostDetailView.vue`
- `frontend/app/src/views/community/MyActivity.vue`
- `frontend/app/src/views/community/CommunityView.vue`
- `frontend/app/src/App.vue`
- `frontend/app/src/components/AppHeader.vue`
- `frontend/app/src/styles/index.css`

## 后端文件定位
- `server/src/main/java/com/airesume/server/service/CommunityService.java`
- `server/src/main/java/com/airesume/server/service/InterviewService.java`
- `server/src/main/java/com/airesume/server/dto/community/CreatePostRequest.java`
- `server/src/main/java/com/airesume/server/dto/community/PostVO.java`
- `server/src/main/java/com/airesume/server/entity/CommunityPost.java`

## 本轮修改文件清单
- `frontend/app/src/components/community/PostEditor.vue`
- `frontend/app/src/components/community/PostCard.vue`
- `frontend/app/src/components/community/ShareReportDialog.vue`
- `frontend/app/src/views/community/PostDetailView.vue`
- `frontend/app/src/views/community/MyActivity.vue`
- `frontend/app/src/views/community/CommunityView.vue`
- `frontend/app/src/App.vue`
- `frontend/app/src/styles/index.css`
- `frontend/app/src/__tests__/components/community/PostEditor.test.js`
- `frontend/app/src/__tests__/components/community/PostCard.test.js`
- `frontend/app/src/__tests__/components/community/ShareReportDialog.test.js`
- `frontend/app/src/__tests__/views/community/PostDetailView.test.js`
- `frontend/app/src/__tests__/views/community/CommunityView.test.js`
- `frontend/app/src/__tests__/views/community/MyActivity.test.js`
- `frontend/app/src/__tests__/components/AppHeader.test.js`
- `frontend/app/src/__tests__/themeTokens.test.js`

## 前端实现方案
- `PostEditor` 增加帖子标题输入和必填校验，提交社区帖子时传递 `title`。
- `PostCard` 和 `PostDetailView` 展示帖子标题；当帖子带有 `sharedInterviewSessionId` 时，额外渲染“查看完整面试报告”的站内链接卡片。
- `PostCard` 和 `PostDetailView` 对超长正文默认折叠，提供“展开 / 收起”入口，避免长文本撑满页面；旧报告分享帖标题为空时前端展示“面试报告分享”兜底标题。
- `ShareReportDialog` 分享面试报告时生成报告标题并允许用户编辑，标题必填且最多 120 字；正文仍为用户文案或简短说明，不再把完整面试报告摘要拼成社区正文；提交 payload 携带 `title` 与 `sharedInterviewSessionId`。
- `MyActivity` 默认分页大小从 2 改为 5，卡片展示标题，并对报告分享帖显示报告链接提示。
- `styles/index.css` 增加全局光标与 caret 基线：普通展示文本为默认箭头且隐藏只读 caret，按钮/链接为点击光标，输入框/文本域和真实 `contenteditable` 为输入光标与可见 caret，禁用态为不可操作光标。
- 光标回归补丁继续强化可点击元素的子节点继承规则：`button`、`a`、`[role="button"]` 内部图标和文字继承父级光标；Element Plus 的 dropdown/tab、Naive UI 的 button/tab 及其子节点统一使用点击光标，禁用态保持不可操作光标，避免第三方组件内部 `span/div` 在文字区域回退成 I-beam。
- `AppHeader` 的历史记录触发器和下拉菜单项显式保持点击光标，并通过 `@mousedown.prevent` 阻止点击菜单文字时触发文本选区或 caret 状态，修复桌面端点击“历史记录”后两个子导航项文字区域出现输入光标的问题。
- `CommunityView` 首页加载态改为贴近真实帖子卡片的结构化骨架屏，补齐 `prefers-reduced-motion` 降级；`App.vue` 降低 Naive UI Skeleton 的橙色面积，让浅色和暗色下的加载态更安静。

## 后端实现方案
- 后端任务记录见根目录 `task-社区帖子标题与报告分享链接修复.md`。
- 后端已补齐标题字段、分享报告会话 ID 字段和社区分享授权校验。

## 数据存储方案
- 前端不新增本地持久化字段。
- 后端数据库字段和迁移脚本已在 `db/` 与 `server/db/` 双副本同步。
- 已执行过旧版 TASK_56 的数据库，应执行 `server/db/migrations/TASK_59_COMMUNITY_POST_TITLE_AND_REPORT_LINK_INCREMENTAL.sql` 补齐本轮字段和索引。
- 本轮展示体验修复不新增 SQL、不新增后端接口、不改数据库结构。

## stage 更新说明
- 已在 `frontend/tasks/stage.md` 追加本轮前端完成状态。
- 根目录 `stage.md` 记录后端和数据库完成状态。

## 构建与测试结果
- 本轮 RED 验证：`npm.cmd test -- --run src/__tests__/components/community/PostCard.test.js src/__tests__/components/community/ShareReportDialog.test.js src/__tests__/views/community/PostDetailView.test.js src/__tests__/views/community/CommunityView.test.js src/__tests__/themeTokens.test.js` 失败于 9 个预期缺失行为，包括长文折叠、标题兜底、分享标题输入、结构化骨架屏和全局 cursor 基线。
- 本轮 GREEN 验证：同一命令通过，5 个测试文件 / 42 个用例通过。
- 本轮前端构建：`npm.cmd run build` 通过。
- 前端定向测试：`npm.cmd test -- --run src/__tests__/components/community/PostEditor.test.js src/__tests__/components/community/PostCard.test.js src/__tests__/components/community/ShareReportDialog.test.js src/__tests__/views/community/MyActivity.test.js src/__tests__/views/community/PostDetailView.test.js` 通过，5 个测试文件 / 13 个用例通过。
- 前端构建：`npm.cmd run build` 通过。
- 后端相关验证：`mvn.cmd test "-Dtest=CommunityServiceValidationTest,CommunityServicePostQueryDeleteTest,InterviewServiceTest,CommunityServiceLikeFavoriteTest,CommunityServiceInteractionTest,CommunityServiceReceivedInteractionsEmptyTest"` 通过，79 个用例通过；`mvn.cmd compile` 通过。
- 光标补丁 RED 验证：`npm.cmd test -- --run src/__tests__/components/AppHeader.test.js src/__tests__/themeTokens.test.js` 先失败于只读文本 caret 隐藏和历史菜单 `mousedown` 拦截缺失；补充 `contenteditable` 子节点保护测试后，`npm.cmd test -- --run src/__tests__/themeTokens.test.js` 先失败于编辑区子节点 caret 恢复缺失。
- 光标补丁 GREEN 验证：`npm.cmd test -- --run src/__tests__/components/AppHeader.test.js src/__tests__/themeTokens.test.js` 通过，2 个测试文件 / 14 个用例通过。
- 光标补丁相关回归：`npm.cmd test -- --run src/__tests__/components/AppHeader.test.js src/__tests__/themeTokens.test.js src/__tests__/views/SettingsView.test.js src/__tests__/views/community/MyActivity.test.js src/__tests__/views/community/CommunityView.test.js src/__tests__/components/community/PostCard.test.js src/__tests__/views/community/PostDetailView.test.js` 通过，7 个测试文件 / 81 个用例通过；`npm.cmd run build` 通过。

## 当前功能验收说明
- 用户在社区发帖时必须填写标题，帖子列表和详情页可直接看到标题。
- 个人动态中心默认每页加载 5 条，减少初始内容过少的问题。
- 从面试报告页分享到社区后，社区帖子展示为带标题的报告链接卡片，点击可跳转 `/interview/report/{sessionId}` 查看发布用户分享的报告。
- 分享弹窗和帖子内容不再展示完整报告正文，避免把报告详情作为普通长文本复制到社区流中。
- 社区列表和详情页超长正文默认折叠，用户可展开/收起，展开按钮不会误触发进入详情。
- 社区只读文本区域不再出现输入态光标；输入控件仍保留输入光标。
- 历史记录下拉菜单、设置页子导航、Element Plus/Naive UI 的按钮和 Tab 内部文字或图标不再回退成输入光标；点击普通只读文字时不再显示可输入 caret；输入框、文本域和真实 `contenteditable` 区域仍保留输入光标。
- 社区首页首屏加载骨架屏改为头像、标题、正文、操作区的结构化占位，视觉上与真实帖子更接近。

## 停止说明
- 本轮只完成社区光标、长文折叠、分享标题编辑/兜底和骨架屏体验修复，不继续推进富文本编辑、报告下载、公开报告列表或其它社区扩展能力。
