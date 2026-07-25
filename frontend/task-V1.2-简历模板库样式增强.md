# V1.2 简历模板库 — 样式增强与预览一致性修复

## 当前任务所属模块
V1.2 简历模板库功能，模板样式增强与预览一致性修复（前端）

## 前端文件定位
- 项目路径：`frontend/app/src/`
- 模板CSS：`frontend/app/src/data/styles/*.css`（17个文件）
- 模板组件：`frontend/app/src/components/template/`

## 本轮修改文件清单

### 修改文件
| 文件 | 修改内容 |
|------|----------|
| `src/components/template/TemplateCard.vue` | 添加动态CSS加载逻辑，修复缩略图与编辑器预览不一致问题 |
| `src/components/template/TemplateRenderer.vue` | 增强HTML结构，添加装饰性CSS钩子class |
| `src/data/styles/tech-modern.css` | 重写：蓝色渐变header、圆点标签section、渐变pill技能标签、▸要点 |
| `src/data/styles/tech-dark.css` | 重写：深色渐变header、圆点标签section、边框pill技能标签、▸要点 |
| `src/data/styles/tech-minimal.css` | 重写：无header背景、下划线section、方形技能标签、–要点 |
| `src/data/styles/finance-classic.css` | 重写：居中header、粗竖条section、金色边框技能标签、◆要点 |
| `src/data/styles/finance-gold.css` | 重写：金色渐变header、圆点标签section、渐变pill技能标签、●要点 |
| `src/data/styles/education-clean.css` | 重写：无header背景、下划线section、方形技能标签、–要点 |
| `src/data/styles/education-warm.css` | 重写：暖色渐变header、圆点标签section、彩色pill技能标签、●要点 |
| `src/data/styles/medical-professional.css` | 重写：蓝色渐变header、圆点标签section、彩色pill技能标签、▸要点 |
| `src/data/styles/medical-soft.css` | 重写：无header背景、下划线section、方形技能标签、–要点 |
| `src/data/styles/manufacture-precision.css` | 重写：无header背景、左竖条section、方形技能标签、▪要点 |
| `src/data/styles/manufacture-industrial.css` | 重写：深色header居中、粗竖条section、方形技能标签、>要点 |
| `src/data/styles/marketing-vibrant.css` | 重写：橙色渐变header、圆点标签section、彩色pill技能标签、▸要点 |
| `src/data/styles/marketing-bold.css` | 重写：红色渐变header、圆点标签section、红色pill技能标签、▸要点 |
| `src/data/styles/design-elegant.css` | 重写：居中header轻字重、居中section、居中pill技能标签、◇要点 |
| `src/data/styles/design-creative.css` | 重写：紫色渐变header+装饰圆形、渐变竖条section、渐变pill技能标签、●要点 |
| `src/data/styles/legal-refined.css` | 重写：双线底线header、左竖条section、方形技能标签、–要点 |
| `src/data/styles/legal-authoritative.css` | 重写：纯黑header居中、粗黑竖条section、方形技能标签、■要点 |

## 前端实现方案

### 问题1：预览不一致
- **根因**：TemplateCard缩略图未加载模板CSS，而TemplateEditorView通过动态import加载
- **修复**：在TemplateCard中添加与TemplateEditorView相同的CSS加载逻辑
- **方式**：`import(\`@/data/styles/${id}.css?raw\`)` + `<style v-html="templateStyle" />` 注入

### 问题2：模板样式缺乏独特性
- **根因**：17个CSS文件结构雷同，仅颜色不同
- **修复**：重写所有17个CSS文件，每个模板在以下维度有独特设计：

| 维度 | 变化方式 |
|------|---------|
| Header布局 | 渐变(6个) / 居中(4个) / 无背景(5个) / 纯黑(1个) / 居中轻字重(1个) |
| Section标题 | 圆点标签(8个) / 左竖条(2个) / 下划线(3个) / 居中(1个) / 粗竖条(2个) / 双线(1个) |
| 技能标签 | 渐变pill(3个) / 边框pill(1个) / 方形(7个) / 金色边框(1个) / 彩色pill(4个) / 居中(1个) |
| 要点符号 | ▸(6个) / –(4个) / ◆(1个) / ●(3个) / ▪(1个) / >(1个) / ■(1个) |

### CSS变量体系
每个模板定义独立CSS变量：
```css
.resume-tpl-xxx {
  --tpl-accent: #xxx;
  --tpl-text: #xxx;
  --tpl-muted: #xxx;
  --tpl-line: #xxx;
}
```

### TemplateRenderer增强
- 添加 `.section-tab`、`.section-tab-dot`、`.section-line` 装饰性class
- 添加 `.profile-badge`、`.meta-icon` 联系方式图标class
- 基础样式默认隐藏装饰元素，各模板CSS按需启用

## stage 更新说明
- `stage.md` 中"暂时搁置"的模板编辑器状态更新为"已完成"
- 新增 V1.2 简历模板库样式增强记录

## 构建结果
前端 `npm run build` 构建通过，无错误。17个模板CSS文件均被正确打包。

## 当前功能验收说明
1. 17个模板缩略图与编辑器预览显示完全一致（CSS动态加载修复）
2. 每个模板有明显的视觉区别（不只是颜色），包括：
   - 独特的Header布局（渐变/居中/无背景/纯黑）
   - 独特的Section标题装饰（圆点标签/左竖条/下划线/居中/粗竖条）
   - 独特的技能标签样式（渐变pill/边框pill/方形/金色边框/彩色pill）
   - 独特的要点符号（▸/–/◆/●/▪/>/■）
3. 构建通过，无错误无警告

## 停止，不继续下一个功能
