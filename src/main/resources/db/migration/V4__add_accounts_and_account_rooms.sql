CREATE TABLE accounts (
    id VARCHAR(36) PRIMARY KEY,
    email VARCHAR(254) NOT NULL UNIQUE,
    password_hash VARCHAR(100) NOT NULL,
    display_name VARCHAR(60) NOT NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE TABLE account_sessions (
    id VARCHAR(36) PRIMARY KEY,
    account_id VARCHAR(36) NOT NULL REFERENCES accounts(id) ON DELETE CASCADE,
    token_hash VARCHAR(64) NOT NULL UNIQUE,
    expires_at TIMESTAMPTZ NOT NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now()
);
CREATE INDEX idx_account_sessions_account_id ON account_sessions(account_id);

ALTER TABLE room_memberships ALTER COLUMN guest_id_hash DROP NOT NULL;
ALTER TABLE room_memberships ALTER COLUMN access_token_hash DROP NOT NULL;
ALTER TABLE room_memberships ADD COLUMN account_id VARCHAR(36) REFERENCES accounts(id) ON DELETE SET NULL;
CREATE INDEX idx_room_memberships_account_id ON room_memberships(account_id);
CREATE UNIQUE INDEX uq_room_memberships_room_account ON room_memberships(room_id, account_id) WHERE account_id IS NOT NULL;
