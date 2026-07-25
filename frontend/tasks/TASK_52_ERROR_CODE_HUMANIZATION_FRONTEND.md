# TASK-52：错误码人性化 — 前端

## 所属模块
前端公共模块 - 错误处理

## 功能目标
在前端构建错误码到用户友好提示的映射层，使 request.js 能按后端返回的业务错误码展示结构化中文提示，而不是直接展示后端原始消息或英文异常。

## 前端文件定位

### 新增文件
| 文件 | 内容 |
|------|------|
| `frontend/app/src/utils/errorMessages.js` | 错误码映射表 + `getErrorMessage()` 查找函数 |

### 修改文件
| 文件 | 修改内容 |
|------|---------|
| `frontend/app/src/utils/request.js` | 响应拦截器增加错误码映射查找，映射命中时展示 title+description |

### 新增测试文件
| 文件 | 测试数量 | 覆盖内容 |
|------|---------|---------|
| `frontend/app/src/__tests__/utils/errorMessages.test.js` | 11 | 映射表完整性、已知码返回、未知码回退、null 码处理 |

## 前端实现方案

### 1. errorMessages.js

导出 `ERROR_MESSAGES` 映射表（code → `{ title, description }`），覆盖 2xxx-6xxx 所有 26 个业务错误码。导出 `getErrorMessage(code, fallbackMessage)` 查找函数：
- 已知码返回映射的 `{ title, description }`
- 未知码返回 `{ title: fallbackMessage, description: '' }`
- 无 fallback 返回 `null`

### 2. request.js 更新

响应拦截器中，业务错误（`res.code !== 200`）时：
1. 调用 `getErrorMessage(res.code, res.message)` 获取映射
2. 映射命中：展示 `title + description`，持续 5 秒
3. 映射未命中：回退展示 `res.message`（原有行为）
4. 保留 `skipDefaultErrorHandler` 转义逻辑
5. 保留 401 登录跳转行为

## 构建结果
- 前端构建：`npm.cmd run build` 通过
- 前端测试：`npx vitest run src/__tests__/utils/errorMessages.test.js` 通过，11 个测试

## 当前功能验收说明
- [x] request.js 使用错误码映射展示更清晰的错误提示
- [x] 401 登录跳转行为保持不变
- [x] skipDefaultErrorHandler 转义逻辑保持不变
- [x] 前端测试覆盖错误码映射、未登录跳转、未知错误码回退
- [x] 前端构建通过

## 停止
不继续实施诊断进度条、失败重试、DOCX 导出、成长中心雷达或任务式新手引导。
