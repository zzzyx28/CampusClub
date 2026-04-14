-- 从旧版「航班 / flight_order」结构迁移到「商品 / product_order」
-- 目标库名：smartshop（与当前 schema.sql、推荐 JDBC 配置一致）。
-- 若你的旧数据仍在库 smartair，可将本文件首行改为 USE `smartair` 执行删表与建表，再 mysqldump 迁到 smartshop，或统一改连接串指向原库。

USE `smartshop`;

SET FOREIGN_KEY_CHECKS = 0;

DROP TABLE IF EXISTS `flight_order`;
DROP TABLE IF EXISTS `flight`;

SET FOREIGN_KEY_CHECKS = 1;

CREATE TABLE IF NOT EXISTS `product` (
  `id` BIGINT AUTO_INCREMENT PRIMARY KEY,
  `sku` VARCHAR(64) NOT NULL COMMENT '商品 SKU',
  `name` VARCHAR(200) NOT NULL COMMENT '商品名称',
  `brand` VARCHAR(80) NOT NULL COMMENT '品牌',
  `category` VARCHAR(80) NOT NULL COMMENT '类目',
  `ship_from` VARCHAR(80) NOT NULL COMMENT '发货地/仓库',
  `available_from` DATETIME NOT NULL COMMENT '开售/可下单起始时间',
  `ship_by` DATETIME NOT NULL COMMENT '预计发货时间',
  `spec` VARCHAR(120) COMMENT '规格简述',
  `price` DECIMAL(10,2) NOT NULL COMMENT '单价',
  `created_at` DATETIME DEFAULT CURRENT_TIMESTAMP,
  `updated_at` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  UNIQUE KEY `uk_product_sku` (`sku`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='商品基础信息表';

CREATE TABLE IF NOT EXISTS `product_order` (
  `id` BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '订单编号',
  `user_id` BIGINT NOT NULL,
  `product_id` BIGINT NOT NULL,
  `spec_option` VARCHAR(64) COMMENT '选购规格（如颜色/尺码）',
  `status` INT DEFAULT 1 COMMENT '订单状态：1-已支付待发货，2-已取消',
  `created_at` DATETIME DEFAULT CURRENT_TIMESTAMP,
  `updated_at` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  CONSTRAINT `fk_porder_user` FOREIGN KEY (`user_id`) REFERENCES `user`(`id`) ON DELETE CASCADE,
  CONSTRAINT `fk_porder_product` FOREIGN KEY (`product_id`) REFERENCES `product`(`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='用户商品订单记录';

-- 完成后请执行 data.sql 导入示例数据
