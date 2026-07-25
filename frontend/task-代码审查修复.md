# 任务：前端代码审查修复

## 当前任务所属模块
- 认证 token 读取一致性
- 简历模板富文本安全净化
- 简历导出静态节点替换

## 前端文件定位
- `frontend/app/src/App.vue`
- `frontend/app/src/layouts/MainLayout.vue`
- `frontend/app/src/components/resume/ResumeTemplate.vue`
- `frontend/app/package.json`
- `frontend/app/package-lock.json`

## 本轮修改文件清单
1. `frontend/app/src/App.vue`
2. `frontend/app/src/layouts/MainLayout.vue`
3. `frontend/app/src/components/resume/ResumeTemplate.vue`
4. `frontend/app/package.json`
5. `frontend/app/package-lock.json`

## 前端实现方案

### 1. 统一 token 读取入口
- `App.vue` 不再直接读取旧键名 `localStorage.getItem('token')`
- 改为统一通过 `getToken()` / `removeToken()` 处理登录态恢复与清理
- `MainLayout.vue` 统一使用 `getToken()` 判断登录状态

### 2. 简历模板富文本净化
- `ResumeTemplate.vue` 新增 `sanitizeRichTextHtml(...)`
- `stripHtmlToText(...)` 在把富文本放入临时 DOM 前，先用 `DOMPurify` 做净化
- 仅保留模板编辑器当前需要的基础排版标签与内联样式

### 3. 导出节点替换不再使用原始 innerHTML
- `replacePhotoFrameButtonWithStaticNode(...)` 改为逐个克隆已有子节点
- 不再把 `buttonNode.innerHTML` 原样写回新节点

## 后端实现方案
- token 对齐轮次无后端改动
- 本轮高优先级前端修复无后端接口改签名

## 数据存储方案
- 继续沿用现有本地存储键：
  - `ai_resume_token`
  - `ai_resume_token_type`
- 不新增前端本地存储结构
- 本轮仅新增前端依赖声明：`dompurify`

## stage 更新说明
- 已同步更新 `frontend/tasks/stage.md`
- 仅记录当前真实完成内容，不扩展到未处理问题

## 构建结果
- 前端构建：`npm.cmd run build` 通过

## 当前功能验收说明
1. 仅存在 `ai_resume_token` 时，页面刷新后仍可恢复登录态
2. 新手引导登录态判断不再依赖旧键名 `token`
3. 简历富文本在进入临时 DOM 做文本提取前会先完成净化
4. 导出静态照片区域不再通过原始 `innerHTML` 回填节点

## 停止，不继续下一个功能
- 已完成前端真实存在的问题修复
- 本轮不继续扩展到其他 `v-html` 场景或新的前端审查项
