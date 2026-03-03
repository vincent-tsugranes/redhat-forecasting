-- Index for efficient historical forecast queries (inactive/archived forecasts)
CREATE INDEX idx_forecast_historical
    ON weather_forecasts (location_id, forecast_time DESC)
    WHERE is_active = false;
