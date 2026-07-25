# TASK 56：成长配置接入用户端成长中心（后端）

## 背景

管理端已有 `sys_growth_config` 表、管理接口和“成长配置”页面，但用户端“个人成长中心”尚未消费这些配置，容易造成后台配置不生效的误解。

## 本轮范围

- `GrowthOverviewResponse` 新增 `growthConfig` 响应字段。
- `GrowthServiceImpl` 注入 `SysGrowthConfigService`，读取：
  - `encouragement` 分组：作为用户端激励文案列表。
  - `milestone` 分组：作为用户端里程碑列表。
- `AdminGrowthConfigController` 在新增、更新、删除、批量删除配置后清理 `user:growthOverview` 缓存，避免用户端读取旧配置。
- 补充成长概览和 Redis 序列化回归测试。

## 不做范围

- 不新增数据库表或字段。
- 不改成就规则计算，不做动态达成状态。
- 不改变个人成长中心原有统计、趋势、雷达数据计算逻辑。

## 验证

- `mvn.cmd -q "-Dtest=GrowthServiceImplTest,RedisSerializationTest" test` 通过。
- `mvn.cmd -q "-Dtest=GrowthServiceImplTest,RedisSerializationTest,AdminGrowthConfigControllerTest" test` 通过。
