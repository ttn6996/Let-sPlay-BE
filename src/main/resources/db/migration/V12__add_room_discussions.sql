CREATE TABLE room_comments (
    id VARCHAR(36) PRIMARY KEY,
    room_id VARCHAR(36) NOT NULL REFERENCES rooms(id) ON DELETE CASCADE,
    membership_id VARCHAR(36) NOT NULL REFERENCES room_memberships(id) ON DELETE CASCADE,
    proposal_id VARCHAR(36) REFERENCES room_proposals(id) ON DELETE CASCADE,
    availability_slot_id VARCHAR(36) REFERENCES room_availability_slots(id) ON DELETE CASCADE,
    content VARCHAR(500) NOT NULL,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL,
    CONSTRAINT ck_room_comment_target CHECK (
        (proposal_id IS NOT NULL AND availability_slot_id IS NULL) OR
        (proposal_id IS NULL AND availability_slot_id IS NOT NULL)
    )
);

CREATE INDEX idx_room_comments_proposal ON room_comments(proposal_id, created_at);
CREATE INDEX idx_room_comments_availability ON room_comments(availability_slot_id, created_at);
