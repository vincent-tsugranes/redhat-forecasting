package com.redhat.weather.service;

import com.redhat.weather.domain.repository.AirportWeatherRepository;
import com.redhat.weather.domain.repository.WeatherForecastRepository;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Tags;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.jboss.logging.Logger;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

@ApplicationScoped
public class DataFreshnessService {

    private static final Logger LOG = Logger.getLogger(DataFreshnessService.class);

    private final Map<String, LocalDateTime> lastSuccessfulFetch = new ConcurrentHashMap<>();
    private final Set<String> registeredGauges = ConcurrentHashMap.newKeySet();

    @Inject
    WeatherForecastRepository forecastRepository;

    @Inject
    AirportWeatherRepository airportWeatherRepository;

    @Inject
    MeterRegistry meterRegistry;

    public void recordSuccess(String source) {
        lastSuccessfulFetch.put(source, LocalDateTime.now());
        meterRegistry.counter("weather_data_fetch_total", "source", source, "result", "success").increment();
        registerFreshnessGauge(source);
        LOG.debug("Recorded successful fetch for source: " + source);
    }

    private void registerFreshnessGauge(String source) {
        if (registeredGauges.add(source)) {
            meterRegistry.gauge("weather_data_freshness_age_seconds",
                Tags.of("source", source), this,
                svc -> {
                    LocalDateTime last = svc.getLastSuccess(source);
                    return last == null ? -1 : Duration.between(last, LocalDateTime.now()).toSeconds();
                });
        }
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
