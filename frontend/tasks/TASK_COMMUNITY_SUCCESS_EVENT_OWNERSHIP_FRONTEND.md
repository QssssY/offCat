# TASK: 社区成功提示事件归属修复（前端）

## 当前任务所属模块
- 前端社区模块。
- 模拟面试报告分享到社区入口。

## 前端文件定位
- `frontend/app/src/components/community/PostEditor.vue`
- `frontend/app/src/views/community/CommunityView.vue`
- `frontend/app/src/components/community/ShareReportDialog.vue`
- `frontend/app/src/__tests__/components/community/PostEditor.test.js`
- `frontend/app/src/__tests__/components/community/ShareReportDialog.test.js`
- `frontend/app/src/__tests__/views/community/CommunityView.test.js`

## 本轮修改文件清单
- `PostEditor.vue`：发帖成功后继续由编辑器内部负责唯一成功提示，但对外事件从通用 `success` 改为业务语义 `published`。
- `CommunityView.vue`：父页面改为监听 `published`，只负责关闭发布弹窗和刷新列表，不承担成功 toast。
- `ShareReportDialog.vue`：移除已无生产消费者的通用 `success` emit，保留弹窗内部唯一“分享成功”提示和关闭弹窗逻辑。
- `PostEditor.test.js`：新增发帖成功事件归属回归测试，确认不会再发出通用 `success`。
- `ShareReportDialog.test.js`：补充分享成功后不再 emit 通用 `success` 的回归测试。
- `CommunityView.test.js`：补充父页面忽略旧 `success`、只响应 `published` 的回归测试。

## 前端实现方案
- 成功 toast 统一由实际发起提交动作的子组件负责。
- 父组件仅处理业务完成后的页面状态变更，例如关闭弹窗和刷新列表。
- 对需要父子通信的发帖流程使用 `published` 表达业务完成事件，避免未来父组件把 `success` 当作再次弹成功提示的入口。
- 对已经不需要父级回调的报告分享流程移除通用成功事件，减少重复提示复发面。

## 后端实现方案
- 本轮不涉及后端接口、DTO、数据库或服务逻辑变更。

## 数据存储方案
- 不新增前端持久化字段。
- 不修改接口响应结构或数据库结构。

## stage 更新说明
- 已在 `frontend/tasks/stage.md` 追加本轮社区成功提示事件归属修复状态。

## 编译与测试结果
- RED：`npm.cmd test -- --run src/__tests__/views/community/CommunityView.test.js src/__tests__/components/community/PostEditor.test.js src/__tests__/components/community/ShareReportDialog.test.js` 失败于旧 `success` 事件仍被监听/发出。
- GREEN：`npm.cmd test -- --run src/__tests__/views/community/CommunityView.test.js src/__tests__/components/community/PostEditor.test.js src/__tests__/components/community/ShareReportDialog.test.js src/__tests__/views/InterviewReportView.test.js` 通过，4 个测试文件 / 25 个用例通过。
- 构建：`npm.cmd run build` 通过。
- 静态扫描：`rg -n "@success=|emit\('success'\)|defineEmits\(\[.*success|onPostSuccess" frontend/app/src/components frontend/app/src/views -S` 在生产组件/页面中无匹配。

## 当前功能验收说明
- 模拟面试报告分享到社区成功后只由分享弹窗显示一次“分享成功”。
- 社区发布帖子成功后只由发帖编辑器显示一次“发布成功”，社区首页只关闭弹窗并刷新列表。
- 旧的通用 `success` 事件不再作为生产父子通信入口，降低后续重复成功提示复发风险。

## 停止说明
- 本轮只修复成功提示事件归属和重复 toast 复发风险，不继续扩展社区发布、评论、分享、报告计算或后端能力。
