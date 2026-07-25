# TASK_54: 面试维度雷达 + 盲区提示（前端）

## 状态：已完成

## 概述
成长中心新增面试维度雷达区块，展示最新面试 6 维度雷达图、维度趋势折线和盲区提示。

## 修改文件
- `frontend/app/src/api/growth.js` — 新增 `getInterviewRadar()` API
- `frontend/app/src/components/resume/LineChart.vue` — 新增 `showLegend` prop（默认 false，向后兼容）
- `frontend/app/src/views/growth/GrowthCenterView.vue` — 新增面试维度雷达区块

## GrowthCenterView 新增内容
- **雷达图 + 维度详情**：复用 `RadarChart` 和 `RadarScorePanel` 组件，展示最新面试的 6 维度评分
- **维度趋势折线**：6 条彩色折线（showLegend=true），展示多次面试的各维度得分变化
- **盲区提示卡片**：持续低分（红色）和下滑趋势（橙色）两种类型，附改进建议
- **空状态**：无维度数据时引导用户开始面试
- **独立加载**：雷达数据独立请求，不阻塞概览数据

## 维度颜色方案
- technicalDepth: #FF8C42
- projectExpression: #3ABAB4
- communication: #5B8DEF
- problemSolving: #E667AF
- pressureResistance: #F5A623
- jobMatch: #7B68EE

## 验证
- `npm run build` 通过
