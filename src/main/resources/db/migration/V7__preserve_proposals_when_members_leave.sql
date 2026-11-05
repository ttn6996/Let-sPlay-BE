ALTER TABLE room_proposals DROP CONSTRAINT room_proposals_created_by_membership_id_fkey;
ALTER TABLE room_proposals ALTER COLUMN created_by_membership_id DROP NOT NULL;
ALTER TABLE room_proposals ADD CONSTRAINT fk_proposal_creator_membership
    FOREIGN KEY (created_by_membership_id) REFERENCES room_memberships(id) ON DELETE SET NULL;
