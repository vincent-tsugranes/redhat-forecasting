CREATE TABLE airport_delays (
    id BIGSERIAL PRIMARY KEY,
    delay_id VARCHAR(100) NOT NULL,
    airport_code VARCHAR(10) NOT NULL,
    airport_name VARCHAR(200),
    delay_type VARCHAR(50) NOT NULL,
    reason VARCHAR(500),
    avg_delay_minutes INTEGER,
    min_delay_minutes INTEGER,
    max_delay_minutes INTEGER,
    trend VARCHAR(20),
    is_delayed BOOLEAN DEFAULT false,
    delay_data JSONB NOT NULL,
    fetched_at TIMESTAMP NOT NULL,
    is_active BOOLEAN DEFAULT true,
    created_at TIMESTAMP NOT NULL
);

CREATE UNIQUE INDEX idx_delay_id ON airport_delays (delay_id);
CREATE INDEX idx_delay_airport_code ON airport_delays (airport_code);
CREATE INDEX idx_delay_type ON airport_delays (delay_type);
CREATE INDEX idx_delay_active_delayed ON airport_delays (is_active, is_delayed) WHERE is_active = true AND is_delayed = true;
