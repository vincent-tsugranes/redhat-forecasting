package com.redhat.weather.resource;

import com.redhat.weather.domain.repository.AirportWeatherRepository;
import com.redhat.weather.domain.repository.EarthquakeRepository;
import com.redhat.weather.domain.repository.HurricaneRepository;
import com.redhat.weather.domain.repository.LocationRepository;
import com.redhat.weather.domain.repository.WeatherForecastRepository;
import com.redhat.weather.service.DataFreshnessService;
import jakarta.inject.Inject;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.eclipse.microprofile.openapi.annotations.Operation;
import org.eclipse.microprofile.openapi.annotations.tags.Tag;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Path("/api/status")
@Tag(name = "Data Status", description = "Data loading and system status information")
public class DataStatusResource {

    @Inject
    LocationRepository locationRepository;

    @Inject
    WeatherForecastRepository forecastRepository;

    @Inject
    EarthquakeRepository earthquakeRepository;

    @Inject
    HurricaneRepository hurricaneRepository;

    @Inject
    AirportWeatherRepository airportWeatherRepository;

    @Inject
    DataFreshnessService dataFreshnessService;

    @ConfigProperty(name = "weather.scheduler.noaa.enabled", defaultValue = "true")
    boolean noaaEnabled;

    @ConfigProperty(name = "weather.scheduler.aviation.enabled", defaultValue = "true")
    boolean aviationEnabled;

    @ConfigProperty(name = "weather.scheduler.earthquake.enabled", defaultValue = "true")
    boolean earthquakeEnabled;

    @ConfigProperty(name = "weather.scheduler.hurricane.enabled", defaultValue = "true")
    boolean hurricaneEnabled;

    @ConfigProperty(name = "weather.scheduler.alerts.enabled", defaultValue = "true")
    boolean alertsEnabled;

    @GET
    @Path("/data")
    @Produces(MediaType.APPLICATION_JSON)
    @Operation(summary = "Get data loading status", description = "Returns the current status of data loading including airport counts")
    public Map<String, Object> getDataStatus() {
        Map<String, Object> status = new HashMap<>();

        long totalLocations = locationRepository.count();
        long airportCount = locationRepository.count("locationType = ?1", "airport");
        long cityCount = locationRepository.count("locationType = ?1", "city");

        status.put("totalLocations", totalLocations);
        status.put("airports", airportCount);
        status.put("cities", cityCount);
        status.put("airportsLoaded", airportCount > 0);
        status.put("expectedAirports", 9313); // From CSV
        status.put("loadingComplete", airportCount >= 9313);

        if (airportCount > 0) {
            double percentLoaded = (airportCount / 9313.0) * 100;
            status.put("percentLoaded", Math.round(percentLoaded * 100.0) / 100.0);
        } else {
            status.put("percentLoaded", 0.0);
        }

        // Active data counts
        status.put("activeForecasts", forecastRepository.count("isActive = true"));
        status.put("activeEarthquakes", earthquakeRepository.count("isActive = true AND eventTime >= ?1", LocalDateTime.now().minusHours(24)));
        status.put("activeHurricanes", hurricaneRepository.count("isActive = true AND status IN ('active', 'Active')"));
        status.put("metarReports", airportWeatherRepository.count("reportType = 'METAR' AND isActive = true"));

        status.put("dataFreshness", dataFreshnessService.getFreshnessSnapshot());

        // Scheduler timing info
        status.put("schedulers", buildSchedulerList());

        return status;
    }

    private List<Map<String, Object>> buildSchedulerList() {
        List<Map<String, Object>> schedulers = new ArrayList<>();
        schedulers.add(buildSchedulerInfo("NOAA Forecasts", "noaa-forecast", 30, noaaEnabled));
        schedulers.add(buildSchedulerInfo("Airport Weather", "aviation-metar", 15, aviationEnabled));
        schedulers.add(buildSchedulerInfo("Earthquakes", "usgs-earthquake", 10, earthquakeEnabled));
        schedulers.add(buildSchedulerInfo("Hurricanes", "nhc-hurricane", 60, hurricaneEnabled));
        schedulers.add(buildSchedulerInfo("Weather Alerts", "noaa-alerts", 15, alertsEnabled));
        return schedulers;
    }

    private Map<String, Object> buildSchedulerInfo(String name, String source, int intervalMinutes, boolean enabled) {
        Map<String, Object> info = new LinkedHashMap<>();
        info.put("name", name);
        info.put("source", source);
        info.put("intervalMinutes", intervalMinutes);
        info.put("enabled", enabled);

        LocalDateTime lastRun = dataFreshnessService.getLastSuccess(source);
        if (lastRun != null) {
            long ageMinutes = Duration.between(lastRun, LocalDateTime.now()).toMinutes();
            long nextRunMinutes = Math.max(0, intervalMinutes - ageMinutes);
            info.put("lastRun", lastRun.toString());
            info.put("ageMinutes", ageMinutes);
            info.put("nextRunMinutes", nextRunMinutes);
        } else {
            info.put("lastRun", null);
            info.put("ageMinutes", null);
            info.put("nextRunMinutes", null);
        }

        return info;
    }
}
