# TASK_21C_ADMIN_NAVIGATION_AND_INFORMATION_ARCHITECTURE_POLISH

## 1. 当前任务所属模块
- 管理端整体收尾优化
- 子模块：导航与信息架构一致性收尾

## 2. 本轮 task 拆分
1. 对侧边栏做分组化信息架构重排。  
2. 统一导航区视觉层级（分组标题 + 模块项）。  
3. 收敛监控页头部信息层级（标题/副标题结构与其他页面一致）。  
4. 增加监控空数据提示，统一“空状态可读性”。  
5. 更新任务文档与阶段文档。  
6. 执行构建验证。

## 3. task 清单
- [x] 侧边栏改为分组导航结构
- [x] 侧边栏分组标题与移动端隐藏策略
- [x] 监控页标题层级统一（`h2 + subtitle`）
- [x] 监控页新增空数据提示
- [x] 更新任务文档：`frontend/tasks/TASK_21C_ADMIN_NAVIGATION_AND_INFORMATION_ARCHITECTURE_POLISH.md`
- [x] 更新阶段文档：`frontend/runtime/STATE.md`
- [x] 构建验证通过

## 4. 本轮实现代码（frontend/app）
- 修改：`src/layouts/AdminLayout.vue`
  - 原平铺导航改为 `navGroups` 分组结构
  - 新增分组标题样式与移动端适配
- 修改：`src/views/admin/AdminMonitorView.vue`
  - 头部结构改为统一 `page-header/page-title/page-subtitle`
  - 新增 `hasMonitorData` 空状态判断
  - 新增空数据 `el-alert` 提示

## 5. 路由与页面变更
- 路由无新增。
- 管理端导航信息架构与监控页展示层级统一性增强。

## 6. 接口联调说明
- 本轮未新增接口。
- 监控页仍复用：`GET /api/admin/monitor/overview`。

## 7. frontend/tasks 文档更新
- 新增：`frontend/tasks/TASK_21C_ADMIN_NAVIGATION_AND_INFORMATION_ARCHITECTURE_POLISH.md`

## 8. frontend/runtime/STATE.md 更新
- 已追加 `TASK_21C_ADMIN_NAVIGATION_AND_INFORMATION_ARCHITECTURE_POLISH` 到已完成任务。
- 本次连续执行达到 3 轮上限，下一组继续管理端整体收尾（提示语统一与包体积告警治理）。

## 9. 构建验证结果
- 命令：`npm.cmd run build`
- 结果：通过

## 10. 下一轮将继续做什么
- 下一组连续轮次将继续管理端收尾优化：统一风险确认文案模板与列表空状态表达，并评估 `index` 大包体积拆分策略。
