-- Create locations table for storing geographic locations
CREATE TABLE locations (
    id BIGSERIAL PRIMARY KEY,
    name VARCHAR(255) NOT NULL,
    latitude DECIMAL(10, 7) NOT NULL,
    longitude DECIMAL(10, 7) NOT NULL,
    location_type VARCHAR(50) NOT NULL,
    airport_code VARCHAR(10),
    state VARCHAR(100),
    country VARCHAR(100) DEFAULT 'USA',
    metadata JSONB,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,

    CONSTRAINT ck_location_type CHECK (location_type IN ('city', 'airport', 'region'))
);

-- Create indexes for efficient querying
CREATE INDEX idx_location_coordinates ON locations(latitude, longitude);
CREATE INDEX idx_location_airport_code ON locations(airport_code) WHERE airport_code IS NOT NULL;
CREATE INDEX idx_location_type ON locations(location_type);
CREATE INDEX idx_location_metadata ON locations USING GIN(metadata);
CREATE INDEX idx_location_name ON locations(name);
CREATE INDEX idx_location_state ON locations(state) WHERE state IS NOT NULL;

-- Create function to update updated_at timestamp
CREATE OR REPLACE FUNCTION update_updated_at_column()
RETURNS TRIGGER AS $$
BEGIN
    NEW.updated_at = CURRENT_TIMESTAMP;
    RETURN NEW;
END;
$$ language 'plpgsql';

-- Create trigger for updated_at
CREATE TRIGGER update_locations_updated_at BEFORE UPDATE ON locations
    FOR EACH ROW EXECUTE FUNCTION update_updated_at_column();

-- Insert some sample city locations for testing
INSERT INTO locations (name, latitude, longitude, location_type, airport_code, state, country) VALUES
('New York City', 40.7128, -74.0060, 'city', NULL, 'New York', 'USA'),
('Los Angeles', 34.0522, -118.2437, 'city', NULL, 'California', 'USA'),
('Chicago', 41.8781, -87.6298, 'city', NULL, 'Illinois', 'USA'),
('Miami', 25.7617, -80.1918, 'city', NULL, 'Florida', 'USA'),
('Denver', 39.7392, -104.9903, 'city', NULL, 'Colorado', 'USA'),
('Seattle', 47.6062, -122.3321, 'city', NULL, 'Washington', 'USA'),
('Boston', 42.3601, -71.0589, 'city', NULL, 'Massachusetts', 'USA'),
('Atlanta', 33.7490, -84.3880, 'city', NULL, 'Georgia', 'USA');

-- Note: Airport data will be loaded from CSV file at application startup

-- Add comment to table
COMMENT ON TABLE locations IS 'Geographic locations for weather data collection';
COMMENT ON COLUMN locations.location_type IS 'Type of location: city, airport, or region';
COMMENT ON COLUMN locations.metadata IS 'Additional location metadata stored as JSON';
