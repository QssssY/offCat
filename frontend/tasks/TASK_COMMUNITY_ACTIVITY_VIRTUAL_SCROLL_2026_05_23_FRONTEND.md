# 社区与个人动态虚拟滚动接入

## 当前任务所属模块
- 前端社区模块。
- 页面：社区首页、个人动态中心。

## 前端文件定位
- `frontend/app/src/main.js`
- `frontend/app/src/views/community/CommunityView.vue`
- `frontend/app/src/views/community/MyActivity.vue`
- `frontend/app/src/__tests__/views/community/MyActivity.test.js`
- `frontend/app/src/components/community/ImageGrid.vue`
- `frontend/app/src/__tests__/components/community/ImageGrid.test.js`
- `frontend/app/package.json`
- `frontend/app/package-lock.json`

## 本轮修改文件清单
- `frontend/app/package.json`
- `frontend/app/package-lock.json`
- `frontend/app/src/main.js`
- `frontend/app/src/views/community/CommunityView.vue`
- `frontend/app/src/views/community/MyActivity.vue`
- `frontend/app/src/__tests__/views/community/MyActivity.test.js`
- `frontend/app/src/__tests__/views/community/CommunityView.test.js`

## 前端实现方案
- 引入成熟虚拟滚动组件库 `vue-virtual-scroller`，不自研虚拟列表。
- 在 `main.js` 引入 `vue-virtual-scroller/dist/vue-virtual-scroller.css`。
- 社区首页帖子流最终回退为普通列表渲染，不再使用 `DynamicScroller`；原因是首页滚动发生在 `.layout-content` 内，虚拟滚动与触底追加组合后会导致向上回滚出现空白。
- 社区首页触底 `IntersectionObserver` 绑定 `.layout-content` 作为真实滚动容器，避免继续按浏览器 viewport 估算可见区。
- 个人动态中心的“我的帖子 / 点赞过 / 收藏 / 评论过 / 收到的点赞 / 收到的评论 / 收到的回复 / 收到的收藏”列表均接入 `DynamicScroller`。
- 收到的点赞和收藏等没有独立记录 ID 的列表，在前端补充 `virtualKey` 作为虚拟列表稳定 key，不改变后端响应结构。
- 保留原有“加载更多”按钮，不把个人动态中心改成自动无限滚动，避免扩大交互变更。
- 修复 `ImageGrid` 多根节点导致父组件传入 `class="card-images"` 无法继承的 Vue warning：组件关闭自动 attrs 继承，并将外部 class 显式合并到 `.image-grid` 根元素。

## 后端实现方案
- 后端 DTO 类加载修复见 `tasks/fixes/TASK_COMMUNITY_ACTIVITY_VIRTUAL_SCROLL_AND_DTO_BUILDER_2026_05_23_BACKEND.md`。

## 数据存储方案
- 不新增前端持久化字段。
- 不修改接口响应结构。

## stage 更新说明
- 已在 `frontend/tasks/stage.md` 追加本轮前端接入状态。
- 根目录 `runtime/STATE.md` 同步记录本轮前后端联动修复。

## 构建与测试结果
- 前端定向测试：`npm.cmd test -- --run src/__tests__/views/community/MyActivity.test.js src/__tests__/views/community/CommunityView.test.js` 通过，14 个用例通过。
- 本轮补充测试：`npm.cmd test -- --run src/__tests__/components/community/ImageGrid.test.js src/__tests__/views/community/CommunityView.test.js src/__tests__/views/community/MyActivity.test.js` 通过，32 个用例通过。
- 前端构建：`npm.cmd run build` 通过。
- 首页空白修复补充测试：`npm.cmd test -- --run src/__tests__/views/community/CommunityView.test.js src/__tests__/components/community/ImageGrid.test.js src/__tests__/views/community/MyActivity.test.js` 通过；`npm.cmd run build` 通过。

## 当前功能验收说明
- 社区首页帖子流恢复为稳定的真实 DOM 列表，向下自动加载后再向上回滚，不会因为虚拟滚动卸载上方节点而出现空白页。
- 个人动态中心各 Tab 已接入虚拟滚动组件；其中“评论过的帖子”和“收到的点赞”新增测试覆盖，确认能正常渲染并触发接口。
- 帖子卡片图片列表继续保留 `card-images` 样式类，控制台不再出现 `Extraneous non-props attributes (class)` 警告。

## 停止说明
- 本轮只修复社区首页触底追加后回滚空白问题，不扩展社区业务功能，不改个人动态中心交互。
