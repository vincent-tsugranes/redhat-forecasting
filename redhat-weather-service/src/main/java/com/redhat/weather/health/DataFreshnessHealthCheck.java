package com.redhat.weather.health;

import com.redhat.weather.service.DataFreshnessService;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.eclipse.microprofile.health.HealthCheck;
import org.eclipse.microprofile.health.HealthCheckResponse;
import org.eclipse.microprofile.health.HealthCheckResponseBuilder;
import org.eclipse.microprofile.health.Readiness;

import java.time.Duration;
import java.time.LocalDateTime;

@Readiness
@ApplicationScoped
public class DataFreshnessHealthCheck implements HealthCheck {

    private static final Duration FORECAST_MAX_AGE = Duration.ofHours(2);
    private static final Duration METAR_MAX_AGE = Duration.ofHours(1);

    @Inject
    DataFreshnessService dataFreshnessService;

    @Override
    public HealthCheckResponse call() {
        HealthCheckResponseBuilder builder = HealthCheckResponse.named("weather-data-freshness");

        boolean forecastFresh = dataFreshnessService.hasRecentForecasts(FORECAST_MAX_AGE);
        boolean metarFresh = dataFreshnessService.hasRecentMetarData(METAR_MAX_AGE);

        builder.withData("forecastDataFresh", forecastFresh);
        builder.withData("metarDataFresh", metarFresh);

        LocalDateTime lastForecast = dataFreshnessService.getLastSuccess("noaa-forecast");
        if (lastForecast != null) {
            builder.withData("lastForecastFetch", lastForecast.toString());
            builder.withData("forecastAgeMinutes", Duration.between(lastForecast, LocalDateTime.now()).toMinutes());
        }

        LocalDateTime lastMetar = dataFreshnessService.getLastSuccess("aviation-metar");
        if (lastMetar != null) {
            builder.withData("lastMetarFetch", lastMetar.toString());
            builder.withData("metarAgeMinutes", Duration.between(lastMetar, LocalDateTime.now()).toMinutes());
        }

        // UP if at least one core data type is fresh (partial data is still useful)
        boolean healthy = forecastFresh || metarFresh;
        builder.status(healthy);

        if (!healthy) {
            builder.withData("warning", "No fresh weather data available. External APIs may be down.");
        }

        return builder.build();
    }
}
