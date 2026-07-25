# TASK_32_OFFER_ACCELERATOR_STAGE4_OFFER_ASSIST_FRONTEND

## 1. 当前任务所属模块
- Offer 加速器实施计划：第 4 部分，Offer 辅助链路第一版。

## 2. 前端文件定位
- `frontend/app/src/api/offer.js`
- `frontend/app/src/views/offer/OfferAssistView.vue`
- `frontend/app/src/router/index.js`
- `frontend/app/src/components/AppHeader.vue`
- `frontend/runtime/STATE.md`
- `frontend/tasks/stage.md`

## 3. 后端文件定位
- `server/src/main/java/com/airesume/server/controller/OfferAssistController.java`
- `server/src/main/java/com/airesume/server/service/OfferAssistService.java`
- `server/src/main/java/com/airesume/server/service/impl/OfferAssistServiceImpl.java`
- `server/src/main/java/com/airesume/server/dto/offer/*`
- `tasks/TASK_32_OFFER_ACCELERATOR_STAGE4_OFFER_ASSIST_BACKEND.md`

## 4. 本轮修改文件清单
- `frontend/app/src/api/offer.js`
- `frontend/app/src/views/offer/OfferAssistView.vue`
- `frontend/app/src/router/index.js`
- `frontend/app/src/components/AppHeader.vue`
- `frontend/runtime/STATE.md`
- `frontend/tasks/stage.md`
- `frontend/tasks/TASK_32_OFFER_ACCELERATOR_STAGE4_OFFER_ASSIST_FRONTEND.md`
- `server/src/main/java/com/airesume/server/controller/OfferAssistController.java`
- `server/src/main/java/com/airesume/server/service/OfferAssistService.java`
- `server/src/main/java/com/airesume/server/service/impl/OfferAssistServiceImpl.java`
- `server/src/main/java/com/airesume/server/dto/offer/SalaryNegotiationSimulationRequest.java`
- `server/src/main/java/com/airesume/server/dto/offer/SalaryNegotiationSimulationResponse.java`
- `server/src/main/java/com/airesume/server/dto/offer/SalaryScriptRequest.java`
- `server/src/main/java/com/airesume/server/dto/offer/SalaryScriptResponse.java`
- `server/src/test/java/com/airesume/server/controller/OfferAssistControllerTest.java`
- `server/src/test/java/com/airesume/server/service/impl/OfferAssistServiceImplTest.java`
- `runtime/STATE.md`
- `tasks/TASK_32_OFFER_ACCELERATOR_STAGE4_OFFER_ASSIST_BACKEND.md`

## 5. 前端实现方案
- 新增 `offer.js` API 封装，沿用现有 `request` 工具。
- 新增 `OfferAssistView.vue` 页面，使用 Element Plus 表单和标签页。
- 第一版仅提供两个能力：
  - 薪资谈判模拟
  - 谈薪话术模板
- 顶部导航和移动端抽屉新增“Offer 辅助”入口。
- 结果区结构化展示，并对可直接发送给 HR 的话术提供复制按钮。

## 6. 后端实现方案
- 后端新增 Offer 辅助接口，前端直接调用。
- 返回结构稳定，前端不解析自由文本。
- AI Prompt 明确不接实时薪资行情。

## 7. 数据存储方案
- 前端不新增本地存储。
- 后端不新增数据库表或字段。
- 本轮不保存历史记录。
- 不做题库、热点、收藏、命中率统计。

## 8. stage 更新说明
- 已更新 `frontend/runtime/STATE.md`。
- 已更新 `frontend/tasks/stage.md`。
- 当前阶段标记为“第 4 部分：Offer 辅助链路第一版，本轮已完成，等待人工验收”。

## 9. 编译结果
- 命令：`mvn.cmd -q -DskipTests compile`
- 结果：通过

## 10. 构建结果
- 命令：`npm.cmd run build`
- 结果：通过

## 11. 测试结果
- 新增后端测试命令：`mvn.cmd -q "-Dtest=OfferAssistServiceImplTest,OfferAssistControllerTest" test`
- 结果：通过
- 后端关键测试集合命令：`mvn.cmd -q "-Dtest=OfferAssistServiceImplTest,OfferAssistControllerTest,InterviewServiceTest,MockInterviewAiServiceImplTest,ResumeDiagnosisTaskServiceImplTest,ResumeDiagnosisProcessorTest,ResumeAiServiceImplTest,ResumePdfControllerTest" test`
- 结果：通过
- 前端 `package.json` 当前没有 test 脚本，本轮以前端构建通过作为前端验证。

## 12. 当前功能验收说明
- 登录后顶部导航和移动端抽屉应出现“Offer 辅助”入口。
- `/offer` 页面两个标签页均可填写表单并提交。
- 薪资谈判模拟展示建议回复、策略、风险提醒和下一步行动。
- 谈薪话术模板展示四段可复制话术和使用提醒。
- 本轮未实现实时薪资行情、录用意向评估、背调准备指导或题库相关功能。

## 13. 停止，不继续下一个功能
- 本轮已完成第 4 部分：Offer 辅助链路第一版。
- 到此停止，等待人工验收。
