package com.redhat.weather.health;

import com.redhat.weather.domain.repository.LocationRepository;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.eclipse.microprofile.health.HealthCheck;
import org.eclipse.microprofile.health.HealthCheckResponse;
import org.eclipse.microprofile.health.Readiness;

@Readiness
@ApplicationScoped
public class DataLoadingHealthCheck implements HealthCheck {

    @Inject
    LocationRepository locationRepository;

    @Override
    public HealthCheckResponse call() {
        long airportCount = locationRepository.count("locationType = ?1", "airport");

        return HealthCheckResponse.named("airport-data-loaded")
                .status(airportCount > 0)
                .withData("airportCount", airportCount)
                .withData("expectedCount", 9313)
                .withData("loadingComplete", airportCount >= 9313)
                .build();
    }
}
