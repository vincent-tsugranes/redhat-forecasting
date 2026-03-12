package com.redhat.weather.service;

import io.quarkus.test.junit.QuarkusTest;
import jakarta.inject.Inject;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.*;

@QuarkusTest
class GroundStopServiceTest {

    @Inject
    GroundStopService groundStopService;

    @Test
    void testGetActiveGroundStopsReturnsNonNull() {
        assertNotNull(groundStopService.getActiveGroundStops());
    }

    @Test
    void testGetByAirportReturnsNonNull() {
        assertNotNull(groundStopService.getByAirport("KJFK"));
    }

    @Test
    void testFetchAndStoreGroundStopsHandlesApiErrors() {
        assertDoesNotThrow(() -> groundStopService.fetchAndStoreGroundStops());
    }

    @Test
    void testDeactivateOldEntriesDoesNotThrow() {
        assertDoesNotThrow(() ->
            groundStopService.deactivateOldEntries(LocalDateTime.now().minusDays(30))
        );
    }
}
