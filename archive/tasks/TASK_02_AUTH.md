# TASK_02_AUTH

## 目标
实现认证模块最小闭环。

## 本轮范围
- 用户注册
- 用户登录
- JWT 生成与解析
- 获取当前登录用户
- 创建用户时初始化 user_quota
- 管理员角色基础识别

## 涉及表
- sys_user
- user_quota

## 输入上下文
- runtime/RULES.md
- runtime/STACK.md
- runtime/STATE.md
- db/DB_AUTH.md
- tasks/TASK_02_AUTH.md

## 本轮禁止
- 不实现支付
- 不实现简历诊断
- 不实现模拟面试
- 不实现复杂管理端功能

## 输出要求
1. 先列影响文件
2. 再逐文件输出完整代码
3. 给出接口清单
4. 给出必要 SQL 变更
5. 最后自检

## 验收标准
- 支持注册
- 支持登录
- 返回 JWT
- 支持获取当前登录用户
- 创建用户时自动初始化额度记录