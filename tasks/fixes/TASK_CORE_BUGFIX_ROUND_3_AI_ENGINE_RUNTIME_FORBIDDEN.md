# TASK_CORE_BUGFIX_ROUND_3_AI_ENGINE_RUNTIME_FORBIDDEN

## 1. 任务目标
- 修复 AI 引擎切换后运行时 403 Forbidden 问题
- 确保激活配置不仅"读到了"，而且"能正确发起请求"
- 补充运行时配置最小可用性校验，避免无效配置被激活后直接影响业务

## 2. 问题根因定位
- **根因 1**：`resolveRuntimeConfig()` 方法中，对 `runtimeApiKey` 缺少兜底处理，当数据库返回空值时会覆盖为 null，导致后续请求带空 key 引发 403
- **根因 2**：可能存在脱敏值误写入数据库问题，前端把列表返回的脱敏值（如 "sk-****abcd"）提交并覆盖真实 API Key
- **根因 3**：缺少运行时配置最小可用性校验，当所有兜底都失败时应抛出明确错误而非静默失败

## 3. 修复实现
- **修复 1**：`InterviewAiServiceImpl.resolveRuntimeConfig()` 补充 apiKey 兜底逻辑
  - 数据库 apiKey 为空时保持使用本地兜底，不覆盖为 null
  - 最后兜底尝试从环境变量获取
  - 所有兜底失败时抛出明确错误 `IllegalStateException`
- **修复 2**：`ResumeAiServiceImpl.resolveRuntimeConfig()` 对齐 interview 侧逻辑
- **修复 3**：`AdminController.updateAiEngineConfig()` 新增 `isMaskedApiKey()` 校验
  - 提交 apiKey 符合脱敏格式（如包含 "****" 且 <= 20 字符）时拒绝更新
  - 防止脱敏值覆盖真实值

## 4. 影响范围
- 后端服务：
  - `InterviewAiServiceImpl` 运行时配置读取
  - `ResumeAiServiceImpl` 运行时配置读取
  - `AdminController` AI 引擎配置更新
- 接口：
  - 面试 AI 调用链路（流式/非流式）
  - 简历 AI 调用链路

## 5. 验证要求
- 后端编译：`mvn.cmd -q -DskipTests compile`
- 运行时配置读取逻辑验证

## 6. 当前状态
- 本轮修复已完成代码改造与构建验证，等待人工验收。

## 7. 修复内容说明

### 7.1 InterviewAiServiceImpl 修复（关键）
```java
// 修复前：仅当数据库返回非空时才覆盖
if (dbApiKey != null) {
    runtimeApiKey = dbApiKey;
}
// 修复后：数据库 apiKey 为空时保持使用本地兜底
if (dbApiKey != null) {
    runtimeApiKey = dbApiKey;
} else {
    log.debug("数据库 apiKey 为空，使用本地兜底");
}

// 修复后：增加最后兜底和错误抛出
if (runtimeApiKey == null || runtimeApiKey.isBlank()) {
    log.warn("[INTERVIEW] runtimeApiKey 仍为空，尝试从环境变量兜底获取");
    runtimeApiKey = getApiKey();
}
if (runtimeApiKey == null || runtimeApiKey.isBlank()) {
    throw new IllegalStateException("面试 AI 密钥不可用：数据库和管理端均无有效配置。"
            + "请在管理端激活 AI 引擎配置，或设置环境变量 DOUBAO_API_KEY");
}
```

### 7.2 AdminController 修复
```java
// 修复前：无校验
if (request.getApiKey() != null) {
    config.setApiKey(normalizeRequiredValue(request.getApiKey(), "API Key 不能为空"));
}

// 修复后：新增脱敏值校验
if (request.getApiKey() != null) {
    if (isMaskedApiKey(request.getApiKey())) {
        throw new BusinessException("API Key 不能为脱敏格式，请输入完整的真实 API Key");
    }
    config.setApiKey(normalizeRequiredValue(request.getApiKey(), "API Key 不能为空"));
}

// 新增方法
private boolean isMaskedApiKey(String apiKey) {
    if (apiKey == null) return false;
    String trimmed = apiKey.trim();
    if (!trimmed.contains("****")) return false;
    if (trimmed.length() <= 20) return true;
    return false;
}
```