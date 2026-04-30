-- Air Quality Index data from OpenWeatherMap
CREATE TABLE IF NOT EXISTS air_quality (
    id              BIGSERIAL PRIMARY KEY,
    location_id     BIGINT REFERENCES locations(id),
    latitude        NUMERIC(10, 7) NOT NULL,
    longitude       NUMERIC(10, 7) NOT NULL,
    aqi             INTEGER NOT NULL,
    co              NUMERIC(10, 2),
    no              NUMERIC(10, 2),
    no2             NUMERIC(10, 2),
    o3              NUMERIC(10, 2),
    so2             NUMERIC(10, 2),
    pm2_5           NUMERIC(10, 2),
    pm10            NUMERIC(10, 2),
    nh3             NUMERIC(10, 2),
    valid_at        TIMESTAMP NOT NULL,
    fetched_at      TIMESTAMP NOT NULL,
    source          VARCHAR(50) NOT NULL DEFAULT 'openweathermap',
    is_active       BOOLEAN DEFAULT TRUE,
    created_at      TIMESTAMP NOT NULL DEFAULT NOW(),
    updated_at      TIMESTAMP
);

CREATE INDEX IF NOT EXISTS idx_aq_location ON air_quality(location_id);
CREATE INDEX IF NOT EXISTS idx_aq_valid_at ON air_quality(valid_at);
CREATE INDEX IF NOT EXISTS idx_aq_active   ON air_quality(is_active);
