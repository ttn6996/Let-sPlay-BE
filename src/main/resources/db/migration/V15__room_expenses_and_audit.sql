CREATE TABLE room_expenses (
    id VARCHAR(36) PRIMARY KEY,
    room_id VARCHAR(36) NOT NULL REFERENCES rooms(id) ON DELETE CASCADE,
    title VARCHAR(160) NOT NULL,
    total_amount BIGINT NOT NULL CHECK (total_amount > 0),
    note VARCHAR(1000),
    created_by_membership_id VARCHAR(36) NOT NULL,
    created_by_display_name VARCHAR(60) NOT NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT now()
);
CREATE INDEX idx_room_expenses_room ON room_expenses(room_id, created_at DESC);

CREATE TABLE room_expense_payers (
    expense_id VARCHAR(36) NOT NULL REFERENCES room_expenses(id) ON DELETE CASCADE,
    membership_id VARCHAR(36) NOT NULL,
    display_name VARCHAR(60) NOT NULL,
    amount BIGINT NOT NULL CHECK (amount > 0),
    PRIMARY KEY (expense_id, membership_id)
);

CREATE TABLE room_expense_shares (
    expense_id VARCHAR(36) NOT NULL REFERENCES room_expenses(id) ON DELETE CASCADE,
    membership_id VARCHAR(36) NOT NULL,
    display_name VARCHAR(60) NOT NULL,
    amount BIGINT NOT NULL CHECK (amount >= 0),
    PRIMARY KEY (expense_id, membership_id)
);

CREATE TABLE room_expense_settlements (
    id VARCHAR(36) PRIMARY KEY,
    room_id VARCHAR(36) NOT NULL REFERENCES rooms(id) ON DELETE CASCADE,
    from_membership_id VARCHAR(36) NOT NULL,
    from_display_name VARCHAR(60) NOT NULL,
    to_membership_id VARCHAR(36) NOT NULL,
    to_display_name VARCHAR(60) NOT NULL,
    amount BIGINT NOT NULL CHECK (amount > 0),
    status VARCHAR(20) NOT NULL DEFAULT 'PENDING' CHECK (status IN ('PENDING','PAID')),
    created_by_membership_id VARCHAR(36) NOT NULL,
    created_by_display_name VARCHAR(60) NOT NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    paid_at TIMESTAMPTZ
);
CREATE INDEX idx_room_settlements_room ON room_expense_settlements(room_id, status);

CREATE TABLE room_expense_audit (
    id VARCHAR(36) PRIMARY KEY,
    room_id VARCHAR(36) NOT NULL REFERENCES rooms(id) ON DELETE CASCADE,
    expense_id VARCHAR(36),
    actor_membership_id VARCHAR(36) NOT NULL,
    actor_display_name VARCHAR(60) NOT NULL,
    action VARCHAR(20) NOT NULL,
    description VARCHAR(1200) NOT NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now()
);
CREATE INDEX idx_room_expense_audit_room ON room_expense_audit(room_id, created_at DESC);
