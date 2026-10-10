CREATE TABLE IF NOT EXISTS `conversation_index` (
  `id` BIGINT AUTO_INCREMENT PRIMARY KEY,
  `user_id` BIGINT NOT NULL,
  `conversation_uuid` VARCHAR(64) NOT NULL UNIQUE,
  `last_message` TEXT,
  `status` INT DEFAULT 1 COMMENT '会话状态：1-活跃，2-已转人工，3-已关闭',
  `service_user_id` BIGINT COMMENT '处理助手ID，转人工时有效',
  `updated_at` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='用户会话索引信息表';
