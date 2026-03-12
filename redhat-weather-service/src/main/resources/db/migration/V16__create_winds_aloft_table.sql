CREATE TABLE IF NOT EXISTS winds_aloft (
    id BIGSERIAL PRIMARY KEY,
    forecast_id VARCHAR(100) NOT NULL UNIQUE,
    station_id VARCHAR(10) NOT NULL,
    latitude NUMERIC(9,6),
    longitude NUMERIC(9,6),
    elevation_ft INTEGER,
    valid_time TIMESTAMP NOT NULL,
    forecast_hour INTEGER,
    altitude_ft INTEGER NOT NULL,
    wind_direction INTEGER,
    wind_speed_knots INTEGER,
    temperature_celsius NUMERIC(5,1),
    raw_data JSONB NOT NULL,
    fetched_at TIMESTAMP NOT NULL,
    is_active BOOLEAN DEFAULT TRUE,
    created_at TIMESTAMP NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_winds_station ON winds_aloft(station_id);
CREATE INDEX idx_winds_valid_time ON winds_aloft(valid_time);
CREATE INDEX idx_winds_altitude ON winds_aloft(altitude_ft);
CREATE UNIQUE INDEX idx_winds_forecast_id ON winds_aloft(forecast_id);
