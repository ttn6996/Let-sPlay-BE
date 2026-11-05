CREATE TABLE system_heartbeat (
    id VARCHAR(16) NOT NULL PRIMARY KEY,
    last_ping_at TIMESTAMP(6) WITH TIME ZONE NOT NULL,
    ping_count BIGINT NOT NULL
);
