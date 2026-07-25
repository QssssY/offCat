# TASK 47 问题反馈/建议模块前端

## 当前任务所属模块

问题反馈/建议模块，覆盖设置中心提交入口与管理端反馈受理页面。

## 前端文件定位

- `frontend/app/src/views/settings/SettingsView.vue`
- `frontend/app/src/views/admin/AdminFeedbackView.vue`
- `frontend/app/src/api/feedback.js`
- `frontend/app/src/api/admin/feedback.js`
- `frontend/app/src/router/index.js`
- `frontend/app/src/layouts/AdminLayout.vue`

## 后端文件定位

后端实现见 `tasks/TASK_47_USER_FEEDBACK_BACKEND.md`。

## 本轮修改文件清单

- 设置中心新增“问题反馈”分组和反馈表单。
- 管理端新增 `/admin/feedback` 页面，支持筛选、分页、详情、状态处理和批量删除。
- 管理端侧边栏“运营管理”新增“问题反馈”入口。
- 新增用户端和管理端反馈 API 封装。
- 新增反馈 API 测试，并更新设置中心测试覆盖新增入口和提交逻辑。
- 验收修复：管理端反馈详情弹窗外层固定不可滚动，短内容按自然高度展示，反馈内容和处理备注超过各自上限时才显示内部滚动条；弹窗遮罩层采用上 20px、下 44px 的外部留白，让弹窗整体略上移并保留底部呼吸感。
- 新增 `AdminFeedbackView` 视图测试，覆盖详情弹窗固定布局与两个长文本区的内部滚动样式。

## 前端实现方案

用户端入口放在设置中心，不增加顶部主导航。反馈表单包含类型、标题、详细内容、联系方式，提交成功后清空表单。管理端页面复用现有后台表格视觉和交互模式，列表支持类型、状态、用户 ID 筛选，详情弹窗内可进入处理弹窗。

## 数据存储方案

前端不新增本地持久化；提交内容通过 `POST /api/user/feedback` 写入后端 `user_feedback` 表。

## 构建与测试结果

- `npm.cmd test -- --run src/__tests__/api/feedback.test.js src/__tests__/api/admin.feedback.test.js src/__tests__/views/SettingsView.test.js` 通过，27 个测试通过。
- `npm.cmd test` 通过，96 个测试通过。
- `npm.cmd run build` 通过。
- 验收修复后验证结果：
  - `npm.cmd test -- --run src/__tests__/views/AdminFeedbackView.test.js` 通过，1 个测试通过。
  - `npm.cmd run build` 通过。

## 当前功能验收说明

- 用户可在设置中心提交反馈。
- 管理端可从侧边栏进入问题反馈模块并受理反馈。
- 管理端查看反馈详情时弹窗本体保持固定；短处理备注不会撑出大面积留白，反馈内容和处理备注过长只在各自内容区域内部滚动，弹窗底部与页面底部保持外部间距。
- 第一版不支持附件上传、用户侧反馈历史和管理员回复通知。

## 停止说明

本轮只完成问题反馈/建议模块最小闭环，不继续扩展客服工单或双向沟通能力。
