# 任务：面试流程重构 — 硬编码开场白 + 分阶段提问策略

## 当前任务所属模块
后端 - 面试 AI Prompt 重构

## 后端文件定位
- `server/src/main/java/com/airesume/server/service/impl/InterviewAiServiceImpl.java` — Prompt 构建核心
- `server/src/main/java/com/airesume/server/service/InterviewService.java` — 开场白生成入口

## 本轮修改文件清单
1. `InterviewAiServiceImpl.java` — `generateOpening()` 改为硬编码模板，不再调用 AI
2. `InterviewAiServiceImpl.java` — `buildDefaultSystemPrompt()` 重写为分阶段面试策略（20+条规则 → 3阶段策略）
3. `InterviewAiServiceImpl.java` — `buildOpeningUserPrompt()` 从4个变体简化为统一模板
4. `InterviewAiServiceImpl.java` — `buildJobTargetInstruction()` 重写为阶段感知规则
5. `InterviewService.java` — `generateOpeningAsync()` 直接使用硬编码模板，绕过 AI 调用

## 后端实现方案

### 1. generateOpening() — 硬编码开场白模板
- 不再调用 `chat()` 生成开场白
- 模板格式：`你好，欢迎参加{难度}{岗位}面试。我是今天的面试官，{简历提示}我们开始吧。`
- 有简历时追加：`我已经看过你的简历，`
- 零 token 消耗，100% 确定性

### 2. buildDefaultSystemPrompt() — 分阶段面试策略
- 核心原则（4条）：角色边界、单问题、简短回应处理、上下文记忆
- 阶段一（第1-5轮）：技能热身
  - 有简历：逐项验证简历中声称的技能
  - 无简历：围绕岗位要求核心技术提问
- 阶段二（第6轮起）：项目/实习深挖
  - 实习优先于项目
  - 深挖细节（技术选型、难点、解决方案）
- 阶段三（最后1-2轮）：综合评估
- 特殊处理：提前结束（至少5轮）、鼓励与安慰（一句话+下一个问题）

### 3. buildOpeningUserPrompt() — 统一模板
- 从4个变体简化为1个统一模板
- 有简历提示：从简历专业技能开始验证
- 无简历提示：从岗位要求核心技术开始提问

### 4. buildJobTargetInstruction() — 阶段控制规则
- 规则7：前5轮只做技能热身，第6轮起才进入项目/实习深挖
- 规则8：技能验证方式——从简历中选取"熟练掌握/精通"的技能提问

### 5. InterviewService.generateOpeningAsync() — 绕过 AI
- 直接拼接硬编码模板字符串
- 不调用 `interviewAiService.generateOpening()`
- 保留异步执行和事务保存逻辑

## 数据存储方案
- 无新增数据库变更

## stage 更新说明
- 本轮为面试流程重构，属于功能改进
- 开场白从 AI 生成改为硬编码模板
- 面试策略从无序提问改为分阶段控制

## 编译结果
- ✅ 后端编译通过（mvn compile -q 无错误输出）

## 当前功能验收说明
1. 开场白为固定模板，不含 AI 生成内容
2. 有简历 + Java 工程师 → 第一轮问技能（如 SpringBoot IoC/AOP），前5轮不问项目
3. 无简历 + 后端开发 → 第一轮问岗位技术（如 Java 框架），前5轮只问技术
4. 第6轮起自然过渡到项目/实习深挖
5. 候选人说"好的" → AI 追问细节，不代替回答
6. 候选人回答好 → 一句话夸奖 + 下一个问题
7. 候选人连续答不上 → 至少5轮后可提前结束
8. 难度（初级/中级/高级）正确传入模板

## 停止，不继续下一个功能
- 本轮重构完成后将停止，等待验收
