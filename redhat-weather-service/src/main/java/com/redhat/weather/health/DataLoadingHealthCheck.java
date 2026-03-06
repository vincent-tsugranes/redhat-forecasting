package com.redhat.weather.health;

import com.redhat.weather.domain.repository.LocationRepository;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.eclipse.microprofile.health.HealthCheck;
import org.eclipse.microprofile.health.HealthCheckResponse;
import org.eclipse.microprofile.health.Readiness;

import java.time.Duration;
import java.time.Instant;

@Readiness
@ApplicationScoped
public class DataLoadingHealthCheck implements HealthCheck {

    private static final Duration STARTUP_GRACE_PERIOD = Duration.ofMinutes(5);
    private final Instant startTime = Instant.now();

    @Inject
    LocationRepository locationRepository;

    @Override
    public HealthCheckResponse call() {
        long airportCount = locationRepository.count("locationType = ?1", "airport");
        boolean withinGracePeriod = Duration.between(startTime, Instant.now()).compareTo(STARTUP_GRACE_PERIOD) < 0;

        return HealthCheckResponse.named("airport-data-loaded")
                .status(airportCount > 0 || withinGracePeriod)
                .withData("airportCount", airportCount)
                .withData("expectedCount", 9313)
                .withData("loadingComplete", airportCount >= 9313)
                .withData("withinStartupGracePeriod", withinGracePeriod)
                .build();
    }
}
