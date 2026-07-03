# 简历诊断与简历处理接口

本文档对应 `ResumeDiagnosisController`、`ResumePdfController` 与 `TemplateController` 的当前实现。

## 通用说明

- 主要前缀：`/api/resume`
- 模板前缀：`/api/template`
- 鉴权：除无特殊说明外均需 `Authorization: Bearer <token>`
- 简历上传当前为 `multipart/form-data`，不是旧版 `fileUrl` JSON 占位模式。
- 诊断任务支持智能路由：低负载时直接异步处理，高负载时进入 RabbitMQ。

## 接口列表

| 方法 | 路径 | 说明 |
|------|------|------|
| POST | `/api/resume/upload` | 上传简历文件并创建诊断任务 |
| GET | `/api/resume/task/{taskId}` | 查询任务详情 |
| GET | `/api/resume/task/{taskId}/status` | 查询任务阶段状态 |
| GET | `/api/resume/history` | 查询诊断历史 |
| DELETE | `/api/resume/history` | 清空诊断历史 |
| DELETE | `/api/resume/history/{taskId}` | 删除单条诊断历史 |
| POST | `/api/resume/task/{taskId}/retry` | 重试失败任务 |
| POST | `/api/resume/job-match/analyze` | 简历与 JD 匹配分析 |
| POST | `/api/resume/polish/analyze` | AI 简历润色 |
| PUT | `/api/resume/polish-records/{polishRecordId}/document` | 更新润色后的结构化文档 |
| POST | `/api/resume/export-pdf` | 导出 PDF |
| GET | `/api/resume/download-pdf/{fileId}` | 下载导出的 PDF |
| POST | `/api/template/use` | 使用模板并扣减模板额度 |

## 上传简历

```http
POST /api/resume/upload
Authorization: Bearer <token>
Content-Type: multipart/form-data
```

表单字段以 `ResumeUploadRequest` 和 Controller 入参为准，核心字段为上传文件。服务端会校验文件类型与用户额度，创建 `resume_diagnosis_task` 后返回任务信息。

支持的业务能力：

- PDF/DOC/DOCX 简历上传。
- PDFBox 文本提取。
- 文本不足时可走多模态或 OCR 解析链路。
- 诊断阶段进度记录。
- AI 诊断结果结构化存储。
- 失败任务自动退还配额，可手动重试。

## 查询任务

```http
GET /api/resume/task/{taskId}
Authorization: Bearer <token>
```

任务详情包含任务状态、阶段进度、文件信息、诊断结果、错误信息、创建和更新时间等字段。只能查询当前用户自己的任务。

```http
GET /api/resume/task/{taskId}/status
Authorization: Bearer <token>
```

状态接口用于前端轮询任务进度，重点关注：

- 当前任务状态。
- 当前处理阶段。
- 是否完成。
- 是否失败及失败原因。

## 历史与清理

```http
GET /api/resume/history
DELETE /api/resume/history
DELETE /api/resume/history/{taskId}
```

历史接口按当前用户隔离。删除接口只影响当前用户可访问的历史记录，不允许越权操作其他用户任务。

## 失败重试

```http
POST /api/resume/task/{taskId}/retry
Authorization: Bearer <token>
```

仅失败任务可重试。重试会重新进入诊断处理链路，并更新任务状态与阶段信息。

## JD 匹配

```http
POST /api/resume/job-match/analyze
Authorization: Bearer <token>
Content-Type: application/json
```

请求体由 `ResumeJobMatchAnalyzeRequest` 约束，用于提交简历任务、目标岗位或 JD 文本，生成岗位匹配度、关键词命中、缺口与优化建议。

## AI 简历润色

```http
POST /api/resume/polish/analyze
Authorization: Bearer <token>
Content-Type: application/json
```

请求体由 `ResumePolishAnalyzeRequest` 约束。返回内容包含润色文本、结构化简历文档和润色记录 ID。

```http
PUT /api/resume/polish-records/{polishRecordId}/document
Authorization: Bearer <token>
Content-Type: application/json
```

用于前端模板编辑器保存润色后的结构化文档。

## PDF 导出

```http
POST /api/resume/export-pdf
Authorization: Bearer <token>
Content-Type: application/json
```

PDF 导出使用 Headless Chrome，依赖 `app.pdf.chrome-path` 配置。服务端会对 HTML 内容做安全处理后生成文件。

```http
GET /api/resume/download-pdf/{fileId}
Authorization: Bearer <token>
```

下载指定导出结果。

## 模板使用

```http
POST /api/template/use
Authorization: Bearer <token>
Content-Type: application/json
```

用于记录模板使用并扣减模板相关额度。前端模板库和模板编辑器依赖该接口完成模板使用链路。
