-- 社区帖子收藏表
CREATE TABLE `community_post_favorite` (
  `id` BIGINT NOT NULL COMMENT '主键',
  `post_id` BIGINT NOT NULL COMMENT '帖子ID',
  `user_id` BIGINT NOT NULL COMMENT '收藏用户ID',
  `create_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `is_deleted` TINYINT NOT NULL DEFAULT 0 COMMENT '逻辑删除标志',
  PRIMARY KEY (`id`),
  UNIQUE INDEX `uk_post_favorite_user` (`post_id`, `user_id`),
  INDEX `idx_community_post_favorite_user_id` (`user_id`),
  INDEX `idx_community_post_favorite_create_time` (`create_time`),
  CONSTRAINT `fk_community_post_favorite_post_id` FOREIGN KEY (`post_id`) REFERENCES `community_post` (`id`),
  CONSTRAINT `fk_community_post_favorite_user_id` FOREIGN KEY (`user_id`) REFERENCES `sys_user` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='社区帖子收藏表';
