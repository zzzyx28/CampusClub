-- ========== 活动 ==========
INSERT INTO `activity` (`activity_code`, `title`, `club_name`, `activity_type`, `description`, `venue`,
                        `registration_start`, `event_start`, `event_end`, `poster_url`, `status`)
SELECT 'ACT-ALGO-001', '算法分享会：图论入门', '计算机协会', '讲座',
       '面向新生的图论基础分享，含现场答疑。', '学生活动中心 201',
       DATE_ADD(NOW(), INTERVAL 1 DAY), DATE_ADD(NOW(), INTERVAL 2 DAY), DATE_ADD(NOW(), INTERVAL 3 DAY),
       'https://example.com/img/algo-talk.jpg', 1
WHERE NOT EXISTS (SELECT 1 FROM `activity` WHERE activity_code = 'ACT-ALGO-001');

INSERT INTO `activity` (`activity_code`, `title`, `club_name`, `activity_type`, `description`, `venue`,
                        `registration_start`, `event_start`, `event_end`, `poster_url`, `status`)
SELECT 'ACT-DEBATE-001', '新生辩论表演赛', '辩论社', '比赛',
       '新生队伍表演赛，欢迎观赛。', '图书馆报告厅',
       DATE_ADD(NOW(), INTERVAL 4 DAY), DATE_ADD(NOW(), INTERVAL 5 DAY), DATE_ADD(NOW(), INTERVAL 6 DAY),
       'https://example.com/img/debate.jpg', 1
WHERE NOT EXISTS (SELECT 1 FROM `activity` WHERE activity_code = 'ACT-DEBATE-001');

INSERT INTO `activity` (`activity_code`, `title`, `club_name`, `activity_type`, `description`, `venue`,
                        `registration_start`, `event_start`, `event_end`, `poster_url`, `status`)
SELECT 'ACT-VOL-001', '校园开放日志愿者', '志愿者协会', '志愿',
       '协助校园开放日引导与咨询。', '校门口集合',
       DATE_ADD(NOW(), INTERVAL 7 DAY), DATE_ADD(NOW(), INTERVAL 8 DAY), DATE_ADD(NOW(), INTERVAL 9 DAY),
       'https://example.com/img/volunteer.jpg', 1
WHERE NOT EXISTS (SELECT 1 FROM `activity` WHERE activity_code = 'ACT-VOL-001');

INSERT INTO `activity` (`activity_code`, `title`, `club_name`, `activity_type`, `description`, `venue`,
                        `registration_start`, `event_start`, `event_end`, `poster_url`, `status`)
SELECT 'ACT-MUSIC-001', '草地音乐节', '音乐社', '文艺',
       '户外乐队演出，自由入场部分区域需预约。', '中心草坪',
       DATE_ADD(NOW(), INTERVAL 10 DAY), DATE_ADD(NOW(), INTERVAL 11 DAY), DATE_ADD(NOW(), INTERVAL 12 DAY),
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

