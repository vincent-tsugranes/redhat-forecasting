package com.redhat.weather.scheduler;

import com.redhat.weather.domain.entity.LocationEntity;
import com.redhat.weather.domain.repository.LocationRepository;
import com.redhat.weather.service.AirportDelayService;
import com.redhat.weather.service.AirportWeatherService;
import com.redhat.weather.service.CwaService;
import com.redhat.weather.service.DataFreshnessService;
import com.redhat.weather.service.EarthquakeService;
import com.redhat.weather.service.HurricaneService;
import com.redhat.weather.service.PirepService;
import com.redhat.weather.service.SigmetService;
import com.redhat.weather.service.WeatherAlertService;
import com.redhat.weather.service.WeatherForecastService;
import com.redhat.weather.service.WindsAloftService;
import io.micrometer.core.instrument.MeterRegistry;
import io.quarkus.runtime.Startup;
import io.quarkus.scheduler.Scheduled;
import jakarta.annotation.PostConstruct;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.jboss.logging.Logger;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Semaphore;

@ApplicationScoped
@Startup
public class WeatherDataScheduler {

    private static final Logger LOG = Logger.getLogger(WeatherDataScheduler.class);

    @Inject
    WeatherForecastService weatherForecastService;

    @Inject
    AirportWeatherService airportWeatherService;

    @Inject
    HurricaneService hurricaneService;

    @Inject
    EarthquakeService earthquakeService;

    @Inject
    WeatherAlertService weatherAlertService;

    @Inject
    PirepService pirepService;

    @Inject
    SigmetService sigmetService;

    @Inject
    AirportDelayService airportDelayService;

    @Inject
    CwaService cwaService;

    @Inject
    WindsAloftService windsAloftService;

    @Inject
    LocationRepository locationRepository;

    @Inject
    DataFreshnessService dataFreshnessService;

    @Inject
    MeterRegistry meterRegistry;

    @ConfigProperty(name = "weather.scheduler.noaa.enabled", defaultValue = "true")
    boolean noaaEnabled;

    @ConfigProperty(name = "weather.scheduler.aviation.enabled", defaultValue = "true")
    boolean aviationEnabled;

    @ConfigProperty(name = "weather.scheduler.hurricane.enabled", defaultValue = "true")
    boolean hurricaneEnabled;

    @ConfigProperty(name = "weather.scheduler.alerts.enabled", defaultValue = "true")
    boolean alertsEnabled;

    @ConfigProperty(name = "weather.scheduler.earthquake.enabled", defaultValue = "true")
    boolean earthquakeEnabled;

    @ConfigProperty(name = "weather.scheduler.openweather.enabled", defaultValue = "false")
    boolean openWeatherEnabled;

    @ConfigProperty(name = "weather.scheduler.pireps.enabled", defaultValue = "true")
    boolean pirepsEnabled;

    @ConfigProperty(name = "weather.scheduler.sigmets.enabled", defaultValue = "true")
    boolean sigmetsEnabled;

    @ConfigProperty(name = "weather.scheduler.delays.enabled", defaultValue = "true")
    boolean delaysEnabled;

    @ConfigProperty(name = "weather.scheduler.cwas.enabled", defaultValue = "true")
    boolean cwasEnabled;

    @ConfigProperty(name = "weather.scheduler.winds-aloft.enabled", defaultValue = "true")
    boolean windsAloftEnabled;

    @ConfigProperty(name = "weather.scheduler.airport.batch-size", defaultValue = "500")
    int airportBatchSize;

    @ConfigProperty(name = "weather.scheduler.forecast.batch-size", defaultValue = "100")
    int forecastBatchSize;

    @ConfigProperty(name = "weather.scheduler.parallelism", defaultValue = "5")
    int parallelism;

    @ConfigProperty(name = "weather.scheduler.airport.sub-batch-size", defaultValue = "50")
    int airportSubBatchSize;

    private int airportOffset = 0;
    private int forecastOffset = 0;

    /**
     * Trigger all enabled data sources on startup so the dashboard has data immediately.
     * Runs in a background thread to avoid blocking application startup.
     */
    @PostConstruct
    void onStartup() {
        LOG.info("Scheduling initial data fetch for all enabled sources");
        CompletableFuture.runAsync(() -> {
            try {
                // Small delay to let the application fully initialize
                Thread.sleep(5000);
                LOG.info("Starting initial data fetch");

                List<CompletableFuture<Void>> initialFetches = new ArrayList<>();

                if (noaaEnabled) {
                    initialFetches.add(CompletableFuture.runAsync(() -> {
                        LOG.info("Initial fetch: NOAA forecasts");
                        fetchNoaaForecasts();
                    }));
                }
                if (aviationEnabled) {
                    initialFetches.add(CompletableFuture.runAsync(() -> {
                        LOG.info("Initial fetch: Airport weather");
                        fetchAirportWeather();
                    }));
                }
                if (hurricaneEnabled) {
                    initialFetches.add(CompletableFuture.runAsync(() -> {
                        LOG.info("Initial fetch: Hurricanes");
                        try {
                            hurricaneService.fetchAndStoreActiveStorms();
                            dataFreshnessService.recordSuccess("nhc-hurricane");
                            meterRegistry.counter("weather_scheduler_execution_total", "job", "nhc-hurricane", "result", "success").increment();
                        } catch (Exception e) {
                            meterRegistry.counter("weather_scheduler_execution_total", "job", "nhc-hurricane", "result", "failure").increment();
                            LOG.error("Initial hurricane fetch failed", e);
                        }
                    }));
                }
                if (earthquakeEnabled) {
                    initialFetches.add(CompletableFuture.runAsync(() -> {
                        LOG.info("Initial fetch: Earthquakes");
                        fetchEarthquakes();
                    }));
                }
                if (alertsEnabled) {
                    initialFetches.add(CompletableFuture.runAsync(() -> {
                        LOG.info("Initial fetch: Weather alerts");
                        fetchWeatherAlerts();
                    }));
                }
                if (pirepsEnabled) {
                    initialFetches.add(CompletableFuture.runAsync(() -> {
                        LOG.info("Initial fetch: PIREPs");
                        fetchPireps();
                    }));
                }
                if (sigmetsEnabled) {
                    initialFetches.add(CompletableFuture.runAsync(() -> {
                        LOG.info("Initial fetch: SIGMETs/AIRMETs");
                        fetchSigmets();
                    }));
                }
                if (delaysEnabled) {
                    initialFetches.add(CompletableFuture.runAsync(() -> {
                        LOG.info("Initial fetch: Airport delays");
                        fetchAirportDelays();
                    }));
                }
                if (cwasEnabled) {
                    initialFetches.add(CompletableFuture.runAsync(() -> {
                        LOG.info("Initial fetch: CWAs");
                        fetchCwas();
                    }));
                }
                if (windsAloftEnabled) {
                    initialFetches.add(CompletableFuture.runAsync(() -> {
                        LOG.info("Initial fetch: Winds aloft");
                        fetchWindsAloft();
                    }));
                }

                CompletableFuture.allOf(initialFetches.toArray(new CompletableFuture[0])).join();
                LOG.info("Initial data fetch completed for all enabled sources");

            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                LOG.warn("Initial data fetch interrupted");
            } catch (Exception e) {
                LOG.error("Error during initial data fetch", e);
            }
        });
    }

    /**
     * Fetch NOAA weather forecasts every 30 minutes
     */
    @Scheduled(cron = "0 */30 * * * ?", identity = "noaa-forecast-fetch")
    public void fetchNoaaForecasts() {
        if (!noaaEnabled) {
            LOG.debug("NOAA scheduler is disabled");
            return;
        }

        try {
            List<LocationEntity> locations = locationRepository.getAllLocations();
            int total = locations.size();

            if (total == 0) {
                LOG.info("No locations found for NOAA forecast fetch");
                return;
            }

            if (forecastOffset >= total) {
                forecastOffset = 0;
            }

            int end = Math.min(forecastOffset + forecastBatchSize, total);
            List<LocationEntity> batch = locations.subList(forecastOffset, end);

            LOG.info("Starting NOAA forecast fetch: processing locations " + forecastOffset + "-" + end + " of " + total + " (parallelism=" + parallelism + ")");

            int[] counts = parallelProcess(batch, location -> {
                weatherForecastService.fetchAndStoreNoaaForecast(location.id);
            }, "NOAA forecast");
            int successCount = counts[0];
            int failureCount = counts[1];

            forecastOffset = (end >= total) ? 0 : end;

            LOG.info("NOAA forecast fetch completed. Success: " + successCount + ", Failures: " + failureCount);
            meterRegistry.counter("weather_scheduler_execution_total", "job", "noaa-forecast", "result", "success").increment(successCount);
            meterRegistry.counter("weather_scheduler_execution_total", "job", "noaa-forecast", "result", "failure").increment(failureCount);

            if (successCount > 0) {
                dataFreshnessService.recordSuccess("noaa-forecast");
            }
            if (failureCount > 0 && successCount == 0) {
                LOG.warn("NOAA forecast fetch completely failed for all " + failureCount + " locations in batch");
            }

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

        try {
            List<LocationEntity> locations = locationRepository.getAllLocations();
            int total = locations.size();

            if (total == 0) {
                LOG.info("No locations found for OpenWeatherMap forecast fetch");
                return;
            }

            // Reuse forecastOffset (shared with NOAA since they cover the same locations)
            int offset = forecastOffset;
            if (offset >= total) {
                offset = 0;
            }

            int end = Math.min(offset + forecastBatchSize, total);
            List<LocationEntity> batch = locations.subList(offset, end);

            LOG.info("Starting OpenWeatherMap forecast fetch: processing locations " + offset + "-" + end + " of " + total + " (parallelism=" + parallelism + ")");

            int[] counts = parallelProcess(batch, location -> {
                weatherForecastService.fetchAndStoreOpenWeatherForecast(location.id);
            }, "OpenWeather forecast");
            int successCount = counts[0];
            int failureCount = counts[1];

            LOG.info("OpenWeatherMap forecast fetch completed. Success: " + successCount + ", Failures: " + failureCount);
            meterRegistry.counter("weather_scheduler_execution_total", "job", "openweather-forecast", "result", "success").increment(successCount);
            meterRegistry.counter("weather_scheduler_execution_total", "job", "openweather-forecast", "result", "failure").increment(failureCount);

            if (successCount > 0) {
                dataFreshnessService.recordSuccess("openweather-forecast");
            }
            if (failureCount > 0 && successCount == 0) {
                LOG.warn("OpenWeatherMap forecast fetch completely failed for all " + failureCount + " locations in batch");
            }

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

        try {
            List<LocationEntity> airports = locationRepository.findAirportLocations();
            int total = airports.size();

            if (total == 0) {
                LOG.info("No airports found for weather fetch");
                return;
            }

            if (airportOffset >= total) {
                airportOffset = 0;
            }

            int end = Math.min(airportOffset + airportBatchSize, total);
            List<LocationEntity> batch = airports.subList(airportOffset, end);

            LOG.info("Starting airport weather fetch: processing airports " + airportOffset + "-" + end + " of " + total);

            int successCount = 0;
            int failureCount = 0;

            // Process in sub-batches for bulk API calls
            for (int i = 0; i < batch.size(); i += airportSubBatchSize) {
                List<LocationEntity> subBatch = batch.subList(i, Math.min(i + airportSubBatchSize, batch.size()));
                try {
                    List<String> codes = new ArrayList<>();
                    for (LocationEntity airport : subBatch) {
                        if (airport.airportCode != null) {
                            codes.add(airport.airportCode);
                        }
                    }
                    airportWeatherService.fetchAndStoreAllBatch(codes);
                    successCount += codes.size();
                } catch (Exception e) {
                    LOG.error("Failed to fetch airport weather for batch starting at offset " + i, e);
                    failureCount += subBatch.size();
                }
            }

            airportOffset = (end >= total) ? 0 : end;

            LOG.info("Airport weather fetch completed. Success: " + successCount + ", Failures: " + failureCount);
            meterRegistry.counter("weather_scheduler_execution_total", "job", "aviation-metar", "result", "success").increment(successCount);
            meterRegistry.counter("weather_scheduler_execution_total", "job", "aviation-metar", "result", "failure").increment(failureCount);

            if (successCount > 0) {
                dataFreshnessService.recordSuccess("aviation-metar");
            }
            if (failureCount > 0 && successCount == 0) {
                LOG.warn("Airport weather fetch completely failed for all " + failureCount + " airports in batch");
            }

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
            dataFreshnessService.recordSuccess("nhc-hurricane");
            meterRegistry.counter("weather_scheduler_execution_total", "job", "nhc-hurricane", "result", "success").increment();
            LOG.info("Hurricane data fetch completed");

        } catch (Exception e) {
            meterRegistry.counter("weather_scheduler_execution_total", "job", "nhc-hurricane", "result", "failure").increment();
            LOG.error("Error in hurricane scheduler", e);
        }
    }

    /**
     * Fetch weather alerts every 15 minutes
     */
    @Scheduled(cron = "0 */15 * * * ?", identity = "weather-alerts-fetch")
    public void fetchWeatherAlerts() {
        if (!alertsEnabled) {
            LOG.debug("Weather alerts scheduler is disabled");
            return;
        }

        LOG.info("Starting weather alerts fetch");

        try {
            weatherAlertService.deactivateExpired();
            weatherAlertService.fetchAndStoreAlerts();
            dataFreshnessService.recordSuccess("noaa-alerts");
            meterRegistry.counter("weather_scheduler_execution_total", "job", "noaa-alerts", "result", "success").increment();
            LOG.info("Weather alerts fetch completed");

        } catch (Exception e) {
            meterRegistry.counter("weather_scheduler_execution_total", "job", "noaa-alerts", "result", "failure").increment();
            LOG.error("Error in weather alerts scheduler", e);
        }
    }

    /**
     * Fetch earthquake data every 10 minutes
     */
    @Scheduled(cron = "0 */10 * * * ?", identity = "earthquake-fetch")
    public void fetchEarthquakes() {
        if (!earthquakeEnabled) {
            LOG.debug("Earthquake scheduler is disabled");
            return;
        }

        LOG.info("Starting earthquake data fetch");

        try {
            earthquakeService.fetchAndStoreEarthquakes();
            dataFreshnessService.recordSuccess("usgs-earthquake");
            meterRegistry.counter("weather_scheduler_execution_total", "job", "usgs-earthquake", "result", "success").increment();
            LOG.info("Earthquake data fetch completed");

        } catch (Exception e) {
            meterRegistry.counter("weather_scheduler_execution_total", "job", "usgs-earthquake", "result", "failure").increment();
            LOG.error("Error in earthquake scheduler", e);
        }
    }

    /**
     * Fetch PIREPs every 10 minutes
     */
    @Scheduled(cron = "0 */10 * * * ?", identity = "pirep-fetch")
    public void fetchPireps() {
        if (!pirepsEnabled) {
            LOG.debug("PIREP scheduler is disabled");
            return;
        }

        LOG.info("Starting PIREP data fetch");
        try {
            pirepService.fetchAndStorePireps();
            dataFreshnessService.recordSuccess("awc-pireps");
            meterRegistry.counter("weather_scheduler_execution_total", "job", "awc-pireps", "result", "success").increment();
            LOG.info("PIREP data fetch completed");
        } catch (Exception e) {
            meterRegistry.counter("weather_scheduler_execution_total", "job", "awc-pireps", "result", "failure").increment();
            LOG.error("Error in PIREP scheduler", e);
        }
    }

    /**
     * Fetch SIGMETs/AIRMETs every 15 minutes
     */
    @Scheduled(cron = "0 */15 * * * ?", identity = "sigmet-fetch")
    public void fetchSigmets() {
        if (!sigmetsEnabled) {
            LOG.debug("SIGMET scheduler is disabled");
            return;
        }

        LOG.info("Starting SIGMET/AIRMET data fetch (domestic + international)");
        try {
            sigmetService.deactivateExpired();
            sigmetService.fetchAndStoreSigmets();
            sigmetService.fetchAndStoreInternationalSigmets();
            dataFreshnessService.recordSuccess("awc-sigmets");
            meterRegistry.counter("weather_scheduler_execution_total", "job", "awc-sigmets", "result", "success").increment();
            LOG.info("SIGMET/AIRMET data fetch completed (domestic + international)");
        } catch (Exception e) {
            meterRegistry.counter("weather_scheduler_execution_total", "job", "awc-sigmets", "result", "failure").increment();
            LOG.error("Error in SIGMET scheduler", e);
        }
    }

    /**
     * Fetch FAA airport delays every 5 minutes
     */
    @Scheduled(cron = "0 */5 * * * ?", identity = "airport-delay-fetch")
    public void fetchAirportDelays() {
        if (!delaysEnabled) {
            LOG.debug("Airport delay scheduler is disabled");
            return;
        }

        LOG.info("Starting airport delay data fetch");
        try {
            airportDelayService.fetchAndStoreDelays();
            dataFreshnessService.recordSuccess("faa-delays");
            meterRegistry.counter("weather_scheduler_execution_total", "job", "faa-delays", "result", "success").increment();
            LOG.info("Airport delay data fetch completed");
        } catch (Exception e) {
            meterRegistry.counter("weather_scheduler_execution_total", "job", "faa-delays", "result", "failure").increment();
            LOG.error("Error in airport delay scheduler", e);
        }
    }

    /**
     * Fetch CWAs every 10 minutes (short-lived advisories)
     */
    @Scheduled(cron = "0 */10 * * * ?", identity = "cwa-fetch")
    public void fetchCwas() {
        if (!cwasEnabled) {
            LOG.debug("CWA scheduler is disabled");
            return;
        }

        LOG.info("Starting CWA data fetch");
        try {
            cwaService.deactivateExpired();
            cwaService.fetchAndStoreCwas();
            dataFreshnessService.recordSuccess("awc-cwas");
            meterRegistry.counter("weather_scheduler_execution_total", "job", "awc-cwas", "result", "success").increment();
            LOG.info("CWA data fetch completed");
        } catch (Exception e) {
            meterRegistry.counter("weather_scheduler_execution_total", "job", "awc-cwas", "result", "failure").increment();
            LOG.error("Error in CWA scheduler", e);
        }
    }

    /**
     * Fetch winds/temps aloft every 6 hours (forecast product, updated 4x daily)
     */
    @Scheduled(cron = "0 0 */6 * * ?", identity = "winds-aloft-fetch")
    public void fetchWindsAloft() {
        if (!windsAloftEnabled) {
            LOG.debug("Winds aloft scheduler is disabled");
            return;
        }

        LOG.info("Starting winds aloft data fetch");
        try {
            windsAloftService.fetchAndStoreWinds();
            dataFreshnessService.recordSuccess("awc-winds-aloft");
            meterRegistry.counter("weather_scheduler_execution_total", "job", "awc-winds-aloft", "result", "success").increment();
            LOG.info("Winds aloft data fetch completed");
        } catch (Exception e) {
            meterRegistry.counter("weather_scheduler_execution_total", "job", "awc-winds-aloft", "result", "failure").increment();
            LOG.error("Error in winds aloft scheduler", e);
        }
    }

    /**
     * Clean up old forecast data daily at 2 AM.
     * Guards against data starvation: only cleans up if fresh data exists.
     */
    @Scheduled(cron = "0 0 2 * * ?", identity = "cleanup-old-data")
    public void cleanupOldData() {
        LOG.info("Starting cleanup of old weather data");

        try {
            LocalDateTime sevenDaysAgo = LocalDateTime.now().minusDays(7);

            // Safety check: only clean up if we have fresh data to replace it
            boolean hasFreshForecasts = dataFreshnessService.hasRecentForecasts(Duration.ofHours(2));
            boolean hasFreshMetar = dataFreshnessService.hasRecentMetarData(Duration.ofHours(1));

            if (!hasFreshForecasts) {
                LOG.warn("SKIPPING forecast cleanup: no fresh forecast data available. "
                    + "Retaining old data to prevent empty dashboard.");
            } else {
                weatherForecastService.deactivateOldForecasts(sevenDaysAgo);
            }

            if (!hasFreshMetar) {
                LOG.warn("SKIPPING airport weather cleanup: no fresh METAR data available. "
                    + "Retaining old data to prevent empty dashboard.");
            } else {
                airportWeatherService.deactivateOldReports(sevenDaysAgo);
            }

            // Hurricane, earthquake, alert, PIREP, SIGMET, and delay cleanup is always safe:
            // old advisories are genuinely stale; deactivateExpired only removes truly expired alerts
            hurricaneService.deactivateOldAdvisories(sevenDaysAgo);
            earthquakeService.deactivateOldEvents(sevenDaysAgo);
            weatherAlertService.deactivateExpired();
            pirepService.deactivateOldReports(sevenDaysAgo);
            sigmetService.deactivateExpired();
            sigmetService.deactivateOldEntries(sevenDaysAgo);
            airportDelayService.deactivateOldDelays(sevenDaysAgo);
            cwaService.deactivateExpired();
            cwaService.deactivateOldEntries(sevenDaysAgo);
            windsAloftService.deactivateOldForecasts(sevenDaysAgo);

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

    private int[] parallelProcess(List<LocationEntity> items,
                                   java.util.function.Consumer<LocationEntity> action,
                                   String jobName) {
        Semaphore semaphore = new Semaphore(parallelism);
        int[] successCount = {0};
        int[] failureCount = {0};

        List<CompletableFuture<Void>> futures = new ArrayList<>();
        for (LocationEntity item : items) {
            futures.add(CompletableFuture.runAsync(() -> {
                try {
                    semaphore.acquire();
                    try {
                        action.accept(item);
                        synchronized (successCount) { successCount[0]++; }
                    } finally {
                        semaphore.release();
                    }
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    synchronized (failureCount) { failureCount[0]++; }
                } catch (Exception e) {
                    LOG.error("Failed " + jobName + " for: " + item.name, e);
                    synchronized (failureCount) { failureCount[0]++; }
                }
            }));
        }

        CompletableFuture.allOf(futures.toArray(new CompletableFuture[0])).join();
        return new int[]{successCount[0], failureCount[0]};
    }
}
