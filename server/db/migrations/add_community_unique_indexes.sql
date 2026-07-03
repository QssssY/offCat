-- 为点赞和收藏表添加唯一索引，防止并发操作产生重复记录
-- Issue #4: 点赞/收藏并发竞态条件修复

-- 检查并添加 community_post_like 唯一索引
ALTER TABLE community_post_like ADD UNIQUE INDEX uk_post_user (post_id, user_id);

-- 检查并添加 community_post_favorite 唯一索引
ALTER TABLE community_post_favorite ADD UNIQUE INDEX uk_post_user (post_id, user_id);
