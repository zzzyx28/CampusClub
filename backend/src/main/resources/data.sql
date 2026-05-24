USE `campusclub`;

DELETE FROM registration;
DELETE FROM conversation_index;
DELETE FROM ticket_tier;
DELETE FROM activity;
DELETE FROM user;
ALTER TABLE user AUTO_INCREMENT = 1;
ALTER TABLE activity AUTO_INCREMENT = 1;
ALTER TABLE ticket_tier AUTO_INCREMENT = 1;
ALTER TABLE registration AUTO_INCREMENT = 1;
ALTER TABLE conversation_index AUTO_INCREMENT = 1;

INSERT INTO `user` (`username`, `password`, `email`, `phone`, `real_name`, `id_card`, `user_type`, `member_level`)
VALUES
    ('alice123', 'pass123', 'alice@campus.edu', '13800001111', 'Alice Wang', '2021001001', 1, 1),
    ('bob_admin', 'admin456', 'bob@campusclub.edu', '13800002222', 'Bob Li', '2020002002', 3, 5),
    ('charlie_cs', 'cs789', 'charlie@campusclub.edu', '13800003333', 'Charlie Zhang', '2020003003', 2, 2),
    ('david_user', 'david2023', 'david@163.com', '13800004444', 'David Chen', '2021004004', 1, 3),
    ('eva_user', 'eva321', 'eva@qq.com', '13800005555', 'Eva Liu', '2022005005', 1, 2);

INSERT INTO `activity` (`activity_code`, `title`, `club_name`, `activity_type`, `description`, `venue`, `registration_start`, `event_start`, `event_end`, `poster_url`, `status`)
VALUES
    ('ACT-ALGO-001', '算法分享会：图论入门', '计算机协会', '讲座', '面向新生的图论基础分享，含现场答疑。', '学生活动中心 201', '2026-05-20 09:00:00', '2026-05-25 19:00:00', '2026-05-25 21:00:00', 'https://example.com/img/algo-talk.jpg', 1),
    ('ACT-DEBATE-001', '新生辩论表演赛', '辩论社', '比赛', '新生队伍表演赛，欢迎观赛。', '图书馆报告厅', '2026-05-18 10:00:00', '2026-05-28 14:00:00', '2026-05-28 17:00:00', 'https://example.com/img/debate.jpg', 1),
    ('ACT-VOL-001', '校园开放日志愿者', '志愿者协会', '志愿', '协助校园开放日引导与咨询。', '校门口集合', '2026-05-22 08:00:00', '2026-06-01 08:30:00', '2026-06-01 17:00:00', 'https://example.com/img/volunteer.jpg', 1),
    ('ACT-MUSIC-001', '草地音乐节', '音乐社', '文艺', '户外乐队演出，自由入场部分区域需预约。', '中心草坪', '2026-05-24 12:00:00', '2026-06-05 18:00:00', '2026-06-05 22:00:00', 'https://example.com/img/music.jpg', 1);

INSERT INTO `ticket_tier` (`activity_id`, `ticket_code`, `tier_name`, `price`, `original_price`, `capacity`, `is_default`)
VALUES
    (1, 'TCK-ALGO-STD', '普通票', 0.00, 0.00, 80, 1),
    (1, 'TCK-ALGO-VIP', '前排预留', 0.00, 0.00, 20, 0),
    (2, 'TCK-DEBATE-STD', '观赛票', 0.00, 0.00, 120, 1),
    (2, 'TCK-DEBATE-VIP', '嘉宾席', 10.00, 15.00, 30, 0),
    (3, 'TCK-VOL-AM', '上午班', 0.00, 0.00, 25, 1),
    (3, 'TCK-VOL-PM', '下午班', 0.00, 0.00, 25, 0),
    (4, 'TCK-MUSIC-FREE', '自由区', 0.00, 0.00, 200, 1),
    (4, 'TCK-MUSIC-SEAT', '座位区', 20.00, 30.00, 50, 0);

INSERT INTO `registration` (`user_id`, `activity_id`, `ticket_tier_id`, `ticket_count`, `unit_price`, `total_price`, `status`)
VALUES
    (1, 1, 1, 1, 0.00, 0.00, 1),
    (4, 1, 2, 1, 0.00, 0.00, 1),
    (5, 2, 3, 1, 0.00, 0.00, 1),
    (1, 3, 5, 1, 0.00, 0.00, 2),
    (4, 4, 7, 2, 0.00, 0.00, 1),
    (2, 2, 4, 1, 10.00, 10.00, 1),
    (3, 4, 8, 1, 20.00, 20.00, 3),
    (1, 4, 8, 1, 20.00, 20.00, 1);

INSERT INTO `conversation_index` (`user_id`, `conversation_uuid`, `last_message`, `status`, `service_user_id`)
VALUES
    (1, 'uuid-001-abc', '请问算法分享会还能报名吗？', 1, NULL),
    (5, 'uuid-002-def', '图书馆辩论赛几点开始？', 2, 3),
    (4, 'uuid-003-ghi', '我的报名被取消了，怎么回事？', 3, 3),
    (1, 'uuid-004-jkl', '志愿者活动在哪里集合？', 1, NULL);
