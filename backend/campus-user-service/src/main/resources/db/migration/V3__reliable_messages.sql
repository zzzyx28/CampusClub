CREATE TABLE outbox_event (
 event_id VARCHAR(36) PRIMARY KEY, event_type VARCHAR(40) NOT NULL, payload LONGTEXT NOT NULL,
 trace_id VARCHAR(64), status VARCHAR(16) NOT NULL DEFAULT 'PENDING', attempts INT NOT NULL DEFAULT 0,
 lease_owner VARCHAR(36), lease_until DATETIME(6), next_attempt DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
 created_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6), last_error VARCHAR(500),
 INDEX idx_outbox_dispatch(status,next_attempt,lease_until)
);
CREATE TABLE consumed_event(event_id VARCHAR(36) PRIMARY KEY, consumed_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6));
CREATE TABLE notification_task(event_id VARCHAR(36) PRIMARY KEY,payload LONGTEXT NOT NULL,trace_id VARCHAR(64),status VARCHAR(16) NOT NULL DEFAULT 'PENDING',attempts INT NOT NULL DEFAULT 0,last_error VARCHAR(500),created_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),updated_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),INDEX idx_notification_status(status,created_at));
ALTER TABLE system_log ADD COLUMN trace_id VARCHAR(64),ADD COLUMN source_service VARCHAR(80);
