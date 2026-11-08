ALTER TABLE room_proposals ADD COLUMN IF NOT EXISTS location VARCHAR(200);
ALTER TABLE room_proposals ADD COLUMN IF NOT EXISTS estimated_cost_per_person BIGINT;
ALTER TABLE room_proposals ADD COLUMN IF NOT EXISTS latitude DOUBLE PRECISION;
ALTER TABLE room_proposals ADD COLUMN IF NOT EXISTS longitude DOUBLE PRECISION;
ALTER TABLE room_proposals ADD CONSTRAINT ck_proposal_cost_nonnegative CHECK (estimated_cost_per_person IS NULL OR estimated_cost_per_person >= 0);
ALTER TABLE room_proposals ADD CONSTRAINT ck_proposal_latitude CHECK (latitude IS NULL OR latitude BETWEEN -90 AND 90);
ALTER TABLE room_proposals ADD CONSTRAINT ck_proposal_longitude CHECK (longitude IS NULL OR longitude BETWEEN -180 AND 180);
