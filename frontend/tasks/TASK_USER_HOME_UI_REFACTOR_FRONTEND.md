# 鐢ㄦ埛绔椤?UI 淇涓庨噸鏋勮褰?
## 本轮补充修正：首页暗转亮月亮落下与云朵回聚

- 当前任务所属模块：用户端首页 `/` 的 hero 表现层。
- 前端文件定位：`frontend/app/src/views/HomePageView.vue`。
- 后端文件定位：本轮不涉及后端、API、数据库或路由。
- 本轮修改文件清单：`frontend/app/src/views/HomePageView.vue`、`frontend/app/src/__tests__/views/HomePageView.test.js`、`frontend/tasks/TASK_USER_HOME_UI_REFACTOR_FRONTEND.md`、`frontend/tasks/stage.md`。
- 前端实现方案：在首页根节点增加 `is-light-return` 临时状态；通过 `MutationObserver` 监听 `html[data-theme]`，仅当主题从 `dark` 切回非 dark 时触发该状态，避免首次亮色加载误播回切动画。
- 动效方案：暗转亮时 `.hero-moon` 执行 1.45s 的 `moon-set` 慢慢落下并淡出，月亮光晕同步执行 `moon-glow-fade`；`.hero-starry-sky` 执行 `star-field-fade` 淡出；7 片 `.hero-cloud` 延迟 1.1s 后执行 1.35s 的 `cloud-regather`，从散开位置慢慢回聚到亮色云朵位置，避免云朵在月亮下落前抢先出现。
- 动效边界：新增回切动效仍只使用 `opacity` 与 `transform`；`prefers-reduced-motion: reduce` 下关闭 `is-light-return` 内的月亮、星空和云朵动画，直接展示亮色最终状态。
- 测试更新：`HomePageView.test.js` 增加暗色切回亮色的运行时测试，断言 `is-light-return` 会出现、1900ms 时仍保留并在 3000ms 后移除；静态断言锁定 `MutationObserver`、`cloud-regather 1.35s`、`moon-set 1.45s`、云朵延迟回聚、`is-light-return` 和 reduced-motion 覆盖。
- 验证结果：先运行 `npm.cmd test -- --run src/__tests__/views/HomePageView.test.js` 失败于缺少更长的 `is-light-return`、延迟回聚和慢速 `moon-set`，确认 RED 有效；实现后同命令通过，1 个测试文件 / 5 个用例通过；`npm.cmd run build` 通过。
- 当前功能验收说明：从暗色切回亮色时，月亮先慢慢落下消失，星星淡出，云朵再从散开状态聚回亮色云层。
- 停止说明：本轮只完成首页暗转亮表现层修正，不继续推进其它页面、全局主题 token、管理端或后端功能。

## 本轮二次修正：首页暗色星空、云朵散去与月亮重绘

- 当前任务所属模块：用户端首页 `/` 的 hero 表现层。
- 前端文件定位：`frontend/app/src/views/HomePageView.vue`。
- 后端文件定位：本轮不涉及后端、API、数据库或路由。
- 本轮修改文件清单：`frontend/app/src/views/HomePageView.vue`、`frontend/app/src/__tests__/views/HomePageView.test.js`、`frontend/tasks/TASK_USER_HOME_UI_REFACTOR_FRONTEND.md`、`frontend/tasks/stage.md`。
- 前端实现方案：亮色模式将首页 hero 云朵从 3 片扩展为 7 片；新增 `aria-hidden="true"` 的 `.hero-starry-sky` 星空容器并渲染 12 颗 `.hero-star`；将 `.hero-moon` 改为 `data-phase="half"` 的纯 CSS 月亮，使用多层 `radial-gradient` 绘制暖杏色月面、暗色斑纹和高光点，不再使用黑色椭圆 cutout。
- 暗色切换方案：继续使用 `:global(html[data-theme="dark"] .xxx)` scoped 安全选择器；暗色模式下 `.hero-cloud` 执行 `cloud-scatter`，用 `opacity` 和 `transform` 慢慢散去；月亮入场延迟到最长云朵退场动画之后，确保云朵先散开再执行 `moon-rise`；`.hero-starry-sky` 执行 `star-field-rise` 并让星星 `star-twinkle`；月亮光晕执行 `moon-glow-breathe`。
- 动效边界：新增动效均限定在 `opacity` 与 `transform`；`prefers-reduced-motion: reduce` 下关闭云朵、星空、星星和月亮动画，直接展示暗色最终状态。
- 测试更新：`HomePageView.test.js` 断言 `.hero-starry-sky`、12 个 `.hero-star`、7 个 `.hero-cloud` / `.hero-motion-cloud`、`data-phase="half"`、暗色 `.hero-starry-sky` 选择器、`cloud-scatter` / `moon-rise` / `moon-glow-breathe` / `star-twinkle`、月亮入场延迟晚于云朵退场以及 reduced-motion 降级规则，并断言源码不再包含 `--home-moon-cutout`。
- 验证结果：先运行 `npm.cmd test -- --run src/__tests__/views/HomePageView.test.js` 失败于月亮入场仍为 `0.3s` 延迟，确认时序断言有效；修复后同命令通过，1 个测试文件 / 4 个用例通过；`npm.cmd run build` 通过。
- 当前功能验收说明：亮色模式保留更多云朵；暗色模式云朵先散去，随后星空点亮并升起月亮，月亮不再出现黑色椭圆遮罩。
- 停止说明：本轮只完成首页暗色 hero 装饰修正，不继续推进其它页面、全局主题 token、管理端或后端功能。

## 本轮补充修正：首页暗色模式云朵退场与月亮升起

- 本轮针对“首页切换暗色后云朵消失并升起月亮”的反馈做表现层补充，范围仅包含首页 `/` 的 hero 装饰、首页测试和前端任务记录；不修改全局主题 token、路由、其它用户端页面、`/admin/**`、API、数据库或后端业务流程。
- 前端文件定位：`frontend/app/src/views/HomePageView.vue`；测试文件定位：`frontend/app/src/__tests__/views/HomePageView.test.js`；任务与阶段记录定位：`frontend/tasks/TASK_USER_HOME_UI_REFACTOR_FRONTEND.md`、`frontend/tasks/stage.md`。
- 实现方式：在 hero 云朵节点旁新增 `aria-hidden="true"` 的 `.hero-moon` 装饰节点；月亮完全由 CSS 绘制，使用暖杏色弯月、低强度光晕和首页局部 `--home-moon-*` 变量控制尺寸、位置、颜色与暗色可见状态。
- 暗色切换策略：亮色模式继续保留 3 个 `.hero-cloud` 云朵；暗色模式通过 `:global(html[data-theme="dark"] .hero-cloud)` 关闭云朵持续漂移并设置 `opacity: 0`、`transform: translate3d(..., -18px, 0)`；通过 `:global(html[data-theme="dark"] .hero-moon)` 让月亮从右上区域轻微升起。
- 动效边界：月亮入场仅使用 `opacity` 与 `transform` 的 `moon-rise`；云朵退场同样只使用 `opacity` 与 `transform`；`prefers-reduced-motion: reduce` 下关闭月亮升起、云朵退场和相关 transition，直接展示暗色最终状态。
- 测试补充：`HomePageView.test.js` 断言 `.hero-moon` 渲染、`.hero-cloud` 仍为 3 个、暗色 scoped 全局选择器包含 `.hero-cloud` 和 `.hero-moon`、源码保留 `moon-rise` 与 reduced-motion 降级规则。
- 验证结果：先运行 `npm.cmd test -- --run src/__tests__/views/HomePageView.test.js` 失败于缺失 `.hero-moon` 和暗色月亮选择器，确认 RED 有效；实现后同命令通过，1 个测试文件 / 4 个用例通过；`npm.cmd run build` 通过。
- 停止说明：本轮只完成首页暗色 hero 装饰切换，不继续推进其它页面、主题系统或新增业务能力。

## 本轮补充修正：首页暗色 scoped 选择器与变量化修复

- 本轮针对“首页暗色模式仍像灰色遮罩、字体看不清、Hero/路径/辅助能力仍保持浅色”的反馈做根因修复，范围仅包含首页 `/` 的主题 CSS 与首页测试；不修改导航栏、其它用户端页面、`/admin/**`、API、路由、数据库或后端业务流程。
- 根因定位：`HomePageView.vue` 原先使用 `:global(html[data-theme="dark"]) .hero-main` 这类 scoped CSS 写法，生产构建后会被编译成只作用在 `html[data-theme=dark]` 的规则，后续 `.hero-main`、`.career-path-node` 等目标类丢失，导致很多暗色覆盖实际没有命中首页元素。
- 修复方式：将暗色选择器统一改为 `:global(html[data-theme="dark"] .xxx)`，确保 Vue scoped 编译后保留完整的 `html[data-theme=dark] .xxx` 选择器。
- 首页样式从零散暗色覆盖改为变量驱动：新增 `--home-page-bg`、`--home-hero-bg-layer`、`--home-hero-surface`、`--home-card-surface`、`--home-workflow-bg`、`--home-text-strong` 等首页专属 token；亮色保持橙白底色，暗色切换为暖橙棕深色表面，避免整页灰蒙或浅色卡片穿透。
- 覆盖范围包括首页大背景、Hero 背景层、猫图滤镜、云元素、Hero 面板、快捷入口、次级 CTA、求职路径节点、使用路径、辅助能力、版本动态卡片和主要文本颜色。
- 测试补充读取 `HomePageView.vue` 源文件，锁定首页必须具备暗色变量入口和完整 scoped 全局选择器，防止后续退回到无效的 `:global(... ) .xxx` 写法。
- 验证结果：`npm.cmd test -- --run src/__tests__/App.test.js src/__tests__/views/HomePageView.test.js` 通过，2 个测试文件 / 6 个用例通过；`npm.cmd run build` 通过；构建产物确认存在 `html[data-theme=dark] .theme-aware-home`、`html[data-theme=dark] .hero-main`、`html[data-theme=dark] .career-path-node` 选择器。

## 本轮补充修正：Naive UI 暗色桥接与首页暖暗色修复

- 本轮针对“暗色模式像灰色遮罩盖住首页”的反馈做最小修复，范围包含应用根层 Naive UI 主题桥接和首页 `/` 暗色视觉修正；不修改导航栏、其它用户端页面、`/admin/**`、API、路由、数据库或后端业务流程。
- 在 `App.vue` 根层新增 `NConfigProvider`，根据 `themeStore.resolvedTheme` 切换 `naive-ui` 的 `darkTheme`，并补充橙色品牌主题覆盖，解决项目暗色方案只桥接 Element Plus、Naive UI 组件未统一跟随主题的问题。
- 首页暗色样式从偏灰紫的 `--bg-page/#22223b` 视觉改为暖暗色背景：使用深橙棕底、暖橙径向光、半透明橙棕表面，减少整屏灰暗感。
- 猫图不再被暗色滤镜压暗，恢复接近原图的亮度与饱和度；云、hero 面板、快捷入口、次级 CTA、路径节点、使用路径区、辅助能力入口和版本卡片统一调整为暖暗色表面。
- 新增 `frontend/app/src/__tests__/App.test.js`，验证暗色模式会传入 Naive UI `darkTheme`，亮色模式保持 Naive UI 亮色主题，同时保留首页结构测试。
- 验证结果：`npm.cmd test -- --run src/__tests__/App.test.js src/__tests__/views/HomePageView.test.js` 通过，2 个测试文件 / 5 个用例通过；`npm.cmd run build` 通过。

## 本轮补充修正：首页暗色模式兼容

- 本轮仅修复首页 `/` 在暗色模式下的主题适配问题，不修改导航栏、其它用户端页面、`/admin/**`、API、路由、数据库或后端业务流程。
- 根节点补充 `.theme-aware-home` 作为首页主题适配测试锚点，避免后续重构误删暗色模式覆盖入口。
- 在 `HomePageView.vue` 的 scoped CSS 中新增 `:global([data-theme="dark"])` 覆盖块，接入项目现有暗色 token：`--bg-page`、`--bg-card`、`--text-title`、`--text-body`、`--text-muted`、`--border-card`、`--orange-light-bg`。
- 暗色覆盖范围包括首页大背景、首屏插画容器、猫图亮度、云元素、hero 信息面板、快捷入口、次级 CTA、路径节点、使用路径区、辅助能力入口和版本动态卡片。
- 视觉策略保留暖橙强调，但降低浅色大底、白色云朵和白色卡片在暗色模式下的突兀感；动画仍只使用 `transform` 和 `opacity`，没有新增动画库。
- 测试补充暗色模式目标表面结构检查，覆盖 `.theme-aware-home`、`.background-hero-section`、`.hero-main`、`.hero-cloud`、`.career-path-node`、`.workflow-section`、`.support-capability-item`、`.version-item`。
- 验证结果：`npm.cmd test -- --run src/__tests__/views/HomePageView.test.js` 通过，1 个测试文件 / 3 个用例通过；`npm.cmd run build` 通过。

## 本轮补充修正：首页过渡动画与动效增强

- 本轮按 `motion-vue`、`impeccable/animate`、`ui-skills/fixing-motion-performance` 的约束执行，仅增强首页 `/` 的动效表现，不新增动画库，不修改路由、接口、后端和其它页面。
- 首屏背景猫图继续使用 `frontend/app/src/assets/background.png`，将图片渲染放到 `.hero-background-art::after`，补充入场 reveal 与轻微呼吸动效；猫图容器本身只保留 `transform`/`opacity` 级别的 motion 动画。
- 3 个云元素保留现有 CSS 结构，新增 `.hero-motion-cloud` 测试锚点，使用一次性进入和低强度漂移动效营造轻量氛围；`prefers-reduced-motion` 下关闭云和猫图的 keyframes。
- 求职路径 `.career-path-rail` 改为 motion 容器并增加 `.motion-path-rail` 锚点，6 个路径节点通过 `pathContainerVariants` / `pathItemVariants` 做顺序进入，hover/press 继续限制在 `translate`、`scale`、阴影和边框反馈。
- 使用路径 `.workflow-steps` 改为 motion 容器并增加 `.motion-workflow-steps` 锚点，4 个步骤通过 `workflowContainerVariants` / `workflowStepVariants` 做 stagger reveal；步骤 hover 只增强序号圆点和文字轻微位移，不回退为卡片堆叠。
- reduced motion 补齐：关闭路径线、猫图、云、箭头、步骤序号和步骤文案的动画/过渡位移，保证用户关闭动态效果后页面仍完整可用。
- 测试补充覆盖 `.hero-motion-cloud`、`.motion-path-rail`、`.motion-workflow-steps`、`.motion-workflow-step`，避免后续把动效结构误删。
- 验证结果：`npm.cmd test -- --run src/__tests__/views/HomePageView.test.js` 通过，1 个测试文件 / 2 个用例通过；`npm.cmd run build` 通过。
## 褰撳墠浠诲姟鎵€灞炴ā鍧?
- 妯″潡锛氱敤鎴风棣栭〉
- 椤甸潰锛歚frontend/app/src/views/HomePageView.vue`
- 娴嬭瘯锛歚frontend/app/src/__tests__/views/HomePageView.test.js`
- 鏈疆鎬ц川锛氶椤佃儗鏅富瑙嗚閲嶆瀯銆佽矾寰勫紡鍔熻兘灞曞紑銆乂ue motion 鍔ㄦ晥澧炲己

## 闂鑳屾櫙

涓婁竴杞椤典粛鐒跺儚鏅€?SaaS 鍗＄墖椤碉紝娌℃湁鐪熸鍚告敹 `lvyovo-wiki.tech` 鐨勪翰鍜岃交閲忔皼鍥村拰 `room-1913.vercel.app` 鐨勮川鎰熷垏鎹㈡柟鍚戙€傜敤鎴锋柊澧?`frontend/app/src/assets/background.png`锛屾湰杞互璇ュ浘浣滀负棣栭〉涓昏瑙夛紝鍥寸粫鈥滀粠绠€鍘嗗埌 Offer鈥濈殑鎻掔敾鍙欎簨閲嶆瀯棣栭〉銆?
## Skill 浣跨敤璇存槑

- 宸叉寜 `Superpowers:test-driven-development` 涓庨」鐩?`tdd-workflow` 鍏堣ˉ缁撴瀯娴嬭瘯锛屽啀淇敼瀹炵幇銆?- 宸插弬鑰?`frontend-design`銆乣ui-ux-pro-max`銆乣impeccable/layout`銆乣impeccable/animate`銆乣baseline-ui` 鍜?`motion-vue` 鐨勭害鏉熸帶鍒跺竷灞€銆佸姩鏁堛€佸浘鏍囧昂瀵稿拰鍝嶅簲寮忓瘑搴︺€?- 鏈疆鏈笅杞芥柊 skill锛岀幇鏈?skill 瓒冲瑕嗙洊棣栭〉淇銆?
## 鏈疆淇敼鏂囦欢

- `frontend/app/src/views/HomePageView.vue`
- `frontend/app/src/__tests__/views/HomePageView.test.js`
- `frontend/app/package.json`
- `frontend/app/package-lock.json`
- `frontend/tasks/TASK_USER_HOME_UI_REFACTOR_FRONTEND.md`
- `frontend/tasks/stage.md`

## 鍓嶇瀹炵幇鏂规

1. 棣栧睆鏀逛负鑳屾櫙鍥句富瑙嗚锛?   - 浣跨敤 `background.png` 浣滀负 `.hero-background-art` 鍙充晶鐙珛鐚浘灞傦紝鑰屼笉鏄暣鍧?cover 鑳屾櫙銆?   - 宸︿晶 `.hero-main` 鎵胯浇鏍囬銆佽鏄庛€丆TA銆佺粺璁″拰杞婚噺鑳藉姏鑳跺泭锛岄伩鍏嶅拰鎶犲浘鐚噸鍙犮€?   - `.background-hero-section` 浣跨敤鏆栬壊鑳屾櫙娓愬彉锛屽苟琛ュ厖 3 涓?CSS 浜戞湹鍏冪礌澧炲己杞婚噺姘涘洿銆?   - 绉诲姩绔皢鐚浘灞傜疆浜庢枃妗堜笂鏂瑰尯鍩燂紝鏂囨娴眰涓嬫矇锛岄伩鍏嶉伄鎸′富浣撱€?2. 鍔熻兘浠嬬粛浠庡崱鐗囩綉鏍兼敼涓?6 鑺傜偣姹傝亴璺緞锛?   - 绠€鍘嗚瘖鏂?   - 宀椾綅鍖归厤
   - 妯℃嫙闈㈣瘯
   - 闈㈣瘯澶嶇洏
   - 绠€鍘嗘ā鏉垮簱
   - Offer 杈呭姪
3. 杈呭姪鑳藉姏鏀逛负杞婚噺鍒楄〃锛?   - 鎴愰暱涓績
   - 绀惧尯浜ゆ祦
   - 浼氬憳涓庨搴?   - 閫氱煡涓庣増鏈姩鎬?4. 淇濈暀鐜版湁 `FeatureIcon` 鍥炬爣浣撶郴锛屽苟缁熶竴棣栭〉鍥炬爣瀹瑰櫒灏哄锛岄伩鍏嶅浘鏍囨姠涓昏瑙夈€?5. Naive UI 浠呯户缁敤浜庨椤靛凡鏈夌殑 `NButton`銆乣NTag`銆乣NSkeleton`锛屼笉鏀瑰叏灞€涓婚鍩哄骇銆?6. 浣跨敤 `motion-v` 瀹炵幇鑳屾櫙杞诲井鍏ュ満銆侀灞忓垎灞傝繘鍏ャ€佽矾寰勮妭鐐硅繘鍏ヨ鍙?reveal銆乭over/press 寰氦浜掑拰杈呭姪鑳藉姏杞婚噺婊戝叆銆?7. 鍔ㄦ晥闄愬畾鍦?`opacity` 鍜?`transform`锛孋SS 淇濈暀缁嗚竟妗嗐€佹煍鍜屾姇褰便€佽交閲忚矾寰勮繘搴︾嚎锛屽苟琛ラ綈 `prefers-reduced-motion`銆?
## 鍋滄杈圭晫

- 涓嶄慨鏀?`AppHeader.vue`銆乣MainLayout.vue`銆?- 涓嶄慨鏀瑰叾瀹冪敤鎴风椤甸潰銆?- 涓嶄慨鏀?`/admin/**`銆?- 涓嶆柊澧炴帴鍙ｏ紝涓嶄慨鏀?API銆佽矾鐢便€佹暟鎹簱鎴栧悗绔笟鍔℃祦绋嬨€?
## 娴嬭瘯涓庢瀯寤?
- RED锛歚npm.cmd test -- --run src/__tests__/views/HomePageView.test.js` 棣栨澶辫触浜?`.background-hero-section` 涓嶅瓨鍦紝楠岃瘉娴嬭瘯鑳芥崟鎹夋棫棣栭〉缂哄皯鑳屾櫙涓昏瑙夌粨鏋勩€?- GREEN锛歚npm.cmd test -- --run src/__tests__/views/HomePageView.test.js` 閫氳繃锛? 涓祴璇曟枃浠?/ 2 涓敤渚嬮€氳繃銆?- 鏋勫缓锛歚npm.cmd run build` 閫氳繃銆?
## 褰撳墠楠屾敹璇存槑

- 棣栭〉宸插叿澶?`.background-hero-section`銆乣.hero-background-art`銆乣.hero-cloud`銆乣.hero-main`銆乣.hero-quick-trails` 涓昏瑙夌粨鏋勩€?- 棣栭〉宸插叿澶?`.career-path-section`銆乣.career-path-rail`銆乣.career-path-node` 璺緞寮忓姛鑳界粨鏋勩€?- 璺緞鑺傜偣鏁伴噺鍥哄畾涓?6銆?- 杈呭姪鑳藉姏娌℃湁杩涘叆澶у崱鐗囩綉鏍笺€?- 鏈疆鍙畬鎴愰椤典慨澶嶄笌閲嶆瀯锛屼笉缁х画鎺ㄨ繘 Dashboard銆佹垚闀夸腑蹇冦€佷細鍛樸€丱ffer 鎴栧叾瀹冮〉闈€?
## 鏈疆琛ュ厖淇锛氬浘鏍囨斁澶т笌鑳屾櫙鍝嶅簲寮?
- 鏍规嵁鏈€鏂板弽棣堬紝棣栭〉璺緞鑺傜偣鍥炬爣缁х画淇濈暀 `FeatureIcon`锛屼絾灞曠ず鏂瑰紡浠庘€滃皬鏂规鍥炬爣鈥濊皟鏁翠负鏃犳澶у浘鏍囷細`.route-icon` 鏀惧ぇ鍒?84px 瀹瑰櫒锛屽唴閮ㄤ笟鍔″浘鏍囦娇鐢?`size="xl"`锛岄€氳繃 `drop-shadow` 寤虹珛灞傛锛屼笉鍐嶄娇鐢ㄨ儗鏅壊鏂规鍖呰９銆?- 杈呭姪鑳藉姏鍒楄〃鐨勫浘鏍囧睍绀哄悓姝ヤ粠 44px 鏂规璋冩暣涓烘棤妗?66px 灞曠ず鍖猴紝涓氬姟鍥炬爣浣跨敤 `size="lg"`锛岄伩鍏嶇敤鎴风湅涓嶆竻鍏ュ彛璇箟銆?- 棣栭〉 CTA 鍥炬爣浠?`size="sm"` 璋冩暣涓?`size="md"`锛岀澶村浘鏍囦粠 `size="sm"` 璋冩暣涓?`size="md"`锛屽苟澧炲己 hover 鏃剁殑妯悜浣嶇Щ涓庤交寰缉鏀惧弽棣堛€?- `background.png` 瀵瑰簲鐨?`.hero-background-art` 鍦ㄦ闈㈢鏀惧ぇ鍒?`clamp(720px, 70vw, 940px)`锛岄殢鏂偣璋冩暣浣嶇疆銆佸昂瀵稿拰閫忔槑搴︼紱鍦?520px 浠ヤ笅閫氳繃 `opacity` 鍜屼綅缃繃娓℃贰鍑猴紝涓嶄娇鐢?`display: none`锛屼繚璇佹秷澶辫繃绋嬫湁杩囨浮鏁堟灉銆?- 鍝嶅簲寮忚竟鐣岀户缁彧浣滅敤浜庨椤碉細妗岄潰淇濇寔澶ц儗鏅富瑙嗚锛屽钩鏉块檷浣庨€忔槑搴﹀拰鍋忕Щ锛屽皬灞忛殣钘忚儗鏅苟淇濈暀鏂囧瓧銆丆TA銆佽矾寰勫叆鍙ｇ殑鍙鎬с€?- 娴嬭瘯琛ュ厖瑕嗙洊鍥炬爣灏哄缁撴瀯锛? 涓矾寰勮妭鐐瑰潎浣跨敤 `.route-icon .size-xl`锛? 涓緟鍔╄兘鍔涘叆鍙ｅ潎浣跨敤 `.support-icon .size-lg`锛孋TA 涓庣澶村浘鏍囧潎鍗囩骇鍒?`size-md`銆?- 鏈疆琛ュ厖楠岃瘉锛歚npm.cmd test -- --run src/__tests__/views/HomePageView.test.js` 閫氳繃锛? 涓祴璇曟枃浠?/ 2 涓敤渚嬮€氳繃锛沗npm.cmd run build` 閫氳繃銆?
## 鏈疆琛ュ厖淇锛氫娇鐢ㄨ矾寰勫尯鍩熺┖鐧芥敹绱?
- 閽堝鈥滀娇鐢ㄨ矾寰勪笅闈㈡樉寰楀お绌衡€濈殑鍙嶉锛屼粎璋冩暣棣栭〉 `.workflow-section`锛屼笉鏀瑰叾瀹冮椤垫ā鍧楀拰涓氬姟鍏ュ彛銆?- `.workflow-section` 浠庢櫘閫氬乏鍙冲垎鏍忔敼涓哄甫娴呮鐧借儗鏅€佺粏杈规鍜屾煍鍜屾姇褰辩殑宸ヤ綔娴侀潰鏉匡紝澧炲己璇ュ尯鍩熺殑鎵胯浇鎰熴€?- 宸︿晶 `.workflow-copy` 澧炲姞 `.workflow-mini-map`锛岀敤 4 涓揣鍑戣兌鍥婂睍绀哄綋鍓嶆眰鑱岃矾寰勬瑙堬紝鍑忓皯璇存槑鏂囨涓嬫柟绌虹櫧銆?- 鍙充晶 `.workflow-steps` 鍦ㄦ闈㈢鏀逛负 2x2 姝ラ鐭╅樀锛岀Щ鍔ㄧ缁х画鍥炶惤涓哄崟鍒楋紝閬垮厤妯悜绌洪棿娴垂銆?- `.workflow-step` 澧炲姞鏈€灏忛珮搴︺€佸簳閮ㄧ煭杩涘害绾垮拰 hover 鍙嶉锛屽姩鏁堜粛闄愬畾鍦?`transform` 涓庨槾褰?杈规鍙嶉锛屽苟鍦?`prefers-reduced-motion` 涓嬪叧闂綅绉汇€?- 娴嬭瘯琛ュ厖瑕嗙洊 `.workflow-mini-map`銆? 涓?`.workflow-mini-item` 鍜?4 涓?`.workflow-step`锛岄槻姝㈠悗缁張閫€鍥炵┖娲炵粨鏋勩€?- 鏈疆琛ュ厖楠岃瘉锛歚npm.cmd test -- --run src/__tests__/views/HomePageView.test.js` 閫氳繃锛? 涓祴璇曟枃浠?/ 2 涓敤渚嬮€氳繃锛沗npm.cmd run build` 閫氳繃銆?
## 鏈疆浜屾淇锛氫娇鐢ㄨ矾寰勫尯鍩熷幓鍗＄墖鍖?
- 鏍规嵁鏈€鏂板弽棣堬紝涓婁竴鐗堚€滀娇鐢ㄨ矾寰勨€濊櫧鐒跺～琛ヤ簡绌虹櫧锛屼絾灏忚兌鍥婂姞 2x2 鍗＄墖鐭╅樀浜х敓浜嗘槑鏄剧殑鍗＄墖鍫嗗彔鎰燂紝鍜岄椤垫彃鐢诲紡杞婚噺椋庢牸鍐茬獊銆?- 鏈疆鎾ゆ帀 `.workflow-mini-map` 鍜?`.workflow-mini-item`锛屼笉鍐嶇敤棰濆鑳跺泭濉厖绌洪棿銆?- `.workflow-section` 鏀逛负鏃犲崱鐗囪竟鐣岀殑妯悜璺緞甯︼紝浠呬繚鐣欎笂涓嬬粏鍒嗛殧绾裤€佹祬姗欑櫧搴曞拰涓€鏉¤繛缁祦绋嬬嚎銆?- `.workflow-steps` 妗岄潰绔敼涓?4 鍒楄繛缁祦绋嬶紝`.step-index` 浣跨敤鍦嗗舰搴忓彿浣滀负鑺傚閿氱偣锛屾楠や箣闂寸敤缁嗙嚎杩炴帴锛涚Щ鍔ㄧ鏀逛负绔栧悜鏃堕棿绾裤€?- 娴嬭瘯鍚屾璋冩暣涓洪獙璇?`.workflow-steps`銆? 涓?`.workflow-step` 鍜?4 涓?`.step-index`锛岄伩鍏嶇户缁攣瀹氫笂涓€鐗堥毦鐪嬬殑鑳跺泭缁撴瀯銆?- 鏈疆琛ュ厖楠岃瘉锛歚npm.cmd test -- --run src/__tests__/views/HomePageView.test.js` 閫氳繃锛? 涓祴璇曟枃浠?/ 2 涓敤渚嬮€氳繃锛沗npm.cmd run build` 閫氳繃銆?
