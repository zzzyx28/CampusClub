CREATE TABLE outbox_event (
 event_id VARCHAR(36) PRIMARY KEY, event_type VARCHAR(40) NOT NULL, payload LONGTEXT NOT NULL,
 trace_id VARCHAR(64), status VARCHAR(16) NOT NULL DEFAULT 'PENDING', attempts INT NOT NULL DEFAULT 0,
 lease_owner VARCHAR(36), lease_until DATETIME(6), next_attempt DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
 created_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6), last_error VARCHAR(500),
 INDEX idx_outbox_dispatch(status,next_attempt,lease_until)
);
CREATE TABLE registration_operation (
 actor_id BIGINT NOT NULL,operation_key VARCHAR(160) COLLATE utf8mb4_bin NOT NULL,request_json TEXT NOT NULL,result_json LONGTEXT,
 created_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),PRIMARY KEY(actor_id,operation_key)
);
