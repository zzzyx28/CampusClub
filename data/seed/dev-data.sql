-- CampusClub 本地开发演示数据（幂等，可重复执行）
-- 密码均为 BCrypt 哈希；明文见 data/README.md

-- ========== 用户 ==========
INSERT INTO `user` (`username`, `password`, `email`, `phone`, `real_name`, `id_card`, `user_type`, `member_level`)
SELECT 'alice123', '$2b$10$zeHELQBshfyBrAhVn6bEKuA525geAeT20CYLwpmS01hIELCrLruze',
       'alice@campus.edu', '13800001111', 'Alice Wang', '2021001001', 1, 1
WHERE NOT EXISTS (SELECT 1 FROM `user` WHERE username = 'alice123');

INSERT INTO `user` (`username`, `password`, `email`, `phone`, `real_name`, `id_card`, `user_type`, `member_level`)
SELECT 'bob_admin', '$2b$10$Hda7Fw/bSwJsRmaDj9nAuu60Unp.5x1aLOBqov5uu2PR2DyIBHky6',
       'bob@campusclub.edu', '13800002222', 'Bob Li', '2020002002', 3, 5
WHERE NOT EXISTS (SELECT 1 FROM `user` WHERE username = 'bob_admin');

INSERT INTO `user` (`username`, `password`, `email`, `phone`, `real_name`, `id_card`, `user_type`, `member_level`)
SELECT 'charlie_cs', '$2b$10$RifpfkVCJJfwfYRva55BluKHeRo95XyMZZygEgltkq7q1rI/KTbnq',
       'charlie@campusclub.edu', '13800003333', 'Charlie Zhang', '2020003003', 2, 2
WHERE NOT EXISTS (SELECT 1 FROM `user` WHERE username = 'charlie_cs');

INSERT INTO `user` (`username`, `password`, `email`, `phone`, `real_name`, `id_card`, `user_type`, `member_level`)
SELECT 'david_user', '$2b$10$IuJAyTTFnbsBw3ny5QdU7.HB0GLrWJnerkzXJRsw3qISbXjjd3X5y',
       'david@163.com', '13800004444', 'David Chen', '2021004004', 1, 3
WHERE NOT EXISTS (SELECT 1 FROM `user` WHERE username = 'david_user');

INSERT INTO `user` (`username`, `password`, `email`, `phone`, `real_name`, `id_card`, `user_type`, `member_level`)
SELECT 'eva_user', '$2b$10$0XbPDIi7Y3ZFUg27/dnR.OBaY5aNsYWyUOriKznzTJ1fn2INDRno2',
       'eva@qq.com', '13800005555', 'Eva Liu', '2022005005', 1, 2
WHERE NOT EXISTS (SELECT 1 FROM `user` WHERE username = 'eva_user');

-- ========== 活动 ==========
INSERT INTO `activity` (`activity_code`, `title`, `club_name`, `activity_type`, `description`, `venue`,
                        `registration_start`, `event_start`, `event_end`, `poster_url`, `status`)
SELECT 'ACT-ALGO-001', '算法分享会：图论入门', '计算机协会', '讲座',
       '面向新生的图论基础分享，含现场答疑。', '学生活动中心 201',
       '2026-05-20 09:00:00', '2026-05-25 19:00:00', '2026-05-25 21:00:00',
       'https://example.com/img/algo-talk.jpg', 1
WHERE NOT EXISTS (SELECT 1 FROM `activity` WHERE activity_code = 'ACT-ALGO-001');

INSERT INTO `activity` (`activity_code`, `title`, `club_name`, `activity_type`, `description`, `venue`,
                        `registration_start`, `event_start`, `event_end`, `poster_url`, `status`)
SELECT 'ACT-DEBATE-001', '新生辩论表演赛', '辩论社', '比赛',
       '新生队伍表演赛，欢迎观赛。', '图书馆报告厅',
       '2026-05-18 10:00:00', '2026-05-28 14:00:00', '2026-05-28 17:00:00',
       'https://example.com/img/debate.jpg', 1
WHERE NOT EXISTS (SELECT 1 FROM `activity` WHERE activity_code = 'ACT-DEBATE-001');

INSERT INTO `activity` (`activity_code`, `title`, `club_name`, `activity_type`, `description`, `venue`,
                        `registration_start`, `event_start`, `event_end`, `poster_url`, `status`)
SELECT 'ACT-VOL-001', '校园开放日志愿者', '志愿者协会', '志愿',
       '协助校园开放日引导与咨询。', '校门口集合',
       '2026-05-22 08:00:00', '2026-06-01 08:30:00', '2026-06-01 17:00:00',
       'https://example.com/img/volunteer.jpg', 1
WHERE NOT EXISTS (SELECT 1 FROM `activity` WHERE activity_code = 'ACT-VOL-001');

INSERT INTO `activity` (`activity_code`, `title`, `club_name`, `activity_type`, `description`, `venue`,
                        `registration_start`, `event_start`, `event_end`, `poster_url`, `status`)
SELECT 'ACT-MUSIC-001', '草地音乐节', '音乐社', '文艺',
       '户外乐队演出，自由入场部分区域需预约。', '中心草坪',
       '2026-05-24 12:00:00', '2026-06-05 18:00:00', '2026-06-05 22:00:00',
       'https://example.com/img/music.jpg', 1
WHERE NOT EXISTS (SELECT 1 FROM `activity` WHERE activity_code = 'ACT-MUSIC-001');

-- ========== 票档 ==========
INSERT INTO `ticket_tier` (`activity_id`, `ticket_code`, `tier_name`, `price`, `original_price`, `capacity`, `is_default`)
SELECT a.id, 'TCK-ALGO-STD', '普通票', 0.00, 0.00, 80, 1
FROM `activity` a
WHERE a.activity_code = 'ACT-ALGO-001'
  AND NOT EXISTS (SELECT 1 FROM `ticket_tier` t WHERE t.ticket_code = 'TCK-ALGO-STD');

INSERT INTO `ticket_tier` (`activity_id`, `ticket_code`, `tier_name`, `price`, `original_price`, `capacity`, `is_default`)
SELECT a.id, 'TCK-ALGO-VIP', '前排预留', 0.00, 0.00, 20, 0
FROM `activity` a
WHERE a.activity_code = 'ACT-ALGO-001'
  AND NOT EXISTS (SELECT 1 FROM `ticket_tier` t WHERE t.ticket_code = 'TCK-ALGO-VIP');

INSERT INTO `ticket_tier` (`activity_id`, `ticket_code`, `tier_name`, `price`, `original_price`, `capacity`, `is_default`)
SELECT a.id, 'TCK-DEBATE-STD', '观赛票', 0.00, 0.00, 120, 1
FROM `activity` a
WHERE a.activity_code = 'ACT-DEBATE-001'
  AND NOT EXISTS (SELECT 1 FROM `ticket_tier` t WHERE t.ticket_code = 'TCK-DEBATE-STD');

INSERT INTO `ticket_tier` (`activity_id`, `ticket_code`, `tier_name`, `price`, `original_price`, `capacity`, `is_default`)
SELECT a.id, 'TCK-DEBATE-VIP', '嘉宾席', 10.00, 15.00, 30, 0
FROM `activity` a
WHERE a.activity_code = 'ACT-DEBATE-001'
  AND NOT EXISTS (SELECT 1 FROM `ticket_tier` t WHERE t.ticket_code = 'TCK-DEBATE-VIP');

INSERT INTO `ticket_tier` (`activity_id`, `ticket_code`, `tier_name`, `price`, `original_price`, `capacity`, `is_default`)
SELECT a.id, 'TCK-VOL-AM', '上午班', 0.00, 0.00, 25, 1
FROM `activity` a
WHERE a.activity_code = 'ACT-VOL-001'
  AND NOT EXISTS (SELECT 1 FROM `ticket_tier` t WHERE t.ticket_code = 'TCK-VOL-AM');

INSERT INTO `ticket_tier` (`activity_id`, `ticket_code`, `tier_name`, `price`, `original_price`, `capacity`, `is_default`)
SELECT a.id, 'TCK-VOL-PM', '下午班', 0.00, 0.00, 25, 0
FROM `activity` a
WHERE a.activity_code = 'ACT-VOL-001'
  AND NOT EXISTS (SELECT 1 FROM `ticket_tier` t WHERE t.ticket_code = 'TCK-VOL-PM');

INSERT INTO `ticket_tier` (`activity_id`, `ticket_code`, `tier_name`, `price`, `original_price`, `capacity`, `is_default`)
SELECT a.id, 'TCK-MUSIC-FREE', '自由区', 0.00, 0.00, 200, 1
FROM `activity` a
WHERE a.activity_code = 'ACT-MUSIC-001'
  AND NOT EXISTS (SELECT 1 FROM `ticket_tier` t WHERE t.ticket_code = 'TCK-MUSIC-FREE');

INSERT INTO `ticket_tier` (`activity_id`, `ticket_code`, `tier_name`, `price`, `original_price`, `capacity`, `is_default`)
SELECT a.id, 'TCK-MUSIC-SEAT', '座位区', 20.00, 30.00, 50, 0
FROM `activity` a
WHERE a.activity_code = 'ACT-MUSIC-001'
  AND NOT EXISTS (SELECT 1 FROM `ticket_tier` t WHERE t.ticket_code = 'TCK-MUSIC-SEAT');

-- ========== 报名 ==========
INSERT INTO `registration` (`user_id`, `activity_id`, `ticket_tier_id`, `ticket_count`, `unit_price`, `total_price`, `status`)
SELECT u.id, a.id, t.id, 1, 0.00, 0.00, 1
FROM `user` u, `activity` a, `ticket_tier` t
WHERE u.username = 'alice123' AND a.activity_code = 'ACT-ALGO-001' AND t.ticket_code = 'TCK-ALGO-STD'
  AND NOT EXISTS (
    SELECT 1 FROM `registration` r
    WHERE r.user_id = u.id AND r.activity_id = a.id AND r.ticket_tier_id = t.id
  );

INSERT INTO `registration` (`user_id`, `activity_id`, `ticket_tier_id`, `ticket_count`, `unit_price`, `total_price`, `status`)
SELECT u.id, a.id, t.id, 1, 0.00, 0.00, 1
FROM `user` u, `activity` a, `ticket_tier` t
WHERE u.username = 'david_user' AND a.activity_code = 'ACT-ALGO-001' AND t.ticket_code = 'TCK-ALGO-VIP'
  AND NOT EXISTS (
    SELECT 1 FROM `registration` r
    WHERE r.user_id = u.id AND r.activity_id = a.id AND r.ticket_tier_id = t.id
  );

INSERT INTO `registration` (`user_id`, `activity_id`, `ticket_tier_id`, `ticket_count`, `unit_price`, `total_price`, `status`)
SELECT u.id, a.id, t.id, 1, 0.00, 0.00, 1
FROM `user` u, `activity` a, `ticket_tier` t
WHERE u.username = 'eva_user' AND a.activity_code = 'ACT-DEBATE-001' AND t.ticket_code = 'TCK-DEBATE-STD'
  AND NOT EXISTS (
    SELECT 1 FROM `registration` r
    WHERE r.user_id = u.id AND r.activity_id = a.id AND r.ticket_tier_id = t.id
  );

INSERT INTO `registration` (`user_id`, `activity_id`, `ticket_tier_id`, `ticket_count`, `unit_price`, `total_price`, `status`)
SELECT u.id, a.id, t.id, 1, 0.00, 0.00, 2
FROM `user` u, `activity` a, `ticket_tier` t
WHERE u.username = 'alice123' AND a.activity_code = 'ACT-VOL-001' AND t.ticket_code = 'TCK-VOL-AM'
  AND NOT EXISTS (
    SELECT 1 FROM `registration` r
    WHERE r.user_id = u.id AND r.activity_id = a.id AND r.ticket_tier_id = t.id
  );

INSERT INTO `registration` (`user_id`, `activity_id`, `ticket_tier_id`, `ticket_count`, `unit_price`, `total_price`, `status`)
SELECT u.id, a.id, t.id, 2, 0.00, 0.00, 1
FROM `user` u, `activity` a, `ticket_tier` t
WHERE u.username = 'david_user' AND a.activity_code = 'ACT-MUSIC-001' AND t.ticket_code = 'TCK-MUSIC-FREE'
  AND NOT EXISTS (
    SELECT 1 FROM `registration` r
    WHERE r.user_id = u.id AND r.activity_id = a.id AND r.ticket_tier_id = t.id
  );

INSERT INTO `registration` (`user_id`, `activity_id`, `ticket_tier_id`, `ticket_count`, `unit_price`, `total_price`, `status`)
SELECT u.id, a.id, t.id, 1, 10.00, 10.00, 1
FROM `user` u, `activity` a, `ticket_tier` t
WHERE u.username = 'bob_admin' AND a.activity_code = 'ACT-DEBATE-001' AND t.ticket_code = 'TCK-DEBATE-VIP'
  AND NOT EXISTS (
    SELECT 1 FROM `registration` r
    WHERE r.user_id = u.id AND r.activity_id = a.id AND r.ticket_tier_id = t.id
  );

INSERT INTO `registration` (`user_id`, `activity_id`, `ticket_tier_id`, `ticket_count`, `unit_price`, `total_price`, `status`)
SELECT u.id, a.id, t.id, 1, 20.00, 20.00, 3
FROM `user` u, `activity` a, `ticket_tier` t
WHERE u.username = 'charlie_cs' AND a.activity_code = 'ACT-MUSIC-001' AND t.ticket_code = 'TCK-MUSIC-SEAT'
  AND NOT EXISTS (
    SELECT 1 FROM `registration` r
    WHERE r.user_id = u.id AND r.activity_id = a.id AND r.ticket_tier_id = t.id
  );

INSERT INTO `registration` (`user_id`, `activity_id`, `ticket_tier_id`, `ticket_count`, `unit_price`, `total_price`, `status`)
SELECT u.id, a.id, t.id, 1, 20.00, 20.00, 1
FROM `user` u, `activity` a, `ticket_tier` t
WHERE u.username = 'alice123' AND a.activity_code = 'ACT-MUSIC-001' AND t.ticket_code = 'TCK-MUSIC-SEAT'
  AND NOT EXISTS (
    SELECT 1 FROM `registration` r
    WHERE r.user_id = u.id AND r.activity_id = a.id AND r.ticket_tier_id = t.id
  );

-- ========== 会话索引 ==========
INSERT INTO `conversation_index` (`user_id`, `conversation_uuid`, `last_message`, `status`, `service_user_id`)
SELECT u.id, 'uuid-001-abc', '请问算法分享会还能报名吗？', 1, NULL
FROM `user` u
WHERE u.username = 'alice123'
  AND NOT EXISTS (SELECT 1 FROM `conversation_index` c WHERE c.conversation_uuid = 'uuid-001-abc');

INSERT INTO `conversation_index` (`user_id`, `conversation_uuid`, `last_message`, `status`, `service_user_id`)
SELECT u.id, 'uuid-002-def', '图书馆辩论赛几点开始？', 2, s.id
FROM `user` u, `user` s
WHERE u.username = 'eva_user' AND s.username = 'charlie_cs'
  AND NOT EXISTS (SELECT 1 FROM `conversation_index` c WHERE c.conversation_uuid = 'uuid-002-def');

INSERT INTO `conversation_index` (`user_id`, `conversation_uuid`, `last_message`, `status`, `service_user_id`)
SELECT u.id, 'uuid-003-ghi', '我的报名被取消了，怎么回事？', 3, s.id
FROM `user` u, `user` s
WHERE u.username = 'david_user' AND s.username = 'charlie_cs'
  AND NOT EXISTS (SELECT 1 FROM `conversation_index` c WHERE c.conversation_uuid = 'uuid-003-ghi');

INSERT INTO `conversation_index` (`user_id`, `conversation_uuid`, `last_message`, `status`, `service_user_id`)
SELECT u.id, 'uuid-004-jkl', '志愿者活动在哪里集合？', 1, NULL
FROM `user` u
WHERE u.username = 'alice123'
  AND NOT EXISTS (SELECT 1 FROM `conversation_index` c WHERE c.conversation_uuid = 'uuid-004-jkl');
