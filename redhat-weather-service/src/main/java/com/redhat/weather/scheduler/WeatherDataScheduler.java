package com.redhat.weather.scheduler;

import com.redhat.weather.domain.entity.LocationEntity;
import com.redhat.weather.domain.repository.LocationRepository;
import com.redhat.weather.service.AirportWeatherService;
import com.redhat.weather.service.HurricaneService;
import com.redhat.weather.service.WeatherForecastService;
import io.quarkus.scheduler.Scheduled;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.jboss.logging.Logger;

import java.time.LocalDateTime;
import java.util.List;

@ApplicationScoped
public class WeatherDataScheduler {

    private static final Logger LOG = Logger.getLogger(WeatherDataScheduler.class);

    @Inject
    WeatherForecastService weatherForecastService;

    @Inject
    AirportWeatherService airportWeatherService;

    @Inject
    HurricaneService hurricaneService;

    @Inject
    LocationRepository locationRepository;

    @ConfigProperty(name = "weather.scheduler.noaa.enabled", defaultValue = "true")
    boolean noaaEnabled;

    @ConfigProperty(name = "weather.scheduler.aviation.enabled", defaultValue = "true")
    boolean aviationEnabled;

    @ConfigProperty(name = "weather.scheduler.hurricane.enabled", defaultValue = "true")
    boolean hurricaneEnabled;

    @ConfigProperty(name = "weather.scheduler.openweather.enabled", defaultValue = "false")
    boolean openWeatherEnabled;

    /**
     * Fetch NOAA weather forecasts every 30 minutes
     */
    @Scheduled(cron = "0 */30 * * * ?", identity = "noaa-forecast-fetch")
    public void fetchNoaaForecasts() {
        if (!noaaEnabled) {
            LOG.debug("NOAA scheduler is disabled");
            return;
        }

        LOG.info("Starting NOAA forecast fetch");

        try {
            List<LocationEntity> locations = locationRepository.getAllLocations();

            int successCount = 0;
            int failureCount = 0;

            for (LocationEntity location : locations) {
                try {
                    weatherForecastService.fetchAndStoreNoaaForecast(location.id);
                    successCount++;
                } catch (Exception e) {
                    LOG.error("Failed to fetch NOAA forecast for location: " + location.name, e);
                    failureCount++;
                }
            }

            LOG.info("NOAA forecast fetch completed. Success: " + successCount + ", Failures: " + failureCount);

        } catch (Exception e) {
            LOG.error("Error in NOAA forecast scheduler", e);
        }
    }

    /**
     * Fetch OpenWeatherMap forecasts every 2 hours (if enabled and API key is configured)
     */
    @Scheduled(cron = "0 0 */2 * * ?", identity = "openweather-forecast-fetch")
    public void fetchOpenWeatherForecasts() {
        if (!openWeatherEnabled) {
            LOG.debug("OpenWeatherMap scheduler is disabled");
            return;
        }

        LOG.info("Starting OpenWeatherMap forecast fetch");

        try {
            List<LocationEntity> locations = locationRepository.getAllLocations();

            int successCount = 0;
            int failureCount = 0;

            for (LocationEntity location : locations) {
                try {
                    weatherForecastService.fetchAndStoreOpenWeatherForecast(location.id);
                    successCount++;
                } catch (Exception e) {
                    LOG.error("Failed to fetch OpenWeather forecast for location: " + location.name, e);
                    failureCount++;
                }
            }

            LOG.info("OpenWeatherMap forecast fetch completed. Success: " + successCount + ", Failures: " + failureCount);

        } catch (Exception e) {
            LOG.error("Error in OpenWeatherMap forecast scheduler", e);
        }
    }

    /**
     * Fetch airport weather (METAR/TAF) every 15 minutes
     */
    @Scheduled(cron = "0 */15 * * * ?", identity = "airport-weather-fetch")
    public void fetchAirportWeather() {
        if (!aviationEnabled) {
            LOG.debug("Aviation weather scheduler is disabled");
            return;
        }

        LOG.info("Starting airport weather fetch");

        try {
            List<LocationEntity> airports = locationRepository.findAirportLocations();

            int successCount = 0;
            int failureCount = 0;

            for (LocationEntity airport : airports) {
                try {
                    airportWeatherService.fetchAndStoreMETAR(airport.airportCode);
                    airportWeatherService.fetchAndStoreTAF(airport.airportCode);
                    successCount++;
                } catch (Exception e) {
                    LOG.error("Failed to fetch airport weather for: " + airport.airportCode, e);
                    failureCount++;
                }
            }

            LOG.info("Airport weather fetch completed. Success: " + successCount + ", Failures: " + failureCount);

        } catch (Exception e) {
            LOG.error("Error in airport weather scheduler", e);
        }
    }

    /**
     * Fetch hurricane data every hour during hurricane season, every 6 hours otherwise
     */
    @Scheduled(cron = "0 0 * * * ?", identity = "hurricane-fetch")
    public void fetchHurricanes() {
        if (!hurricaneEnabled) {
            LOG.debug("Hurricane scheduler is disabled");
            return;
        }

        // Check if it's hurricane season or if it's a 6-hour interval
        boolean isHurricaneSeason = isHurricaneSeason();
        int currentHour = LocalDateTime.now().getHour();

        if (!isHurricaneSeason && currentHour % 6 != 0) {
            LOG.debug("Not hurricane season and not 6-hour interval, skipping");
            return;
        }

        LOG.info("Starting hurricane data fetch");

        try {
            hurricaneService.fetchAndStoreActiveStorms();
            LOG.info("Hurricane data fetch completed");

        } catch (Exception e) {
            LOG.error("Error in hurricane scheduler", e);
        }
    }

    /**
     * Clean up old forecast data daily at 2 AM
     */
    @Scheduled(cron = "0 0 2 * * ?", identity = "cleanup-old-data")
    public void cleanupOldData() {
        LOG.info("Starting cleanup of old weather data");

        try {
            LocalDateTime sevenDaysAgo = LocalDateTime.now().minusDays(7);

            weatherForecastService.deactivateOldForecasts(sevenDaysAgo);
            airportWeatherService.deactivateOldReports(sevenDaysAgo);
            hurricaneService.deactivateOldAdvisories(sevenDaysAgo);

            LOG.info("Old data cleanup completed");

        } catch (Exception e) {
            LOG.error("Error in cleanup scheduler", e);
        }
    }

    private boolean isHurricaneSeason() {
        int month = LocalDateTime.now().getMonthValue();
        // Atlantic hurricane season: June 1 - November 30
        return month >= 6 && month <= 11;
    }
}
