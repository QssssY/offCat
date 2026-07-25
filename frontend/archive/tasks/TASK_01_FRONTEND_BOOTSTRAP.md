# TASK 01 - 前端工程初始化

## 1. 任务目标

在 `frontend/app/` 下初始化独立前端工程。

本任务仅处理工程启动和基础结构搭建，
不包含复杂业务页面开发。

## 2. 固定技术栈

- Vue 3
- Vite
- Element Plus
- Vue Router
- Pinia
- Axios

## 3. 本任务范围

本任务需要完成以下事项：

1. 在 `frontend/app/` 下创建 Vue 3 + Vite 工程
2. 安装并配置 Element Plus
3. 安装并配置 Vue Router
4. 安装并配置 Pinia
5. 安装并配置 Axios
6. 建立 `src/` 下基础目录结构
7. 创建最小可运行布局、路由和首页占位页面
8. 验证本地启动成功

## 4. 预期目录结果

示例结构如下：

frontend/app/
- package.json
- vite.config.js
- index.html
- src/
  - api/
  - assets/
  - components/
  - layouts/
  - router/
  - stores/
  - styles/
  - utils/
  - views/
  - App.vue
  - main.js

## 5. 验收标准

满足以下条件可视为本任务完成：

1. 前端依赖可以正常安装
2. 开发环境可以正常启动
3. 路由功能可用
4. Pinia 已接入
5. Element Plus 已接入
6. Axios 基础封装已存在
7. 基础页面可以正常渲染

## 6. 任务限制

1. 不修改任何后端文件
2. 不迁移任何后端目录
3. 本任务不启动复杂业务页面开发
4. 不绕过前端运行规则直接堆代码
5. 本任务虽不涉及复杂业务页面，但后续目录组织、基础布局和页面骨架设计必须预留对以下规范的承接能力：
   - `frontend/docs/UI_STYLE_GUIDE.md`
   - `frontend/docs/PAGE_SKELETON_GUIDE.md`
   - `frontend/docs/COMPONENT_GUIDE.md`

## 7. 交付要求

本任务完成后，需要同步更新：

- `frontend/runtime/STATE.md`

如有必要，可将工程初始化说明补充到：

- `frontend/docs/`

