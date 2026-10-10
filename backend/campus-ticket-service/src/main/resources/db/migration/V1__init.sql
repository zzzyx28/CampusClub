CREATE TABLE IF NOT EXISTS `activity` (
  `id` BIGINT AUTO_INCREMENT PRIMARY KEY,
  `activity_code` VARCHAR(64) NOT NULL COMMENT '活动编号',
  `title` VARCHAR(200) NOT NULL COMMENT '活动标题',
  `club_name` VARCHAR(80) NOT NULL COMMENT '主办社团',
  `activity_type` VARCHAR(80) NOT NULL COMMENT '活动类型',
  `description` VARCHAR(500) COMMENT '活动简介',
  `venue` VARCHAR(120) NOT NULL COMMENT '活动地点',
  `registration_start` DATETIME NOT NULL COMMENT '报名开始时间',
  `event_start` DATETIME NOT NULL COMMENT '活动开始时间',
  `event_end` DATETIME COMMENT '活动结束时间',
  `poster_url` VARCHAR(255) COMMENT '活动海报',
  `status` TINYINT NOT NULL DEFAULT 1 COMMENT '状态：1-开放报名，0-已关闭',
  `created_at` DATETIME DEFAULT CURRENT_TIMESTAMP,
  `updated_at` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  UNIQUE KEY `uk_activity_code` (`activity_code`),
  INDEX `idx_activity_registration_start` (`registration_start`),
  INDEX `idx_activity_event_start` (`event_start`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='社团活动表';

CREATE TABLE IF NOT EXISTS `ticket_tier` (
  `id` BIGINT AUTO_INCREMENT PRIMARY KEY,
  `activity_id` BIGINT NOT NULL COMMENT '关联活动ID',
  `ticket_code` VARCHAR(64) NOT NULL COMMENT '票种编码',
  `tier_name` VARCHAR(120) NOT NULL COMMENT '票档名称',
  `price` DECIMAL(10,2) NOT NULL COMMENT '票价，0 表示免费',
  `original_price` DECIMAL(10,2) COMMENT '原价/划线价',
  `capacity` INT NOT NULL DEFAULT 0 COMMENT '剩余名额',
  `is_default` TINYINT NOT NULL DEFAULT 0 COMMENT '是否默认票档：1-是，0-否',
  `created_at` DATETIME DEFAULT CURRENT_TIMESTAMP,
  `updated_at` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  UNIQUE KEY `uk_ticket_code` (`ticket_code`),
  INDEX `idx_ticket_tier_activity` (`activity_id`),
  CONSTRAINT `fk_ticket_tier_activity` FOREIGN KEY (`activity_id`) REFERENCES `activity`(`id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='活动票档与名额表';

CREATE TABLE IF NOT EXISTS `registration` (
  `id` BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '报名编号',
  `user_id` BIGINT NOT NULL,
  `activity_id` BIGINT NOT NULL,
  `ticket_tier_id` BIGINT NOT NULL COMMENT '所选票档ID',
  `ticket_count` INT NOT NULL DEFAULT 1 COMMENT '报名数量',
  `unit_price` DECIMAL(10,2) NOT NULL COMMENT '报名时单价快照',
  `total_price` DECIMAL(10,2) NOT NULL COMMENT '报名总价快照',
  `status` INT DEFAULT 1 COMMENT '状态：1-已报名，2-已取消，3-已参加',
  `created_at` DATETIME DEFAULT CURRENT_TIMESTAMP,
  `updated_at` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  CONSTRAINT `fk_registration_activity` FOREIGN KEY (`activity_id`) REFERENCES `activity`(`id`),
  CONSTRAINT `fk_registration_ticket_tier` FOREIGN KEY (`ticket_tier_id`) REFERENCES `ticket_tier`(`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='活动报名记录';
