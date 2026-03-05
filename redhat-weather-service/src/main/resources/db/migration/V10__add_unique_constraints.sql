-- Add unique constraints for data integrity

-- Unique index on earthquake USGS ID to prevent duplicate events
DROP INDEX IF EXISTS idx_earthquake_usgs_id;
CREATE UNIQUE INDEX idx_earthquake_usgs_id_unique ON earthquakes (usgs_id);

-- Unique index on hurricane storm_id + advisory_time to prevent duplicate advisories
CREATE UNIQUE INDEX idx_hurricane_storm_advisory_unique ON hurricanes (storm_id, advisory_time);
