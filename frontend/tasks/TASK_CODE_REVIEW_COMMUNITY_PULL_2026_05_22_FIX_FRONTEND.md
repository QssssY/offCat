# TASK：2026-05-22 社区 Pull 代码审查修复（前端）

## 当前任务所属模块
- 社区图片预览组件、设置中心账号注销页签。

## 前端文件定位
- `frontend/app/src/components/community/ImageGrid.vue`
- `frontend/app/src/__tests__/components/community/ImageGrid.test.js`
- `frontend/app/src/views/settings/SettingsView.vue`

## 本轮修改文件清单
- `ImageGrid.vue`：图片预览层改为组件内渲染，关闭后立即移除 DOM；箭头保留社区命名空间类并兼容既有测试选择器。
- `ImageGrid.test.js`：测试清理阶段统一卸载已挂载 wrapper，再清理可能残留的预览层。
- `SettingsView.vue`：账号注销页签补齐“注销后不可恢复”风险文案、冷静期按钮禁用和提交确认流程。
- `SettingsView.vue`：账号注销确认框取消时直接保留表单状态并停止提交，不再向外抛出取消异常。

## 前端实现方案
- 图片预览不再使用 Teleport，避免测试环境手工清理 Teleport DOM 后 Vue patch 访问空父节点。
- 账号注销提交先通过表单校验和冷静期检查，再弹出确认框并复用 `deleteAccount` 现有 API payload。
- 用户取消确认框时不调用注销接口，避免取消操作产生未处理 Promise rejection。

## 数据存储方案
- 不新增前端持久化字段。
- 不修改后端接口或数据库结构。

## 验证记录
- 定向前端测试：`npm.cmd test -- --run src/__tests__/views/SettingsView.test.js` 通过，1 个测试文件 / 25 个用例通过。
- 前端构建：`npm.cmd run build` 通过。
- 前端完整测试：`npm.cmd test` 通过，38 个测试文件 / 242 个用例通过。

## 停止说明
- 本轮只处理评审报告中的前端测试失败和交互稳定性问题，不继续扩展社区页面或设置中心能力。
