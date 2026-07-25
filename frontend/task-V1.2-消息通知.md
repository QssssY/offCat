# V1.2 功能三：消息通知 — 前端 Task

## 当前任务所属模块
V1.2 用户体验增强版，功能三：消息通知（前端）

## 前端文件定位
- 项目路径：`frontend/app/src/`

## 本轮修改文件清单

### 新建文件
| 文件 | 说明 |
|------|------|
| `frontend/app/src/api/notification.js` | 通知 API 模块（4 个接口） |
| `frontend/app/src/views/notification/NotificationView.vue` | 消息通知完整页面 |

### 修改文件
| 文件 | 修改内容 |
|------|----------|
| `frontend/app/src/components/AppHeader.vue` | 添加通知铃铛入口 + 下拉面板 + 移动端入口 + 未读轮询 |
| `frontend/app/src/router/index.js` | 添加 `/notifications` 路由 |

## 前端实现方案

### API 模块（notification.js）
- `getNotifications(params)` — 查询通知列表
- `getUnreadCount()` — 获取未读数量
- `markAsRead(id)` — 单条标记已读
- `markAllAsRead()` — 全部标记已读

### AppHeader 改造
1. **通知铃铛入口**：在用户头像下拉菜单左侧添加铃铛图标，使用 `el-popover` 实现下拉面板
2. **未读角标**：铃铛右上角显示未读数量（红色圆角角标），数量为 0 时隐藏
3. **下拉面板**：
   - 顶部：标题"消息通知" + "全部已读"按钮
   - 中间：最近 10 条通知列表，每条显示类型图标、标题、内容摘要、时间
   - 底部："查看全部消息"链接跳转到完整通知页面
   - 空状态：友好提示"暂无消息"
   - 加载状态：CSS 旋转动画
4. **移动端**：在汉堡抽屉中添加"消息通知"入口，附带未读数量角标
5. **未读轮询**：登录状态下，页面加载时查询一次未读数量，之后每 60 秒轮询一次
6. **生命周期管理**：`onUnmounted` 清除定时器

### 通知完整页面（NotificationView.vue）
- 路由：`/notifications`（需认证，useLayout: true）
- 页面标题区：标题 + 未读数量徽章 + "全部已读"按钮
- 筛选栏：按通知类型筛选 + 按已读状态筛选
- 通知列表：每条显示类型图标（颜色区分）、标题、类型标签、内容摘要、时间、未读点
- 空状态：铃铛图标 + 友好提示
- 加载状态：CSS 旋转动画
- 分页：Element Plus Pagination
- 点击行为：标记已读 + 根据 bizType 跳转到对应业务页面

### 通知类型视觉映射
| type | 图标颜色 | 标签样式 |
|------|----------|----------|
| resume | 橙色 #ff8c42 | warning |
| polish | 蓝色 #409eff | default |
| interview | 绿色 #67c23a | success |
| quota | 红色 #f56c6c | danger |
| system | 灰色 #909399 | info |

### 业务跳转规则
| bizType | 跳转路径 |
|---------|----------|
| resume_diagnosis | `/resume/result/{bizId}` |
| resume_polish | `/resume/result/{bizId}` |
| mock_interview | `/interview/report/{bizId}` |
| quota | 不跳转 |

## stage 更新说明
- `frontend/runtime/STATE.md` 中功能一、二状态更新为"已完成，已验收通过"
- 功能三状态更新为"开发中"

## 构建结果
前端 `npm run build` 构建通过，7.76s，无错误。NotificationView 已正确打包。

## 当前功能验收说明
1. 登录后顶部导航栏显示通知铃铛图标
2. 铃铛右上角显示未读数量角标
3. 点击铃铛打开通知下拉面板
4. 面板展示最近 10 条通知（标题、内容摘要、时间、已读状态）
5. 没有通知时展示"暂无消息"空状态
6. 点击单条通知可标记已读并跳转到对应页面
7. 面板内"全部已读"按钮可一键标记所有通知为已读
8. 面板底部"查看全部消息"跳转到完整通知页面
9. 完整通知页面支持分页、按类型筛选、按已读状态筛选
10. 未读数量每 60 秒自动刷新
11. 移动端汉堡抽屉中显示"消息通知"入口
12. 不影响新手引导、个人成长中心等已有功能

## 停止，不继续下一个功能
