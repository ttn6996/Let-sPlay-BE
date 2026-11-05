CREATE TABLE rooms (
    id VARCHAR(36) NOT NULL PRIMARY KEY,
    name VARCHAR(100) NOT NULL,
    invite_code VARCHAR(8) NOT NULL UNIQUE,
    creation_request_id VARCHAR(36) NOT NULL UNIQUE,
    phase VARCHAR(20) NOT NULL,
    status VARCHAR(20) NOT NULL,
    event_date DATE NULL,
    event_time TIME NULL,
    region VARCHAR(120) NULL,
    created_at TIMESTAMP(6) WITH TIME ZONE NOT NULL
);

CREATE TABLE room_memberships (
    id VARCHAR(36) NOT NULL PRIMARY KEY,
    room_id VARCHAR(36) NOT NULL,
    guest_id_hash VARCHAR(64) NOT NULL,
    access_token_hash VARCHAR(64) NOT NULL UNIQUE,
    display_name VARCHAR(60) NOT NULL,
    role VARCHAR(10) NOT NULL,
    created_at TIMESTAMP(6) WITH TIME ZONE NOT NULL,
    CONSTRAINT fk_membership_room FOREIGN KEY (room_id) REFERENCES rooms(id) ON DELETE CASCADE,
    CONSTRAINT uq_membership_guest UNIQUE (room_id, guest_id_hash)
);

CREATE INDEX idx_membership_room ON room_memberships (room_id);
