CREATE TABLE IF NOT EXISTS cwas (
    id BIGSERIAL PRIMARY KEY,
    cwa_id VARCHAR(100) NOT NULL UNIQUE,
    artcc VARCHAR(10) NOT NULL,
    hazard VARCHAR(50),
    severity VARCHAR(20),
    valid_time_from TIMESTAMP NOT NULL,
    valid_time_to TIMESTAMP NOT NULL,
    altitude_low_ft INTEGER,
    altitude_high_ft INTEGER,
    raw_text TEXT,
    geojson JSONB,
    cwa_data JSONB NOT NULL,
    fetched_at TIMESTAMP NOT NULL,
    is_active BOOLEAN DEFAULT TRUE,
    created_at TIMESTAMP NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_cwa_id ON cwas(cwa_id);
CREATE INDEX idx_cwa_artcc ON cwas(artcc);
CREATE INDEX idx_cwa_hazard ON cwas(hazard);
CREATE INDEX idx_cwa_valid_to ON cwas(valid_time_to);
