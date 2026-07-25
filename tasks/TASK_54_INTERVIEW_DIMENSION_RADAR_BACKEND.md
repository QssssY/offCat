# TASK_54: 面试维度雷达 + 盲区提示（后端）

## 状态：已完成

## 概述
为成长中心新增面试维度雷达功能，将面试 6 维度评分沉淀到独立表，提供雷达图数据、维度趋势和盲区分析能力。

## 新增文件
- `db/migrations/TASK_54_INTERVIEW_DIMENSION_SCORE.sql` — 建表迁移脚本
- `server/db/migrations/TASK_54_INTERVIEW_DIMENSION_SCORE.sql` — 同上（双路径约定）
- `server/.../entity/InterviewDimensionScore.java` — 面试维度评分实体
- `server/.../mapper/InterviewDimensionScoreMapper.java` — MyBatis-Plus Mapper
- `server/.../dto/growth/InterviewRadarResponse.java` — 雷达响应 DTO

## 修改文件
- `db/schema.sql` + `server/db/schema.sql` — 新增 `interview_dimension_score` 表定义
- `server/.../service/GrowthService.java` — 新增 `getInterviewRadar()` 接口方法
- `server/.../service/impl/GrowthServiceImpl.java` — 实现雷达接口（回填、雷达数据、维度趋势、盲区分析）
- `server/.../service/InterviewService.java` — 报告生成后同步写入维度评分并清除雷达缓存
- `server/.../controller/GrowthController.java` — 新增 `GET /api/user/growth/interview-radar` 端点

## 关键设计
- **写入策略**：面试报告生成时同步写入维度表，雷达接口只读维度表，不在缓存读取路径做数据库写入。
- **实时写入**：面试报告生成时同步写入维度表，失败不影响主流程。
- **盲区规则**：近 3 次均分 < 60 → persistent_low；最新比上次下降 > 5 且 < 70 → declining_trend。
- **缓存一致性**：报告写入、清空历史、删除单条会话时同步清理 `user:interviewRadar` 与 `user:growthOverview`。
- **数据清理**：清空历史或删除单条面试时，按 sessionId 逻辑删除 `interview_dimension_score`，避免孤儿雷达数据。

## 审查修复（2026-05-21）
- 雷达查询改为按 `evaluation_report` 非空筛选，不再复用 `comprehensive_score IS NOT NULL` 的成长概览查询。
- 移除 `getInterviewRadar()` 中的回填写库逻辑，避免 `@Cacheable` 读路径产生副作用。
- `sessionCount` 调整为有评估报告的候选会话数；当暂无维度评分时返回候选数和空雷达数据，便于前端区分空状态。
- `InterviewDimensionScore` 的 JSON 字段补充 JPA `columnDefinition = "json"`。
- 盲区均分文案改用 `Math.round`，避免区域设置影响。
- 同步修复简历诊断总分缺失维度时的权重归一化问题。

## 第二轮审查修复（2026-05-21）
- `InterviewService` 的成长缓存驱逐改为通过 `CacheManager.getCache(...).evict(userId)` 按逻辑缓存名清理，不再依赖 Redis 物理 key 拼接格式。
- `GrowthServiceImplTest` 增补正向雷达数据、盲区提示和成长概览聚合场景，补齐核心读路径覆盖。
- `GrowthServiceImpl` 移除 `java.util.*` 通配符导入，改为显式导入。
- `TASK_54_INTERVIEW_DIMENSION_SCORE.sql`、`db/schema.sql`、`server/db/schema.sql` 为 `uk_session_dimension` 增加中文注释，说明逻辑删除后的幂等写入约束。
- `InterviewService` 将 fallback 报告中重复的 `Boolean.TRUE.equals(...)` 提取为局部变量，缩小重复判断。

## 验证
- `mvn clean compile` 通过
- `mvn.cmd test -Dtest=ResumeDiagnosisProcessorTest,InterviewServiceTest,GrowthServiceImplTest` 通过（32 个用例）
- `mvn.cmd test` 通过（459 个用例）
