-- 创建数据库（如果不存在）
CREATE DATABASE IF NOT EXISTS `campusclub`;

USE `campusclub`;

-- 用户表
CREATE TABLE IF NOT EXISTS `user` (
  `id` BIGINT AUTO_INCREMENT PRIMARY KEY,
  `username` VARCHAR(50) NOT NULL UNIQUE,
  `password` VARCHAR(255) NOT NULL,
  `email` VARCHAR(100),
  `phone` VARCHAR(20),
  `real_name` VARCHAR(50) COMMENT '真实姓名',
  `id_card` VARCHAR(30) COMMENT '学号/身份证号',
  `user_type` INT NOT NULL DEFAULT 1 COMMENT '用户类型：1-普通用户，2-社团助手，3-系统管理员',
  `member_level` INT DEFAULT 1 COMMENT '社团活跃度：1-新成员，2-活跃，3-骨干，4-核心，5-荣誉',
  `created_at` DATETIME DEFAULT CURRENT_TIMESTAMP,
  `updated_at` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='用户基础信息表';

-- 社团活动表
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

-- 票档/名额表
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

-- 报名记录表
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
  CONSTRAINT `fk_registration_user` FOREIGN KEY (`user_id`) REFERENCES `user`(`id`) ON DELETE CASCADE,
  CONSTRAINT `fk_registration_activity` FOREIGN KEY (`activity_id`) REFERENCES `activity`(`id`),
  CONSTRAINT `fk_registration_ticket_tier` FOREIGN KEY (`ticket_tier_id`) REFERENCES `ticket_tier`(`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='活动报名记录';

-- 会话索引表
CREATE TABLE IF NOT EXISTS `conversation_index` (
  `id` BIGINT AUTO_INCREMENT PRIMARY KEY,
  `user_id` BIGINT NOT NULL,
  `conversation_uuid` VARCHAR(64) NOT NULL UNIQUE,
  `last_message` TEXT,
  `status` INT DEFAULT 1 COMMENT '会话状态：1-活跃，2-已转人工，3-已关闭',
  `service_user_id` BIGINT COMMENT '处理助手ID，转人工时有效',
  `updated_at` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  CONSTRAINT `fk_conversation_user` FOREIGN KEY (`user_id`) REFERENCES `user`(`id`) ON DELETE CASCADE,
  CONSTRAINT `fk_conversation_service` FOREIGN KEY (`service_user_id`) REFERENCES `user`(`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='用户会话索引信息表';

-- 系统日志表
CREATE TABLE IF NOT EXISTS `system_log` (
  `id` BIGINT AUTO_INCREMENT PRIMARY KEY,
  `operator_id` BIGINT COMMENT '操作者ID',
  `class_name` VARCHAR(255) COMMENT '操作类名',
  `method_name` VARCHAR(100) COMMENT '操作方法名',
  `execution_time` BIGINT COMMENT '操作耗时(ms)',
  `message` TEXT NOT NULL COMMENT '日志消息',
  `created_at` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  INDEX `idx_operator` (`operator_id`),
  INDEX `idx_created_at` (`created_at`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='系统操作日志表';
