-- Remove non-airport locations (cities, regions) and simplify the data model
-- Forecasts linked to city locations will cascade-delete (ON DELETE CASCADE in V2)
DELETE FROM locations WHERE location_type != 'airport';

-- Drop the location_type constraint and index since all remaining rows are airports
ALTER TABLE locations DROP CONSTRAINT IF EXISTS ck_location_type;
DROP INDEX IF EXISTS idx_location_type;

-- Make airport_code NOT NULL since all locations are now airports
-- First clean up any airports without codes (shouldn't exist, but be safe)
DELETE FROM locations WHERE airport_code IS NULL;
ALTER TABLE locations ALTER COLUMN airport_code SET NOT NULL;
