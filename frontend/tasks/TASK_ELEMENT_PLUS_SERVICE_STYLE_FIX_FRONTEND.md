# Element Plus 服务弹窗样式修复

## 当前任务所属模块
- 前端全局入口
- Element Plus 按需样式
- 全局消息提示与确认弹窗

## 前端文件定位
- `frontend/app/src/main.js`
- `frontend/app/src/__tests__/elementPlusServiceStyles.test.js`

## 后端文件定位
本轮不涉及后端接口、服务或数据库改动。

## 本轮修改文件清单
- 修改 `frontend/app/src/main.js`
- 新增 `frontend/app/src/__tests__/elementPlusServiceStyles.test.js`
- 新增 `frontend/tasks/TASK_ELEMENT_PLUS_SERVICE_STYLE_FIX_FRONTEND.md`
- 修改 `frontend/tasks/stage.md`

## 前端实现方案
- 保留现有 `unplugin-vue-components` 对模板组件的 Element Plus 按需样式引入方式。
- 在应用入口显式导入 `element-plus/es/components/message/style/css` 和 `element-plus/es/components/message-box/style/css`。
- 原因是 `ElMessage`、`ElMessageBox` 属于 JS 服务调用，项目中大量通过 `import { ElMessage, ElMessageBox } from 'element-plus'` 手动调用，不能稳定依赖模板组件解析器自动补齐样式。
- 补充入口静态回归测试，确保后续不会误删这两个服务样式导入，避免提示/确认浮层退化为裸 HTML。

## 同类问题排查结果
- 已排查 `frontend/app/src` 内手动从 `element-plus` 导入的符号：`ElMessage` 涉及 33 个源码文件，`ElMessageBox` 涉及 14 个源码文件，均由入口两条服务样式导入统一覆盖。
- 未发现 `ElNotification`、JS 服务 `ElLoading`、`$loading` 或 `Loading.service` 使用，因此暂不需要补充 notification/loading 服务样式导入。
- 管理端 11 个源码文件使用 `v-loading` 指令；当前 `ElementPlusResolver()` 默认开启 directive 解析，会为 `Loading` 指令注入 `loading` 样式副作用，生产构建产物也包含 `.el-loading-mask`，不属于本次裸弹窗问题。
- `VersionLogView.vue` 存在一个手动 `ElPagination` 导入，但页面模板仍使用 `<el-pagination>`，项目其它分页也走模板解析器；生产构建产物包含 `.el-pagination`，不属于 JS 服务浮层样式缺失问题。

## 后端实现方案
无后端改动。

## 数据存储方案
不新增数据库表、字段、本地存储或缓存结构。

## stage 更新说明
- 已在 `frontend/tasks/stage.md` 追加本轮“Element Plus 服务弹窗样式修复”记录。

## 编译结果
- `npm.cmd test -- --run src/__tests__/elementPlusServiceStyles.test.js` 通过，1 个测试文件 / 2 个用例通过。
- `npm.cmd run build` 通过。

## 构建结果
- 生产构建通过。
- Element Plus 仍保持独立 vendor chunk；本轮仅补充 `ElMessage` / `ElMessageBox` 服务样式，不恢复全量 `element-plus/dist/index.css`。

## 当前功能验收说明
- 触发 `ElMessage.warning/error/success` 时，消息提示应恢复 Element Plus 正常定位、边框、背景和图标样式。
- 触发 `ElMessageBox.confirm/prompt` 时，确认弹窗应恢复遮罩、居中弹窗、标题、按钮和关闭按钮样式。
- 截图中左下角裸露关闭按钮和提示文案的异常形态，符合 JS 服务样式未加载导致的表现，本轮从入口补齐服务样式。
- 其它排查项暂未发现需要继续补样式的服务弹窗入口；`v-loading` 和分页组件已由现有按需解析链路覆盖。

## 停止，不继续下一功能
本轮只处理 Element Plus JS 服务浮层样式缺失问题，不改业务接口、不改页面布局、不替换 UI 组件库、不扩展全局错误提示逻辑。
