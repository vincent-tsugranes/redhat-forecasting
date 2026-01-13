-- Create hurricanes table for storing tropical storm and hurricane data
CREATE TABLE hurricanes (
    id BIGSERIAL PRIMARY KEY,

    -- Hurricane identification
    storm_id VARCHAR(50) NOT NULL,
    storm_name VARCHAR(100),
    storm_number INTEGER,
    basin VARCHAR(10),
    year INTEGER,

    -- Time information
    advisory_time TIMESTAMP NOT NULL,
    forecast_time TIMESTAMP NOT NULL,
    fetched_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,

    -- Current position
    latitude DECIMAL(10, 7) NOT NULL,
    longitude DECIMAL(10, 7) NOT NULL,

    -- Hurricane data
    category INTEGER,
    max_sustained_winds_mph INTEGER,
    max_sustained_winds_knots INTEGER,
    min_central_pressure_mb INTEGER,
    movement_direction INTEGER,
    movement_speed_mph DECIMAL(5, 2),
    movement_speed_knots DECIMAL(5, 2),

    -- Full forecast data (track, intensity, watches/warnings)
    forecast_data JSONB NOT NULL,

    -- Status and classification
    status VARCHAR(50),
    classification VARCHAR(50),
    intensity VARCHAR(50),

    -- Metadata
    is_active BOOLEAN DEFAULT true,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,

    CONSTRAINT ck_category CHECK (category >= 0 AND category <= 5),
    CONSTRAINT ck_movement_direction CHECK (movement_direction >= 0 AND movement_direction <= 360)
);

-- Create indexes for efficient querying
CREATE INDEX idx_hurricane_storm_id ON hurricanes(storm_id);
CREATE INDEX idx_hurricane_storm_name ON hurricanes(storm_name);
CREATE INDEX idx_hurricane_advisory_time ON hurricanes(advisory_time);
CREATE INDEX idx_hurricane_forecast_time ON hurricanes(forecast_time);
CREATE INDEX idx_hurricane_fetched_at ON hurricanes(fetched_at);
CREATE INDEX idx_hurricane_coordinates ON hurricanes(latitude, longitude);
CREATE INDEX idx_hurricane_category ON hurricanes(category);
CREATE INDEX idx_hurricane_status ON hurricanes(status);
CREATE INDEX idx_hurricane_active ON hurricanes(is_active);
CREATE INDEX idx_hurricane_basin ON hurricanes(basin);
CREATE INDEX idx_hurricane_year ON hurricanes(year);
CREATE INDEX idx_hurricane_forecast_data ON hurricanes USING GIN(forecast_data);

-- Composite indexes for common query patterns
CREATE INDEX idx_hurricane_storm_time ON hurricanes(storm_id, advisory_time);
CREATE INDEX idx_hurricane_active_storms ON hurricanes(is_active, status, advisory_time) WHERE is_active = true;
CREATE INDEX idx_hurricane_basin_year ON hurricanes(basin, year, storm_number);

-- Add comments to table
COMMENT ON TABLE hurricanes IS 'Hurricane and tropical storm data from National Hurricane Center';
COMMENT ON COLUMN hurricanes.storm_id IS 'Unique storm identifier (e.g., AL012024)';
COMMENT ON COLUMN hurricanes.basin IS 'Basin code: AL (Atlantic), EP (East Pacific), CP (Central Pacific), etc.';
COMMENT ON COLUMN hurricanes.advisory_time IS 'Time of the advisory';
COMMENT ON COLUMN hurricanes.category IS 'Saffir-Simpson category (0-5), 0 for tropical storm';
COMMENT ON COLUMN hurricanes.status IS 'Storm status: active, dissipated, post-tropical, etc.';
COMMENT ON COLUMN hurricanes.classification IS 'Storm classification: Tropical Depression, Tropical Storm, Hurricane, etc.';
COMMENT ON COLUMN hurricanes.forecast_data IS 'Full forecast track and intensity data as JSON';
COMMENT ON COLUMN hurricanes.is_active IS 'Whether this storm is still active';
