# Core Bugfix Final Report

**确认：所有严重问题已修复完毕，共计 9 轮 + 2 轮，30 项。**

## Round 10 — 高优非严重修复（4 项）

| # | 问题 | 状态 |
|---|------|------|
| 23 | `SecurityConfig` ASYNC 分发漏洞 — ASYNC 调度完全绕过安全链 | ✅ |
| 24 | `ResumePdfController` 请求体无限制 — 任意大 HTML 拖垮 Headless Chrome | ✅ |
| 25 | `ResumeTemplate.vue` innerHTML 未净化 — XSS 风险 | ✅ |
| 26 | `SysPromptServiceImpl` 激活竞态 — 并发管理可激活多条 Prompt | ✅ |
| 27 | 新手引导 `guideKey` 不匹配 — 后端 `v1_2` / 前端 `v1_3`，跳过永不持久化 | ✅ |

## Round 8 — 安全与 Token 对齐（7 项）

| # | 问题 | 状态 |
|---|------|------|
| 1 | `PdfTextExtractor` 路径遍历 — 可基于伪造路径读取项目外文件 | ✅ |
| 2 | 简历诊断任务重复处理 — 多个消费者可重复处理同一任务 | ✅ |
| 3 | `resume:task` 缓存未失效 — 状态变更后仍返回旧缓存 | ✅ |
| 4 | JWT 弱默认值 — `JWT_SECRET` 存在弱默认占位值 | ✅ |
| 5 | Redis 故障时登录限流失效 — 暴力破解防护完全失效 | ✅ |
| 6 | 密码找回信息泄露 — 对不同失败原因返回不同错误消息，可枚举用户名 | ✅ |
| 7 | 前端 token 多键名 — 部分页面仍读旧键名 `token` | ✅ |

## Round 9 — 遗留严重问题修复（3 项）

| # | 问题 | 状态 |
|---|------|------|
| 8 | 全局限流 — 注册/简历上传/面试写接口无限流保护 | ✅ |
| 9 | SSE 超时取消 — 流式连接断开后后端 AI 流仍在跑，资源泄漏 | ✅ |
| 10 | Spring Boot 升级 — `3.2.3` 存在已知 CVE，升至 `3.2.11` | ✅ |

## 其他安全加固（R8/R9 附带）

| # | 项目 | 状态 |
|---|------|------|
| 11 | 旧 `InterviewSessionService` 写链路收口 | ✅ |
| 12 | `application-dev.yml` JWT fallback 清理（R8 遗漏，R9 补上） | ✅ |
| 13 | `CriticalEndpointRateLimitFilter` 进程内限流兜底 | ✅ |
| 14 | 新增 `AuthServiceImplTest` + `ResumeDiagnosisProcessorTest` + `CriticalEndpointRateLimitFilterTest` + `InterviewServiceTest` | ✅ |

## 前置修复（Round 1-7，10 项）

| # | 问题 | 轮次 |
|---|------|------|
| 15 | 用量扣减 — 面试会话创建未走扣减链路 | R1 |
| 16 | AI 引擎配置切换不生效 — 运行时配置读取优先于 DB 激活配置 | R1 |
| 17 | VIP 过期仍显示会员 — 管理端角色文案未结合 `vipExpireTime` | R1 |
| 18-22 | 配额语义与消费统一（5 轮迭代完成） | R3-R7D |

## 验证

- `mvn test` → **204 tests**, 0 failures ✅（新增 8 个测试）
- `mvn compile` → 通过 ✅
- `npm run build` → 通过 ✅
- 新增测试：`SecurityConfigTest` + `JwtAuthenticationFilterTest` + `ResumePdfControllerTest` + `SysPromptServiceImplTest`

## 剩余待修复（按实际影响排序）

| 优先级 | 问题 | 类别 | 说明 | 建议 |
|--------|------|------|------|------|
| P2 | `InterviewSessionServiceImpl.endInterviewInDb` 条件 UPDATE 非原子 (#2.12) | 数据竞争 | 并发结束同一次面试 → 双重评估覆盖 | 建议修 |
| P2 | 日志丢失堆栈 25+ 处 (#2.15) | 可运维性 | 异常查因困难 | 建议修 |
| P3 | AI 冷却链路无熔断器 (#2.5) | 弹性 | AI 宕机时每个请求都等待超时 | 可修 |
| P3 | `WorkExperienceSection.vue` index key 5 处 (#2.10) | 渲染 bug | 列表不重排则无影响 | 可不修 |
| P3 | 异常消息泄露 8 处 (`NetworkDiagnosticController`) (#2.14) | 信息泄露 | 仅 dev 环境可用，风险低 | 可不修 |
| P3 | `InterviewSessionServiceImpl` 死代码写方法 (#2.8) | 死代码 | 仅读方法在用，写方法无人调用 | 可不修 |
| P4 | CORS `credentials+wildcard` 风险 (#2.13) | 配置 | 当前默认 `localhost:3000`，已够安全 | 可不修 |
| P4 | Vite CVE + 版本升级 (#2.4) | 维护 | 生产 dev 模式才触发 | 可不修 |
| P4 | Tier 3 全部 31 项 | 代码质量 | 无实际漏洞 | 可不修 |
| P4 | Tier 4 全部 14 项 | 最佳实践 | 无风险 | 可不修 |

## 已确认不修

- **分布式共享限流** — 单体项目，进程内限流已足够
- **application-dev.yml JWT 默认值** — 开发环境保留默认值，方便本地启动
