-- ============================================================
-- V2: API Keys table
-- Manages API keys for authentication and rate limiting
-- ============================================================

CREATE TABLE api_keys (
    id          UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    api_key     VARCHAR(64) NOT NULL,
    name        VARCHAR(100) NOT NULL,
    rate_limit  INT NOT NULL DEFAULT 100,
    created_at  TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW(),
    is_active   BOOLEAN NOT NULL DEFAULT TRUE,

    CONSTRAINT uq_api_key UNIQUE (api_key)
);

-- Index for API key lookups during authentication
CREATE INDEX idx_api_keys_key ON api_keys (api_key) WHERE is_active = TRUE;

-- Add foreign key constraint to url_mappings
ALTER TABLE url_mappings
    ADD CONSTRAINT fk_url_mappings_api_key
    FOREIGN KEY (api_key_id) REFERENCES api_keys (id);

-- ============================================================
-- V2: Click Analytics table
-- Stores individual click events for analytics
-- ============================================================

CREATE TABLE click_analytics (
    id          BIGSERIAL PRIMARY KEY,
    short_code  VARCHAR(10) NOT NULL,
    clicked_at  TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW(),
    ip_address  VARCHAR(45),
    user_agent  TEXT,
    referrer    VARCHAR(2048),
    country     VARCHAR(2),
    device_type VARCHAR(20),

    CONSTRAINT fk_click_short_code
        FOREIGN KEY (short_code) REFERENCES url_mappings (short_code)
);

-- Index for analytics queries by short_code
CREATE INDEX idx_click_analytics_short_code ON click_analytics (short_code, clicked_at DESC);

-- Index for time-range analytics
CREATE INDEX idx_click_analytics_clicked_at ON click_analytics (clicked_at DESC);
