# 任务：管理端系统 STT 配置新增「获取模型」

## 背景

管理端「系统 STT 配置」卡片目前只有「保存配置」「测试连通性」两个按钮，缺少像
TTS 那样的「获取模型」能力，管理员只能手动输入模型名（如
`FunAudioLLM/SenseVoiceSmall`）。需要补上「获取模型」按钮，调用 OpenAI 兼容的
`GET /models` 拉取可选模型列表，供下拉选择。

STT 比 TTS 精简：只发现模型，**没有音色、没有合成端点探测**。实现镜像
`TtsDiscoveryServiceImpl.fetchTtsModels` 那一段即可。

## 顺带修复

- `SysSttConfigService.java` 第 6-7 行重复 import 了 `ResolvedSttConfig`，删掉一行。

## 后端改动

### 1. 新增 DTO `SttModelDiscoveryResponse`（dto/user/）
镜像 `UserTtsDiscoveryResponse` 精简版：
```java
@Data @Builder @NoArgsConstructor @AllArgsConstructor
public class SttModelDiscoveryResponse {
    private Boolean success;
    private String message;
    private List<SttModelOption> models;   // 复用 or 新增 SttModelOption{id,name}
    private String errorMessage;
}
```
模型项复用一个简单结构 `SttModelOption{id,name}`（新增，和 TtsModelOption 同形，
避免跨域耦合 TTS DTO）。

### 2. `SysSttConfigService` 接口新增
```java
SttModelDiscoveryResponse discoverModels(AdminSttConfigRequest request);
```

### 3. `SysSttConfigServiceImpl` 实现 `discoverModels`
- 校验 baseUrl（复用已有 `validateBaseUrl`）
- 解析明文 Key（复用已有 `normalizePlainApiKey`，支持脱敏值复用已存 Key）
- 调 `GET {baseUrl}/models`，`Authorization: Bearer {key}`，超时 10s（新增常量
  `DISCOVERY_TIMEOUT_MS = 10000`、`MODELS_ENDPOINT = "/models"`、
  `MAX_MODEL_COUNT = 500`）
- 解析 `data[].id`，全部返回（STT 模型无统一命名关键字，不做过滤）
- 认证失败（401/403）→ `success=false` 提示「API Key 无效或已过期」
- 其它异常 → `success=false` 提示「模型列表获取失败」
- 需要 `ObjectMapper` 解析 JSON：impl 目前构造参数是
  `(SysSttConfigMapper, AiCredentialCrypto, RestClient.Builder)` 3 个，**新增
  ObjectMapper 会改构造签名，影响现有 3 个测试的 `new SysSttConfigServiceImpl(...)`**。
  → 同步更新 `SysSttConfigServiceImplTest` 的构造调用。

### 4. `AdminSttConfigController` 新增端点
```java
@PostMapping("/discover-models")
public Result<SttModelDiscoveryResponse> discoverModels(@Valid @RequestBody AdminSttConfigRequest request)
```

## 前端改动

### 5. `api/admin/sttConfig.js` 新增 `discoverAdminSttModels(data)`
POST `/api/admin/stt-config/discover-models`

### 6. `AdminAiEngineView.vue`
- 模板：「模型」表单项改为 `el-select`（`filterable allow-create`，有发现结果时
  用下拉，否则回落 `el-input`），镜像 TTS 模型项写法
- 新增「获取模型」按钮到 `.system-stt-actions`，`:loading="systemSttDiscovering"`
- 新增 state：`systemSttDiscovering`、`systemSttModelOptions`（computed）、
  `systemSttDiscoveryResult`
- 新增 handler `handleSystemSttDiscoverModels`：校验 baseUrl、首次要 Key，调
  API，成功后把返回模型灌入下拉、model 为空时自动选第一个
- import 加 `discoverAdminSttModels`

## 测试

### 后端
- `SysSttConfigServiceImplTest` 新增：`discoverModels` 成功解析模型、认证失败返回
  `success=false`、baseUrl 非法返回失败；修构造调用为 4 参
- （可选）`AdminSttConfigControllerTest` 若存在则加端点用例

### 前端
- `admin.sttConfig.test.js` 新增 `discoverAdminSttModels` 走对端点用例
- `AdminAiEngineView.test.js` 的 sttConfig mock 补 `discoverAdminSttModels`，
  新增用例：点「获取模型」后下拉出现模型、model 自动回填

## 验证
- 后端 `mvn clean test`（至少 STT 相关 + 新增用例通过，不破坏现有）
- 前端 `npx vitest run src/__tests__/views/AdminAiEngineView.test.js src/__tests__/api/admin.sttConfig.test.js`
- 前端 `npm run build`

## 提交
STT 相关文件单独提交，commit type `feat`，推送到 `origin/master`（与前几次一致）。
含明文口令的 6 个未跟踪文件继续不提交。
