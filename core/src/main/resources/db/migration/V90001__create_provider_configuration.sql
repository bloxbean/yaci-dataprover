-- V2: Create provider_configuration table
-- Stores UI-configured connection settings for data providers

CREATE TABLE provider_configuration (
    id BIGSERIAL PRIMARY KEY,
    provider_name VARCHAR(64) NOT NULL UNIQUE,
    config_json TEXT NOT NULL,
    encrypted_secrets TEXT,
    source VARCHAR(20) NOT NULL DEFAULT 'UI',
    created_at TIMESTAMP NOT NULL DEFAULT NOW(),
    last_updated TIMESTAMP,
    version BIGINT DEFAULT 0
);

-- Create index for provider name lookups
CREATE INDEX idx_provider_config_name ON provider_configuration(provider_name);

-- Add check constraint for valid sources
ALTER TABLE provider_configuration
ADD CONSTRAINT chk_provider_config_source
CHECK (source IN ('UI', 'ENV'));

-- Comment on table and columns
COMMENT ON TABLE provider_configuration IS 'Stores UI-configured connection settings for data providers';
COMMENT ON COLUMN provider_configuration.provider_name IS 'Unique provider name (e.g., epoch-stake)';
COMMENT ON COLUMN provider_configuration.config_json IS 'Non-sensitive configuration values as JSON';
COMMENT ON COLUMN provider_configuration.encrypted_secrets IS 'AES-256-GCM encrypted sensitive values (passwords, keys)';
COMMENT ON COLUMN provider_configuration.source IS 'Configuration source: UI or ENV';
COMMENT ON COLUMN provider_configuration.version IS 'Optimistic locking version';
