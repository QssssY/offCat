# 社区图片上传公网占位 URL 修复

## 当前任务所属模块
- 后端社区模块。
- 关联接口：`POST /api/community/images/upload`。
- 关联测试数据：`db/community_seed_existing_users.sql`、`db/community_seed_root_user.sql`。

## 后端文件定位
- `server/src/main/java/com/airesume/server/service/CommunityService.java`
- `server/src/test/java/com/airesume/server/service/CommunityServiceValidationTest.java`

## 本轮修改文件清单
- `server/src/main/java/com/airesume/server/service/CommunityService.java`
- `server/src/main/java/com/airesume/server/common/exception/GlobalExceptionHandler.java`
- `server/src/test/java/com/airesume/server/service/CommunityServiceValidationTest.java`
- `server/src/test/java/com/airesume/server/common/exception/GlobalExceptionHandlerTest.java`
- `db/community_seed_existing_users.sql`
- `db/community_seed_root_user.sql`
- `db/community_cleanup_local_image_urls.sql`

## 后端实现方案
- 保留社区图片上传的空文件、大小、扩展名和魔术字节校验。
- 在阿里云 OCS/OSS 密钥暂未配置前，上传校验通过后不再写入本机 `uploads/community` 静态目录。
- 新增 `app.upload.community-placeholder-url` 配置占位，默认返回公网图片 `https://ts3.tc.mm.bing.net/th/id/OIP-C.TmvkuikpStxy5wKWiziR1AHaE7?rs=1&pid=ImgDetMain&o=7&rm=3`。
- 这样帖子和评论保存的是可跨用户访问的完整 URL，不再触发 `NoResourceFoundException: No static resource root-test-data.png`。
- 补充 `NoResourceFoundException` 专用异常处理，缺失静态资源返回 404，不再被通用异常处理成 500。

## 数据存储方案
- 不新增表。
- 不修改字段。
- 不新增迁移脚本。
- 两份社区测试数据 SQL 改为通过 `@community_test_image_url` 写入公网占位图，避免测试数据继续写入 `/uploads/community/*.png` 本地路径。
- 新增 `db/community_cleanup_local_image_urls.sql`，用于清理已经执行过旧 seed 后残留在数据库中的 `/uploads/community/*.png` JSON 图片 URL。

## stage 更新说明
- 已在根目录 `runtime/STATE.md` 追加本轮社区图片上传占位 URL 修复状态。

## 编译与测试结果
- RED 验证：`mvn.cmd test "-Dtest=CommunityServiceValidationTest"` 先失败，确认旧实现仍返回 `/uploads/community/*.png`。
- 后端定向测试：`mvn.cmd test "-Dtest=CommunityServiceValidationTest"` 通过，16 个用例通过。
- 后端回归测试：`mvn.cmd test "-Dtest=GlobalExceptionHandlerTest,CommunityServiceValidationTest,SecurityConfigTest"` 通过，25 个用例通过。

## 当前功能验收说明
- 上传图片接口成功响应中的 `url` 现在是公网占位图 URL。
- 新发帖子、评论上传图片后保存完整 URL，其他用户访问时不依赖本机静态目录。
- 旧库里已经存在的 `/uploads/community/*.png` 数据可执行 `db/community_cleanup_local_image_urls.sql` 清理；日志中的 `99400011`、`99400014` 等 root seed 记录说明当前运行数据库仍有旧图片路径残留。

## 停止说明
- 本轮只做对象存储凭证缺失期间的公网占位 URL 修复，不接入真实阿里云 OCS/OSS SDK，不新增对象存储配置页面，不调整数据库结构。
