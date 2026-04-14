USE `smartshop`;

SET FOREIGN_KEY_CHECKS = 0;
TRUNCATE TABLE product_order;
TRUNCATE TABLE product;
SET FOREIGN_KEY_CHECKS = 1;

INSERT INTO `product`
(`sku`, `name`, `brand`, `category`, `ship_from`, `available_from`, `ship_by`, `spec`, `price`)
VALUES
('REF-SKU01', '降噪耳机 Lite', '云听声学', '数码配件', '上海仓',
 DATE_ADD(CONCAT(CURDATE(), ' 09:00:00'), INTERVAL FLOOR(RAND() * 5 - 2) DAY),
 DATE_ADD(CONCAT(CURDATE(), ' 18:00:00'), INTERVAL FLOOR(RAND() * 5 - 2) DAY), '白色', 299.00),
('REF-SKU02', '智能手环 SE', '跃动健康', '智能穿戴', '广州仓',
 DATE_ADD(CONCAT(CURDATE(), ' 10:30:00'), INTERVAL FLOOR(RAND() * 5 - 2) DAY),
 DATE_ADD(CONCAT(CURDATE(), ' 20:00:00'), INTERVAL FLOOR(RAND() * 5 - 2) DAY), '黑色', 199.00),
('REF-SKU03', '保温杯 500ml', '生活优选', '家居日用', '北京仓',
 DATE_ADD(CONCAT(CURDATE(), ' 08:00:00'), INTERVAL FLOOR(RAND() * 5 - 2) DAY),
 DATE_ADD(CONCAT(CURDATE(), ' 15:00:00'), INTERVAL FLOOR(RAND() * 5 - 2) DAY), '磨砂灰', 89.00),
('REF-SKU04', '燕麦奶1L×6', '谷悦食品', '食品饮料', '杭州仓',
 DATE_ADD(CONCAT(CURDATE(), ' 11:00:00'), INTERVAL FLOOR(RAND() * 5 - 2) DAY),
 DATE_ADD(CONCAT(CURDATE(), ' 14:00:00'), INTERVAL FLOOR(RAND() * 5 - 2) DAY), '箱装', 72.00),
('REF-SKU05', '纯棉T恤', '棉语服饰', '服装鞋包', '深圳仓',
 DATE_ADD(CONCAT(CURDATE(), ' 07:30:00'), INTERVAL FLOOR(RAND() * 5 - 2) DAY),
 DATE_ADD(CONCAT(CURDATE(), ' 19:00:00'), INTERVAL FLOOR(RAND() * 5 - 2) DAY), '灰色 M', 79.00);

INSERT INTO `product_order` (`user_id`, `product_id`, `spec_option`, `status`)
VALUES (1, 1, '白色', 1),
 (2, 2, '黑色', 1),
       (1, 3, '磨砂灰', 2);
