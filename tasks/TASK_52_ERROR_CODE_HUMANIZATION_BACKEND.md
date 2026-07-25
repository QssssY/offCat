# TASK-52：错误码人性化 — 后端

## 所属模块
公共模块 - 错误码体系

## 功能目标
为系统构建结构化的业务错误码体系，将 4 个指定模块内的英文 BusinessException 消息替换为中文 ResultCode 枚举，并在同文件内将中文消息同步迁移到 ResultCode，使前端能按错误码程序化展示用户友好提示。

## 后端文件定位

### 修改文件
| 文件 | 修改内容 |
|------|---------|
| `server/src/main/java/com/airesume/server/common/result/ResultCode.java` | 新增 26 个业务域错误码（1xxx-6xxx） |
| `server/src/main/java/com/airesume/server/common/exception/BusinessException.java` | 新增 `BusinessException(ResultCode, String dynamicMessage)` 构造函数 |
| `server/src/main/java/com/airesume/server/service/impl/ResumeDiagnosisTaskServiceImpl.java` | 16 处英文/中文 BusinessException 替换为 ResultCode |
| `server/src/main/java/com/airesume/server/service/impl/UserQuotaServiceImpl.java` | 5 处英文/中文 BusinessException 替换为 ResultCode |
| `server/src/main/java/com/airesume/server/service/impl/MembershipServiceImpl.java` | 4 处英文 BusinessException 替换为 ResultCode |
| `server/src/main/java/com/airesume/server/service/impl/AiEngineConnectivityTestServiceImpl.java` | 4 处英文 BusinessException 替换为 ResultCode |

### 验证但未修改
| 文件 | 说明 |
|------|------|
| `server/src/main/java/com/airesume/server/common/exception/GlobalExceptionHandler.java` | 已正确返回 `e.getCode()` + `e.getMessage()`，无需修改 |
| `server/src/main/java/com/airesume/server/common/result/Result.java` | 已有 `error(ResultCode)` 和 `error(Integer, String)` 方法，无需修改 |

### 新增测试文件
| 文件 | 测试数量 | 覆盖内容 |
|------|---------|---------|
| `ResultCodeTest.java` | 8 | 各分段 code 范围、消息非空、中文消息、现有值未改变 |
| `BusinessExceptionTest.java` | 7 | 4 个构造函数行为、动态消息覆盖、Result 集成 |
| `GlobalExceptionHandlerTest.java` | 4 | ResultCode 传播、动态消息、通用异常不泄露细节 |

### 修改测试文件
| 文件 | 修改内容 |
|------|---------|
| `ResumeDiagnosisTaskServiceImplTest.java` | 更新 `sanitizeOriginalFilenameShouldRejectNull` 断言，从消息内容匹配改为 ResultCode 匹配 |

## 后端实现方案

### 1. ResultCode 枚举扩展

新增 6 个分段共 26 个业务错误码：

- **2xxx 简历模块**（10 个）：文件为空、格式不支持、文件过大、解析失败、额度用完、路径非法、任务不存在、无权访问、保存失败、清理失败
- **3xxx 面试模块**（5 个）：额度用完、会话不存在、无权访问、会话已结束、AI 超时
- **4xxx AI 服务**（4 个）：不可用、返回为空、解析失败、配额不足
- **5xxx 会员**（4 个）：套餐不存在、账号禁用、用户不存在、未登录
- **6xxx 管理端**（6 个）：配置不存在、编码重复、批量超限、Prompt 不存在、AI 引擎不存在、岗位不存在

### 2. BusinessException 动态消息构造函数

新增 `BusinessException(ResultCode resultCode, String dynamicMessage)`：错误码取自枚举，消息使用传入的动态中文（保留"文件大小不能超过 10MB"等动态信息）。

### 3. 服务层迁移

按文件逐一替换，英文消息直接替换为 `ResultCode` 枚举，含动态参数的使用新构造函数。ResumeDiagnosisTaskServiceImpl 中同语义的中文消息也同步迁移。

## 数据存储方案
不新增表、不修改字段、不新增迁移脚本。

## 编译结果
- 后端编译：`mvn.cmd -q -DskipTests compile` 通过
- 后端新增测试：`mvn.cmd test "-Dtest=ResultCodeTest,BusinessExceptionTest,GlobalExceptionHandlerTest"` 通过，19 个测试
- 后端受影响测试：`mvn.cmd test "-Dtest=ResumeDiagnosisTaskServiceImplTest,..."` 通过
- 后端完整测试：待验证

## 当前功能验收说明
- [x] ResultCode 至少覆盖通用、简历、面试、AI、会员、管理端的主要业务错误
- [x] BusinessException 支持通过 ResultCode 构造，并能保留动态中文 message
- [x] GlobalExceptionHandler 对业务异常返回稳定 code 和 message
- [x] 4 个指定模块内的英文用户可见异常被替换为中文业务错误
- [x] ResumeDiagnosisTaskServiceImpl 中同语义中文消息同步迁移到 ResultCode
- [x] 后端单元测试覆盖正常路径、异常路径和边界条件

## 停止
不继续实施诊断进度条、失败重试、DOCX 导出、成长中心雷达或任务式新手引导。
