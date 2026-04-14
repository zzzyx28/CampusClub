-- 创建数据库（如果不存在）
CREATE DATABASE IF NOT EXISTS `smartshop`;

-- 切换数据库
USE `smartshop`;

-- 用户表：user
CREATE TABLE IF NOT EXISTS `user` (
  `id` BIGINT AUTO_INCREMENT PRIMARY KEY,
  `username` VARCHAR(50) NOT NULL UNIQUE,
  `password` VARCHAR(255) NOT NULL,
  `email` VARCHAR(100),
  `phone` VARCHAR(20),
  `real_name` VARCHAR(50) COMMENT '真实姓名',
  `id_card` VARCHAR(30) COMMENT '身份证号',
  `user_type` INT NOT NULL DEFAULT 1 COMMENT '用户类型：1-普通用户，2-客服人员，3-系统管理员',
  `member_level` INT DEFAULT 1 COMMENT '会员等级：1-普通会员，2-白银会员，3-黄金会员，4-白金会员，5-钻石会员',
  `created_at` DATETIME DEFAULT CURRENT_TIMESTAMP,
  `updated_at` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='用户基础信息表';

-- 商品信息表：product
CREATE TABLE IF NOT EXISTS `product` (
  `id` BIGINT AUTO_INCREMENT PRIMARY KEY,
  `spu` VARCHAR(64) NOT NULL COMMENT '商品 SPU',
  `name` VARCHAR(200) NOT NULL COMMENT '商品名称',
  `brand` VARCHAR(80) NOT NULL COMMENT '品牌',
  `category` VARCHAR(80) NOT NULL COMMENT '类目',
  `description` VARCHAR(500) COMMENT '商品描述',
  `cover_image` VARCHAR(255) COMMENT '商品主图',
  `status` TINYINT NOT NULL DEFAULT 1 COMMENT '状态：1-上架，0-下架',
  `created_at` DATETIME DEFAULT CURRENT_TIMESTAMP,
  `updated_at` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  UNIQUE KEY `uk_product_spu` (`spu`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='商品基础信息表';

-- 商品规格表：product_sku（一个商品可有多个规格及价格）
CREATE TABLE IF NOT EXISTS `product_sku` (
  `id` BIGINT AUTO_INCREMENT PRIMARY KEY,
  `product_id` BIGINT NOT NULL COMMENT '关联商品ID',
  `sku` VARCHAR(64) NOT NULL COMMENT '规格 SKU',
  `spec_option` VARCHAR(120) NOT NULL COMMENT '规格选项，如 256G/深空灰',
  `sale_price` DECIMAL(10,2) NOT NULL COMMENT '销售价',
  `market_price` DECIMAL(10,2) COMMENT '划线价',
  `stock` INT NOT NULL DEFAULT 0 COMMENT '库存',
  `is_default` TINYINT NOT NULL DEFAULT 0 COMMENT '是否默认规格：1-是，0-否',
  `created_at` DATETIME DEFAULT CURRENT_TIMESTAMP,
  `updated_at` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  UNIQUE KEY `uk_product_sku_sku` (`sku`),
  INDEX `idx_product_sku_product` (`product_id`),
  CONSTRAINT `fk_product_sku_product` FOREIGN KEY (`product_id`) REFERENCES `product`(`id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='商品规格与价格库存表';

-- 商品订单表：product_order
CREATE TABLE IF NOT EXISTS `product_order` (
  `id` BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '订单编号',
  `user_id` BIGINT NOT NULL,
  `product_id` BIGINT NOT NULL,
  `product_sku_id` BIGINT NOT NULL COMMENT '选购规格ID',
  `quantity` INT NOT NULL DEFAULT 1 COMMENT '购买数量',
  `unit_price` DECIMAL(10,2) NOT NULL COMMENT '下单时单价快照',
  `total_price` DECIMAL(10,2) NOT NULL COMMENT '下单总价快照',
  `status` INT DEFAULT 1 COMMENT '订单状态：1-进行中，2-已取消，3-已完成',
  `created_at` DATETIME DEFAULT CURRENT_TIMESTAMP,
  `updated_at` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  CONSTRAINT `fk_porder_user` FOREIGN KEY (`user_id`) REFERENCES `user`(`id`) ON DELETE CASCADE,
  CONSTRAINT `fk_porder_product` FOREIGN KEY (`product_id`) REFERENCES `product`(`id`),
  CONSTRAINT `fk_porder_product_sku` FOREIGN KEY (`product_sku_id`) REFERENCES `product_sku`(`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='用户商品订单记录';

-- 会话索引表：conversation_index
CREATE TABLE IF NOT EXISTS `conversation_index` (
  `id` BIGINT AUTO_INCREMENT PRIMARY KEY,
  `user_id` BIGINT NOT NULL,
  `conversation_uuid` VARCHAR(64) NOT NULL UNIQUE,
  `last_message` TEXT,
  `status` INT DEFAULT 1 COMMENT '会话状态：1-活跃(active)、2-已转人工(transferred)、3-已关闭(closed)',
  `service_user_id` BIGINT COMMENT '处理客服ID，转人工时有效',
  `updated_at` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  CONSTRAINT `fk_conversation_user` FOREIGN KEY (`user_id`) REFERENCES `user`(`id`) ON DELETE CASCADE,
  CONSTRAINT `fk_conversation_service` FOREIGN KEY (`service_user_id`) REFERENCES `user`(`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='用户会话索引信息表';

-- 系统日志表：system_log
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
