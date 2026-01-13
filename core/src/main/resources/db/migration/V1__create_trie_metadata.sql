-- V1: Create trie_metadata table
-- This table stores metadata about each trie instance in the system

CREATE TABLE trie_metadata (
    id BIGSERIAL PRIMARY KEY,
    identifier VARCHAR(64) NOT NULL UNIQUE,
    trie_type VARCHAR(20) NOT NULL DEFAULT 'mpf',
    root_hash VARCHAR(128),
    record_count INTEGER NOT NULL DEFAULT 0,
    created_at TIMESTAMP NOT NULL DEFAULT NOW(),
    last_updated TIMESTAMP,
    metadata JSONB,
    status VARCHAR(20) NOT NULL DEFAULT 'ACTIVE',
    version BIGINT DEFAULT 0
);

-- Create indexes for common queries
CREATE INDEX idx_trie_status ON trie_metadata(status);
CREATE INDEX idx_trie_created_at ON trie_metadata(created_at DESC);
CREATE INDEX idx_trie_type ON trie_metadata(trie_type);

-- Add check constraint for valid statuses
ALTER TABLE trie_metadata
ADD CONSTRAINT chk_trie_status
CHECK (status IN ('ACTIVE', 'BUILDING', 'ARCHIVED', 'DELETED'));

-- Add check constraint for valid trie types
ALTER TABLE trie_metadata
ADD CONSTRAINT chk_trie_type
CHECK (trie_type IN ('mpf', 'jmt'));

-- Add check constraint for non-negative record count
ALTER TABLE trie_metadata
ADD CONSTRAINT chk_record_count
CHECK (record_count >= 0);

-- Comment on table and columns
COMMENT ON TABLE trie_metadata IS 'Metadata for trie instances including configuration and statistics';
COMMENT ON COLUMN trie_metadata.identifier IS 'Unique trie identifier (3-64 chars, lowercase, numbers, hyphens)';
COMMENT ON COLUMN trie_metadata.trie_type IS 'Type of trie implementation (mpf, jmt, etc.)';
COMMENT ON COLUMN trie_metadata.root_hash IS 'Current root hash in hex format';
COMMENT ON COLUMN trie_metadata.record_count IS 'Number of entries in the trie';
COMMENT ON COLUMN trie_metadata.metadata IS 'Custom metadata as JSON (use case specific)';
COMMENT ON COLUMN trie_metadata.status IS 'Lifecycle status of the trie';
COMMENT ON COLUMN trie_metadata.version IS 'Optimistic locking version';
