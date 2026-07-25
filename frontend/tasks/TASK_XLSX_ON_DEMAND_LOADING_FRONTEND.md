# XLSX 导出依赖按需加载优化

## 当前任务所属模块

管理端导出功能、前端依赖加载性能、Vite 开发环境首开体验。

## 前端文件定位

- `frontend/app/src/utils/export.js`
- `frontend/app/src/views/admin/AdminAuditLogView.vue`
- `frontend/app/src/views/admin/AdminMembershipOrderView.vue`
- `frontend/app/vite.config.js`
- `frontend/app/src/__tests__/utils/export.test.js`
- `frontend/app/src/__tests__/viteConfig.test.js`

## 后端文件定位

本轮不涉及后端文件、接口、数据库或服务层改动。

## 本轮修改文件清单

- `export.js`：移除顶部 `xlsx` 静态导入，改为 `exportToXlsx()` 内部动态 `import('xlsx')`。
- `AdminAuditLogView.vue`：导出处理函数改为 `async`，等待 XLSX 按需加载和文件生成完成后再提示导出成功。
- `AdminMembershipOrderView.vue`：导出处理函数改为 `async`，等待 XLSX 按需加载和文件生成完成后再提示导出成功。
- `vite.config.js`：从 `optimizeDeps.include` 移除 `xlsx`，避免开发环境进入管理端时预先拉入导出专用重依赖。
- `export.test.js`、`viteConfig.test.js`：补充回归测试，锁定 `xlsx` 不静态导入、不进入 Vite 预优化，并验证导出函数动态加载后仍能生成 workbook。

## 前端实现方案

- 保留现有 `exportToXlsx()` 对外函数名和调用参数，避免影响管理端现有导出入口。
- 将 `xlsx` 加载移动到用户点击导出后的函数内部，只有实际导出审计日志或订单数据时才下载和执行 SheetJS。
- 调用侧使用 `await exportToXlsx()`，保证首次导出时依赖加载、工作簿生成和文件写出完成后才出现成功提示。
- Vite 开发环境继续预优化管理端首开高频依赖，但导出类依赖不再放入 `optimizeDeps.include`。

## 后端实现方案

无后端实现。本轮不修改导出接口、订单接口、审计日志接口或鉴权逻辑。

## 数据存储方案

无数据存储改动。本轮只调整前端依赖加载时机，不新增本地存储、缓存字段或数据库结构。

## stage 更新说明

`frontend/tasks/stage.md` 顶部新增“XLSX 导出依赖按需加载优化”记录，说明原因、实现范围、验证结果和停止边界。

## 编译结果

- `npm.cmd test -- --run src/__tests__/utils/export.test.js src/__tests__/viteConfig.test.js` 通过，2 个测试文件 / 4 个用例。

## 构建结果

- `npm.cmd run build` 通过。

## 当前功能验收说明

初次进入管理端和切换管理端路由时，不再因为导出专用 `xlsx` 依赖被预构建或静态导入而增加冷加载负担；只有点击审计日志或订单管理的导出按钮时才加载 `xlsx` 并生成 Excel 文件。

## 停止，不继续下一功能

本轮只处理 `xlsx` 按需加载，不继续扩展全量依赖治理、Service Worker、导出队列、更多导出格式或其它页面性能优化。
