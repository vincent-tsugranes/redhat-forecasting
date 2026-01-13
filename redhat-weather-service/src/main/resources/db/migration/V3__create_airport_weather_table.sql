-- Create airport_weather table for storing METAR and TAF data
CREATE TABLE airport_weather (
    id BIGSERIAL PRIMARY KEY,
    location_id BIGINT NOT NULL REFERENCES locations(id) ON DELETE CASCADE,
    airport_code VARCHAR(10) NOT NULL,

    -- Time information
    observation_time TIMESTAMP NOT NULL,
    fetched_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,

    -- Coordinates
    latitude DECIMAL(10, 7) NOT NULL,
    longitude DECIMAL(10, 7) NOT NULL,

    -- METAR/TAF data
    report_type VARCHAR(10) NOT NULL,
    raw_text TEXT NOT NULL,
    metar_data JSONB,
    taf_data JSONB,

    -- Extracted fields for searching
    visibility_miles DECIMAL(5, 2),
    ceiling_feet INTEGER,
    wind_speed_knots INTEGER,
    wind_direction INTEGER,
    wind_gust_knots INTEGER,
    temperature_celsius DECIMAL(5, 2),
    dewpoint_celsius DECIMAL(5, 2),
    altimeter_inches DECIMAL(5, 2),
    flight_category VARCHAR(10),
    sky_condition VARCHAR(255),
    weather_conditions VARCHAR(255),

    -- Metadata
    is_active BOOLEAN DEFAULT true,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,

    CONSTRAINT ck_report_type CHECK (report_type IN ('METAR', 'TAF')),
    CONSTRAINT ck_flight_category CHECK (flight_category IN ('VFR', 'MVFR', 'IFR', 'LIFR')),
    CONSTRAINT ck_wind_direction_airport CHECK (wind_direction >= 0 AND wind_direction <= 360)
);

-- Create indexes for efficient querying
CREATE INDEX idx_airport_location ON airport_weather(location_id);
CREATE INDEX idx_airport_code ON airport_weather(airport_code);
CREATE INDEX idx_airport_observation_time ON airport_weather(observation_time);
CREATE INDEX idx_airport_fetched_at ON airport_weather(fetched_at);
CREATE INDEX idx_airport_report_type ON airport_weather(report_type);
CREATE INDEX idx_airport_flight_category ON airport_weather(flight_category);
CREATE INDEX idx_airport_active ON airport_weather(is_active);
CREATE INDEX idx_airport_metar_data ON airport_weather USING GIN(metar_data);
CREATE INDEX idx_airport_taf_data ON airport_weather USING GIN(taf_data);

-- Composite indexes for common query patterns
CREATE INDEX idx_airport_code_time ON airport_weather(airport_code, observation_time);
CREATE INDEX idx_airport_code_report_active ON airport_weather(airport_code, report_type, is_active) WHERE is_active = true;
CREATE INDEX idx_airport_flight_category_active ON airport_weather(flight_category, is_active) WHERE is_active = true;

-- Add comments to table
COMMENT ON TABLE airport_weather IS 'Airport weather data from Aviation Weather Center (METAR/TAF)';
COMMENT ON COLUMN airport_weather.report_type IS 'Type of report: METAR (observation) or TAF (forecast)';
COMMENT ON COLUMN airport_weather.raw_text IS 'Raw METAR or TAF text';
COMMENT ON COLUMN airport_weather.flight_category IS 'Flight category: VFR, MVFR, IFR, or LIFR';
COMMENT ON COLUMN airport_weather.visibility_miles IS 'Visibility in statute miles';
COMMENT ON COLUMN airport_weather.ceiling_feet IS 'Ceiling height in feet AGL';
COMMENT ON COLUMN airport_weather.altimeter_inches IS 'Altimeter setting in inches of mercury';
COMMENT ON COLUMN airport_weather.is_active IS 'Whether this report is still active (for soft deletes)';
