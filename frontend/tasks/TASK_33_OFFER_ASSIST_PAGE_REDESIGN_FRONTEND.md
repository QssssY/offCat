# TASK_33_OFFER_ASSIST_PAGE_REDESIGN_FRONTEND

## 1. 当前任务所属模块
- Offer 加速器实施计划：第 4 部分，Offer 辅助链路第一版页面体验优化。

## 2. 前端文件定位
- `frontend/app/src/views/offer/OfferAssistView.vue`
- `frontend/runtime/STATE.md`
- `frontend/tasks/stage.md`

## 3. 后端文件定位
- 本轮后端仅优化 Offer 辅助 Prompt，见根目录后端 task 文件。
- 前端不新增接口，不修改 DTO、Controller。

## 4. 本轮修改文件清单
- `frontend/app/src/views/offer/OfferAssistView.vue`
- `frontend/app/src/__tests__/views/OfferAssistView.test.js`
- `frontend/runtime/STATE.md`
- `frontend/tasks/stage.md`
- `frontend/tasks/TASK_33_OFFER_ASSIST_PAGE_REDESIGN_FRONTEND.md`

## 5. 前端实现方案
- 按要求先读取 `runtime/DEVELOPMENT_RULES.txt`。
- 读取 `frontend/.claude/skills` 下的以下 UI 优化说明：
  - `ui-skills/baseline-ui`
  - `taste-skill/redesign-skill`
  - `impeccable/layout`
  - `impeccable/polish`
  - `better-icons`
- 保留原有接口、表单字段、校验规则、提交逻辑和复制逻辑。
- 将原来的基础标签页表单重构为工作台布局：
  - 顶部标题区和当前范围说明
  - 分段式能力切换
  - 左侧输入区
  - 右侧结果区
- 增加结果空态、加载骨架、重点话术高亮、列表编号和更清晰的复制按钮。
- 根据验收反馈追加修复复制按钮层级：复制按钮从标题文字流中移出，作为结果块右上角独立操作层展示，并为标题预留右侧空间防止遮挡。
- 根据二次验收反馈继续修复复制按钮质感：
  - 去掉裸露的“复制”文字按钮，改为 32px 图标型操作按钮。
  - 保留 `aria-label` 和 `title`，默认弱化显示，hover/focus 时增强。
  - 复制成功后短暂切换为完成图标，不改变结果卡片布局。
- 根据本轮验收反馈继续做最小修复：
  - 提交接口失败时显式捕获异常并提示，避免网络错误冒泡为 Vue event handler 警告。
  - 将复制按钮改为常显的“图标 + 复制/已复制”小按钮，解决结果区难以判断是否可复制的问题。
  - 将“开场确认”“争取报价”等结果标题改为胶囊标签样式，和正文形成明显区分。
- 根据最新验收反馈继续做最小 UI 修复：
  - 当前不再展示复制按钮，移除结果块内复制入口和对应前端复制逻辑。
  - 所有结果块统一使用独立背景、边框和正文浅底内容区，不再只有“建议回复”呈现气泡。
  - “场景判断”“建议回复”“推进策略”等小标题改为标签化标题栏，并与下方正文保持视觉分隔。
- 根据本轮验收反馈继续做最小布局修复：
  - 桌面端通过 `ResizeObserver` 读取左侧输入面板真实高度，并同步到右侧生成结果面板，右侧初始空态与左侧输入面板同高。
  - 右侧结果区增加独立 `result-scroll` 滚动承载层，生成内容过长时在结果面板内部滚动，不继续拉长右侧面板。
  - 窄屏维持上下堆叠，结果区恢复自然高度，避免移动端出现内嵌小滚动区域。
- 移动端改为上下堆叠，按钮宽度占满，保证表单和结果不会横向溢出。

## 6. 后端实现方案
- 本轮后端只优化 Offer 辅助 AI Prompt，不新增数据结构和接口。
- Prompt 优化记录见 `tasks/TASK_32_OFFER_ACCELERATOR_STAGE4_OFFER_ASSIST_BACKEND.md`。

## 7. 数据存储方案
- 不新增数据库表。
- 不新增数据库字段。
- 不新增本地存储。
- 不保存 Offer 辅助历史。

## 8. stage 更新说明
- 已更新 `frontend/runtime/STATE.md`。
- 已更新 `frontend/tasks/stage.md`。
- 当前阶段标记为“第 4 部分：Offer 辅助链路第一版结果面板等高滚动修复，本轮已完成，等待人工验收”。

## 9. 编译结果
- 本轮另有后端 Prompt 优化，后端编译结果见根目录后端 task 文件。

## 10. 构建结果
- 命令：`npm.cmd run build`
- 结果：通过
- 复制按钮图层修复后再次执行：`npm.cmd run build`
- 结果：通过
- 复制按钮二次修复后再次执行：`npm.cmd run build`
- 结果：通过
- 本轮复制按钮可见性、结果标签区分度和提交异常处理修复后再次执行：`npm.cmd run build`
- 结果：通过
- 本轮移除复制按钮并统一结果块标题、背景和正文分层后再次执行：`npm.cmd run build`
- 结果：通过
- 本轮结果面板等高滚动修复新增定向回归测试：`npm.cmd test -- --run src/__tests__/views/OfferAssistView.test.js`
- 结果：通过，1 个测试文件 / 1 个用例通过。
- 本轮结果面板等高滚动修复后再次执行：`npm.cmd run build`
- 结果：通过。

## 11. 当前功能验收说明
- `/offer` 页面应呈现为 Offer 辅助工作台，而不是简单表单卡片。
- “薪资谈判模拟”和“谈薪话术模板”可以通过顶部模式按钮切换。
- 两个模式的原有表单字段、提交按钮和接口调用保持可用。
- 结果为空时展示明确空态；生成中展示骨架加载态；生成后结构化展示结果。
- 当前不展示复制按钮，结果区不应再出现复制图标或复制文案。
- “场景判断”“建议回复”“推进策略”“风险提醒”“下一步行动”等标题应呈现为小标签标题栏，不应和正文同一视觉层级。
- 每个结果块都应有独立背景和正文浅底内容区，避免只有“建议回复”有气泡导致视觉不协调。
- 初始无内容时右侧生成结果面板应与左侧输入面板同高，不再显得过短。
- 生成结果内容过长时，应在右侧结果面板内部出现纵向滚动，不把页面横向或纵向撑出异常布局。
- 提交接口网络失败时应显示错误提示，不应出现未处理的 Vue event handler 报错。
- 移动端页面上下布局，不应出现横向滚动或按钮过窄。
- 本轮未实现实时薪资行情、录用意向评估、背调准备指导或题库相关功能。

## 12. 停止，不继续下一个功能
- 本轮仅完成 Offer 辅助结果面板等高和内部滚动修复。
- 到此停止，等待人工验收。
