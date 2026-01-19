-- V1: Create merkle_metadata table
-- This table stores metadata about each merkle instance in the system

CREATE TABLE merkle_metadata (
    id BIGSERIAL PRIMARY KEY,
    identifier VARCHAR(64) NOT NULL UNIQUE,
    scheme VARCHAR(20) NOT NULL DEFAULT 'mpf',
    root_hash VARCHAR(128),
    record_count INTEGER NOT NULL DEFAULT 0,
    created_at TIMESTAMP NOT NULL DEFAULT NOW(),
    last_updated TIMESTAMP,
    metadata TEXT,
    status VARCHAR(20) NOT NULL DEFAULT 'ACTIVE',
    version BIGINT DEFAULT 0
);

-- Create indexes for common queries
CREATE INDEX idx_merkle_status ON merkle_metadata(status);
CREATE INDEX idx_merkle_created_at ON merkle_metadata(created_at DESC);
CREATE INDEX idx_merkle_scheme ON merkle_metadata(scheme);

-- Add check constraint for valid statuses
ALTER TABLE merkle_metadata
ADD CONSTRAINT chk_merkle_status
CHECK (status IN ('ACTIVE', 'BUILDING', 'ARCHIVED', 'DELETED'));

-- Add check constraint for valid merkle schemes
ALTER TABLE merkle_metadata
ADD CONSTRAINT chk_merkle_scheme
CHECK (scheme IN ('mpf', 'jmt'));

-- Add check constraint for non-negative record count
ALTER TABLE merkle_metadata
ADD CONSTRAINT chk_record_count
CHECK (record_count >= 0);

-- Comment on table and columns
COMMENT ON TABLE merkle_metadata IS 'Metadata for merkle instances including configuration and statistics';
COMMENT ON COLUMN merkle_metadata.identifier IS 'Unique merkle identifier (3-64 chars, lowercase, numbers, hyphens)';
COMMENT ON COLUMN merkle_metadata.scheme IS 'Merkle scheme implementation (mpf, jmt, etc.)';
COMMENT ON COLUMN merkle_metadata.root_hash IS 'Current root hash in hex format';
COMMENT ON COLUMN merkle_metadata.record_count IS 'Number of entries in the merkle';
COMMENT ON COLUMN merkle_metadata.metadata IS 'Custom metadata as JSON (use case specific)';
COMMENT ON COLUMN merkle_metadata.status IS 'Lifecycle status of the merkle';
COMMENT ON COLUMN merkle_metadata.version IS 'Optimistic locking version';
