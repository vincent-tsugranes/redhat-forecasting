package com.redhat.weather.service;

import com.redhat.weather.domain.entity.EarthquakeEntity;
import io.quarkus.test.junit.QuarkusTest;
import jakarta.inject.Inject;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@QuarkusTest
class EarthquakeServiceTest {

    @Inject
    EarthquakeService earthquakeService;

    @Test
    void testGetRecentEarthquakesReturnsNonNull() {
        List<EarthquakeEntity> earthquakes = earthquakeService.getRecentEarthquakes();
        assertNotNull(earthquakes);
    }

    @Test
    void testGetSignificantEarthquakesReturnsNonNull() {
        List<EarthquakeEntity> earthquakes = earthquakeService.getSignificantEarthquakes();
        assertNotNull(earthquakes);
    }

    @Test
    void testDeactivateOldEventsDoesNotThrow() {
        // Deactivating events older than now should succeed without error
        assertDoesNotThrow(() ->
            earthquakeService.deactivateOldEvents(LocalDateTime.now().minusDays(30))
        );
    }

    @Test
    void testFetchAndStoreHandlesApiErrors() {
        // This calls the actual USGS API which may or may not be available in test
        // The method should handle errors gracefully without throwing
        assertDoesNotThrow(() -> earthquakeService.fetchAndStoreEarthquakes());
    }
}
