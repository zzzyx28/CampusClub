-- User IDs mirror the deterministic user-service demo contract; there is intentionally no cross-database FK.
INSERT INTO registration(user_id,activity_id,ticket_tier_id,ticket_count,unit_price,total_price,status)
SELECT 1,a.id,t.id,1,t.price,t.price,1 FROM activity a JOIN ticket_tier t ON t.activity_id=a.id
WHERE a.activity_code='ACT-ALGO-001' AND t.ticket_code='TCK-ALGO-STD'
AND NOT EXISTS(SELECT 1 FROM registration r WHERE r.user_id=1 AND r.activity_id=a.id AND r.status=1);
INSERT INTO registration(user_id,activity_id,ticket_tier_id,ticket_count,unit_price,total_price,status)
SELECT 4,a.id,t.id,1,t.price,t.price,1 FROM activity a JOIN ticket_tier t ON t.activity_id=a.id
WHERE a.activity_code='ACT-ALGO-001' AND t.ticket_code='TCK-ALGO-VIP'
AND NOT EXISTS(SELECT 1 FROM registration r WHERE r.user_id=4 AND r.activity_id=a.id AND r.status=1);
INSERT INTO registration(user_id,activity_id,ticket_tier_id,ticket_count,unit_price,total_price,status)
SELECT 5,a.id,t.id,1,t.price,t.price,1 FROM activity a JOIN ticket_tier t ON t.activity_id=a.id
WHERE a.activity_code='ACT-DEBATE-001' AND t.ticket_code='TCK-DEBATE-STD'
AND NOT EXISTS(SELECT 1 FROM registration r WHERE r.user_id=5 AND r.activity_id=a.id AND r.status=1);
