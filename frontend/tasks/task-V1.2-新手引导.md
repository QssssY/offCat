# 任务：V1.2 功能一 — 新手引导（用户体验增强版）

## 当前任务所属模块
V1.2 用户体验增强版 — 功能一：新手引导

## 前端文件定位
- `frontend/app/src/api/onboarding.js` — 引导状态 API 模块
- `frontend/app/src/components/OnboardingGuide.vue` — 引导弹窗组件
- `frontend/app/src/layouts/MainLayout.vue` — 引导集成入口

## 后端文件定位
- `server/src/main/java/com/airesume/server/entity/UserOnboardingState.java` — 引导状态实体
- `server/src/main/java/com/airesume/server/mapper/UserOnboardingStateMapper.java` — Mapper
- `server/src/main/java/com/airesume/server/dto/onboarding/OnboardingStatusResponse.java` — 状态响应 DTO
- `server/src/main/java/com/airesume/server/dto/onboarding/OnboardingUpdateRequest.java` — 更新请求 DTO
- `server/src/main/java/com/airesume/server/service/UserOnboardingService.java` — 服务接口
- `server/src/main/java/com/airesume/server/service/impl/UserOnboardingServiceImpl.java` — 服务实现
- `server/src/main/java/com/airesume/server/controller/UserOnboardingController.java` — 控制器
- `server/src/main/java/com/airesume/server/config/SecurityConfig.java` — 安全配置（修改）

## 本轮修改文件清单

### 新建文件
1. `db/migrations/TASK_16_USER_ONBOARDING_STATE.sql` — 数据库迁移脚本
2. `server/.../entity/UserOnboardingState.java` — 引导状态实体类
3. `server/.../mapper/UserOnboardingStateMapper.java` — MyBatis-Plus Mapper
4. `server/.../dto/onboarding/OnboardingStatusResponse.java` — 状态响应 DTO
5. `server/.../dto/onboarding/OnboardingUpdateRequest.java` — 更新请求 DTO
6. `server/.../service/UserOnboardingService.java` — 服务接口
7. `server/.../service/impl/UserOnboardingServiceImpl.java` — 服务实现
8. `server/.../controller/UserOnboardingController.java` — REST 控制器
9. `frontend/app/src/api/onboarding.js` — 前端 API 模块
10. `frontend/app/src/components/OnboardingGuide.vue` — 引导弹窗组件

### 修改文件
11. `db/schema.sql` — 新增 `user_onboarding_state` 表定义
12. `server/.../config/SecurityConfig.java` — 新增 `/api/user/**` 认证规则
13. `frontend/app/src/layouts/MainLayout.vue` — 集成引导组件

## 前端实现方案

### 1. API 模块（onboarding.js）
- `getOnboardingStatus()` → `GET /api/user/onboarding/status`
- `updateOnboardingStatus(data)` → `POST /api/user/onboarding/status`
- 使用项目现有的 `request` axios 实例，自动携带 JWT

### 2. 引导组件（OnboardingGuide.vue）
- 使用 Element Plus `el-dialog` 实现弹窗
- 7 个引导步骤：欢迎 → 上传简历 → JD匹配 → AI润色 → 模拟面试 → 历史记录 → 准备就绪
- 步骤指示器：圆点样式，当前步骤橙色高亮，已完成步骤半透明
- 按钮逻辑：
  - 第一步：主按钮"开始体验"（=下一步），无"上一步"
  - 中间步骤：主按钮"下一步"，有"上一步"
  - 最后一步：主按钮"完成"，有"上一步"
  - 所有步骤：右侧"跳过引导"按钮
- 每次"下一步"调用 API 保存当前步骤
- "跳过"和"完成"调用 API 记录终态后关闭弹窗
- "上一步"仅本地操作，不调用 API
- 弹窗不可点击遮罩关闭，不可按 ESC 关闭
- 沿用项目橙色主题 `#FF8C42`

### 3. MainLayout 集成
- 在 MainLayout 中导入并渲染 `OnboardingGuide` 组件
- `onMounted` 时检查登录状态，调用 `getOnboardingStatus()` 查询引导状态
- 如果 `showGuide=true`，弹出引导弹窗
- 同时检查 token 和 userInfo，兼容 `fetchUserInfo` 尚未完成的时序
- API 失败时静默处理，不阻塞页面

## 后端实现方案

### 1. 数据模型
- `UserOnboardingState` 继承 `BaseEntity`，复用 `id`/`createTime`/`updateTime`/`isDeleted`
- 字段：`userId`, `guideKey`, `status`, `currentStep`, `completedTime`, `skipTime`
- `guideKey` 支持未来多版本引导扩展

### 2. Controller 层
- `GET /api/user/onboarding/status` — 查询引导状态，无记录返回默认 `not_started`
- `POST /api/user/onboarding/status` — 更新引导状态
- 用户ID 从 Spring Security `Authentication` 对象提取
- 返回统一 `Result<T>` 响应格式

### 3. Service 层
- `getStatus()`：LambdaQueryWrapper 查询，无记录返回默认响应，`showGuide=true`
- `updateStatus()`：
  - 校验状态值合法性（in_progress/completed/skipped）
  - 进行中状态必须携带 `currentStep`
  - 已完成/已跳过的记录幂等处理，不重复打开
  - 设置 `completedTime` 或 `skipTime`

### 4. SecurityConfig
- 显式添加 `.requestMatchers("/api/user/**").authenticated()`
- 虽然 `.anyRequest().authenticated()` 已覆盖，但显式声明更清晰

## 数据存储方案
- 新增独立表 `user_onboarding_state`，不修改用户主表
- 唯一索引 `(user_id, guide_key)` 确保每用户每版本一条记录
- 外键关联 `sys_user(id)`
- 迁移脚本：`db/migrations/TASK_16_USER_ONBOARDING_STATE.sql`
- 同步更新 `db/schema.sql`

## stage 更新说明
- V1.1：已完成，已验收通过
- V1.2 简历模板编辑器：暂时搁置（存在 bug）
- V1.2 新手引导：开发完成，等待验收

## task 更新说明
- 根目录：新建 `task-V1.2-新手引导.md`（后端为主）
- 前端目录：新建 `frontend/task-V1.2-新手引导.md`（前端为主）

## 编译结果
- 后端编译通过（`mvn compile -q` 无错误输出）

## 构建结果
- 前端构建通过（`npm run build` 成功，7.90s）

## 当前功能验收说明
1. 首次进入系统可以看到新手引导弹窗
2. 引导内容覆盖核心使用路径（上传简历 → JD匹配 → AI润色 → 模拟面试 → 历史记录）
3. 可以点击"开始体验"进入引导
4. 可以点击"下一步"逐步浏览
5. 可以点击"跳过引导"跳过
6. 可以点击"完成"结束引导
7. 完成或跳过后再次进入不会重复弹出
8. 中途关闭浏览器再次登录会恢复到上次步骤
9. `GET /api/user/onboarding/status` 接口正常返回状态
10. `POST /api/user/onboarding/status` 接口正常更新状态
11. 数据正常落库（`user_onboarding_state` 表）
12. 不影响原有首页、简历诊断、JD 匹配、AI 润色、模拟面试功能
13. 后端编译通过
14. 前端构建通过
15. 新增代码包含中文注释且无乱码

## 停止，不继续下一个功能
- 本轮完成后立即停止，等待验收
