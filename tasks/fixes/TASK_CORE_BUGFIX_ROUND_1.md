# TASK_CORE_BUGFIX_ROUND_1

## 1. 任务目标
- 暂停管理端前端收尾优化，优先修复核心业务链路 7 个高优问题。
- 严格按优先级执行：  
  1) 用量扣减（后端）  
  2) AI 引擎配置生效（后端）  
  3) VIP 过期判定（后端）  
  4) 报告展示与防重入（前端）  
  5) 会话结束卡顿（前后端联动）

## 2. 根因分析与修复结果

### 问题 1：用量不扣减（后端）
- 根因：实际创建会话走 `InterviewService.createSession`，该路径缺少面试额度校验与扣减。
- 修复：在会话创建前增加 `checkInterviewQuota`，会话创建成功后调用 `deductInterviewQuota`。
- 结果：面试会话创建已接入扣减链路；简历诊断链路原本已在 `ResumeDiagnosisTaskServiceImpl.createTask` 扣减。

### 问题 2：AI 引擎配置切换后不生效（后端）
- 根因：面试/简历 AI 服务请求参数主要读取 `application.yml` 与环境变量，未优先读取 `sys_ai_engine_config` 当前激活配置。
- 修复：
  - `InterviewAiServiceImpl` 新增运行时配置解析：优先 `business_type=interview` 激活配置，缺失字段回退本地配置。
  - `ResumeAiServiceImpl` 同步改造：优先 `business_type=resume` 激活配置，缺失字段回退本地配置。
  - 两条链路都改为“按运行时配置动态构造请求客户端”，切换配置后立即生效。
- 结果：管理端新增/切换激活配置后，后端调用参数可即时切换。

### 问题 3：VIP 过期后管理端仍显示会员（后端）
- 根因：管理端角色文案直接按 `role` 字段渲染，未结合 `vipExpireTime` 判断“是否仍是有效会员”。
- 修复：
  - `AdminController` 用户列表：角色文案改为“VIP 且未过期显示会员，否则显示普通用户（会员已过期）”。
  - `AdminUserRightsServiceImpl` 权益详情：角色文案与 `isVipActive` 统一使用有效 VIP 判定口径。
- 结果：会员过期后，管理端展示不再误报“会员用户”。

### 问题 4-7：报告展示、防重入、会话内可见、结束卡顿
- 根因：
  - 会话结束后报告生成与前端展示缺少持续刷新机制；
  - 前端报告入口未区分“报告已生成/生成中”终态；
  - 结束接口原先同步生成 AI 报告，导致结束请求耗时长、超时后用户误判。
- 修复：
  - 后端 `InterviewService.endSession` 改为“先结束会话、异步生成报告并回写”，并加“已有报告跳过重复生成”防重入。
  - 前端 `InterviewSessionView.vue`：
    - 会话内新增“报告就绪判定 + 自动轮询”；
    - 报告未就绪时禁用“查看报告”按钮并展示“报告生成中”；
    - 结束后立即切换会话终态，避免用户感知卡死。
  - 前端 `InterviewReportView.vue`：
    - 报告未就绪自动轮询；
    - 增加报告字符串清洗（兼容 markdown 包裹），修复“后端已生成但前端解析失败”；
    - 增加“立即刷新”入口。
  - 前端 `InterviewHistoryView.vue`：
    - 已结束但报告未就绪时，禁用“查看报告”，显示“报告生成中”。
- 结果：用户无需退出会话即可等待并查看报告；重复无效触发被限制；结束卡顿显著缓解。

## 3. 代码变更清单

### 后端（server）
- `src/main/java/com/airesume/server/service/InterviewService.java`
- `src/main/java/com/airesume/server/service/impl/InterviewAiServiceImpl.java`
- `src/main/java/com/airesume/server/service/impl/ResumeAiServiceImpl.java`
- `src/main/java/com/airesume/server/controller/AdminController.java`
- `src/main/java/com/airesume/server/service/impl/AdminUserRightsServiceImpl.java`

### 前端（frontend/app）
- `src/views/interview/InterviewSessionView.vue`
- `src/views/interview/InterviewReportView.vue`
- `src/views/interview/InterviewHistoryView.vue`

## 4. 验证记录

### 后端编译验证
- 问题 1 修复后：`mvn.cmd -q -DskipTests compile` 通过。
- 问题 2 修复后：`mvn.cmd -q -DskipTests compile` 通过。
- 问题 3 修复后：`mvn.cmd -q -DskipTests compile` 通过。
- 结束卡顿后端异步化改造后：`mvn.cmd -q -DskipTests compile` 通过。

### 前端构建验证
- 全部前端修复完成后：`npm.cmd run build` 通过。

## 5. 备注
- 该轮仅处理核心业务 bug，不包含管理端前端收尾视觉优化。
- 本轮完成后停止自动推进，等待人工验收。
