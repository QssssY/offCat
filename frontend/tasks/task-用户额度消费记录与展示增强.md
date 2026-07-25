# 用户额度消费记录与展示增强 — 前端 Task

## 当前任务所属模块
V1.2 用户额度消费记录与展示增强（前端）

## 前端文件定位
- `frontend/app/src/views/DashboardView.vue`
- `frontend/app/src/views/growth/GrowthCenterView.vue`
- `frontend/app/src/views/admin/AdminUserRightsView.vue`
- `frontend/app/src/components/growth/ConsumptionLogPanel.vue`
- `frontend/app/src/components/admin/AdminConsumptionLog.vue`
- `frontend/app/src/api/quota.js`
- `frontend/app/src/api/admin/users.js`

## 本轮修改文件清单

### 新建文件
| 文件 | 说明 |
|------|------|
| `api/quota.js` | 用户端额度消费记录 API 模块 |
| `components/growth/ConsumptionLogPanel.vue` | 成长中心消费记录面板组件 |
| `components/admin/AdminConsumptionLog.vue` | 管理端消费记录组件 |

### 修改文件
| 文件 | 修改内容 |
|------|----------|
| `views/DashboardView.vue` | quota-card 从 2 项 flex 布局改为 6 宫格 CSS Grid |
| `views/growth/GrowthCenterView.vue` | 添加 Tab 切换（成长概览/额度明细），集成 ConsumptionLogPanel |
| `views/admin/AdminUserRightsView.vue` | 用户详情 Drawer 新增「消费记录」Tab，集成 AdminConsumptionLog |
| `api/admin/users.js` | 新增 getAdminConsumptionLog() 管理端 API 函数 |

## 前端实现方案

### Dashboard 6 宫格
- 原 quota-card（简历诊断 + 模拟面试 2 项 flex 布局）改为 6 宫格 CSS Grid
- `grid-template-columns: repeat(3, 1fr)`，gap: 20px 24px
- 响应式：≤1023px 切换为 `repeat(2, 1fr)`，≤767px 保持 2 列（更紧凑）
- 新增 `quotaItems` computed 属性，统一处理 VIP/非 VIP 数据源：
  - VIP 用户：使用 vipDailyXxxQuota 字段
  - 非 VIP 用户：使用 freeXxxLeft 字段（interviewQuota/resumeQuota 保持原逻辑）
- 额度耗尽数字变红（`.text-danger` class）+ 显示「升级会员」router-link
- 删除不再需要的 `.quota-divider` 和暗黑模式适配

### 成长中心 Tab
- GrowthCenterView 顶部新增 Tab 切换栏（成长概览/额度明细）
- 使用 `.tab-bar` + `.tab-btn` 实现，active 状态橙色背景白字
- `activeTab` ref 控制切换，默认 'overview'
- ConsumptionLogPanel 组件功能：
  - 类型筛选栏：全部/模拟面试/简历诊断/AI润色/JD匹配/模板库/Offer
  - 消费记录列表：类型图标+名称、变动数量（消耗红/退款绿）、来源标签、描述、时间
  - 底部 el-pagination 分页器
  - 类型切换时自动重置页码并重新加载

### 管理端消费记录
- AdminUserRightsView 用户详情 Drawer 新增「消费记录」Tab
- AdminConsumptionLog 组件：
  - el-select 类型筛选（可清空）
  - el-table 记录表格：类型、变动、余额、来源、描述、时间
  - el-pagination 分页器
- `api/admin/users.js` 新增 `getAdminConsumptionLog(userId, params)`
- `rightsUserId` computed 属性从 `rightsData.userId` 派生，传递给 AdminConsumptionLog

## stage 更新说明
- 本轮完成用户额度消费记录与展示增强的前端部分
- 涉及 Dashboard 6 宫格改造、成长中心 Tab 新增、管理端消费记录 Tab

## 构建结果
- `npm run build` ✅ 通过（5182 modules, 19.17s）

## 当前功能验收说明
- Dashboard 展示全部 6 种额度的剩余（6 宫格），VIP/非 VIP 数据正确区分
- 成长中心「额度明细」Tab 支持按类型筛选 + 标准分页
- 管理端用户详情 Drawer 可查看消费记录
- 响应式布局在 1280/1024/768/480 断点正常

## 停止，不继续下一个功能
本轮仅完成用户额度消费记录与展示增强的前端部分，等待验收，不继续推进其他功能。
