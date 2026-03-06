package com.redhat.weather.scheduler;

import com.redhat.weather.service.DataFreshnessService;
import io.quarkus.test.junit.QuarkusTest;
import jakarta.inject.Inject;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

@QuarkusTest
class WeatherDataSchedulerTest {

    @Inject
    WeatherDataScheduler scheduler;

    @Inject
    DataFreshnessService dataFreshnessService;

    @Test
    void testSchedulerBeanIsCreated() {
        assertNotNull(scheduler);
    }

    @Test
    void testFetchEarthquakesDoesNotThrow() {
        // Earthquake fetch may fail due to external API being unavailable,
        // but it should handle errors gracefully without throwing
        assertDoesNotThrow(() -> scheduler.fetchEarthquakes());
    }

    @Test
    void testFetchWeatherAlertsDoesNotThrow() {
        assertDoesNotThrow(() -> scheduler.fetchWeatherAlerts());
    }

    @Test
    void testFetchHurricanesDoesNotThrow() {
        assertDoesNotThrow(() -> scheduler.fetchHurricanes());
    }

    @Test
    void testFetchNoaaForecastsDoesNotThrow() {
        assertDoesNotThrow(() -> scheduler.fetchNoaaForecasts());
    }

    @Test
    void testFetchAirportWeatherDoesNotThrow() {
        assertDoesNotThrow(() -> scheduler.fetchAirportWeather());
    }

    @Test
    void testFetchOpenWeatherForecastsDoesNotThrowWhenDisabled() {
        // OpenWeatherMap is disabled by default in test profile
        assertDoesNotThrow(() -> scheduler.fetchOpenWeatherForecasts());
    }

    @Test
    void testCleanupOldDataDoesNotThrow() {
        assertDoesNotThrow(() -> scheduler.cleanupOldData());
    }
}
