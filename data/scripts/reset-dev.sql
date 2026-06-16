-- 清空本地演示数据（保留 admin 系统账号）
-- 用法: mysql -uroot -p campusclub < data/scripts/reset-dev.sql

SET FOREIGN_KEY_CHECKS = 0;

TRUNCATE TABLE `registration`;
TRUNCATE TABLE `conversation_index`;
TRUNCATE TABLE `ticket_tier`;
TRUNCATE TABLE `activity`;
DELETE FROM `user` WHERE username <> 'admin';

SET FOREIGN_KEY_CHECKS = 1;
