-- Create earthquakes table for storing USGS earthquake data
CREATE TABLE earthquakes (
    id BIGSERIAL PRIMARY KEY,

    -- Earthquake identification
    usgs_id VARCHAR(50) NOT NULL,

    -- Event data
    magnitude DECIMAL(4, 2),
    place VARCHAR(255),
    event_time TIMESTAMP NOT NULL,

    -- Position
    latitude DECIMAL(10, 7) NOT NULL,
    longitude DECIMAL(10, 7) NOT NULL,
    depth_km DECIMAL(7, 2),

    -- Classification
    magnitude_type VARCHAR(10),
    status VARCHAR(20),
    tsunami BOOLEAN DEFAULT false,
    felt INTEGER,
    cdi DECIMAL(4, 2),
    alert VARCHAR(10),
    significance INTEGER,

    -- Full event data
    event_data JSONB NOT NULL,

    -- Metadata
    fetched_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    is_active BOOLEAN DEFAULT true,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

-- Indexes
CREATE INDEX idx_earthquake_usgs_id ON earthquakes(usgs_id);
CREATE INDEX idx_earthquake_event_time ON earthquakes(event_time);
CREATE INDEX idx_earthquake_magnitude ON earthquakes(magnitude);
CREATE INDEX idx_earthquake_coordinates ON earthquakes(latitude, longitude);
CREATE INDEX idx_earthquake_active ON earthquakes(is_active);
CREATE INDEX idx_earthquake_alert ON earthquakes(alert);
CREATE INDEX idx_earthquake_significance ON earthquakes(significance);
CREATE INDEX idx_earthquake_active_recent ON earthquakes(is_active, event_time DESC) WHERE is_active = true;
CREATE INDEX idx_earthquake_event_data ON earthquakes USING GIN(event_data);

COMMENT ON TABLE earthquakes IS 'Earthquake data from USGS Earthquake Hazards Program';
COMMENT ON COLUMN earthquakes.usgs_id IS 'Unique USGS event identifier';
COMMENT ON COLUMN earthquakes.magnitude IS 'Earthquake magnitude (Richter scale)';
COMMENT ON COLUMN earthquakes.depth_km IS 'Depth of earthquake in kilometers';
COMMENT ON COLUMN earthquakes.alert IS 'PAGER alert level: green, yellow, orange, red';
COMMENT ON COLUMN earthquakes.significance IS 'USGS significance score (0-1000+)';
COMMENT ON COLUMN earthquakes.felt IS 'Number of felt reports submitted';
COMMENT ON COLUMN earthquakes.cdi IS 'Community Decimal Intensity (max reported)';
