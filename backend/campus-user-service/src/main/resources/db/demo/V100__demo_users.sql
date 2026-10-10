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

