package com.redhat.weather.startup;

import com.redhat.weather.domain.repository.LocationRepository;
import com.redhat.weather.service.AirportCsvLoader;
import io.quarkus.logging.Log;
import io.quarkus.runtime.StartupEvent;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.event.Observes;
import jakarta.inject.Inject;
import org.eclipse.microprofile.config.inject.ConfigProperty;

import java.nio.file.Files;
import java.nio.file.Paths;

/**
 * Initializes airport data from CSV file on application startup
 * Runs after Flyway migrations have completed
 */
@ApplicationScoped
public class AirportDataInitializer {

    @Inject
    AirportCsvLoader airportCsvLoader;

    @Inject
    LocationRepository locationRepository;

    @ConfigProperty(name = "airport.csv.path", defaultValue = "../data/airports.csv")
    String csvPath;

    @ConfigProperty(name = "airport.csv.load-on-startup", defaultValue = "true")
    boolean loadOnStartup;

    @ConfigProperty(name = "airport.csv.skip-if-exists", defaultValue = "true")
    boolean skipIfExists;

    /**
     * Load airports from CSV on application startup
     */
    void onStart(@Observes StartupEvent event) {
        if (!loadOnStartup) {
            Log.info("Airport CSV loading disabled via configuration");
            return;
        }

        try {
            // Check if we should skip loading if airports already exist
            if (skipIfExists) {
                long existingAirportCount = locationRepository.count("locationType = ?1", "airport");
                if (existingAirportCount > 0) {
                    Log.infof("Skipping airport CSV load - %d airports already exist in database", existingAirportCount);
                    return;
                }
            }

            // Verify CSV file exists
            if (!Files.exists(Paths.get(csvPath))) {
                Log.warnf("Airport CSV file not found at: %s - skipping airport data load", csvPath);
                return;
            }

            // Count how many airports will be loaded
            int validAirportCount = airportCsvLoader.countValidAirports(csvPath);
            Log.infof("Found %d valid airports with ICAO codes in CSV file", validAirportCount);

            // Load airports from CSV
            int loadedCount = airportCsvLoader.loadAirportsFromCsv(csvPath);

            if (loadedCount > 0) {
                Log.infof("Successfully loaded %d airports from CSV file", loadedCount);
            } else {
                Log.warn("No airports were loaded from CSV file");
            }

        } catch (Exception e) {
            Log.errorf(e, "Error loading airports from CSV during startup");
            // Don't fail application startup if airport loading fails
        }
    }
}
