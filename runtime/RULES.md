# 后端开发最小规则（JDK 21）

1. 技术栈固定：
   - JDK 21
   - Spring Boot 3.x
   - MyBatis-Plus
   - MySQL 8
   - Redis
   - RabbitMQ
   - Spring AI

2. 严格 MVC 三层结构：
   - Controller 只接收请求与返回结果
   - Service 写业务逻辑
   - Mapper 只做数据库访问

3. 所有接口统一返回结构：
   - code
   - message
   - data

4. 所有表必须遵守：
   - 主键 id (BIGINT, 雪花算法)
   - create_time
   - update_time
   - is_deleted

5. 每次只实现当前 TASK 文件指定模块。

6. 不允许：
   - 伪代码
   - 省略实现
   - 修改无关模块
   - 重写整个项目

7. 所有代码必须：
   - 可编译
   - 可运行
   - 命名统一
   - 不重复输出未修改文件
   - 所有新生成代码必须添加必要中文注释：
     - 类注释
     - 关键字段注释
     - 关键方法注释
     - 复杂业务逻辑注释
   - 如果本轮新增、修改了 Web 接口，必须同步生成对应 Markdown 接口文档。
   - 接口文档必须至少包含：
     - 接口名称
     - 请求路径
     - 请求方法
     - 接口说明
     - 请求参数
     - 返回结果
     - 示例请求
     - 示例响应
     - 鉴权要求（如有）
   - 输出代码时，除了代码文件外，还必须输出本轮新增或更新的接口文档文件。
   - 新增或修改前端需要访问的 HTTP 接口时，必须同步按照 runtime/API_DOC_RULES.md 生成或更新 docs/api/ 下的 Markdown 接口文档。
   - 所有新增或修改代码必须按照 `runtime/LOG_RULES.md` 添加必要日志；关键业务链路、状态流转、异常处理必须有日志。
   - 所有 Controller 中 `Result.success(...)` / `Result.error(...)` 的调用，必须与当前 `Result` 工具类的真实方法签名保持一致；任务输出前必须完成一次统一返回体调用检查，避免因参数顺序或参数类型错误导致编译失败。
