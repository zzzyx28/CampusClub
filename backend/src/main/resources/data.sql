USE `smartshop`;

-- 清空表数据并重置自增计数器
DELETE FROM product_order;
DELETE FROM conversation_index;
DELETE FROM product_sku;
DELETE FROM product;
DELETE FROM user;
ALTER TABLE user AUTO_INCREMENT = 1;
ALTER TABLE product AUTO_INCREMENT = 1;
ALTER TABLE product_sku AUTO_INCREMENT = 1;
ALTER TABLE product_order AUTO_INCREMENT = 1;
ALTER TABLE conversation_index AUTO_INCREMENT = 1;

-- 用户数据
INSERT INTO `user` (`username`, `password`, `email`, `phone`, `real_name`, `id_card`, `user_type`, `member_level`)
VALUES
    ('alice123', 'pass123', 'alice@example.com', '13800001111', 'Alice Wang', '110101199001014321', 1, 1),
    ('bob_admin', 'admin456', 'bob@smartshop.com', '13800002222', 'Bob Li', '110101198805012233', 3, 5),
    ('charlie_cs', 'cs789', 'charlie@support.com', '13800003333', 'Charlie Zhang', '110101197903039876', 2, 2),
    ('david_user', 'david2023', 'david@163.com', '13800004444', 'David Chen', '110101199511110123', 1, 3),
    ('eva_user', 'eva321', 'eva@qq.com', '13800005555', 'Eva Liu', '110101200010106789', 1, 2);

-- 商品（SPU）数据
INSERT INTO `product` (`spu`, `name`, `brand`, `category`, `description`, `cover_image`, `status`)
VALUES
    ('SPU-PHONE-001', '智能手机 Pro Max', '星辰数码', '手机通讯', '旗舰性能，支持双卡双待与高速快充。', 'https://example.com/img/phone-pro-max.jpg', 1),
    ('SPU-WATCH-001', '智能手表 Ultra', '跃动健康', '智能穿戴', '支持多运动模式和心率睡眠监测。', 'https://example.com/img/watch-ultra.jpg', 1),
    ('SPU-TSHIRT-001', '纯棉休闲T恤', '棉语服饰', '服装鞋包', '100% 纯棉，透气舒适。', 'https://example.com/img/tshirt.jpg', 1),
    ('SPU-AIRFRYER-001', '空气炸锅 5L', '清朗家电', '家用电器', '5L 大容量，可视化烹饪。', 'https://example.com/img/airfryer.jpg', 1);

-- 商品规格（SKU）数据：一个 SPU 对应多个规格与价格
INSERT INTO `product_sku` (`product_id`, `sku`, `spec_option`, `sale_price`, `market_price`, `stock`, `is_default`)
VALUES
    (1, 'SKU-PHONE-001-256-GRAY', '256G 深空灰', 6999.00, 7399.00, 120, 1),
    (1, 'SKU-PHONE-001-512-GRAY', '512G 深空灰', 7999.00, 8399.00, 80, 0),
    (1, 'SKU-PHONE-001-512-SILVER', '512G 银色', 8099.00, 8499.00, 60, 0),
    (2, 'SKU-WATCH-001-TI-49', '钛金属 49mm', 2999.00, 3299.00, 90, 1),
    (2, 'SKU-WATCH-001-AL-45', '铝合金 45mm', 2299.00, 2599.00, 150, 0),
    (3, 'SKU-TSHIRT-001-W-L', '白色 L', 99.00, 129.00, 300, 1),
    (3, 'SKU-TSHIRT-001-W-XL', '白色 XL', 99.00, 129.00, 240, 0),
    (3, 'SKU-TSHIRT-001-B-L', '黑色 L', 109.00, 139.00, 200, 0),
    (4, 'SKU-AIRFRYER-001-GREEN', '墨绿 5L', 399.00, 499.00, 180, 1),
    (4, 'SKU-AIRFRYER-001-WHITE', '白色 5L', 429.00, 529.00, 120, 0);

-- 商品订单数据
INSERT INTO `product_order` (`user_id`, `product_id`, `product_sku_id`, `quantity`, `unit_price`, `total_price`, `status`)
VALUES
    (1, 1, 1, 1, 6999.00, 6999.00, 1),
    (4, 1, 2, 1, 7999.00, 7999.00, 1),
    (5, 2, 4, 1, 2999.00, 2999.00, 1),
    (1, 3, 6, 2, 99.00, 198.00, 2),
    (4, 4, 9, 1, 399.00, 399.00, 1),
    (2, 3, 8, 1, 109.00, 109.00, 1),
    (3, 2, 5, 1, 2299.00, 2299.00, 3),
    (1, 4, 10, 1, 429.00, 429.00, 1);

-- 会话索引数据
INSERT INTO `conversation_index` (`user_id`, `conversation_uuid`, `last_message`, `status`, `service_user_id`)
VALUES
    (1, 'uuid-001-abc', '您好，我想改一下收货地址。', 1, NULL),
    (5, 'uuid-002-def', '请问我的订单什么时候发货？', 2, 3),
    (4, 'uuid-003-ghi', '订单被取消了，怎么回事？', 3, 3),
    (1, 'uuid-004-jkl', '这款还有优惠券吗？', 1, NULL);
