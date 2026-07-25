# TASK_CORE_BUGFIX_ROUND_1（前端子任务）

## 任务范围
- 模拟面试报告链路核心问题修复（不含管理端收尾优化）。

## 修复点
- `InterviewSessionView.vue`
  - 报告就绪态判定与按钮防重入
  - 会话内报告轮询
  - 结束后终态即时反馈
- `InterviewReportView.vue`
  - 报告状态自动轮询
  - 手动刷新入口
  - 报告字符串清洗与健壮解析
- `InterviewHistoryView.vue`
  - 报告未就绪时入口禁用并提示“报告生成中”

## 验证
- `npm.cmd run build` 通过。

## 说明
- 本轮完成后停止推进，等待人工验收。
