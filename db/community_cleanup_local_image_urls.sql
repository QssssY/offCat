-- ============================================================
-- 社区旧本地图片 URL 清理脚本
--
-- 用途：
-- - 清理已经写入数据库的 /uploads/community/*.png 等本机静态路径。
-- - 将帖子和评论 images JSON 数组中的旧本地路径替换为当前公网占位图。
--
-- 说明：
-- - 当前脚本适用于 MySQL 8 JSON_TABLE。
-- - 执行前可先运行下方 SELECT 检查命中行数。
-- - 本脚本不删除帖子、评论、点赞、收藏等业务数据。
-- ============================================================

SET @community_test_image_url := 'https://ts3.tc.mm.bing.net/th/id/OIP-C.TmvkuikpStxy5wKWiziR1AHaE7?rs=1&pid=ImgDetMain&o=7&rm=3';

-- 执行前检查：如果这两个查询还有结果，说明数据库里仍有旧本地图片 URL。
SELECT id, images
FROM community_post
WHERE images IS NOT NULL
  AND JSON_SEARCH(images, 'one', '/uploads/community/%') IS NOT NULL;

SELECT id, images
FROM community_comment
WHERE images IS NOT NULL
  AND JSON_SEARCH(images, 'one', '/uploads/community/%') IS NOT NULL;

-- 清理帖子图片 JSON 数组：只替换 /uploads/community/ 开头的旧路径，保留已经是外链的图片。
UPDATE community_post p
JOIN (
  SELECT p2.id,
         JSON_ARRAYAGG(
           CASE
             WHEN jt.image_url LIKE '/uploads/community/%' THEN @community_test_image_url
             ELSE jt.image_url
           END
         ) AS fixed_images
  FROM community_post p2
  JOIN JSON_TABLE(p2.images, '$[*]' COLUMNS (image_url VARCHAR(1000) PATH '$')) jt
  WHERE p2.images IS NOT NULL
    AND JSON_SEARCH(p2.images, 'one', '/uploads/community/%') IS NOT NULL
  GROUP BY p2.id
) x ON x.id = p.id
SET p.images = x.fixed_images;

-- 清理评论图片 JSON 数组：只替换 /uploads/community/ 开头的旧路径，保留已经是外链的图片。
UPDATE community_comment c
JOIN (
  SELECT c2.id,
         JSON_ARRAYAGG(
           CASE
             WHEN jt.image_url LIKE '/uploads/community/%' THEN @community_test_image_url
             ELSE jt.image_url
           END
         ) AS fixed_images
  FROM community_comment c2
  JOIN JSON_TABLE(c2.images, '$[*]' COLUMNS (image_url VARCHAR(1000) PATH '$')) jt
  WHERE c2.images IS NOT NULL
    AND JSON_SEARCH(c2.images, 'one', '/uploads/community/%') IS NOT NULL
  GROUP BY c2.id
) x ON x.id = c.id
SET c.images = x.fixed_images;

-- 执行后检查：应返回 0。
SELECT COUNT(*) AS remaining_post_local_image_rows
FROM community_post
WHERE images IS NOT NULL
  AND JSON_SEARCH(images, 'one', '/uploads/community/%') IS NOT NULL;

SELECT COUNT(*) AS remaining_comment_local_image_rows
FROM community_comment
WHERE images IS NOT NULL
  AND JSON_SEARCH(images, 'one', '/uploads/community/%') IS NOT NULL;
