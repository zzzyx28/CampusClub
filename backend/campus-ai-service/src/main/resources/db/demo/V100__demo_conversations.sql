-- IDs are stable demo identities owned by user-service; no cross-service FK is created.
INSERT INTO conversation_index(user_id,conversation_uuid,last_message,status,service_user_id)
VALUES (1,'uuid-001-abc','请问算法分享会还能报名吗？',1,NULL),
       (5,'uuid-002-def','图书馆辩论赛几点开始？',2,3),
       (4,'uuid-003-ghi','我的报名被取消了，怎么回事？',3,3),
       (1,'uuid-004-jkl','志愿者活动在哪里集合？',1,NULL);
