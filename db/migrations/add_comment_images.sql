-- Add images column to community_comment table for comment image support

ALTER TABLE `community_comment`
  ADD COLUMN `images` JSON NULL COMMENT '评论图片URL列表JSON数组' AFTER `content`;
