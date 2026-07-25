# BUGFIX_11_SERVER_GITIGNORE_SETUP

## 1. 背景

当前前端目录已经单独创建并配置好了 `.gitignore`，前端部分不需要再分析。

现在需要单独处理项目根目录下的后端代码目录 `server/`，分析其中哪些内容应该被 Git 忽略，并直接创建后端专用 `.gitignore` 文件。

本次任务只针对 `server/` 目录，不处理前端目录。

---

## 2. 任务目标

在 `server/` 目录下生成一份适用于当前后端项目的 `.gitignore`，用于忽略不应提交到 Git 的后端本地文件和构建产物。

---

## 3. 范围限制

本任务仅处理：

- `server/` 目录中的后端工程文件

本任务不处理：

- `frontend/`
- 前端 `.gitignore`
- 项目根目录其他模块
- 前端构建产物和依赖目录

---

## 4. 分析要求

必须先分析 `server/` 目录的工程类型和文件结构，再决定 `.gitignore` 内容。

重点识别以下内容：

1. Java / Spring Boot / Maven 的构建产物
2. 编译输出目录
3. IDE 配置文件
4. 本地日志文件
5. 本地运行时临时文件
6. 本地敏感配置文件
7. 测试报告和覆盖率输出
8. 其他不应纳入版本控制的后端本地文件

同时要明确哪些内容**不应被忽略**，例如：

- `src/main/java/`
- `src/main/resources/`
- `src/test/`
- `pom.xml`
- `README.md`
- `db/`（如果位于 server 内且属于项目脚本）
- `.sql`
- `.md`
- 工程说明和任务文档

---

## 5. 生成要求

必须在 `server/` 目录下创建：

```text
server/.gitignore
```