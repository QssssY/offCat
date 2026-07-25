# TASK-53：简历导出 DOCX 快速止血方案 — 前端

## 所属模块
简历诊断模块 — 导出功能

## 功能目标
在 AI 润色结果页新增"导出 Word"按钮，从结构化 block 模型客户端生成 .docx 文件，文本可复制、标题段落结构完整，满足用户向不同公司投递简历的格式需求。

## 前端变更清单

### 依赖
- `package.json` — 新增 `docx` 客户端 DOCX 生成库

### Vite 配置
- `vite.config.js` — `manualChunks` 新增 `docx-vendor` chunk，避免 docx 库打入主包

### 核心模块（新建）
- `src/utils/resumeDocxExport.js` — DOCX 导出核心逻辑
  - `parseHtmlToDocxRuns(html)` — 解析 HTML 富文本为 docx TextRun 配置数组，支持 bold/italic/break
  - `convertBlockToParagraphs(block)` — 将 block 模型转为段落配置，覆盖 text/bullet/heading/row/label/banner_title/section_title 全部 block 类型
  - `exportResumeToDocx(jsonString, filename)` — 主入口：JSON → 验证 → 动态 import('docx') → 构建 Document → Packer.toBlob → 浏览器下载
  - 样式：Microsoft YaHei 字体、A4 页面、1.27cm 边距、28pt 姓名/14pt section heading/11pt 正文

### ResultView.vue
- 新增"导出 Word"按钮（位于"导出 PDF"和"导出图片"之间）
- 新增 `docxExporting` ref 控制 loading 状态
- 新增 `exportResumeDocx` 异步处理函数（获取 documentJson → 调用 exportResumeToDocx → 成功/失败提示）
- 更新提示文案加入 Word 格式说明

### 测试（新建）
- `src/__tests__/utils/resumeDocxExport.test.js` — 24 个测试用例
  - parseHtmlToDocxRuns：纯文本、bold、italic、嵌套标签、br 标签、空/null 输入
  - convertBlockToParagraphs：text/bullet/heading/row/label/banner_title/section_title + 边界（空 row、null、未知类型）
  - exportResumeToDocx：空 JSON、无效 JSON、空模型、完整模型生成 Blob
  - 异常清理：`link.click()` 抛错时仍通过 `finally` 释放 Object URL

## 第二轮审查修复（2026-05-21）
- `src/utils/resumeDocxExport.js` 在 detached DOM 写入 `innerHTML` 前引入 DOMPurify 消毒，避免脚本标签和事件属性被解析。
- `ResultView.vue` 的图片导出下载改为 `try/finally` 释放 Object URL，和 DOCX 下载链路保持一致。
- `src/__tests__/utils/resumeDocxExport.test.js` 增补不安全 HTML 消毒用例。
- `src/__tests__/views/ResumeResultView.test.js` 增补图片下载点击异常时仍释放 Object URL 的回归用例。
- 新增 `src/__tests__/components/OnboardingTaskCard.test.js`，覆盖 `totalCount=0` 时环形进度不产生 `NaN`。

## 本轮不做的边界

### PDF 质量改造（留后续轮次）
- 现有截图式 PDF（html2canvas + jsPDF）保持不动
- 现有后端 Chrome 文本 PDF（ExportToolbar.vue）保持不动
- 不改进 PDF 文本可选、ATS 识别、分页稳定

### DOCX 扩展（留后续轮次）
- ExportToolbar.vue 模板编辑器的 DOCX 导出（不同数据模型，需单独转换器）
- block.style 自定义字号/字重映射到 DOCX
- 简历照片嵌入 DOCX
- DOCX 页眉页脚/页码

## 验证状态
- [x] DOCX 导出单元测试：24 个测试通过
- [x] 前端构建：`npm run build` 通过
- [x] docx-vendor chunk：346kB（101kB gzipped），未超过 1000kB 告警阈值
- [x] 前端全量测试回归：194 通过，3 个 SettingsView 预存在失败（与本轮无关）
- [x] 审查修复回归：`npm.cmd test -- --run src/__tests__/utils/resumeDocxExport.test.js` 通过
- [x] 第二轮审查回归：`npm.cmd test -- --run src/__tests__/utils/resumeDocxExport.test.js src/__tests__/views/ResumeResultView.test.js src/__tests__/components/OnboardingTaskCard.test.js` 通过，28 个用例通过
- [x] 第二轮审查构建：`npm.cmd run build` 通过
