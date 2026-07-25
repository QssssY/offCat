# TASK_23_V11_RESUME_AI_POLISH_FRONTEND

## AI 润色 PDF 仍导出两页的分页阈值修复记录（2026-06-04）
- 本轮仅修复 AI 润色结果页 `导出 PDF` 在内容略超一页时仍生成两页、尾部关键内容被切到第二页的问题；不修改 AI 润色生成内容、不改模板编辑器结构、不改后端接口、不新增导出格式。
- 问题根因：`resumePdfPagination` 之前只允许接近一页的截图最多压缩 10%（`minSinglePageScale = 0.9`）。真实 AI 润色模板在 190mm 截图宽度下，如果高度约为 310mm，写入 A4 时需要约 13.4% 的等比缩放才能成为单页；旧阈值会直接返回 2 页，导致用户看到尾部内容被截到第二页。
- 修复方案：将近一页单页压缩阈值放宽到 `0.85`，允许最多约 15% 的等比缩放把中等超出一页的润色简历收进单页；明显很长的简历仍保持分页，避免过度压缩影响可读性。
- 测试覆盖：`resumePdfPagination.test.js` 新增 `1900x3100` 截图回归用例，验证中等超出一页的 AI 润色简历导出为 1 页，并保持图片居中；原有短简历单页、长简历分页、自定义边距用例保持不变。
- 前端验证：`npm.cmd test -- --run src/__tests__/utils/resumePdfPagination.test.js` 通过，5 个用例通过；`npm.cmd test -- --run src/__tests__/views/ResumeResultView.test.js src/__tests__/utils/resumePdfPagination.test.js` 通过，2 个测试文件 / 12 个用例通过；`npm.cmd run build` 通过。
- 停止说明：本轮只处理 AI 润色 PDF 近一页分页阈值问题，不继续推进后端 PDF、DOCX、图片导出、模板视觉重构或 AI 润色内容生成调整。

## AI 润色模板预览 DataCloneError 修复记录（2026-05-31）
- 本轮仅修复 AI 润色结果页“润色后的简历内容”区域模板组件初始化失败的问题，不修改后端接口、不调整 AI 生成内容、不扩展导出能力、不继续改动 PDF 分页策略。
- 实际根因：上一轮只修复了外层 `ResultView.vue` 的显示条件，但真实页面仍然空白，是因为 `ResumeTemplate.vue` 在 `applyTemplateText()` 后执行 `history.initialize()`，历史快照构建时把 `header.value`、`sections.value`、`activeTarget.value` 这些 Vue 响应式 Proxy 直接传给 `structuredClone()`。浏览器无法克隆 Proxy，于是抛出 `DataCloneError`，Vue 子组件 setup/watch 被中断，导致模板 DOM 没有渲染出来。`debug.txt` 中的堆栈与新增组件测试均复现了这一点。
- 修复方案：`ResumeTemplate.vue` 的 `cloneModel()` 在调用 `structuredClone()` 前先通过 `toRaw()` 递归转换为纯数据对象，保证历史快照、脏状态签名、保存用 `documentJson` 都不携带 Vue 响应式代理。
- 测试覆盖：新增 `frontend/app/src/__tests__/components/resume/ResumeTemplate.test.js`，真实挂载 `ResumeTemplate` 父组件，仅 stub 内部 Tiptap 子编辑器；RED 阶段复现 `DataCloneError`，GREEN 阶段验证模板纸张和已解析的姓名/求职意向正常渲染。
- 前端验证：`npm.cmd test -- --run src/__tests__/components/resume/ResumeTemplate.test.js` 通过；`npm.cmd test -- --run src/__tests__/components/resume/ResumeTemplate.test.js src/__tests__/views/ResumeResultView.test.js src/__tests__/utils/resumePdfPagination.test.js` 通过，3 个测试文件 / 10 个用例通过；`npm.cmd run build` 通过。
- 停止说明：本轮只处理 AI 润色模板预览空白的真实运行时异常，不继续推进模板视觉重构、后端 PDF、DOCX、图片导出或 AI 润色内容生成。

## AI 润色模板预览空白修复记录（2026-05-31）
- 本轮仅修复 AI 润色结果页中“润色后的简历内容”区域在部分记录下不显示模板预览的问题，不修改 AI 润色生成提示词、不改后端数据结构、不调整 PDF 分页策略、不扩展新的导出能力。
- 问题归因：5/29 的 AI 简历模板编辑器重构把预览区域条件写成只判断 `polishedResumeText`。当记录只有已保存的 `documentJson` 或 `editedPlainText`，但原始 `polishedResumeText` 为空时，标题、按钮和修改说明仍会显示，`ResumeTemplate` 预览容器会被 `v-if` 整块隐藏，表现为用户截图中的模板区域空白。本问题不是 5/31 PDF 近一页分页修复导致，PDF 修复只改动 `resumePdfPagination` 分页计算。
- 修复方案：`ResultView.vue` 新增 `hasPolishTemplateContent` 与 `polishTemplateText`，预览区域改为 AI 原文、结构化文档、编辑后纯文本三者任一存在即渲染；文本输入优先使用 `polishedResumeText`，缺失时回退到 `editedPlainText`，结构化文档继续传给 `ResumeTemplate` 恢复编辑态。
- 测试覆盖：`ResumeResultView.test.js` 新增回归用例，覆盖 `documentJson` 和 `editedPlainText` 存在但 `polishedResumeText` 为空时仍渲染 `.polish-preview-shell`；同时补充 mock onboarding 完成接口，避免结果页测试产生无关 401 请求噪音。
- 前端验证：`npm.cmd test -- --run src/__tests__/views/ResumeResultView.test.js` 通过，1 个测试文件 / 5 个用例通过；`npm.cmd test -- --run src/__tests__/views/ResumeResultView.test.js src/__tests__/utils/resumePdfPagination.test.js` 通过，2 个测试文件 / 9 个用例通过；`npm.cmd run build` 通过。
- 停止说明：本轮只处理 AI 润色结果页模板预览空白问题，不继续推进模板视觉重构、后端 PDF、DOCX、图片导出或 AI 内容生成调整。

## PDF 近一页内容被截到第二页修复记录（2026-05-31）
- 本轮仅修复 AI 简历润色结果页 `导出 PDF` 的分页观感问题，不修改 AI 润色内容、模板编辑能力、DOCX 导出、图片导出、后端接口或数据结构。
- 问题根因：导出链路先按 `190mm` 宽度挂载简历模板并截图，但写入 A4 PDF 时按 `210mm` 页面宽度铺满，截图高度会同步放大；当简历本身接近一页时，放大后的高度略超 `297mm`，旧分页逻辑直接 `ceil()` 为 2 页，导致少量关键尾部内容被截到第二页。
- 修复方案：在 `resumePdfPagination` 中新增受限的近一页压缩规则。若内容只需不超过 10% 的轻微缩小即可放进一页，则居中缩小为单页；若需要压缩更多，继续保持原宽度分页，避免长简历被压得过窄影响阅读。
- 测试覆盖：新增 `1900x2800` 截图尺寸回归用例，验证接近一页的 AI 润色简历会生成 1 页，并保持居中宽度；保留长简历分页、自定义边距和短简历单页用例。
- 前端验证：`npm.cmd test -- --run src/__tests__/utils/resumePdfPagination.test.js` 通过，4 个用例通过；`npm.cmd test -- --run src/__tests__/utils/resumePdfPagination.test.js src/__tests__/views/ResumeResultView.test.js` 通过，2 个测试文件 / 8 个用例通过；`npm.cmd run build` 通过。
- 停止说明：本轮只处理 PDF 近一页分页切分问题，不继续推进 PDF 文本可选、后端 Chrome PDF、模板视觉重构或更多导出格式。

## PDF 导出左右空白修复记录（2026-05-18）
- 本轮仅修复 AI 润色结果页 `导出 PDF` 的排版缩放问题，不修改 AI 润色内容、不插入广告词、不改模板预览样式。
- `ResultView.vue` 的 PDF 导出改为按 A4 页面宽度等比铺满，并通过纵向负偏移分页绘制，避免长简历截图被整体压进单页后左右空白过大。
- 新增 `frontend/app/src/utils/resumePdfPagination.js`，集中计算 PDF 图片分页参数；新增 `resumePdfPagination.test.js` 覆盖短简历单页、长简历分页、宽度铺满和自定义边距场景。
- 图片导出继续沿用原有 `captureResumeCanvas()` 和 PNG 下载链路，本轮不改动。

## 1. 当前任务所属模块
- V1.1 功能二：AI 简历润色

## 2. 前端文件定位
- `frontend/app/src/views/resume/ResultView.vue`
- `frontend/app/src/components/resume/ResumeTemplate.vue`
- `frontend/app/src/components/resume/ResumeRichBlockEditor.vue`
- `frontend/app/src/components/resume/ResumeInlineRichEditor.vue`
- `frontend/app/src/components/resume/resumeTemplateParser.js`
- `frontend/app/package.json`
- `frontend/app/package-lock.json`

## 3. 后端文件定位
- 本轮未改动后端文件

## 4. 本轮修改文件清单
- `frontend/app/src/views/resume/ResultView.vue`
- `frontend/app/src/components/resume/ResumeTemplate.vue`
- `frontend/app/src/components/resume/ResumeRichBlockEditor.vue`
- `frontend/app/src/components/resume/resumeTemplateParser.js`
- `frontend/app/package.json`
- `frontend/app/package-lock.json`
- `frontend/tasks/TASK_23_V11_RESUME_AI_POLISH_FRONTEND.md`
- `frontend/runtime/STATE.md`
- 导出一致性补充修复：取消手工重建个人信息区导出 DOM，改为基于原模板克隆节点做静态化替换。
- 导出静态节点会复制 Vue `scoped` 作用域属性，避免个人信息区样式失效、结构崩坏。
- 照片上传按钮导出时转为静态外观节点，避免浏览器原生按钮样式干扰截图。
- 补充修正求职方向行：统一预览与导出态的整行胶囊底样式，并固定左对齐，避免只剩内缩文字导致视觉错位。
- 继续修正导出未生效问题：导出静态节点移除 `resume-inline-input / resume-textarea-input` 编辑态通用类，避免 print 模式透明背景重置覆盖求职方向行的胶囊底样式。
- 继续补充视觉对齐修复：求职方向胶囊背景整体向左外扩少量距离，并同步增加左内边距，消除圆角带来的“比下方文字更靠右”的视觉偏差。
- 已将求职方向胶囊行的补偿实现从 `calc(100% + 10px)` 调整为 `width: 100% + 负左边距`，降低 html2canvas 截图阶段出现零尺寸图案画布异常的风险。
- 已补充导出专用安全背景降级：在导出克隆节点上将高风险渐变/纹理背景改为近似纯色，规避 `html2canvas` 的 `createPattern` 零尺寸异常。
- 已恢复编辑器中的胶囊章节标题插入能力：工具栏“章节标题”现在插入与“个人信息 / 教育背景”同风格的胶囊标题块，而不是普通小标题文本块。

## 5. 前端实现方案
- 将旧的“多块 `contenteditable` + 手写块状态”方案替换为“模板数据层 + Tiptap 段落编辑块”方案。
- 新增 `resumeTemplateParser.js`：
  - 负责把 AI 返回的 `polishedResumeText` 解析成头部信息与正文章节。
  - 统一处理章节近义词归并。
  - 统一处理项目标题、标签行、行内多列信息、小标题块的识别。
- 新增 `ResumeRichBlockEditor.vue`：
  - 负责正文富文本段落编辑。
  - 支持 `Enter` 在当前块后新增段落。
  - 支持 `Ctrl+Enter` 在当前块内插入换行。
  - 支持选区加粗与选区字号调整。
  - 支持空块删除。
- 重写 `ResumeTemplate.vue`：
  - 头部信息区改为受控字段模型，不再散落使用 `contenteditable`。
  - 左侧为姓名、求职方向、联系方式、补充说明；右侧为照片上传与占位区。
  - 联系方式支持拖拽重排。
  - 正文区保留原有图一模板样式，但块编辑改为文档流行为。
  - 正文块支持跨章节拖拽重排，不做整页绝对坐标布局。
  - 导出时不再直接复用编辑态，而是克隆当前模板并清理工具栏、拖拽手柄、上传控件、焦点态、提示文案。
- 调整 `ResultView.vue`：
  - 将 `ResumeTemplate` 改为异步加载，降低 `ResultView` 主包体积。
  - 复制文本、导出文件名继续优先读取模板当前编辑状态。
  - PDF / 图片导出前先等待模板状态稳定，再使用只读导出节点截图。

## 6. 后端实现方案
- 本轮不改动后端接口、DTO、Service 和存储结构。
- 继续沿用后端返回的 `polishedResumeText` 作为前端模板解析输入。

## 7. 数据存储方案
- 本轮不新增持久化表、不新增草稿存储。
- 用户在模板中的编辑结果仍为当前前端会话态。
- 导出、复制、文件命名均基于当前前端编辑状态生成。

## 8. stage 更新说明
- 已同步更新 `frontend/runtime/STATE.md`。
- 本轮状态聚焦于“AI 简历模板编辑器重构完成，待人工验收”。

## 9. 编译结果
- 后端编译：本轮未改动后端，未执行

## 10. 构建结果
- 前端构建命令：`npm.cmd run build`
- 构建结果：通过
- 构建补充说明：
  - `ResultView` 主 chunk 已明显缩小。
  - `ResumeTemplate` 被拆成独立异步 chunk，降低了结果页主包压力。

## 11. 当前功能验收说明
- AI 润色结果页现在展示新的可编辑模板：
  - 正文段落使用文档流编辑块。
  - `Enter` 新建下一个段落。
  - `Ctrl+Enter` 在当前段落内换行。
  - 点击模板外空白区域会清理当前激活态。
- 头部信息区现在支持：
  - 直接修改姓名、求职方向、联系方式、补充说明。
  - 联系方式拖拽排序。
  - 上传、替换、清空照片。
- 正文区现在支持：
  - 富文本段落编辑。
  - 小标题块与标签块插入。
  - 跨章节拖拽重排。
  - 当前段落删除。
  - 当前段落/选区加粗与字号调整。
- 导出现在基于用户修改后的模板状态：
  - 直接下载 PDF。
  - 直接下载图片。
  - 导出结果中不包含工具栏、拖拽手柄、上传按钮、焦点边框、占位提示。
  - 个人信息区导出已改为保留原始输入节点布局，只移除交互态，避免导出后头部排版与模板预览态不一致。
  - 进一步修正为：导出前将 `input/textarea` 转为同类名静态文本节点，规避 html2canvas 对原生表单值渲染导致的姓名/地址截断问题。
  - 个人信息区现已单独重建静态导出 DOM，不再和正文共用通用字段替换逻辑，以保留头部模板样式与层级。
- 个人信息区预览已将删除按钮、拖拽手柄、照片操作按钮改为悬浮式控件，减少对内容排版的占位影响，使诊断页展示更接近最终导出效果。
- 正文块与个人信息项的拖拽手柄均改为悬浮式显示，不再在内容前保留固定空白占位。
- 复制文本与导出文件名均优先读取用户当前模板编辑结果，而不是回退到 AI 原始文本。

## 12. 停止，不继续下一个功能
- 本轮仅完成 AI 简历模板编辑器重构、导出链路修复与包体拆分，到此停止，等待验收。
## 13. 本轮补充修复记录
- 本轮仅补充修复简历模板编辑器交互稳定性，不改后端接口、不改 AI 返回结构。
- `ResumeTemplate.vue` 新增统一激活态 `activeTarget`，工具栏改为统一作用于正文块和个人信息字段。
- `ResumeInlineRichEditor.vue` 用于姓名、求职方向、联系方式、补充说明的轻量富文本编辑，支持加粗、字号、重置、焦点同步。
- 恢复正文拖拽和联系方式拖拽：显式补齐 HTML5 DnD 的 `dataTransfer`、`effectAllowed` 与 drop 后状态清理。
- “标签样式 / 章节标题” 改为可切换：普通单行文本可原地转换，再次点击同一按钮可恢复为普通文本块。
- 新增全模板级“上一步 / 下一步”按钮，撤销重做覆盖正文输入、头部编辑、拖拽排序、样式切换、照片增删、插入删除。
- 删除按钮改为删除当前激活目标，不再只允许删除空块；原有 AI 解析出的标签块也可直接删除。
- 导出链路继续沿用当前直接下载方案，导出源仍为 `buildExportElement()` 生成的只读克隆节点，保证预览与导出一致性。
- 本轮继续做最小视觉修正：将导出态求职方向胶囊恢复为与预览接近的高度，并补齐纵向居中；同时适度增大章节标签上下间距，缓解整体排版过紧的问题。
- 根据最新验收反馈，章节标签相关间距再次上调一档，仅微调预览与导出态的章节间距和标题下方留白，不改模板结构与导出链路。
