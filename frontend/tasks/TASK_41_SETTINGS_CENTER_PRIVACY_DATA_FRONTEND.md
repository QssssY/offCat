# TASK_41_SETTINGS_CENTER_PRIVACY_DATA_FRONTEND

## 1. 当前任务所属模块
- 用户侧设置中心

## 2. 前端文件定位
- `frontend/app/src/views/settings/SettingsView.vue`
- `frontend/app/src/views/interview/InterviewEntryView.vue`
- `frontend/app/src/utils/settingsPreferences.js`
- `frontend/app/src/__tests__/views/SettingsView.test.js`
- `frontend/app/src/__tests__/views/InterviewEntryView.test.js`
- `frontend/app/src/__tests__/utils/settingsPreferences.test.js`

## 3. 后端文件定位
- 本轮不修改后端
- 账号注销、历史记录批量清理、服务端自动保留天数清理均为待后端接口接入状态

## 4. 本轮修改文件清单
- `frontend/app/src/utils/settingsPreferences.js`
- `frontend/app/src/views/settings/SettingsView.vue`
- `frontend/app/src/views/interview/InterviewEntryView.vue`
- `frontend/app/src/__tests__/utils/settingsPreferences.test.js`
- `frontend/app/src/__tests__/views/SettingsView.test.js`
- `frontend/app/src/__tests__/views/InterviewEntryView.test.js`
- `frontend/tasks/TASK_41_SETTINGS_CENTER_PRIVACY_DATA_FRONTEND.md`
- `frontend/tasks/stage.md`

## 5. 前端实现方案
- 扩展本机设置偏好，新增默认面试岗位、岗位编码、面试级别、面试模式、反馈模式、面试记录保留天数字段。
- 设置中心新增“面试偏好”“隐私与数据”“数据管理”分区。
- 面试偏好复用现有面试岗位接口和面试常量，保存到当前浏览器 `localStorage`。
- 隐私与数据复用个人成长概览接口展示账号数据概览；清空本地缓存只清理设置偏好、主题偏好和通知筛选缓存，不清理用户或管理端登录 token。
- 账号注销、面试记录清理、简历诊断清理均以禁用按钮展示“待后端接入”，不做假删除。
- 面试入口页读取本机默认面试偏好；路由 query 优先级高于本机默认值；默认岗位必须仍存在于启用岗位列表才自动回填。

## 6. 后端实现方案
- 不新增接口
- 不新增数据库表
- 不修改账号、面试历史、简历诊断记录删除链路

## 7. 数据存储方案
- 继续使用浏览器 `localStorage` 的 `ai_resume_settings_preferences`
- 新增字段：
  - `defaultInterviewJobRole`
  - `defaultInterviewJobRoleCode`
  - `defaultInterviewDifficulty`
  - `defaultInterviewMode`
  - `defaultFeedbackMode`
  - `interviewRetentionDays`
- 清空本地缓存仅移除：
  - `ai_resume_settings_preferences`
  - `theme`
  - `followSystem`

## 8. stage 更新说明
- 已更新 `frontend/tasks/stage.md`，记录设置中心新增面试偏好、隐私与数据管理页面一期已完成并等待验收

## 9. 编译结果
- 本轮不涉及后端编译

## 10. 构建结果
- `npm.cmd run build` 通过

## 11. 测试结果
- `npm.cmd test` 通过，18 个测试文件、79 个测试用例通过

## 12. 当前功能验收说明
- 用户可在设置中心配置默认面试岗位、级别、模式和反馈模式
- 用户可查看账号数据概览，数据来自现有成长概览接口
- 用户可清空本机设置缓存，且不会退出用户端或管理端登录态
- 用户可设置面试记录保留天数偏好，但真实自动清理仍待后端接入
- 账号注销、面试记录清理、简历诊断清理均明确展示为待后端接入的禁用状态
- 面试入口页可按本机偏好回填默认配置，且“再来一次”等路由参数会覆盖本机默认值
- 默认岗位下线或不存在时不会自动回填，避免使用过期配置

## 13. 停止，不继续下一个功能
- 本轮只实现设置中心页面一期和本机偏好接入
- 不实现真实账号注销
- 不实现服务端历史记录批量删除
- 不实现服务端自动清理任务
- 不修改数据库结构
