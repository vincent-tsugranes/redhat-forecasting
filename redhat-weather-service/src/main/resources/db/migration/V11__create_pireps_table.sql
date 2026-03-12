CREATE TABLE pireps (
    id BIGSERIAL PRIMARY KEY,
    pirep_id VARCHAR(100) NOT NULL,
    report_type VARCHAR(10) NOT NULL,
    raw_text TEXT NOT NULL,
    observation_time TIMESTAMP NOT NULL,
    latitude DECIMAL(10,7) NOT NULL,
    longitude DECIMAL(10,7) NOT NULL,
    altitude_ft INTEGER,
    aircraft_type VARCHAR(20),
    sky_condition VARCHAR(255),
    turbulence_intensity VARCHAR(20),
    turbulence_type VARCHAR(20),
    icing_intensity VARCHAR(20),
    icing_type VARCHAR(20),
    weather_conditions VARCHAR(255),
    temperature_celsius DECIMAL(5,2),
    wind_speed_knots INTEGER,
    wind_direction INTEGER,
    visibility_miles DECIMAL(5,2),
    pirep_data JSONB NOT NULL,
    fetched_at TIMESTAMP NOT NULL,
    is_active BOOLEAN DEFAULT true,
    created_at TIMESTAMP NOT NULL
);

CREATE UNIQUE INDEX idx_pirep_id ON pireps (pirep_id);
CREATE INDEX idx_pirep_observation_time ON pireps (observation_time);
CREATE INDEX idx_pirep_turbulence ON pireps (turbulence_intensity) WHERE turbulence_intensity IS NOT NULL;
CREATE INDEX idx_pirep_icing ON pireps (icing_intensity) WHERE icing_intensity IS NOT NULL;
CREATE INDEX idx_pirep_active_time ON pireps (observation_time DESC) WHERE is_active = true;
