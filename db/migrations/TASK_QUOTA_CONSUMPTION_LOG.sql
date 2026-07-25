SET NAMES utf8mb4;

-- ============================================================
-- 用户额度消费记录表迁移脚本
-- 功能：新增 user_quota_consumption_log 表，记录每次额度扣减和退款
-- ============================================================

-- 用户额度消费记录表
-- 记录每一次额度扣减和退款，支持消费溯源
CREATE TABLE IF NOT EXISTS user_quota_consumption_log (
    id              BIGINT          NOT NULL COMMENT '雪花ID主键',
    user_id         BIGINT          NOT NULL COMMENT '用户ID',
    quota_type      VARCHAR(32)     NOT NULL COMMENT '额度类型: INTERVIEW/RESUME/POLISH/JD_MATCH/TEMPLATE/OFFER',
    change_amount   INT             NOT NULL COMMENT '变动数量 正数=消耗 负数=退款',
    balance_after   INT             DEFAULT NULL COMMENT '变动后额度余额',
    source          VARCHAR(32)     NOT NULL COMMENT '扣减来源: FREE/VIP_DAILY/VIP_CYCLE',
    billing_source  VARCHAR(32)     DEFAULT NULL COMMENT 'AI计费来源: PLATFORM/USER_CUSTOM/PLATFORM_FALLBACK',
    business_id     BIGINT          DEFAULT NULL COMMENT '关联业务ID',
    business_type   VARCHAR(32)     DEFAULT NULL COMMENT '业务类型标识',
    description     VARCHAR(255)    DEFAULT NULL COMMENT '操作描述',
    create_time     DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '记录时间',
    update_time     DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    is_deleted      TINYINT         NOT NULL DEFAULT 0 COMMENT '逻辑删除',
    PRIMARY KEY (id),
    INDEX idx_user_type_time (user_id, quota_type, create_time),
    INDEX idx_user_time (user_id, create_time),
    INDEX idx_create_time (create_time)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='用户额度消费记录表';

-- 消费记录保留天数配置（默认 90 天）
INSERT INTO sys_config (id, config_key, config_value, description, create_time, update_time, is_deleted)
VALUES (
    FLOOR(RAND() * 9000000000000000000 + 1000000000000000000),
    'consumption_log_retention_days',
    '90',
    '消费记录保留天数，定时任务据此清理过期记录',
    NOW(),
    NOW(),
    0
) ON DUPLICATE KEY UPDATE config_value = config_value;
