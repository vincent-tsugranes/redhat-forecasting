package com.redhat.weather.service;

import com.redhat.weather.domain.entity.AirportWeatherEntity;
import io.quarkus.test.junit.QuarkusTest;
import jakarta.inject.Inject;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

@QuarkusTest
class AirportWeatherServiceTest {

    @Inject
    AirportWeatherService airportWeatherService;

    @Test
    void testGetAirportWeatherReturnsNonNull() {
        List<AirportWeatherEntity> weather = airportWeatherService.getAirportWeather("KJFK");
        assertNotNull(weather);
    }

    @Test
    void testGetLatestMetarReturnsOptional() {
        Optional<AirportWeatherEntity> metar = airportWeatherService.getLatestMetar("NONEXISTENT");
        assertNotNull(metar);
        assertTrue(metar.isEmpty());
    }

    @Test
    void testGetLatestTafReturnsOptional() {
        Optional<AirportWeatherEntity> taf = airportWeatherService.getLatestTaf("NONEXISTENT");
        assertNotNull(taf);
        assertTrue(taf.isEmpty());
    }

    @Test
    void testDeactivateOldReportsDoesNotThrow() {
        assertDoesNotThrow(() ->
            airportWeatherService.deactivateOldReports(LocalDateTime.now().minusDays(30))
        );
    }

    @Test
    void testFetchAndStoreHandlesNonexistentAirport() {
        // Should handle gracefully when airport doesn't exist in database
        assertDoesNotThrow(() -> airportWeatherService.fetchAndStoreAll("ZZZZ"));
    }

    @Test
    void testFetchAndStoreBatchHandlesEmptyList() {
        assertDoesNotThrow(() -> airportWeatherService.fetchAndStoreAllBatch(List.of()));
    }
}
