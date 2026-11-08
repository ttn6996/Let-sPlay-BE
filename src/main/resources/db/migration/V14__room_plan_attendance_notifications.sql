ALTER TABLE rooms ADD COLUMN IF NOT EXISTS plan_notes VARCHAR(2000);
ALTER TABLE rooms ADD COLUMN IF NOT EXISTS event_latitude DOUBLE PRECISION;
ALTER TABLE rooms ADD COLUMN IF NOT EXISTS event_longitude DOUBLE PRECISION;
ALTER TABLE rooms ADD COLUMN IF NOT EXISTS meetup_latitude DOUBLE PRECISION;
ALTER TABLE rooms ADD COLUMN IF NOT EXISTS meetup_longitude DOUBLE PRECISION;
ALTER TABLE rooms ADD COLUMN IF NOT EXISTS weather_fallback VARCHAR(1000);

CREATE TABLE room_plan_history (
    id VARCHAR(36) PRIMARY KEY,
    room_id VARCHAR(36) NOT NULL REFERENCES rooms(id) ON DELETE CASCADE,
    actor_membership_id VARCHAR(36) NOT NULL,
    actor_display_name VARCHAR(60) NOT NULL,
    change_summary VARCHAR(2000) NOT NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now()
);
CREATE INDEX idx_room_plan_history_room ON room_plan_history(room_id, created_at DESC);

CREATE TABLE room_attendance (
    room_id VARCHAR(36) NOT NULL REFERENCES rooms(id) ON DELETE CASCADE,
    membership_id VARCHAR(36) NOT NULL REFERENCES room_memberships(id) ON DELETE CASCADE,
    attendance_status VARCHAR(20) NOT NULL,
    updated_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    PRIMARY KEY (room_id, membership_id),
    CONSTRAINT ck_room_attendance_status CHECK (attendance_status IN ('GOING','MAYBE','NOT_GOING','ON_THE_WAY','LATE','ARRIVED'))
);

CREATE TABLE room_notifications (
    id VARCHAR(36) PRIMARY KEY,
    room_id VARCHAR(36) NOT NULL REFERENCES rooms(id) ON DELETE CASCADE,
    membership_id VARCHAR(36) NOT NULL REFERENCES room_memberships(id) ON DELETE CASCADE,
    notification_type VARCHAR(40) NOT NULL,
    title VARCHAR(160) NOT NULL,
    body VARCHAR(500) NOT NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    read_at TIMESTAMPTZ
);
CREATE INDEX idx_room_notifications_member ON room_notifications(membership_id, created_at DESC);

CREATE TABLE room_member_preferences (
    room_id VARCHAR(36) NOT NULL REFERENCES rooms(id) ON DELETE CASCADE,
    membership_id VARCHAR(36) NOT NULL REFERENCES room_memberships(id) ON DELETE CASCADE,
    preferences VARCHAR(1000),
    budget_min BIGINT,
    budget_max BIGINT,
    updated_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    PRIMARY KEY (room_id, membership_id),
    CONSTRAINT ck_preferences_budget CHECK (budget_min IS NULL OR budget_max IS NULL OR budget_min <= budget_max)
);

CREATE TABLE room_rides (
    room_id VARCHAR(36) NOT NULL REFERENCES rooms(id) ON DELETE CASCADE,
    membership_id VARCHAR(36) NOT NULL REFERENCES room_memberships(id) ON DELETE CASCADE,
    has_vehicle BOOLEAN NOT NULL DEFAULT FALSE,
    seats_available INTEGER NOT NULL DEFAULT 0,
    pickup_area VARCHAR(160),
    note VARCHAR(300),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    PRIMARY KEY (room_id, membership_id),
    CONSTRAINT ck_room_rides_seats CHECK (seats_available BETWEEN 0 AND 20)
);

CREATE TABLE room_calendar_links (
    room_id VARCHAR(36) NOT NULL REFERENCES rooms(id) ON DELETE CASCADE,
    membership_id VARCHAR(36) NOT NULL REFERENCES room_memberships(id) ON DELETE CASCADE,
    google_event_id VARCHAR(255) NOT NULL,
    updated_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    PRIMARY KEY (room_id, membership_id)
);
