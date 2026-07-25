# TASK_32_OFFER_ACCELERATOR_STAGE4_OFFER_ASSIST_BACKEND

## 1. 当前任务所属模块
- Offer 加速器实施计划：第 4 部分，Offer 辅助链路第一版。

## 2. 前端文件定位
- `frontend/app/src/api/offer.js`
- `frontend/app/src/views/offer/OfferAssistView.vue`
- `frontend/app/src/router/index.js`
- `frontend/app/src/components/AppHeader.vue`
- `frontend/runtime/STATE.md`
- `frontend/tasks/stage.md`
- `frontend/tasks/TASK_32_OFFER_ACCELERATOR_STAGE4_OFFER_ASSIST_FRONTEND.md`

## 3. 后端文件定位
- `server/src/main/java/com/airesume/server/controller/OfferAssistController.java`
- `server/src/main/java/com/airesume/server/service/OfferAssistService.java`
- `server/src/main/java/com/airesume/server/service/impl/OfferAssistServiceImpl.java`
- `server/src/main/java/com/airesume/server/dto/offer/*`
- `server/src/test/java/com/airesume/server/controller/OfferAssistControllerTest.java`
- `server/src/test/java/com/airesume/server/service/impl/OfferAssistServiceImplTest.java`
- `runtime/STATE.md`

## 4. 本轮修改文件清单
- `server/src/main/java/com/airesume/server/service/impl/OfferAssistServiceImpl.java`
- `server/src/test/java/com/airesume/server/service/impl/OfferAssistServiceImplTest.java`
- `runtime/STATE.md`
- `tasks/TASK_32_OFFER_ACCELERATOR_STAGE4_OFFER_ASSIST_BACKEND.md`
- `frontend/app/src/views/offer/OfferAssistView.vue`
- `frontend/runtime/STATE.md`
- `frontend/tasks/stage.md`
- `frontend/tasks/TASK_33_OFFER_ASSIST_PAGE_REDESIGN_FRONTEND.md`

## 5. 前端实现方案
- 新增 `/offer` 页面，不改动面试主流程。
- 页面包含“薪资谈判模拟”和“谈薪话术模板”两个标签页。
- 顶部导航和移动端抽屉新增 Offer 辅助入口。
- 结果区按结构化字段渲染，并支持复制关键话术。
- 根据二次验收反馈，将结果区复制按钮从文字按钮改为 32px 图标按钮：
  - 默认弱化显示，不抢正文层级。
  - hover/focus 时增强可见性，保留无障碍名称。
  - 复制成功后短暂显示完成图标，不改变结果卡片布局。
- 根据本轮验收反馈，前端继续做最小修复：
  - 复制按钮改为常显的图标加文字按钮，解决复制入口不明显的问题。
  - 结果标题改为胶囊标签，区分“开场确认”“争取报价”等标题和正文。
  - 提交接口失败时捕获异常并提示，避免未处理异常冒泡为 Vue event handler 警告。

## 6. 后端实现方案
- 新增 `OfferAssistController`，提供两个接口：
  - `POST /api/offer/salary-negotiation/simulate`
  - `POST /api/offer/salary-negotiation/script`
- 新增 `OfferAssistService` 和 `OfferAssistServiceImpl`。
- 复用现有 `AiChatClient` 进行非流式 AI 调用。
- Prompt 明确要求：只根据用户输入生成谈薪建议，不接实时薪资行情，不编造市场薪资数据。
- AI 返回严格 JSON，服务端使用 Jackson 解析为固定 DTO，避免前端解析不稳定文本。
- 根据本轮要求优化 Prompt 细腻度，但不改变 API 和 JSON 字段：
  - 系统 Prompt 增加“克制、尊重、保留回旋空间、可直接发给 HR”的谈判教练口径。
  - 薪资谈判模拟补充局面判断、可发送话术、推进策略、风险提醒和下一步动作的细化要求。
  - 谈薪话术模板补充开场确认、价值锚定、报价争取、交换项、收口确认的细化要求。
  - 明确禁止威胁式表达、虚构竞品 Offer、编造市场数据或把用户未提供的信息当事实。

## 7. 数据存储方案
- 不新增数据库表。
- 不新增数据库字段。
- 不新增迁移脚本。
- 本轮结果即时生成，不做历史记录存储。
- 不做题库、热点、收藏、命中率统计。
- 不做录用意向评估、背调准备指导。

## 8. stage 更新说明
- 已更新 `runtime/STATE.md`。
- 已更新 `frontend/runtime/STATE.md`。
- 已更新 `frontend/tasks/stage.md`。
- 当前阶段标记为“第 4 部分：Offer 辅助链路第一版 Prompt 优化与复制按钮二次修复，本轮已完成，等待人工验收”。

## 9. 编译结果
- 命令：`mvn.cmd -q -DskipTests compile`
- 结果：通过
- Prompt 优化后再次执行：`mvn.cmd -q -DskipTests compile`
- 结果：通过

## 10. 构建结果
- 命令：`npm.cmd run build`
- 结果：通过
- 复制按钮二次修复后再次执行：`npm.cmd run build`
- 结果：通过
- 本轮复制按钮可见性、结果标签和提交异常处理修复后再次执行：`npm.cmd run build`
- 结果：通过

## 11. 测试结果
- 新增后端测试命令：`mvn.cmd -q "-Dtest=OfferAssistServiceImplTest,OfferAssistControllerTest" test`
- 结果：通过
- Prompt 优化后已补充服务层断言，验证新 Prompt 包含“保留回旋空间”“避免虚构竞品 Offer”“价值锚定”“可直接复制给 HR”等关键约束。
- Prompt 优化后再次执行：`mvn.cmd -q "-Dtest=OfferAssistServiceImplTest,OfferAssistControllerTest" test`
- 结果：通过
- 后端关键测试集合命令：`mvn.cmd -q "-Dtest=OfferAssistServiceImplTest,OfferAssistControllerTest,InterviewServiceTest,MockInterviewAiServiceImplTest,ResumeDiagnosisTaskServiceImplTest,ResumeDiagnosisProcessorTest,ResumeAiServiceImplTest,ResumePdfControllerTest" test`
- 结果：通过
- 测试日志中的简历诊断超时、`DB connection lost` 为既有测试刻意覆盖的异常路径，最终测试通过。

## 12. 当前功能验收说明
- 登录后顶部导航应出现“Offer 辅助”入口。
- `/offer` 页面应能切换“薪资谈判模拟”和“谈薪话术模板”。
- 薪资谈判模拟应返回场景判断、建议回复、推进策略、风险提醒、下一步行动。
- 谈薪话术模板应返回开场确认、争取报价、交换项话术、收口确认、使用提醒。
- 页面关键话术支持复制。
- 复制按钮应为常显的图标加文字按钮，不应挤占标题或遮挡正文。
- “开场确认”“争取报价”等结果标题应以标签样式展示，不应和正文同一视觉层级。
- 接口或网络失败时应显示错误提示，不应出现未处理的 Vue event handler 报错。
- AI 生成的话术应更细腻：包含感谢、入职意愿、价值依据、明确请求和可协商余地，并避免威胁或虚构信息。
- 本轮不应出现题库、热点、收藏、命中率、实时薪资行情、录用意向评估或背调准备能力。

## 13. 停止，不继续下一个功能
- 本轮已完成第 4 部分：Offer 辅助链路第一版 Prompt 优化与复制按钮二次修复。
- 到此停止，等待人工验收。
