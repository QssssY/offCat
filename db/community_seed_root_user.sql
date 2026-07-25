-- ============================================================
-- 社区功能测试数据种子脚本（root 账号专用）
--
-- 使用方式：
-- 1. 确认 sys_user.username = 'root' 的账号存在且未逻辑删除。
-- 2. 执行整份 SQL。
-- 3. 登录 root 账号测试 /community 与 /community/my。
--
-- 覆盖模块：
-- - 社区首页：全部、面试经验、内推广场、最新、最热、虚拟滚动分页。
-- - 个人动态：我的帖子、点赞过、收藏、评论过、收到点赞、收到评论、收到回复、收到收藏。
-- - 特殊场景：图片帖、图片评论、楼中楼回复、评论过但原帖已删除。
-- ============================================================

SET NAMES utf8mb4;
SET FOREIGN_KEY_CHECKS = 1;

SET @me_user_id := (
  SELECT id
  FROM sys_user
  WHERE username = 'root' AND is_deleted = 0
  ORDER BY id DESC
  LIMIT 1
);
SET @community_test_image_url := 'https://ts3.tc.mm.bing.net/th/id/OIP-C.TmvkuikpStxy5wKWiziR1AHaE7?rs=1&pid=ImgDetMain&o=7&rm=3';

-- 如果这里返回 NULL，说明 root 账号不存在或已被逻辑删除。
SELECT 'root_user_check' AS info, @me_user_id AS root_user_id;

-- 自动选择互动用户，尽量避开 root。
SET @actor1 := (
  SELECT id FROM sys_user
  WHERE is_deleted = 0 AND status = 1 AND id <> @me_user_id
  ORDER BY create_time DESC, id DESC
  LIMIT 1 OFFSET 0
);

SET @actor2 := (
  SELECT id FROM sys_user
  WHERE is_deleted = 0 AND status = 1 AND id <> @me_user_id
  ORDER BY create_time DESC, id DESC
  LIMIT 1 OFFSET 1
);

SET @actor3 := (
  SELECT id FROM sys_user
  WHERE is_deleted = 0 AND status = 1 AND id <> @me_user_id
  ORDER BY create_time DESC, id DESC
  LIMIT 1 OFFSET 2
);

SET @actor4 := (
  SELECT id FROM sys_user
  WHERE is_deleted = 0 AND status = 1 AND id <> @me_user_id
  ORDER BY create_time DESC, id DESC
  LIMIT 1 OFFSET 3
);

-- 用户不足时兜底复用已有互动用户；如果库里只有 root 一个用户，收到互动类数据会较少。
SET @actor1 := COALESCE(@actor1, @me_user_id);
SET @actor2 := COALESCE(@actor2, @actor1);
SET @actor3 := COALESCE(@actor3, @actor2);
SET @actor4 := COALESCE(@actor4, @actor3);

SELECT '本次使用用户' AS info, @me_user_id AS root_user_id, @actor1 AS actor1, @actor2 AS actor2, @actor3 AS actor3, @actor4 AS actor4;

-- ============================================================
-- 清理旧测试数据，保证脚本可重复执行。
-- ============================================================

DELETE FROM community_post_favorite WHERE id BETWEEN 99430000 AND 99430999;
DELETE FROM community_post_like WHERE id BETWEEN 99420000 AND 99420999;
DELETE FROM community_comment WHERE id BETWEEN 99410000 AND 99410999;
DELETE FROM community_post WHERE id BETWEEN 99400000 AND 99400999;

-- ============================================================
-- 1. 帖子数据。
-- ============================================================

INSERT INTO community_post (
  id, user_id, category, title, content, images,
  like_count, comment_count,
  create_time, update_time, is_deleted
)
VALUES
  (99400001, @me_user_id, 'interview_exp', 'Java 后端一面复盘',
   '【root测试-我的帖子】Java 后端一面复盘：JVM、线程池、Redis 缓存一致性都被追问。重点是项目细节、指标变化和风险兜底。',
   JSON_ARRAY(@community_test_image_url, @community_test_image_url),
   0, 0, NOW() - INTERVAL 50 MINUTE, NOW() - INTERVAL 50 MINUTE, 0),

  (99400002, @me_user_id, 'referral', '后端开发内推',
   '【root测试-我的内推】后端开发内推，base 上海，要求 Spring Boot、MySQL、Redis，有高并发经验加分。',
   NULL,
   0, 0, NOW() - INTERVAL 48 MINUTE, NOW() - INTERVAL 48 MINUTE, 0),

  (99400003, @me_user_id, 'interview_exp', '算法岗面试复盘',
   '【root测试-我的帖子】算法岗面试复盘：项目被追问特征工程、离线评估和线上 A/B 实验。',
   JSON_ARRAY(@community_test_image_url),
   0, 0, NOW() - INTERVAL 46 MINUTE, NOW() - INTERVAL 46 MINUTE, 0),

  (99400011, @actor1, 'interview_exp', '前端一面经验',
   '【root测试-别人帖子】前端一面经验：Vue 响应式、组件通信、虚拟滚动、长列表性能优化都被问到了。',
   JSON_ARRAY(@community_test_image_url),
   0, 0, NOW() - INTERVAL 44 MINUTE, NOW() - INTERVAL 44 MINUTE, 0),

  (99400012, @actor2, 'referral', '前端开发内推',
   '【root测试-别人内推】前端开发内推，要求 Vue3、TypeScript、工程化经验。',
   NULL,
   0, 0, NOW() - INTERVAL 42 MINUTE, NOW() - INTERVAL 42 MINUTE, 0),

  (99400013, @actor3, 'interview_exp', '产品经理面试复盘',
   '【root测试-别人帖子】产品经理面试复盘：重点问需求优先级、用户分层、数据指标和跨团队推进。',
   NULL,
   0, 0, NOW() - INTERVAL 40 MINUTE, NOW() - INTERVAL 40 MINUTE, 0),

  (99400014, @actor4, 'referral', '数据分析师内推',
   '【root测试-别人内推】数据分析师内推，要求 SQL、指标体系、可视化和业务沟通能力。',
   JSON_ARRAY(@community_test_image_url),
   0, 0, NOW() - INTERVAL 38 MINUTE, NOW() - INTERVAL 38 MINUTE, 0),

  (99400015, @actor1, 'interview_exp', '项目细节追问清单',
   '【root测试-高热帖】面试官最常追问的项目细节清单：背景、难点、方案、权衡、指标、复盘。',
   NULL,
   0, 0, NOW() - INTERVAL 36 MINUTE, NOW() - INTERVAL 36 MINUTE, 0),

  (99400099, @actor2, 'interview_exp', '已删除原帖测试',
   '【root测试-已删除原帖】这条帖子逻辑删除，用于测试 root 个人动态里“评论过但原帖已删除”。',
   NULL,
   0, 0, NOW() - INTERVAL 2 DAY, NOW() - INTERVAL 2 DAY, 1);

-- 虚拟滚动/分页用批量帖子。
INSERT INTO community_post (
  id, user_id, category, title, content, images,
  like_count, comment_count,
  create_time, update_time, is_deleted
)
VALUES
  (99400101, @actor1, 'interview_exp', '后端项目复盘 01', '【root虚拟滚动 01】后端项目复盘：接口限流、缓存击穿和慢 SQL 优化。', NULL, 0, 0, NOW() - INTERVAL 101 MINUTE, NOW(), 0),
  (99400102, @actor2, 'referral',      '后端开发内推 02', '【root虚拟滚动 02】内推后端开发，要求 Java 基础扎实，熟悉 Redis。', NULL, 0, 0, NOW() - INTERVAL 102 MINUTE, NOW(), 0),
  (99400103, @actor3, 'interview_exp', '前端性能优化 03', '【root虚拟滚动 03】前端性能优化：路由懒加载、组件拆分、长列表裁剪。', NULL, 0, 0, NOW() - INTERVAL 103 MINUTE, NOW(), 0),
  (99400104, @actor4, 'referral',      '产品经理内推 04', '【root虚拟滚动 04】内推产品经理，要求数据分析和复杂项目经验。', NULL, 0, 0, NOW() - INTERVAL 104 MINUTE, NOW(), 0),
  (99400105, @actor1, 'interview_exp', '算法面试准备 05', '【root虚拟滚动 05】算法面试准备：特征、模型、评估、线上实验。', NULL, 0, 0, NOW() - INTERVAL 105 MINUTE, NOW(), 0),
  (99400106, @actor2, 'referral',      '测试开发内推 06', '【root虚拟滚动 06】内推测试开发，要求自动化测试和 CI 经验。', NULL, 0, 0, NOW() - INTERVAL 106 MINUTE, NOW(), 0),
  (99400107, @actor3, 'interview_exp', 'HR 面试问题 07', '【root虚拟滚动 07】HR 面试常见问题：离职原因、职业规划、薪资期望。', NULL, 0, 0, NOW() - INTERVAL 107 MINUTE, NOW(), 0),
  (99400108, @actor4, 'referral',      '数据开发内推 08', '【root虚拟滚动 08】内推数据开发，要求 Spark、Flink、数仓建模。', NULL, 0, 0, NOW() - INTERVAL 108 MINUTE, NOW(), 0),
  (99400109, @actor1, 'interview_exp', '项目表达建议 09', '【root虚拟滚动 09】项目表达建议：先讲业务，再讲技术，再讲结果。', NULL, 0, 0, NOW() - INTERVAL 109 MINUTE, NOW(), 0),
  (99400110, @actor2, 'referral',      '运营岗位内推 10', '【root虚拟滚动 10】内推运营，要求活动策划、数据复盘和用户增长。', NULL, 0, 0, NOW() - INTERVAL 110 MINUTE, NOW(), 0),
  (99400111, @actor3, 'interview_exp', '数据库面试 11', '【root虚拟滚动 11】数据库面试：索引、事务、锁、MVCC、执行计划。', NULL, 0, 0, NOW() - INTERVAL 111 MINUTE, NOW(), 0),
  (99400112, @actor4, 'referral',      '安全工程师内推 12', '【root虚拟滚动 12】内推安全工程师，要求渗透测试和安全开发经验。', NULL, 0, 0, NOW() - INTERVAL 112 MINUTE, NOW(), 0),
  (99400113, @actor1, 'interview_exp', '二面系统设计 13', '【root虚拟滚动 13】二面重点：系统设计、容量评估、降级限流。', NULL, 0, 0, NOW() - INTERVAL 113 MINUTE, NOW(), 0),
  (99400114, @actor2, 'referral',      'Java 实习生内推 14', '【root虚拟滚动 14】内推 Java 实习生，要求基础扎实，项目表达清楚。', NULL, 0, 0, NOW() - INTERVAL 114 MINUTE, NOW(), 0),
  (99400115, @actor3, 'interview_exp', '简历项目追问 15', '【root虚拟滚动 15】简历项目被追问时，先给结论再展开细节。', NULL, 0, 0, NOW() - INTERVAL 115 MINUTE, NOW(), 0),
  (99400116, @actor4, 'referral',      'AI 应用开发内推 16', '【root虚拟滚动 16】内推 AI 应用开发，要求 LLM 调用和工程落地经验。', NULL, 0, 0, NOW() - INTERVAL 116 MINUTE, NOW(), 0);

-- ============================================================
-- 2. 评论数据。
-- ============================================================

INSERT INTO community_comment (
  id, post_id, user_id, parent_comment_id, reply_to_user_id,
  content, images,
  create_time, update_time, is_deleted
)
VALUES
  (99410001, 99400001, @actor1, NULL, NULL,
   'root 这条 JVM 复盘挺完整，建议再补一下 GC 日志和 dump 分析细节。',
   NULL, NOW() - INTERVAL 49 MINUTE, NOW(), 0),

  (99410002, 99400001, @actor2, NULL, NULL,
   '线程池参数是怎么定的？核心线程数、队列长度和拒绝策略怎么解释？',
   JSON_ARRAY(@community_test_image_url),
   NOW() - INTERVAL 48 MINUTE, NOW(), 0),

  (99410003, 99400002, @actor3, NULL, NULL,
   '我对 root 发的这个内推感兴趣，3 年 Java 后端，主要做订单和库存系统。',
   NULL, NOW() - INTERVAL 47 MINUTE, NOW(), 0),

  (99410004, 99400003, @actor4, NULL, NULL,
   '算法岗这个复盘有参考价值，线上指标和离线指标差异确实容易被追问。',
   NULL, NOW() - INTERVAL 46 MINUTE, NOW(), 0),

  (99410005, 99400001, @me_user_id, 99410001, @actor1,
   '我当时主要讲 Full GC 排查链路、堆 dump 分析和最终缓存对象释放方案。',
   NULL, NOW() - INTERVAL 45 MINUTE, NOW(), 0),

  (99410006, 99400011, @me_user_id, NULL, NULL,
   'root 也在准备前端面试，虚拟滚动这块想看看大家怎么讲。',
   NULL, NOW() - INTERVAL 43 MINUTE, NOW(), 0),

  (99410007, 99400011, @actor1, 99410006, @me_user_id,
   '虚拟滚动重点讲清楚可视区计算、占位高度、动态高度和滚动性能就可以。',
   NULL, NOW() - INTERVAL 42 MINUTE, NOW(), 0),

  (99410008, 99400012, @me_user_id, NULL, NULL,
   '这个前端内推还招中级吗？root 主要做 Vue3 和中后台性能优化。',
   NULL, NOW() - INTERVAL 41 MINUTE, NOW(), 0),

  (99410009, 99400013, @me_user_id, NULL, NULL,
   '产品经理复盘这个点很有用，失败项目确实比成功项目更能看出思考深度。',
   NULL, NOW() - INTERVAL 40 MINUTE, NOW(), 0),

  (99410010, 99400015, @me_user_id, NULL, NULL,
   '这份项目追问清单适合面试前一天快速过一遍。',
   NULL, NOW() - INTERVAL 39 MINUTE, NOW(), 0),

  (99410099, 99400099, @me_user_id, NULL, NULL,
   '这条评论用于测试 root 评论过但原帖已删除时个人动态中心的展示。',
   NULL, NOW() - INTERVAL 2 DAY, NOW() - INTERVAL 2 DAY, 0);

-- ============================================================
-- 3. 点赞数据。
-- ============================================================

INSERT IGNORE INTO community_post_like (
  id, post_id, user_id, create_time
)
VALUES
  (99420001, 99400011, @me_user_id, NOW() - INTERVAL 38 MINUTE),
  (99420002, 99400012, @me_user_id, NOW() - INTERVAL 37 MINUTE),
  (99420003, 99400013, @me_user_id, NOW() - INTERVAL 36 MINUTE),
  (99420004, 99400015, @me_user_id, NOW() - INTERVAL 35 MINUTE),

  (99420011, 99400001, @actor1, NOW() - INTERVAL 49 MINUTE),
  (99420012, 99400001, @actor2, NOW() - INTERVAL 48 MINUTE),
  (99420013, 99400001, @actor3, NOW() - INTERVAL 47 MINUTE),
  (99420014, 99400002, @actor4, NOW() - INTERVAL 46 MINUTE),
  (99420015, 99400003, @actor1, NOW() - INTERVAL 45 MINUTE),
  (99420016, 99400003, @actor2, NOW() - INTERVAL 44 MINUTE),

  (99420031, 99400015, @actor1, NOW() - INTERVAL 34 MINUTE),
  (99420032, 99400015, @actor2, NOW() - INTERVAL 33 MINUTE),
  (99420033, 99400015, @actor3, NOW() - INTERVAL 32 MINUTE),
  (99420034, 99400015, @actor4, NOW() - INTERVAL 31 MINUTE);

-- ============================================================
-- 4. 收藏数据。
-- ============================================================

INSERT IGNORE INTO community_post_favorite (
  id, post_id, user_id, create_time
)
VALUES
  (99430001, 99400011, @me_user_id, NOW() - INTERVAL 30 MINUTE),
  (99430002, 99400014, @me_user_id, NOW() - INTERVAL 29 MINUTE),
  (99430003, 99400015, @me_user_id, NOW() - INTERVAL 28 MINUTE),

  (99430011, 99400001, @actor1, NOW() - INTERVAL 27 MINUTE),
  (99430012, 99400001, @actor2, NOW() - INTERVAL 26 MINUTE),
  (99430013, 99400002, @actor3, NOW() - INTERVAL 25 MINUTE),
  (99430014, 99400003, @actor4, NOW() - INTERVAL 24 MINUTE);

-- ============================================================
-- 5. 回写帖子计数。
-- ============================================================

UPDATE community_post p
LEFT JOIN (
  SELECT post_id, COUNT(*) AS cnt
  FROM community_post_like
  GROUP BY post_id
) l ON l.post_id = p.id
LEFT JOIN (
  SELECT post_id, COUNT(*) AS cnt
  FROM community_comment
  WHERE is_deleted = 0
  GROUP BY post_id
) c ON c.post_id = p.id
SET
  p.like_count = COALESCE(l.cnt, 0),
  p.comment_count = COALESCE(c.cnt, 0),
  p.update_time = NOW()
WHERE p.id BETWEEN 99400000 AND 99400999;

-- ============================================================
-- 6. 验证数据概览。
-- ============================================================

SELECT 'root_seed_users' AS info, @me_user_id AS root_user_id, @actor1 AS actor1, @actor2 AS actor2, @actor3 AS actor3, @actor4 AS actor4;

SELECT 'community_post' AS table_name, COUNT(*) AS rows_count
FROM community_post
WHERE id BETWEEN 99400000 AND 99400999
UNION ALL
SELECT 'community_comment', COUNT(*)
FROM community_comment
WHERE id BETWEEN 99410000 AND 99410999
UNION ALL
SELECT 'community_post_like', COUNT(*)
FROM community_post_like
WHERE id BETWEEN 99420000 AND 99420999
UNION ALL
SELECT 'community_post_favorite', COUNT(*)
FROM community_post_favorite
WHERE id BETWEEN 99430000 AND 99430999;

SELECT 'my_posts' AS module_name, COUNT(*) AS rows_count
FROM community_post
WHERE user_id = @me_user_id AND is_deleted = 0
UNION ALL
SELECT 'my_liked_posts', COUNT(*)
FROM community_post_like
WHERE user_id = @me_user_id
UNION ALL
SELECT 'my_favorited_posts', COUNT(*)
FROM community_post_favorite
WHERE user_id = @me_user_id
UNION ALL
SELECT 'my_comments', COUNT(*)
FROM community_comment
WHERE user_id = @me_user_id AND is_deleted = 0
UNION ALL
SELECT 'received_likes', COUNT(*)
FROM community_post_like
WHERE post_id IN (
  SELECT id FROM community_post WHERE user_id = @me_user_id AND is_deleted = 0
)
AND user_id <> @me_user_id
UNION ALL
SELECT 'received_top_comments', COUNT(*)
FROM community_comment
WHERE post_id IN (
  SELECT id FROM community_post WHERE user_id = @me_user_id AND is_deleted = 0
)
AND user_id <> @me_user_id
AND parent_comment_id IS NULL
AND is_deleted = 0
UNION ALL
SELECT 'received_replies', COUNT(*)
FROM community_comment
WHERE reply_to_user_id = @me_user_id
AND user_id <> @me_user_id
AND is_deleted = 0
UNION ALL
SELECT 'received_favorites', COUNT(*)
FROM community_post_favorite
WHERE post_id IN (
  SELECT id FROM community_post WHERE user_id = @me_user_id AND is_deleted = 0
)
AND user_id <> @me_user_id;

-- ============================================================
-- 清理脚本：需要删除本批 root 测试数据时，单独执行下面四行。
-- ============================================================
-- DELETE FROM community_post_favorite WHERE id BETWEEN 99430000 AND 99430999;
-- DELETE FROM community_post_like WHERE id BETWEEN 99420000 AND 99420999;
-- DELETE FROM community_comment WHERE id BETWEEN 99410000 AND 99410999;
-- DELETE FROM community_post WHERE id BETWEEN 99400000 AND 99400999;
