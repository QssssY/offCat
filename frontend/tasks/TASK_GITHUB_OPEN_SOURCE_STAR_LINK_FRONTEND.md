# GitHub 开源求 Star 入口前端

## 当前任务所属模块

全局用户端顶部导航与移动端导航抽屉。

## 前端文件定位

- `frontend/app/src/components/AppHeader.vue`
- `frontend/app/src/components/common/GitHubIcon.vue`
- `frontend/app/src/__tests__/components/AppHeader.test.js`
- `frontend/app/src/__tests__/components/common/GitHubIcon.test.js`

## 后端文件定位

本轮不修改后端文件，不新增接口或调整鉴权、业务数据链路。

## 本轮修改文件清单

- `frontend/app/src/components/AppHeader.vue`
- `frontend/app/src/components/common/GitHubIcon.vue`
- `frontend/app/src/__tests__/components/AppHeader.test.js`
- `frontend/app/src/__tests__/components/common/GitHubIcon.test.js`
- `frontend/tasks/TASK_GITHUB_OPEN_SOURCE_STAR_LINK_FRONTEND.md`
- `frontend/tasks/stage.md`

## 前端实现方案

- 桌面端在全局顶部导航右侧增加“开源 · 求 Star”次级入口，让访客在主要业务页面都能看到开源信息，同时不抢占核心功能导航。
- `1439px` 及以下统一切换为汉堡导航，并在导航抽屉底部展示“GitHub 开源 · 求 Star”，避免登录后导航项较多时与右侧操作区重叠。
- 链接跳转至 `https://github.com/QssssY/offCat`，使用新窗口打开，并设置 `noopener noreferrer`、可读的 `aria-label` 和键盘焦点样式。
- 新增独立 `GitHubIcon` 公共组件，使用 GitHub 官方品牌图形，颜色继承当前文本以适配亮色和暗色主题。

## 后端实现方案

无后端改动。开源入口是静态外部链接，不经过 OfferCat 后端。

## 数据存储方案

无数据存储改动，不新增数据库表、字段、索引或 migration。

## RED 验证

在旧实现下执行：

```bash
npm.cmd test -- --run src/__tests__/components/AppHeader.test.js src/__tests__/components/common/GitHubIcon.test.js
```

结果：失败。旧版顶部导航不存在 `.github-star-link`、移动抽屉不存在 `.mobile-github-link`，且没有 `GitHubIcon.vue` 组件，证明测试能捕获本轮缺失功能。

## GREEN 与回归验证

```bash
npm.cmd test -- --run src/__tests__/components/AppHeader.test.js src/__tests__/components/common/GitHubIcon.test.js
npm.cmd run build
git diff --check
```

结果：目标测试 2 个文件 / 11 个用例全部通过，前端生产构建通过，差异格式检查通过。

## 响应式验收说明

- `1440px` 桌面宽度：顶部导航右侧显示“开源 · 求 Star”，导航项与操作按钮无重叠。
- `1280px` 与 `1024px` 中等宽度：桌面导航和入口隐藏，导航抽屉中保留完整 GitHub 开源入口。
- `375px` 移动宽度：抽屉入口完整显示，文字不溢出，点击区域高度满足触屏操作。

## 当前功能验收说明

用户可从桌面顶部导航或移动端导航抽屉点击开源入口，在新窗口进入 OfferCat GitHub 仓库；入口明确传达项目完全开源并邀请用户 Star，不影响登录、主题切换或现有业务导航。

## stage 更新说明

已在 `frontend/tasks/stage.md` 顶部追加本轮 GitHub 开源求 Star 入口完成记录。

## 生产部署结果

- 2026-08-18 使用源码提交 `70d5ccb` 对应的本地已验证 `dist` 上传部署，服务器未执行 Node、npm 或 Vite 构建，未停止 Nginx、OfferCat 后端、MySQL 或其它项目。
- 上传包 SHA-256 为 `ea6cfb0fc3014a64f3ca7da8bce48b1ace6577c4436746f85393df5e8299fd70`，本地与服务器校验一致。
- 上线前完整前端备份位于 `/opt/offercat/backups/github-star-20260818-182516`，可用于快速回滚。
- 部署后 Nginx 与 `offercat.service` 均为 `active`，后端 `/actuator/health` 返回 `200` 和 `UP`。
- `https://kelin.cyou/`、`https://kelin.cyou/api/stats` 和新主包 `assets/index-usizkytV.js` 均返回 `200`；新主包已确认包含 GitHub 仓库地址及“求 Star”文案。
- 服务器部署后约有 2.3 GiB 可用内存和 24 GiB 可用磁盘；上传压缩包已从本地及服务器清理。

## 停止，不继续下一个功能

本轮只增加 GitHub 开源求 Star 入口及其响应式、无障碍和测试保障，不继续修改页脚、首页营销内容、GitHub API Star 数量展示或其它功能。
