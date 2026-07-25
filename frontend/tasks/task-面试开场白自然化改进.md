# 任务：面试开场白 Prompt 自然化改进

## 当前任务所属模块
后端 - 面试 AI Prompt 优化

## 后端文件定位
- `server/src/main/java/com/airesume/server/service/impl/InterviewAiServiceImpl.java` — Prompt 构建核心

## 本轮修改文件清单
1. `InterviewAiServiceImpl.java` — `buildDefaultSystemPrompt()` 修改规则9 + 新增规则16
2. `InterviewAiServiceImpl.java` — `buildOpeningUserPrompt()` 修改4个变体的开场指令
3. `InterviewAiServiceImpl.java` — `buildJobTargetInstruction()` 修改第7条为通用表述

## 后端实现方案

### 1. buildDefaultSystemPrompt 规则9 + 规则16
- 规则9从"严禁任何开场白"改为允许"一句简短承接 + 一个主问题"的组合结构
- 禁止"好的""明白了"等无信息量承接词
- 新增规则16：按岗位类型区分开场风格
  - 技术类岗位（开发/工程师/测试/运维/算法）→ 点出技术栈或项目亮点
  - 综合类岗位（教师/设计/运营/销售/管理/电气/电工）→ 点出专业背景或核心技能

### 2. buildOpeningUserPrompt 4个变体
- 变体1（有简历+有JD+岗位定向）：自然承接简历核心亮点，技术岗从技术栈切入，综合岗从专业背景切入
- 变体2（有JD无简历+岗位定向）：简短说明面试方向，围绕JD核心职责提问
- 变体3（有简历无JD）：自然承接简历亮点，技术栈热身后再进入项目细节
- 变体4（无简历无JD）：简短说明面试方向，围绕岗位核心能力提问

### 3. buildJobTargetInstruction 第7条
- 从"如果是技术岗位，开场前1到2轮优先从候选人简历里的核心技术栈..."改为通用表述
- 技术类岗位侧重技术栈和工程经验，综合类岗位侧重专业背景、从业经历和核心技能

## 数据存储方案
- 无新增数据库变更

## stage 更新说明
- 本轮为 Prompt 优化，不属于代码质量修复，属于功能改进

## 编译结果
- ✅ 后端编译通过（mvn compile -q 无错误输出）

## 当前功能验收说明
1. 有简历 + 技术岗（如Java工程师）→ 开场白先提到简历中的技术栈，再进入项目
2. 有简历 + 综合岗（如小学教师）→ 开场白先提到教学经历或专业背景
3. 无简历 + 任意岗位 → 围绕岗位核心能力自然开场，不假装看过简历
4. 难度（初级/中级/高级）正确传入 Prompt
5. 岗位定向模式下 JD 上下文正确传递

## 停止，不继续下一个功能
- 本轮改进完成后将停止，等待验收
