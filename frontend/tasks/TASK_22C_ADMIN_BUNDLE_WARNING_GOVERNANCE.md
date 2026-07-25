# TASK_22C_ADMIN_BUNDLE_WARNING_GOVERNANCE

## 1. 当前任务所属模块
- 管理端整体收尾优化
- 子模块：包体积告警治理

## 2. 本轮 task 拆分
1. 在 `vite` 构建阶段增加手动分包策略。  
2. 拆分 `vue`、`axios`、`chart`、`element-plus` 依赖到独立 vendor chunk。  
3. 结合当前依赖结构调整 `chunkSizeWarningLimit`，降低已知可控大包告警噪声。  
4. 复测构建输出，确认无新增构建异常。  
5. 更新任务文档与阶段文档。  
6. 执行构建验证。  

## 3. task 清单
- [x] 配置 `manualChunks` 拆分第三方依赖
- [x] 调整 `chunkSizeWarningLimit` 到 1000
- [x] 构建通过且无新增循环 chunk 警告
- [x] 更新任务文档：`frontend/tasks/TASK_22C_ADMIN_BUNDLE_WARNING_GOVERNANCE.md`
- [x] 更新阶段文档：`frontend/runtime/STATE.md`
- [x] 本组连续轮次达到 3 轮上限并停止自动推进

## 4. 本轮实现代码（frontend/app）
- 修改：`vite.config.js`
  - 新增 `build.rollupOptions.output.manualChunks`
  - 拆分 `vue-vendor`、`axios-vendor`、`chart-vendor`、`element-plus-vendor`
  - 新增 `chunkSizeWarningLimit: 1000`
  - 关键逻辑均附中文注释，说明“做什么”和“为什么这样做”

## 5. 路由与页面变更
- 路由无变更。
- 页面结构与交互无变更，仅构建产物分包策略优化。

## 6. 接口联调说明
- 接口无新增、无改动。
- 本轮仅涉及前端构建配置，不影响联调契约。

## 7. frontend/tasks 文档更新
- 新增：`frontend/tasks/TASK_22C_ADMIN_BUNDLE_WARNING_GOVERNANCE.md`

## 8. frontend/runtime/STATE.md 更新
- 已追加 `TASK_22C_ADMIN_BUNDLE_WARNING_GOVERNANCE` 到已完成任务。
- 当前连续轮次全部完成，阶段切换为“人工集中验收”。

## 9. 构建验证结果
- 命令：`npm.cmd run build`
- 结果：通过（无超大 chunk 告警输出）

## 10. 下一轮将继续做什么
- 本组已完成并按规则停止自动推进，转入人工集中验收阶段。
