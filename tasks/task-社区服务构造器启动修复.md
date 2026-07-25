# 社区服务构造器启动修复

## 当前任务所属模块
- 后端社区模块。
- 修复 Spring 启动期创建 `communityService` Bean 时，由多构造器选择不明确导致的启动失败。

## 前端文件定位
- 本轮不涉及前端文件。

## 后端文件定位
- `server/src/main/java/com/airesume/server/service/CommunityService.java`
- `server/src/test/java/com/airesume/server/service/CommunityServiceConstructorInjectionTest.java`

## 本轮修改文件清单
- `CommunityService.java`：移除 Lombok `@RequiredArgsConstructor`，改为显式 8 参数生产注入构造器，并用 `@Autowired` 标记 Spring 注入入口；保留 7 参数单元测试兼容构造器。
- `CommunityServiceConstructorInjectionTest.java`：新增构造器注入回归测试，锁定生产构造器必须包含 `CommunityTextModerationService` 完整依赖。
- `tasks/stage.md`：同步本轮启动修复和验证结果。

## 前端实现方案
- 本轮不涉及前端实现。

## 后端实现方案
- 根因：`CommunityService` 同时存在 7 参数测试兼容构造器和 Lombok 生成的 8 参数构造器，Spring 面对多个构造器时没有显式注入入口，启动期尝试回退默认构造器并失败。
- 修复：显式声明 8 参数构造器并标记 `@Autowired`，保证生产环境注入完整依赖；7 参数构造器仅用于已有单元测试兼容，内部委托到完整构造器并创建默认审核服务。
- 影响范围：仅调整构造器注入方式，不改变社区发帖、评论、点赞、收藏、审核等业务行为。

## 数据存储方案
- 本轮不涉及数据库结构、迁移脚本或数据修复。

## stage 更新说明
- 根目录 `tasks/stage.md` 已新增“社区服务构造器启动修复”记录。

## 编译结果
- `mvn.cmd -q -DskipTests compile` 通过。

## 构建与测试结果
- `mvn.cmd -q "-Dtest=CommunityServiceConstructorInjectionTest" test` 通过。
- `mvn.cmd -q "-Dtest=CommunityService*Test,CommunityTextModerationServiceTest" test` 通过。
- `mvn.cmd -q "-Dtest=ServerApplicationTests" test` 通过，Spring 上下文已能启动到 `Started ServerApplicationTests`。

## 当前功能验收说明
- 启动期 `communityService` Bean 可通过显式 8 参数构造器完成依赖注入。
- 既有社区服务单元测试仍可使用 7 参数兼容构造器。
- 本轮未扩展任何社区新功能。

## 停止说明
- 本轮仅修复社区服务构造器导致的后端启动失败，不继续推进下一项功能。
