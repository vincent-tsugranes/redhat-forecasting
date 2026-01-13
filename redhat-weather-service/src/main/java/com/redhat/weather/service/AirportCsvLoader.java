package com.redhat.weather.service;

import com.redhat.weather.domain.entity.LocationEntity;
import com.redhat.weather.domain.repository.LocationRepository;
import io.quarkus.logging.Log;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.math.BigDecimal;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;

@ApplicationScoped
public class AirportCsvLoader {

    @Inject
    LocationRepository locationRepository;

    /**
     * Load airports from CSV file
     * Filters for airports with ICAO codes (required for METAR/TAF weather data)
     *
     * @param csvFilePath Path to the airports.csv file
     * @return Number of airports loaded
     */
    @Transactional
    public int loadAirportsFromCsv(String csvFilePath) {
        Log.infof("Starting airport CSV import from: %s", csvFilePath);

        Path path = Paths.get(csvFilePath);
        if (!Files.exists(path)) {
            Log.errorf("CSV file not found: %s", csvFilePath);
            return 0;
        }

        int totalCount = 0;
        int loadedCount = 0;
        int skippedCount = 0;
        List<LocationEntity> batch = new ArrayList<>();
        final int BATCH_SIZE = 100;

        try (BufferedReader reader = new BufferedReader(new FileReader(csvFilePath))) {
            // Skip header line
            String headerLine = reader.readLine();
            if (headerLine == null) {
                Log.error("CSV file is empty");
                return 0;
            }

            String line;
            while ((line = reader.readLine()) != null) {
                totalCount++;

                try {
                    LocationEntity airport = parseCsvLine(line);

                    // Only load airports with ICAO codes (needed for weather data)
                    if (airport != null && airport.airportCode != null && !airport.airportCode.isEmpty()) {
                        // Check if airport already exists
                        if (locationRepository.findByAirportCode(airport.airportCode).isEmpty()) {
                            batch.add(airport);
                            loadedCount++;

                            // Persist in batches for performance
                            if (batch.size() >= BATCH_SIZE) {
                                persistBatch(batch);
                                batch.clear();
                                Log.infof("Loaded %d airports so far...", loadedCount);
                            }
                        } else {
                            skippedCount++;
                        }
                    } else {
                        skippedCount++;
                    }
                } catch (Exception e) {
                    Log.warnf("Error parsing line %d: %s - Error: %s", totalCount, line, e.getMessage());
                    skippedCount++;
                }
            }

            // Persist remaining batch
            if (!batch.isEmpty()) {
                persistBatch(batch);
            }

            Log.infof("Airport CSV import complete. Total rows: %d, Loaded: %d, Skipped: %d",
                      totalCount, loadedCount, skippedCount);

        } catch (IOException e) {
            Log.errorf(e, "Error reading CSV file: %s", csvFilePath);
        }

        return loadedCount;
    }

    /**
     * Parse a CSV line into a LocationEntity
     * CSV format: "id","ident","type","name","latitude_deg","longitude_deg","elevation_ft","continent","iso_country","iso_region","municipality","scheduled_service","icao_code","iata_code","gps_code","local_code","home_link","wikipedia_link","keywords"
     */
    private LocationEntity parseCsvLine(String line) {
        // Simple CSV parsing - handles quoted fields
        List<String> fields = parseCsvFields(line);

        if (fields.size() < 14) {
            return null;
        }

        String type = fields.get(2);
        String name = fields.get(3);
        String latStr = fields.get(4);
        String lonStr = fields.get(5);
        String isoCountry = fields.get(8);
        String isoRegion = fields.get(9);
        String municipality = fields.get(10);
        String icaoCode = fields.get(12);

        // Filter for relevant airport types
        if (!isRelevantAirportType(type)) {
            return null;
        }

        // Skip if no ICAO code
        if (icaoCode == null || icaoCode.trim().isEmpty()) {
            return null;
        }

        try {
            BigDecimal latitude = new BigDecimal(latStr);
            BigDecimal longitude = new BigDecimal(lonStr);

            LocationEntity location = new LocationEntity();
            location.name = name;
            location.latitude = latitude;
            location.longitude = longitude;
            location.locationType = "airport";
            location.airportCode = icaoCode.trim();
            location.country = parseCountryName(isoCountry);
            location.state = parseRegionName(isoRegion, municipality);

            return location;
        } catch (NumberFormatException e) {
            Log.debugf("Invalid coordinates for airport: %s", name);
            return null;
        }
    }

    /**
     * Parse CSV fields handling quoted values
     */
    private List<String> parseCsvFields(String line) {
        List<String> fields = new ArrayList<>();
        StringBuilder currentField = new StringBuilder();
        boolean inQuotes = false;

        for (int i = 0; i < line.length(); i++) {
            char c = line.charAt(i);

            if (c == '"') {
                inQuotes = !inQuotes;
            } else if (c == ',' && !inQuotes) {
                fields.add(currentField.toString().trim());
                currentField = new StringBuilder();
            } else {
                currentField.append(c);
            }
        }
        fields.add(currentField.toString().trim());

        return fields;
    }

    /**
     * Check if airport type is relevant for weather data
     */
    private boolean isRelevantAirportType(String type) {
        return type != null && (
            type.equals("large_airport") ||
            type.equals("medium_airport") ||
            type.equals("small_airport")
        );
    }

    /**
     * Parse country code to country name
     */
    private String parseCountryName(String isoCountry) {
        if (isoCountry == null || isoCountry.isEmpty()) {
            return "Unknown";
        }
        // For now, return ISO code - could be enhanced with a country name mapping
        return isoCountry;
    }

    /**
     * Parse region/state name from ISO region code
     */
    private String parseRegionName(String isoRegion, String municipality) {
        if (isoRegion == null || isoRegion.isEmpty()) {
            return municipality != null ? municipality : "Unknown";
        }

        // Extract state/region from ISO region code (e.g., "US-CA" -> "CA")
        String[] parts = isoRegion.split("-");
        if (parts.length > 1) {
            return parts[1];
        }

        return isoRegion;
    }

    /**
     * Persist a batch of locations
     */
    private void persistBatch(List<LocationEntity> batch) {
        for (LocationEntity location : batch) {
            locationRepository.persist(location);
        }
    }

    /**
     * Count how many airports would be loaded (without actually loading)
     */
    public int countValidAirports(String csvFilePath) {
        Path path = Paths.get(csvFilePath);
        if (!Files.exists(path)) {
            return 0;
        }

        int count = 0;
        try (BufferedReader reader = new BufferedReader(new FileReader(csvFilePath))) {
            reader.readLine(); // Skip header

            String line;
            while ((line = reader.readLine()) != null) {
                try {
                    LocationEntity airport = parseCsvLine(line);
                    if (airport != null && airport.airportCode != null && !airport.airportCode.isEmpty()) {
                        count++;
                    }
                } catch (Exception e) {
                    // Skip invalid lines
                }
            }
        } catch (IOException e) {
            Log.errorf(e, "Error counting airports in CSV: %s", csvFilePath);
        }

        return count;
    }
}
