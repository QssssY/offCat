# 开发提示词：额度消费记录与展示增强

> 直接将下方代码块中的内容复制给 AI 即可。

---

```markdown
# 任务：实现「用户额度消费记录与展示增强」

## 设计文档

严格按 `docs/dev/QUOTA_CONSUMPTION_LOG_DESIGN.md` 实现，该文档包含完整的数据库设计、API 设计、前端改造方案和任务分解。

## 执行要求

分 5 个 Phase 依次执行（对应设计文档第五章），每个 Phase 完成后先验证再继续下一个。

## 编码前必读

先完整阅读以下文件理解项目约定和现有代码：

- `CLAUDE.md` — 项目架构、编码约定、构建命令
- `server/CLAUDE.md` — 后端架构细节
- `runtime/DEVELOPMENT_RULES.txt` — 开发约束
- `docs/dev/QUOTA_CONSUMPTION_LOG_DESIGN.md` — 本功能设计文档（核心参考）
- `server/src/main/java/com/airesume/server/service/impl/UserQuotaServiceImpl.java` — 额度扣减逻辑（Phase 2 修改目标）
- `server/src/main/java/com/airesume/server/service/UserQuotaService.java` — 额度服务接口
- `server/src/main/java/com/airesume/server/entity/UserQuota.java` — Entity 继承 BaseEntity 的参考模式
- `server/src/main/java/com/airesume/server/mapper/UserQuotaMapper.java` — Mapper 模式参考
- `server/src/main/java/com/airesume/server/dto/auth/UserInfoResponse.java` — 需新增 4 个字段
- `server/src/main/java/com/airesume/server/service/impl/AuthServiceImpl.java` 第 248-310 行 — getCurrentUserInfo() 需补充免费额度计算
- `server/src/main/java/com/airesume/server/common/result/PageResult.java` — 分页结果封装
- `server/src/main/java/com/airesume/server/controller/AdminController.java` — 需新增管理端接口
- `frontend/app/src/views/DashboardView.vue` — 仪表盘 6 宫格改造目标
- `frontend/app/src/views/growth/GrowthCenterView.vue` — 成长中心 Tab 改造目标
- `frontend/app/src/api/growth.js` — 前端 API 模块参考
- `frontend/app/src/views/admin/AdminUserRightsView.vue` — 管理端消费记录展示
- `frontend/app/src/api/admin/users.js` — 管理端 API 参考
- `db/schema.sql` 第 63-96 行 — user_quota 表结构

## 关键约束

1. **后端依赖注入**: `@RequiredArgsConstructor` + `private final`，不用 `@Autowired`
2. **Response DTO**: `@Data` + `@Builder` + `@NoArgsConstructor` + `@AllArgsConstructor`
3. **API 响应**: `Result.success(data)` / `Result.error(code, message)`
4. **分页**: 使用 `PageResult<T>`
5. **前端 HTTP**: 全部通过 `api/` 模块，用 `@/utils/request.js`
6. **中文注释**: 所有核心代码必须有
7. **安全**: 用户端 API 通过 `Authentication` 获取 userId，不接受客户端传入
8. **事务**: logConsumption 与额度扣减在同一事务内

## Phase 顺序

1. 数据库 + 后端基础（Entity/Mapper/DTO/Service）
2. 后端集成（7个扣减点 + UserInfo扩展 + Controller）
3. 前端 Dashboard 6 宫格改造
4. 前端成长中心 Tab + 消费记录组件
5. 管理端 + 定时任务 + 编译验证

每个 Phase 完成后执行：
- `cd server && mvn clean compile`（Phase 1-2, 5）
- `cd frontend/app && npm run build`（Phase 3-5）
```
