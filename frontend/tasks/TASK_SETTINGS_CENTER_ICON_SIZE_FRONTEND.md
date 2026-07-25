# 个人中心图标可读性与简历结果页按钮图标修复

## 当前任务所属模块
用户端前端展示修复：个人中心/设置中心图标尺寸优化，并补齐简历诊断结果页岗位匹配分析与 AI 润色按钮的本地功能图标。

## 前端文件定位
- 个人中心页面：`frontend/app/src/views/settings/SettingsView.vue`
- 简历诊断结果页：`frontend/app/src/views/resume/ResultView.vue`
- 设置中心测试：`frontend/app/src/__tests__/views/SettingsView.test.js`
- 简历结果页测试：`frontend/app/src/__tests__/views/ResumeResultView.test.js`
- 图标映射测试：`frontend/app/src/__tests__/utils/featureIcons.test.js`

## 后端文件定位
本轮不涉及后端，不修改 API、路由、数据库、鉴权、简历分析、岗位匹配分析或 AI 润色逻辑。

## 本轮修改文件清单
- `SettingsView.vue`
- `ResultView.vue`
- `SettingsView.test.js`
- `ResumeResultView.test.js`
- `frontend/tasks/TASK_SETTINGS_CENTER_ICON_SIZE_FRONTEND.md`
- `frontend/tasks/stage.md`

## 前端实现方案
- 将个人中心/设置中心内的本地 `FeatureIcon` 从 `sm` 调整为更清晰的 `md`，覆盖左侧设置分组、语音试听、账号安全警告、注销确认弹窗和数据刷新入口。
- 设置中心左侧导航图标保留无光晕、无淡框，仅增加轻微 `transform` hover/active 反馈，遵守首页、导航栏、头像下拉菜单不加光晕的边界。
- 放大语音试听按钮和数据刷新按钮的点击面积，避免 `md` 图标被按钮容器压缩。
- 简历诊断结果页复用已存在的本地图标映射：`job-match-analysis` 用于“开始分析”，`resume-optimization` 用于“去 AI 润色”。
- 不新增图标库，不修改 `FeatureIcon` 全局尺寸规则，不改通知栏图标尺寸。

## 数据存储方案
本轮不涉及数据存储，不新增表、字段或本地持久化逻辑。

## stage 更新说明
已在 `frontend/tasks/stage.md` 追加“个人中心图标可读性与简历结果页按钮图标修复”记录，包含修改范围、验证结果和停止边界。

## 验证结果
- RED：先新增 `SettingsView.test.js` 源码约束，`npm.cmd test -- --run src/__tests__/views/SettingsView.test.js` 失败于设置中心图标仍为 `sm`。
- GREEN：修复后 `npm.cmd test -- --run src/__tests__/views/SettingsView.test.js src/__tests__/views/ResumeResultView.test.js src/__tests__/utils/featureIcons.test.js` 通过，3 个测试文件 / 31 个用例通过。
- 构建：`npm.cmd run build` 通过。

## 当前功能验收说明
验收时重点检查个人中心/设置中心左侧导航、语音试听、隐私数据刷新、账号安全警告和注销确认弹窗图标是否更清楚；简历诊断结果页“开始分析”和“去 AI 润色”按钮是否出现对应本地图标。

## 停止说明
本轮只完成个人中心图标可读性修复和简历结果页按钮图标补齐，不继续推进其它页面重构、不修改 `/admin/**`、API、路由、数据库或后端业务流程。
