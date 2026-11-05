CREATE TABLE room_availability_slots (
    id VARCHAR(36) PRIMARY KEY,
    room_id VARCHAR(36) NOT NULL REFERENCES rooms(id) ON DELETE CASCADE,
    starts_at TIMESTAMP WITHOUT TIME ZONE NOT NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    CONSTRAINT uq_availability_slot_time UNIQUE (room_id, starts_at)
);
CREATE INDEX idx_availability_slots_room ON room_availability_slots(room_id, starts_at);

CREATE TABLE room_availability_votes (
    id VARCHAR(36) PRIMARY KEY,
    slot_id VARCHAR(36) NOT NULL REFERENCES room_availability_slots(id) ON DELETE CASCADE,
    membership_id VARCHAR(36) NOT NULL REFERENCES room_memberships(id) ON DELETE CASCADE,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    CONSTRAINT uq_availability_vote UNIQUE (slot_id, membership_id)
);
CREATE INDEX idx_availability_votes_slot ON room_availability_votes(slot_id);
