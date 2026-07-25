# 消息通知栏 hover 光标前端修复

## 当前任务所属模块
- 前端消息通知模块。
- 页面入口：顶部导航栏消息通知铃铛、通知下拉面板、通知中心页关联组件。

## 前端文件定位
- `frontend/app/src/components/AppHeader.vue`
- `frontend/app/src/components/notification/NotificationTypeIcon.vue`
- `frontend/app/src/styles/index.css`

## 后端文件定位
- 本轮不涉及后端修改。
- 通知接口和 SSE 实时通知链路保持现状。

## 本轮修改文件清单
- `frontend/app/src/components/AppHeader.vue`
- `frontend/app/src/components/notification/NotificationTypeIcon.vue`
- `frontend/app/src/__tests__/components/AppHeader.test.js`
- `frontend/app/src/__tests__/components/notification/NotificationTypeIcon.test.js`
- `frontend/tasks/stage.md`
- `frontend/tasks/TASK_NOTIFICATION_CURSOR_POINTER_FIX_FRONTEND.md`

## 前端实现方案
- 根因是全局只读文本基线 `body * { cursor: default; caret-color: transparent; }` 会覆盖可点击 `div` 内部文字和图标节点的光标；通知栏旧代码只在父级 `div` 上设置 `cursor: pointer`，鼠标移入子节点时仍可能回退成默认箭头。
- `AppHeader.vue` 将通知铃铛、通知列表项和底部“查看全部消息”从可点击 `div` 改为原生 `button type="button"`。
- 复用现有全局规则 `button * { cursor: inherit; }`，让按钮内部的标题、内容、时间、角标和图标都继承点击光标。
- 为通知按钮补齐 `appearance: none`、透明背景、无默认边框、继承字体、宽度和文本对齐样式，避免原生按钮默认样式改变通知面板视觉。
- `NotificationTypeIcon.vue` 根节点从 `div` 改为 `span`，保证通知项按钮内部结构语义合法，同时保留原有 flex 尺寸、光晕和动效样式。

## 后端实现方案
- 本轮不修改后端。
- 不修改通知查询、未读数量、标记已读、全部已读、公告弹窗或 SSE 推送逻辑。

## 数据存储方案
- 本轮不新增本地存储字段。
- 本轮不修改数据库表、迁移脚本或接口 payload。

## stage 更新说明
- 已在 `frontend/tasks/stage.md` 追加“消息通知栏 hover 光标修复（2026-05-30）”记录。
- stage 中明确本轮范围只包含通知栏 hover 光标和语义化点击区域，不继续扩展通知业务能力。

## 编译结果
- 前端定向回归：`npm.cmd test -- --run src/__tests__/components/AppHeader.test.js src/__tests__/components/notification/NotificationTypeIcon.test.js src/__tests__/views/NotificationView.test.js src/__tests__/utils/notificationMeta.test.js src/__tests__/themeTokens.test.js` 通过，5 个测试文件 / 24 个用例通过。

## 构建结果
- 前端生产构建：`npm.cmd run build` 通过。

## 当前功能验收说明
- 鼠标移入顶部通知铃铛时保持小手光标。
- 打开通知下拉面板后，鼠标移入通知标题、正文、时间、类型图标、未读点所在的通知项区域时保持小手光标。
- 鼠标移入底部“查看全部消息”入口时保持小手光标。
- 通知项点击已读、公告弹窗、跳转通知中心和全部已读逻辑保持原行为。
- 输入框、文本域和真实可编辑区域仍保留输入光标；本轮没有移除全局只读文本 cursor 基线。

## 停止说明
- 本轮只处理用户反馈的通知栏鼠标小手问题，不继续推进新的通知类型、通知中心视觉重构、实时推送增强或其它页面改造。
