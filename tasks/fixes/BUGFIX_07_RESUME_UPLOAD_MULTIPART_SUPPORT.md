# BUGFIX 07 - 简历上传接口改为支持 multipart/form-data

## 1. 修复目标

修复当前简历上传接口在接收前端 PDF 文件上传请求时返回 `HttpMediaTypeNotSupportedException` 的问题，使后端 `/api/resume/upload` 接口能够正确接收 `multipart/form-data` 请求，并支持通过 `MultipartFile` 处理真实文件上传。

---

## 2. 问题背景

当前前端上传页已经改为使用真实文件上传，请求方式为：

- `POST /api/resume/upload`
- Content-Type: `multipart/form-data`

在上传 PDF 文件时，后端抛出如下异常：

- `HttpMediaTypeNotSupportedException`
- `Content-Type 'multipart/form-data; ...' is not supported`

这表明：

- 前端已正确按文件上传方式提交
- 后端接口当前并未按文件上传协议定义
- Spring 当前仍试图按 JSON / `@RequestBody` 方式处理请求体
- 导致媒体类型不匹配并直接返回 500

因此，当前问题不是前端页面问题，而是后端上传接口定义与前端请求协议不一致。

---

## 3. 本修复范围

本修复应完成以下内容：

1. 检查 `/api/resume/upload` 当前接口定义
2. 将上传接口改为支持 `multipart/form-data`
3. 使用 `MultipartFile` 正确接收前端上传的 PDF 文件
4. 如当前上传接口还依赖其他字段，按 multipart 方式一并处理
5. 保持现有任务创建与异步处理链路继续可用
6. 返回前端可继续承接的成功响应结构（包括任务标识，如已有）
7. 更新必要接口文档

---

## 4. 不在本次修复范围内的内容

本次修复不包含：

- 重构整个简历诊断业务链
- 重写前端上传页面
- 修改结果页逻辑
- 替换文件存储方案
- 修改无关 Controller 或无关模块

---

## 5. 根因说明

当前根因已明确：

- 前端发送的是 `multipart/form-data`
- 后端接口未按 `MultipartFile` 接收
- 接口参数设计仍偏向 `@RequestBody` JSON 请求体
- Spring 因无法用消息转换器处理该媒体类型而抛出 `HttpMediaTypeNotSupportedException`

---

## 6. 修复要求

### 6.1 接口签名修复

必须检查并修正 `/api/resume/upload` Controller 方法签名，确保其支持：

- `MultipartFile file`
- 或当前真实字段名对应的文件参数

如当前接口还需其他参数，应按 multipart 方式配合 `@RequestParam` 或其他合适方式接收。

### 6.2 请求协议声明

应明确接口支持：

- `multipart/form-data`

必要时在映射上增加：

- `consumes = MediaType.MULTIPART_FORM_DATA_VALUE`

### 6.3 参数名对齐

必须确保：

- 前端 `FormData.append()` 的字段名
- 与后端接口参数名

完全一致。

### 6.4 业务链保持可用

修复后应确保：

- 文件可成功接收
- 文件后续处理逻辑可继续执行
- 任务可正常创建
- 成功响应结构保持稳定
- 前端可继续拿到任务标识并跳转结果页

---

## 7. 排查重点

重点检查以下内容：

1. `/api/resume/upload` 当前 Controller 方法签名
2. 是否存在 `@RequestBody` 导致按 JSON 解析
3. 前端上传字段名是什么
4. 后端 `MultipartFile` 参数名是否一致
5. 后续 service 层是否接受的是文件对象、文件路径还是其他形式
6. 成功响应中是否返回任务标识供前端继续使用

---

## 8. 验收标准

满足以下条件可视为修复完成：

1. 上传 PDF 时后端不再抛 `HttpMediaTypeNotSupportedException`
2. 上传接口可正常接收 `multipart/form-data`
3. Controller 方法签名与前端请求协议一致
4. 文件上传后可继续进入任务创建链路
5. 成功响应可供前端继续承接结果页跳转
6. 无新增明显副作用

---

## 9. 验证要求

修复后至少验证以下场景：

1. 前端上传一个符合要求的 PDF 文件
2. 后端接口正常返回成功结果
3. 后端日志中不再出现 `Content-Type ... is not supported`
4. 如上传成功会创建任务，确认任务创建成功
5. 前端可继续进入结果页承接流程

---

## 10. 输出要求

完成本修复后，应输出：

1. 根因分析结论
2. 需要修改的后端文件清单
3. 逐文件完整修改内容
4. 前端字段名与后端参数名对齐说明
5. 修复后的上传接口签名说明
6. 修复后的验证步骤与结果
7. 接口文档更新内容

---

## 11. 备注

本修复属于简历诊断真实上传链路中的协议对齐问题修复。

只有完成本修复，前端真实 PDF 上传、任务创建、结果页跳转的闭环才有可能真正跑通。