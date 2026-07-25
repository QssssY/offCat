# RabbitMQ 队列 TTL 启动修复任务记录

## 当前任务所属模块

后端 RabbitMQ 简历诊断队列启动兼容性修复。

## 前端文件定位

本轮不涉及前端文件，不修改页面、不修改前端接口调用。

## 后端文件定位

- `server/src/main/java/com/airesume/server/config/RabbitMQConfig.java`
- `server/src/main/java/com/airesume/server/mq/ResumeDiagnosisProducer.java`
- `server/src/main/java/com/airesume/server/common/constants/ResumeDiagnosisConstants.java`
- `server/src/test/java/com/airesume/server/config/RabbitMQConfigTest.java`
- `server/src/test/java/com/airesume/server/mq/ResumeDiagnosisProducerTest.java`

## 本轮修改文件清单

- 移除 `queue.resume.diagnosis` 队列声明上的 `x-message-ttl` 参数，避免已存在队列没有 TTL 参数时 RabbitMQ 报 `PRECONDITION_FAILED`。
- 保留简历诊断队列已有死信交换机和死信 routing key 声明。
- 新增简历诊断消息单消息 TTL 常量，生产者发送消息时通过 `MessagePostProcessor` 设置 `expiration=3600000`。
- 新增 `ResumeDiagnosisProducerTest`，验证发送诊断消息时写入单消息 TTL。
- 更新 `RabbitMQConfigTest`，锁定既有队列声明不再包含不可变 TTL 参数。

## 前端实现方案

本轮不涉及前端实现。

## 后端实现方案

- RabbitMQ 队列参数在队列创建后不可变，给已有队列补 `x-message-ttl` 会触发 RabbitMQ 406 `PRECONDITION_FAILED`。
- 本轮改为消息级 TTL，不要求删除或重建线上既有队列。
- 新发送的简历诊断消息仍保持 1 小时存活时间；队列声明保持向后兼容，应用可直接重启。

## 数据存储方案

本轮不新增表、不修改字段、不新增索引，不涉及 `db/` 或 `server/db/` SQL 脚本变更。

## stage 更新说明

已更新根目录 `stage.md`，记录 RabbitMQ 队列 TTL 启动失败已修复，并修正此前 RabbitMQ 优化记录中的 TTL 表述。

## 编译结果

- `mvn compile` 通过。

## 构建结果

本轮为后端小修，已执行定向测试和编译验证；未重新执行打包。

## 当前功能验收说明

- RED 验证：`mvn test "-Dtest=RabbitMQConfigTest,ResumeDiagnosisProducerTest"` 在生产代码修改前失败，失败点为队列仍包含 `x-message-ttl` 且生产者未设置单消息 TTL。
- GREEN 验证：`mvn test "-Dtest=RabbitMQConfigTest,ResumeDiagnosisProducerTest"` 通过，结果为 4 个测试，0 失败，0 错误。
- `mvn compile` 通过。
- 修复后无需删除本地或线上已有 `queue.resume.diagnosis` 队列即可避免该启动报错。

## 停止，不继续下一功能

本轮仅修复 RabbitMQ 既有队列 TTL 参数不兼容导致的启动失败问题，未继续扩大优化范围。
