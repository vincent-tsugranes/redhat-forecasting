-- Create weather_alerts table for storing NOAA weather alerts, watches, and warnings
CREATE TABLE weather_alerts (
    id BIGSERIAL PRIMARY KEY,

    -- Alert identification
    alert_id VARCHAR(255) NOT NULL UNIQUE,
    event VARCHAR(200) NOT NULL,

    -- Alert details
    headline TEXT,
    description TEXT,
    severity VARCHAR(20),
    certainty VARCHAR(20),
    urgency VARCHAR(20),

    -- Affected area
    area_desc TEXT,

    -- Time information
    effective TIMESTAMP,
    expires TIMESTAMP,

    -- Source
    sender_name VARCHAR(200),

    -- Full alert data
    alert_data JSONB NOT NULL,

    -- Metadata
    is_active BOOLEAN DEFAULT true,
    fetched_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

-- Create indexes for efficient querying
CREATE INDEX idx_alert_alert_id ON weather_alerts(alert_id);
CREATE INDEX idx_alert_event ON weather_alerts(event);
CREATE INDEX idx_alert_severity ON weather_alerts(severity);
CREATE INDEX idx_alert_active ON weather_alerts(is_active);
CREATE INDEX idx_alert_expires ON weather_alerts(expires);
CREATE INDEX idx_alert_effective ON weather_alerts(effective);
CREATE INDEX idx_alert_fetched_at ON weather_alerts(fetched_at);
CREATE INDEX idx_alert_data ON weather_alerts USING GIN(alert_data);

-- Composite indexes for common query patterns
CREATE INDEX idx_alert_active_expires ON weather_alerts(is_active, expires) WHERE is_active = true;
CREATE INDEX idx_alert_active_severity ON weather_alerts(is_active, severity) WHERE is_active = true;

-- Add comments
COMMENT ON TABLE weather_alerts IS 'Weather alerts, watches, and warnings from NOAA Weather API';
COMMENT ON COLUMN weather_alerts.alert_id IS 'Unique NOAA alert identifier';
COMMENT ON COLUMN weather_alerts.event IS 'Alert event type (e.g., Tornado Warning, Severe Thunderstorm Watch)';
COMMENT ON COLUMN weather_alerts.severity IS 'Alert severity: Extreme, Severe, Moderate, Minor, Unknown';
COMMENT ON COLUMN weather_alerts.certainty IS 'Alert certainty: Observed, Likely, Possible, Unlikely, Unknown';
COMMENT ON COLUMN weather_alerts.urgency IS 'Alert urgency: Immediate, Expected, Future, Past, Unknown';
COMMENT ON COLUMN weather_alerts.area_desc IS 'Description of affected geographic area';
COMMENT ON COLUMN weather_alerts.is_active IS 'Whether this alert is still active and not expired';
