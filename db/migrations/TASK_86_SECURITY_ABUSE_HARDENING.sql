SET NAMES utf8mb4;

CREATE TABLE IF NOT EXISTS `community_image` (
  `id` BIGINT NOT NULL COMMENT '主键',
  `user_id` BIGINT NOT NULL COMMENT '上传用户ID',
  `object_key` VARCHAR(512) NOT NULL COMMENT 'OSS对象键',
  `proxy_url` VARCHAR(600) NOT NULL COMMENT '本站代理访问地址',
  `status` VARCHAR(16) NOT NULL DEFAULT 'uploaded' COMMENT '状态：uploaded-已上传待绑定，bound-已绑定',
  `bound_type` VARCHAR(16) NULL COMMENT '绑定类型：post/comment',
  `bound_id` BIGINT NULL COMMENT '绑定内容ID',
  `bound_time` DATETIME NULL COMMENT '绑定时间',
  `create_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `is_deleted` TINYINT NOT NULL DEFAULT 0 COMMENT '逻辑删除标识',
  PRIMARY KEY (`id`),
  UNIQUE INDEX `uk_community_image_object_key` (`object_key`),
  INDEX `idx_community_image_user_status` (`user_id`, `status`, `create_time`),
  INDEX `idx_community_image_cleanup` (`status`, `is_deleted`, `create_time`),
  INDEX `idx_community_image_bound` (`bound_type`, `bound_id`),
  CONSTRAINT `fk_community_image_user_id` FOREIGN KEY (`user_id`) REFERENCES `sys_user` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='社区图片上传登记表';
