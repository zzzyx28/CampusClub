#!/bin/bash
set -euo pipefail
cd "$(dirname "$0")/.."
docker compose exec -T mysql bash -c 'MYSQL_PWD="$MYSQL_ROOT_PASSWORD" exec mysql -uroot campus_user' <<'SQL'
INSERT IGNORE INTO user(id,username,password,user_type,member_level)
WITH RECURSIVE seq AS (SELECT 0 n UNION ALL SELECT n+1 FROM seq WHERE n<99)
SELECT 10000+n,CONCAT('load_',n),'$2b$10$zeHELQBshfyBrAhVn6bEKuA525geAeT20CYLwpmS01hIELCrLruze',1,1 FROM seq;
SQL
docker compose exec -T mysql bash -c 'MYSQL_PWD="$MYSQL_ROOT_PASSWORD" exec mysql -uroot campus_ticket' <<'SQL'
INSERT INTO activity(activity_code,title,club_name,activity_type,venue,registration_start,event_start,status)
VALUES('LOAD-TEST','Load test','Test','Test','Room',NOW(),DATE_ADD(NOW(),INTERVAL 1 DAY),1)
ON DUPLICATE KEY UPDATE status=1;
SET @activity=(SELECT id FROM activity WHERE activity_code='LOAD-TEST');
INSERT INTO ticket_tier(activity_id,ticket_code,tier_name,price,capacity,is_default)
VALUES(@activity,'LOAD-TICKET','Standard',0,10,1)
ON DUPLICATE KEY UPDATE capacity=10;
DELETE FROM registration WHERE activity_id=@activity;
SELECT @activity AS ACTIVITY_ID;
SQL
