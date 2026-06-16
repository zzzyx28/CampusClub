-- 仅用于本地开发环境演示数据，请勿在生产环境执行

INSERT INTO `user` (`username`, `password`, `email`, `phone`, `real_name`, `id_card`, `user_type`, `member_level`)
SELECT * FROM (
    SELECT 'alice123' AS username, '$2a$10$N9qo8uLOickgx2ZMRZoMyeIjZAgcfl7p92ldGxad68LJZdL17lhWy' AS password,
           'alice@campus.edu' AS email, '13800001111' AS phone, 'Alice Wang' AS real_name,
           '2021001001' AS id_card, 1 AS user_type, 1 AS member_level
) AS tmp
WHERE NOT EXISTS (SELECT 1 FROM `user` WHERE username = 'alice123');
