package com.redhat.weather.service;

import com.redhat.weather.domain.repository.AirportWeatherRepository;
import com.redhat.weather.domain.repository.WeatherForecastRepository;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.jboss.logging.Logger;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@ApplicationScoped
public class DataFreshnessService {

    private static final Logger LOG = Logger.getLogger(DataFreshnessService.class);

    private final Map<String, LocalDateTime> lastSuccessfulFetch = new ConcurrentHashMap<>();

    @Inject
    WeatherForecastRepository forecastRepository;

    @Inject
    AirportWeatherRepository airportWeatherRepository;

    public void recordSuccess(String source) {
        lastSuccessfulFetch.put(source, LocalDateTime.now());
        LOG.debug("Recorded successful fetch for source: " + source);
    }

    public LocalDateTime getLastSuccess(String source) {
        return lastSuccessfulFetch.get(source);
    }

    public boolean hasRecentForecasts(Duration maxAge) {
        LocalDateTime cutoff = LocalDateTime.now().minus(maxAge);
        return forecastRepository.count("fetchedAt > ?1 AND isActive = true", cutoff) > 0;
    }

    public boolean hasRecentMetarData(Duration maxAge) {
        LocalDateTime cutoff = LocalDateTime.now().minus(maxAge);
        return airportWeatherRepository.count(
            "reportType = 'METAR' AND fetchedAt > ?1 AND isActive = true", cutoff) > 0;
    }

    public Map<String, Object> getFreshnessSnapshot() {
        Map<String, Object> snapshot = new LinkedHashMap<>();
        lastSuccessfulFetch.forEach((source, time) -> {
            Duration age = Duration.between(time, LocalDateTime.now());
            snapshot.put(source + ".lastSuccess", time.toString());
            snapshot.put(source + ".ageMinutes", age.toMinutes());
        });
        return snapshot;
    }
}
