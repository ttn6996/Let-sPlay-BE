CREATE TABLE room_push_subscriptions (
    id VARCHAR(36) PRIMARY KEY,
    room_id VARCHAR(36) NOT NULL REFERENCES rooms(id) ON DELETE CASCADE,
    membership_id VARCHAR(36) NOT NULL REFERENCES room_memberships(id) ON DELETE CASCADE,
    endpoint TEXT NOT NULL,
    p256dh_key TEXT NOT NULL,
    auth_key VARCHAR(200) NOT NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    CONSTRAINT uq_room_push_endpoint UNIQUE (room_id, endpoint)
);
CREATE INDEX idx_room_push_subscriptions_room ON room_push_subscriptions(room_id);
