-- V17: Photo Memories, Multi-Tier Storage, 3-Level Voting, and Itinerary

-- 1. Media Assets
CREATE TABLE media_assets (
    id VARCHAR(36) PRIMARY KEY,
    owner_account_id VARCHAR(36) NOT NULL REFERENCES accounts(id) ON DELETE CASCADE,
    storage_provider VARCHAR(32) NOT NULL,
    storage_file_id VARCHAR(255) NOT NULL,
    url TEXT NOT NULL,
    thumbnail_url TEXT NOT NULL,
    content_type VARCHAR(64) NOT NULL,
    byte_size BIGINT NOT NULL,
    width INT,
    height INT,
    sha256 VARCHAR(64),
    status VARCHAR(32) NOT NULL DEFAULT 'READY',
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    ready_at TIMESTAMPTZ,
    expires_at TIMESTAMPTZ
);
CREATE INDEX idx_media_assets_owner ON media_assets(owner_account_id);

-- 2. Moments (Photo Moments / Posts)
CREATE TABLE moments (
    id VARCHAR(36) PRIMARY KEY,
    owner_account_id VARCHAR(36) NOT NULL REFERENCES accounts(id) ON DELETE CASCADE,
    media_asset_id VARCHAR(36) NOT NULL REFERENCES media_assets(id) ON DELETE CASCADE,
    caption TEXT,
    revision INT NOT NULL DEFAULT 1,
    status VARCHAR(32) NOT NULL DEFAULT 'PUBLISHED',
    client_mutation_id VARCHAR(64),
    published_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    deleted_at TIMESTAMPTZ
);
CREATE INDEX idx_moments_owner ON moments(owner_account_id, created_at DESC);

-- 3. Room Albums (One album per room with post-end policy)
CREATE TABLE room_albums (
    id VARCHAR(36) PRIMARY KEY,
    room_id VARCHAR(36) NOT NULL UNIQUE REFERENCES rooms(id) ON DELETE CASCADE,
    policy_mode VARCHAR(32) NOT NULL DEFAULT 'OPEN',
    window_hours INT,
    ended_at TIMESTAMPTZ,
    deadline_at TIMESTAMPTZ,
    revision INT NOT NULL DEFAULT 1,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT now()
);
CREATE INDEX idx_room_albums_room ON room_albums(room_id);

-- 4. Moment Placements (Placement into Personal feed or Room Album)
CREATE TABLE moment_placements (
    id VARCHAR(36) PRIMARY KEY,
    moment_id VARCHAR(36) NOT NULL REFERENCES moments(id) ON DELETE CASCADE,
    kind VARCHAR(32) NOT NULL,
    room_id VARCHAR(36) REFERENCES rooms(id) ON DELETE CASCADE,
    album_id VARCHAR(36) REFERENCES room_albums(id) ON DELETE CASCADE,
    status VARCHAR(32) NOT NULL DEFAULT 'ACTIVE',
    removed_by_id VARCHAR(36) REFERENCES accounts(id) ON DELETE SET NULL,
    removed_reason VARCHAR(500),
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    removed_at TIMESTAMPTZ
);
CREATE INDEX idx_moment_placements_album ON moment_placements(album_id, created_at DESC);
CREATE INDEX idx_moment_placements_moment ON moment_placements(moment_id);

-- 5. Personal Grants (Audience grant for personal feed)
CREATE TABLE personal_grants (
    id VARCHAR(36) PRIMARY KEY,
    placement_id VARCHAR(36) NOT NULL REFERENCES moment_placements(id) ON DELETE CASCADE,
    grantee_account_id VARCHAR(36) NOT NULL REFERENCES accounts(id) ON DELETE CASCADE,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now()
);
CREATE INDEX idx_personal_grants_grantee ON personal_grants(grantee_account_id, created_at DESC);

-- 6. Placement Reactions (Likes, hearts, emojis on photo placements)
CREATE TABLE placement_reactions (
    id VARCHAR(36) PRIMARY KEY,
    placement_id VARCHAR(36) NOT NULL REFERENCES moment_placements(id) ON DELETE CASCADE,
    account_id VARCHAR(36) NOT NULL REFERENCES accounts(id) ON DELETE CASCADE,
    reaction_type VARCHAR(32) NOT NULL DEFAULT 'HEART',
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    CONSTRAINT uq_placement_reaction UNIQUE (placement_id, account_id)
);
CREATE INDEX idx_placement_reactions_placement ON placement_reactions(placement_id);

-- 7. Placement Comments (Comments on specific photo placement)
CREATE TABLE placement_comments (
    id VARCHAR(36) PRIMARY KEY,
    placement_id VARCHAR(36) NOT NULL REFERENCES moment_placements(id) ON DELETE CASCADE,
    author_account_id VARCHAR(36) NOT NULL REFERENCES accounts(id) ON DELETE CASCADE,
    text VARCHAR(1000) NOT NULL,
    state VARCHAR(32) NOT NULL DEFAULT 'ACTIVE',
    client_mutation_id VARCHAR(64),
    created_at TIMESTAMPTZ NOT NULL DEFAULT now()
);
CREATE INDEX idx_placement_comments_placement ON placement_comments(placement_id, created_at ASC);

-- 8. Album Comments (General discussion thread for the whole room album)
CREATE TABLE album_comments (
    id VARCHAR(36) PRIMARY KEY,
    album_id VARCHAR(36) NOT NULL REFERENCES room_albums(id) ON DELETE CASCADE,
    author_account_id VARCHAR(36) NOT NULL REFERENCES accounts(id) ON DELETE CASCADE,
    text VARCHAR(1000) NOT NULL,
    state VARCHAR(32) NOT NULL DEFAULT 'ACTIVE',
    client_mutation_id VARCHAR(64),
    created_at TIMESTAMPTZ NOT NULL DEFAULT now()
);
CREATE INDEX idx_album_comments_album ON album_comments(album_id, created_at ASC);

-- 9. GAP-01: 3-level Voting (2: Strongly want / High, 1: Support, 0: Pass)
ALTER TABLE room_votes ADD COLUMN IF NOT EXISTS vote_value INT NOT NULL DEFAULT 2;

-- 10. GAP-02: Multi-stage Itinerary
ALTER TABLE rooms ADD COLUMN IF NOT EXISTS selected_proposal_ids TEXT;
ALTER TABLE rooms ADD COLUMN IF NOT EXISTS ended_at TIMESTAMPTZ;
