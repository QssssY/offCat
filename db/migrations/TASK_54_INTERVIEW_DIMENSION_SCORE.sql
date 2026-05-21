-- 面试维度评分独立表
-- 每次面试产生 6 行维度评分记录（technicalDepth/projectExpression/communication/problemSolving/pressureResistance/jobMatch）
-- 支持成长中心维度雷达图、维度趋势和盲区分析的 SQL 聚合查询

CREATE TABLE IF NOT EXISTS `interview_dimension_score` (
  `id`             BIGINT       NOT NULL COMMENT '主键（雪花ID）',
  `user_id`        BIGINT       NOT NULL COMMENT '用户ID',
  `session_id`     VARCHAR(64)  NOT NULL COMMENT '面试会话ID',
  `dimension_key`  VARCHAR(32)  NOT NULL COMMENT '维度标识: technicalDepth/projectExpression/communication/problemSolving/pressureResistance/jobMatch',
  `score`          INT          NOT NULL COMMENT '维度分数 0-100',
  `comment`        TEXT         NULL     COMMENT '维度评价说明',
  `strengths`      JSON         NULL     COMMENT '加分项列表',
  `weaknesses`     JSON         NULL     COMMENT '扣分项列表',
  `create_time`    DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time`    DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `is_deleted`     TINYINT      NOT NULL DEFAULT 0 COMMENT '逻辑删除标志',
  PRIMARY KEY (`id`),
  INDEX `idx_dim_user` (`user_id`, `is_deleted`),
  INDEX `idx_dim_session` (`session_id`),
  -- 唯一约束不包含 is_deleted：历史记录逻辑删除后不自动复写，重复写入由业务幂等保护处理。
  UNIQUE INDEX `uk_session_dimension` (`session_id`, `dimension_key`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='面试维度评分表';
