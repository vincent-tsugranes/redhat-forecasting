package com.redhat.weather.health;

import jakarta.enterprise.context.ApplicationScoped;
import org.eclipse.microprofile.health.HealthCheck;
import org.eclipse.microprofile.health.HealthCheckResponse;
import org.eclipse.microprofile.health.Liveness;

import java.lang.management.ManagementFactory;
import java.lang.management.MemoryMXBean;
import java.time.Duration;
import java.time.Instant;

@Liveness
@ApplicationScoped
public class LivenessHealthCheck implements HealthCheck {

    private final Instant startTime = Instant.now();

    @Override
    public HealthCheckResponse call() {
        MemoryMXBean memory = ManagementFactory.getMemoryMXBean();
        long heapUsed = memory.getHeapMemoryUsage().getUsed();
        long heapMax = memory.getHeapMemoryUsage().getMax();
        double heapPercent = heapMax > 0 ? (double) heapUsed / heapMax * 100 : 0;
        long uptimeSeconds = Duration.between(startTime, Instant.now()).toSeconds();

        return HealthCheckResponse.named("weather-service-live")
                .status(true)
                .withData("uptimeSeconds", uptimeSeconds)
                .withData("heapUsedMB", heapUsed / (1024 * 1024))
                .withData("heapMaxMB", heapMax / (1024 * 1024))
                .withData("heapUsedPercent", String.format("%.1f%%", heapPercent))
                .build();
    }
}
