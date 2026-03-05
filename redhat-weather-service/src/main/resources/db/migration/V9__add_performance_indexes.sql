-- Performance indexes for common query patterns

CREATE INDEX IF NOT EXISTS idx_forecast_location_time
    ON weather_forecasts (location_id, forecast_time DESC);

CREATE INDEX IF NOT EXISTS idx_forecast_coords
    ON weather_forecasts (latitude, longitude);

CREATE INDEX IF NOT EXISTS idx_forecast_active_valid
    ON weather_forecasts (is_active, valid_from, valid_to);

CREATE INDEX IF NOT EXISTS idx_earthquake_event_time
    ON earthquakes (event_time DESC);

CREATE INDEX IF NOT EXISTS idx_earthquake_active
    ON earthquakes (is_active, event_time DESC);

CREATE INDEX IF NOT EXISTS idx_location_airport_code
    ON locations (airport_code) WHERE airport_code IS NOT NULL;

CREATE INDEX IF NOT EXISTS idx_airport_weather_code_type
    ON airport_weather (airport_code, report_type, fetched_at DESC);
