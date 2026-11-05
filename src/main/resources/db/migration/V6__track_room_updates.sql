ALTER TABLE rooms ADD COLUMN updated_at TIMESTAMPTZ;
UPDATE rooms SET updated_at = created_at;
ALTER TABLE rooms ALTER COLUMN updated_at SET DEFAULT now();
ALTER TABLE rooms ALTER COLUMN updated_at SET NOT NULL;
CREATE INDEX idx_rooms_updated_at ON rooms(updated_at DESC);
