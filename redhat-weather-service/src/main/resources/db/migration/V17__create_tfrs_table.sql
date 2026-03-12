CREATE TABLE IF NOT EXISTS tfrs (
    id BIGSERIAL PRIMARY KEY,
    notam_id VARCHAR(20) NOT NULL UNIQUE,
    notam_key VARCHAR(50),
    facility VARCHAR(10) NOT NULL,
    state VARCHAR(10),
    tfr_type VARCHAR(50) NOT NULL,
    description TEXT,
    effective_date TIMESTAMP,
    expire_date TIMESTAMP,
    latitude NUMERIC(9,6),
    longitude NUMERIC(9,6),
    geojson JSONB,
    tfr_data JSONB NOT NULL,
    is_new BOOLEAN DEFAULT FALSE,
    fetched_at TIMESTAMP NOT NULL,
    is_active BOOLEAN DEFAULT TRUE,
    created_at TIMESTAMP NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_tfr_notam_id ON tfrs(notam_id);
CREATE INDEX idx_tfr_facility ON tfrs(facility);
CREATE INDEX idx_tfr_type ON tfrs(tfr_type);
CREATE INDEX idx_tfr_state ON tfrs(state);
CREATE INDEX idx_tfr_active ON tfrs(is_active) WHERE is_active = true;
