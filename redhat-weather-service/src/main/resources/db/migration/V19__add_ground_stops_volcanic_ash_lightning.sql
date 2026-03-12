-- Ground Stops table (FAA ground stop and ground delay programs)
CREATE TABLE ground_stops (
    id BIGSERIAL PRIMARY KEY,
    ground_stop_id VARCHAR(100) NOT NULL UNIQUE,
    airport_code VARCHAR(10) NOT NULL,
    airport_name VARCHAR(200),
    program_type VARCHAR(50) NOT NULL,
    reason VARCHAR(500),
    end_time TIMESTAMP,
    avg_delay_minutes INTEGER,
    max_delay_minutes INTEGER,
    raw_data JSONB NOT NULL,
    fetched_at TIMESTAMP NOT NULL,
    is_active BOOLEAN DEFAULT TRUE,
    created_at TIMESTAMP NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_ground_stop_id ON ground_stops(ground_stop_id);
CREATE INDEX idx_ground_stop_airport ON ground_stops(airport_code);
CREATE INDEX idx_ground_stop_active ON ground_stops(is_active);
CREATE INDEX idx_ground_stop_type ON ground_stops(program_type);

-- Volcanic Ash Advisories table
CREATE TABLE volcanic_ash_advisories (
    id BIGSERIAL PRIMARY KEY,
    advisory_id VARCHAR(100) NOT NULL UNIQUE,
    fir_id VARCHAR(20),
    fir_name VARCHAR(200),
    volcano_name VARCHAR(200),
    hazard VARCHAR(50),
    severity VARCHAR(20),
    valid_time_from TIMESTAMP NOT NULL,
    valid_time_to TIMESTAMP NOT NULL,
    altitude_low_ft INTEGER,
    altitude_high_ft INTEGER,
    raw_text TEXT,
    geojson JSONB,
    advisory_data JSONB NOT NULL,
    fetched_at TIMESTAMP NOT NULL,
    is_active BOOLEAN DEFAULT TRUE,
    created_at TIMESTAMP NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_vaa_advisory_id ON volcanic_ash_advisories(advisory_id);
CREATE INDEX idx_vaa_valid_to ON volcanic_ash_advisories(valid_time_to);
CREATE INDEX idx_vaa_active ON volcanic_ash_advisories(is_active);

-- Lightning Strikes table
CREATE TABLE lightning_strikes (
    id BIGSERIAL PRIMARY KEY,
    strike_id VARCHAR(100) NOT NULL UNIQUE,
    latitude NUMERIC(10, 7) NOT NULL,
    longitude NUMERIC(10, 7) NOT NULL,
    strike_time TIMESTAMP NOT NULL,
    amplitude_ka DOUBLE PRECISION,
    strike_type VARCHAR(20),
    raw_data JSONB,
    fetched_at TIMESTAMP NOT NULL,
    is_active BOOLEAN DEFAULT TRUE,
    created_at TIMESTAMP NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_lightning_strike_id ON lightning_strikes(strike_id);
CREATE INDEX idx_lightning_time ON lightning_strikes(strike_time);
CREATE INDEX idx_lightning_active ON lightning_strikes(is_active);
CREATE INDEX idx_lightning_coords ON lightning_strikes(latitude, longitude);
