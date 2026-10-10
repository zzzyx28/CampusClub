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
