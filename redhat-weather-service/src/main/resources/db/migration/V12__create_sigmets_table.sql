CREATE TABLE sigmets (
    id BIGSERIAL PRIMARY KEY,
    sigmet_id VARCHAR(100) NOT NULL,
    sigmet_type VARCHAR(20) NOT NULL,
    hazard VARCHAR(50),
    severity VARCHAR(20),
    valid_time_from TIMESTAMP NOT NULL,
    valid_time_to TIMESTAMP NOT NULL,
    altitude_low_ft INTEGER,
    altitude_high_ft INTEGER,
    raw_text TEXT,
    geojson JSONB,
    sigmet_data JSONB NOT NULL,
    fetched_at TIMESTAMP NOT NULL,
    is_active BOOLEAN DEFAULT true,
    created_at TIMESTAMP NOT NULL
);

CREATE UNIQUE INDEX idx_sigmet_id ON sigmets (sigmet_id);
CREATE INDEX idx_sigmet_type ON sigmets (sigmet_type);
CREATE INDEX idx_sigmet_hazard ON sigmets (hazard) WHERE hazard IS NOT NULL;
CREATE INDEX idx_sigmet_valid_to ON sigmets (valid_time_to);
CREATE INDEX idx_sigmet_active_valid ON sigmets (valid_time_to DESC) WHERE is_active = true;
