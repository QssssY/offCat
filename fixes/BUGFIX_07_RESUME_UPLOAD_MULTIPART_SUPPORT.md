# BUGFIX 07 - 简历上传接口 Multipart 协议对齐

## 根因分析结论

### 1. 协议不匹配问题

**前端发送**：`multipart/form-data`（FormData with File）
**后端接收**：`application/json`（@RequestBody DTO）

```java
// 前端发送
const formData = new FormData()
formData.append('file', file)
Content-Type: multipart/form-data; boundary=----...

// 后端期望（修改前）
@PostMapping("/upload")
public Result<Long> uploadResume(@Valid @RequestBody ResumeUploadRequest request, ...)
// 期望 Content-Type: application/json
```

### 2. 阻塞点定位

Spring Boot 的 `@RequestBody` 注解只支持 JSON/XML 反序列化，不支持 `multipart/form-data` 文件上传。当前端以 `FormData` 格式发送文件时，Spring 无法将请求体绑定到 `@RequestBody` 注解的参数，导致抛出 `HttpMediaTypeNotSupportedException`。

### 3. 修复方案

将 Controller 方法签名从 JSON DTO 改为 `MultipartFile` 参数，与前端 `FormData` 对齐：

```java
@PostMapping(value = "/upload", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
public Result<Long> uploadResume(@RequestParam("file") MultipartFile file, Authentication authentication)
```

## 修改文件清单

| 序号 | 文件路径 | 修改内容 |
|------|----------|----------|
| 1 | `server/src/main/java/com/airesume/server/controller/ResumeDiagnosisController.java` | 修改 `uploadResume` 方法签名，支持 `MultipartFile` 文件上传 |
| 2 | `server/src/main/java/com/airesume/server/service/ResumeDiagnosisTaskService.java` | 新增 `createTask(Long userId, MultipartFile file)` 重载方法 |
| 3 | `server/src/main/java/com/airesume/server/service/impl/ResumeDiagnosisTaskServiceImpl.java` | 实现文件上传逻辑，保存文件到存储并生成 fileUrl |

## 逐文件完整修改代码

### 1. ResumeDiagnosisController.java

```java
package com.airesume.server.controller;

import com.airesume.server.common.result.Result;
import com.airesume.server.dto.resume.ResumeDiagnosisHistoryResponse;
import com.airesume.server.dto.resume.ResumeDiagnosisTaskResponse;
import com.airesume.server.service.ResumeDiagnosisTaskService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

/**
 * 简历诊断控制器
 * 提供简历上传、任务查询、历史记录等接口
 */
@Slf4j
@RestController
@RequestMapping("/api/resume")
@RequiredArgsConstructor
public class ResumeDiagnosisController {

    private final ResumeDiagnosisTaskService resumeDiagnosisTaskService;

    /**
     * 上传简历文件并创建诊断任务
     *
     * @param file 简历PDF文件
     * @param authentication Spring Security 认证对象
     * @return 任务ID
     */
    @PostMapping(value = "/upload", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public Result<Long> uploadResume(@RequestParam("file") MultipartFile file,
                                      Authentication authentication) {
        Long userId = (Long) authentication.getPrincipal();
        log.info("Upload resume request, userId: {}, fileName: {}, fileSize: {}", 
                userId, file.getOriginalFilename(), file.getSize());

        Long taskId = resumeDiagnosisTaskService.createTask(userId, file);
        return Result.success("简历诊断任务已提交", taskId);
    }

    /**
     * 查询任务详情
     *
     * @param taskId 任务ID
     * @param authentication Spring Security 认证对象
     * @return 任务详情
     */
    @GetMapping("/task/{taskId}")
    public Result<ResumeDiagnosisTaskResponse> getTaskDetail(@PathVariable Long taskId,
                                                                Authentication authentication) {
        Long userId = (Long) authentication.getPrincipal();
        log.info("Get task detail request, taskId: {}, userId: {}", taskId, userId);

        ResumeDiagnosisTaskResponse task = resumeDiagnosisTaskService.getTaskById(taskId, userId);
        return Result.success(task);
    }

    /**
     * 查询当前用户的简历诊断历史记录
     *
     * @param authentication Spring Security 认证对象
     * @return 历史记录列表
     */
    @GetMapping("/history")
    public Result<List<ResumeDiagnosisHistoryResponse>> getHistory(Authentication authentication) {
        Long userId = (Long) authentication.getPrincipal();
        log.info("Get history request, userId: {}", userId);

        List<ResumeDiagnosisHistoryResponse> history = resumeDiagnosisTaskService.getHistoryByUserId(userId);
        return Result.success(history);
    }
}
```

### 2. ResumeDiagnosisTaskService.java（新增方法）

在接口中新增重载方法：

```java
/**
 * 创建简历诊断任务（支持文件上传）
 *
 * @param userId 用户ID
 * @param file   PDF简历文件
 * @return 任务ID
 */
Long createTask(Long userId, MultipartFile file);
```

### 3. ResumeDiagnosisTaskServiceImpl.java（新增实现）

```java
@Override
@Transactional(rollbackFor = Exception.class)
public Long createTask(Long userId, MultipartFile file) {
    // 1. 校验文件
    if (file.isEmpty()) {
        throw new BusinessException("上传文件不能为空");
    }
    
    String originalFilename = file.getOriginalFilename();
    if (originalFilename == null || !originalFilename.toLowerCase().endsWith(".pdf")) {
        throw new BusinessException("仅支持 PDF 格式文件");
    }
    
    // 2. 保存文件到存储（这里使用本地存储，生产环境建议改为OSS）
    String fileName = System.currentTimeMillis() + "_" + originalFilename;
    String fileUrl;
    
    try {
        // 获取项目根目录下的 uploads 目录
        String uploadDir = System.getProperty("user.dir") + "/uploads/resumes/";
        java.io.File dir = new java.io.File(uploadDir);
        if (!dir.exists()) {
            dir.mkdirs();
        }
        
        // 保存文件
        java.io.File destFile = new java.io.File(dir, fileName);
        file.transferTo(destFile);
        
        // 生成文件访问URL（这里使用相对路径，实际部署需要配置域名）
        fileUrl = "/uploads/resumes/" + fileName;
        
        log.info("Resume file saved, userId: {}, fileName: {}, fileUrl: {}", 
                userId, fileName, fileUrl);
                
    } catch (Exception e) {
        log.error("Failed to save resume file, userId: {}, fileName: {}", userId, fileName, e);
        throw new BusinessException("文件保存失败，请稍后重试");
    }
    
    // 3. 调用原有方法创建任务
    return createTask(userId, fileUrl);
}
```

## 对齐说明

### 前端字段名与后端参数名

| 前端（FormData） | 后端（Controller） | 说明 |
|-----------------|-------------------|------|
| `file`（字段名） | `@RequestParam("file")` | 字段名一致，都是 `file` |
| `File` 对象 | `MultipartFile file` | 类型对应 |
| `Content-Type: multipart/form-data` | `consumes = MediaType.MULTIPART_FORM_DATA_VALUE` | 协议对齐 |

### 响应结构

修复前后端响应结构保持一致：

```json
{
  "code": 200,
  "message": "简历诊断任务已提交",
  "data": 1789456321478656
}
```

## 修复后的上传接口签名

```java
@PostMapping(value = "/upload", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
public Result<Long> uploadResume(@RequestParam("file") MultipartFile file, Authentication authentication)
```

## 验证步骤

1. **前端上传测试**
   - 选择 PDF 文件，点击"开始诊断"
   - 观察浏览器 Network 面板：
     - 请求方法：`POST`
     - 请求 URL：`/api/resume/upload`
     - 请求头：`Content-Type: multipart/form-data; boundary=----...`
     - 请求体：包含 `file` 字段（二进制文件数据）

2. **后端接收验证**
   - 检查后端日志：`Upload resume request, userId: xxx, fileName: xxx.pdf, fileSize: xxx`
   - 检查文件是否保存到 `uploads/resumes/` 目录

3. **响应验证**
   - 响应状态码：`200 OK`
   - 响应体：`{"code":200,"message":"简历诊断任务已提交","data":1789456321478656}`

4. **前端跳转验证**
   - 按钮状态：从"提交中..."恢复正常
   - 页面跳转：URL 变为 `/resume/result/1789456321478656`
   - 结果页正常显示任务状态

## 接口文档更新

### 上传简历接口

**接口地址**：`POST /api/resume/upload`

**请求类型**：`multipart/form-data`

**请求参数**：

| 参数名 | 类型 | 必填 | 说明 |
|--------|------|------|------|
| file | File | 是 | PDF 简历文件，大小不超过 10MB |

**响应示例**：

```json
{
  "code": 200,
  "message": "简历诊断任务已提交",
  "data": 1789456321478656
}
```

**错误码**：

| 错误码 | 说明 |
|--------|------|
| 400 | 上传文件不能为空或格式错误 |
| 500 | 文件保存失败 |

---

**修复完成时间：** 2026-04-02