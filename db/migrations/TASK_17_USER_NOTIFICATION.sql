-- V1.2 功能三：消息通知
-- 新增用户站内通知表

CREATE TABLE user_notification (
  id BIGINT PRIMARY KEY,
  user_id BIGINT NOT NULL COMMENT '所属用户ID',
  type VARCHAR(32) NOT NULL COMMENT '通知类型: resume/polish/interview/quota/system',
  title VARCHAR(200) NOT NULL COMMENT '通知标题',
  content TEXT COMMENT '通知内容',
  biz_type VARCHAR(64) COMMENT '关联业务类型: resume_diagnosis/resume_polish/mock_interview/quota',
  biz_id VARCHAR(64) COMMENT '关联业务ID',
  read_status TINYINT DEFAULT 0 COMMENT '已读状态: 0未读 1已读',
  read_time DATETIME COMMENT '已读时间',
  create_time DATETIME DEFAULT CURRENT_TIMESTAMP,
  update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  is_deleted TINYINT DEFAULT 0,
  INDEX idx_user_id (user_id),
  INDEX idx_user_read_status (user_id, read_status),
  INDEX idx_user_type (user_id, type)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='用户站内通知表';
