-- ============================================================
-- V1: URL Mappings table
-- Core table for storing original → short URL mappings
-- ============================================================

CREATE TABLE url_mappings (
    id          BIGSERIAL PRIMARY KEY,
    short_code  VARCHAR(10) NOT NULL,
    long_url    TEXT NOT NULL,
    created_at  TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW(),
    expires_at  TIMESTAMP WITH TIME ZONE,
    click_count BIGINT NOT NULL DEFAULT 0,
    is_active   BOOLEAN NOT NULL DEFAULT TRUE,
    api_key_id  UUID,

    CONSTRAINT uq_short_code UNIQUE (short_code)
);

-- Index for the hot path: redirect lookups by short_code
CREATE INDEX idx_url_mappings_short_code ON url_mappings (short_code) WHERE is_active = TRUE;

-- Index for listing URLs by API key
CREATE INDEX idx_url_mappings_api_key ON url_mappings (api_key_id, created_at DESC);

-- Index for expiration cleanup job
CREATE INDEX idx_url_mappings_expires_at ON url_mappings (expires_at) WHERE expires_at IS NOT NULL AND is_active = TRUE;
