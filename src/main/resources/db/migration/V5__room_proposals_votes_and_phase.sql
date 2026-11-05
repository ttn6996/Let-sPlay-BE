CREATE TABLE room_proposals (
    id VARCHAR(36) PRIMARY KEY,
    room_id VARCHAR(36) NOT NULL REFERENCES rooms(id) ON DELETE CASCADE,
    created_by_membership_id VARCHAR(36) NOT NULL REFERENCES room_memberships(id) ON DELETE CASCADE,
    title VARCHAR(120) NOT NULL,
    description VARCHAR(500),
    scheduled_time TIMESTAMP WITHOUT TIME ZONE,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now()
);
CREATE INDEX idx_room_proposals_room ON room_proposals(room_id, created_at);

CREATE TABLE room_votes (
    id VARCHAR(36) PRIMARY KEY,
    proposal_id VARCHAR(36) NOT NULL REFERENCES room_proposals(id) ON DELETE CASCADE,
    membership_id VARCHAR(36) NOT NULL REFERENCES room_memberships(id) ON DELETE CASCADE,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    CONSTRAINT uq_room_vote_proposal_member UNIQUE (proposal_id, membership_id)
);
CREATE INDEX idx_room_votes_proposal ON room_votes(proposal_id);

ALTER TABLE rooms ADD COLUMN selected_proposal_id VARCHAR(36) REFERENCES room_proposals(id) ON DELETE SET NULL;
UPDATE rooms SET phase = 'COLLECTING' WHERE phase = 'IDEAS';
