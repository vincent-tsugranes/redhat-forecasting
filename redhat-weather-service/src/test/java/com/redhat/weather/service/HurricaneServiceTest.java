package com.redhat.weather.service;

import com.redhat.weather.domain.entity.HurricaneEntity;
import io.quarkus.test.junit.QuarkusTest;
import jakarta.inject.Inject;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@QuarkusTest
class HurricaneServiceTest {

    @Inject
    HurricaneService hurricaneService;

    @Test
    void testGetActiveStormsReturnsNonNull() {
        List<HurricaneEntity> storms = hurricaneService.getActiveStorms();
        assertNotNull(storms);
    }

    @Test
    void testGetStormByIdReturnsNonNull() {
        List<HurricaneEntity> storms = hurricaneService.getStormById("NONEXISTENT");
        assertNotNull(storms);
        assertTrue(storms.isEmpty());
    }

    @Test
    void testGetStormTrackReturnsNonNull() {
        LocalDateTime from = LocalDateTime.now().minusDays(7);
        LocalDateTime to = LocalDateTime.now();
        List<HurricaneEntity> track = hurricaneService.getStormTrack("NONEXISTENT", from, to);
        assertNotNull(track);
        assertTrue(track.isEmpty());
    }

    @Test
    void testDeactivateOldAdvisoriesDoesNotThrow() {
        assertDoesNotThrow(() ->
            hurricaneService.deactivateOldAdvisories(LocalDateTime.now().minusDays(30))
        );
    }

    @Test
    void testFetchAndStoreHandlesApiErrors() {
        assertDoesNotThrow(() -> hurricaneService.fetchAndStoreActiveStorms());
    }
}
