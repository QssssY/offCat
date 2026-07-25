# 公开版本日志匿名访问修复（后端）

## 当前任务所属模块
- 后端安全配置与公开版本日志接口。

## 后端文件定位
- `server/src/main/java/com/airesume/server/config/SecurityConfig.java`
- `server/src/test/java/com/airesume/server/config/SecurityConfigTest.java`
- `server/src/main/java/com/airesume/server/controller/VersionLogController.java`

## 本轮修改文件清单
- `SecurityConfig.java`：放行 `GET /api/version-logs` 与 `GET /api/version-logs/latest`，保留写操作与管理端接口鉴权。
- `SecurityConfigTest.java`：新增公开版本日志接口匿名放行回归测试。

## 后端实现方案
- 将首页依赖的公开版本日志接口纳入匿名 GET 白名单，避免首页首屏请求被安全层拦截成 401。
- 继续保持版本日志写接口、管理端版本日志接口和其它受保护 API 的原有鉴权规则。

## 数据存储方案
- 本轮不涉及数据库结构、迁移脚本或数据表变更。

## stage 更新说明
- 已在 `tasks/stage.md` 追加本轮后端修复状态，记录根因、修改范围、测试与线上验证结果。

## 编译结果
- `mvn.cmd -q -Dtest=SecurityConfigTest test` 通过。
- `mvn.cmd -q -DskipTests compile` 通过。

## 构建结果
- 服务器端已重新打包并替换 `offercat` 后端 jar，重启服务成功。

## 当前功能验收说明
- 匿名访问 `https://kelin.cyou/api/version-logs/latest?limit=5` 返回 `200`。
- 匿名访问 `https://kelin.cyou/` 返回 `200`，首页不再因公开版本日志接口 401 而误弹“登录已过期”。
- 原有 `http://103.231.57.13/` 的 80 端口项目未受影响。

## 停止说明
- 本轮只修复公开版本日志接口匿名放行问题，不继续扩展首页其它公开接口、鉴权文案、前端请求层或管理端权限体系。