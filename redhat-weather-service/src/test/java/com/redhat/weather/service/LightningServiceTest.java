package com.redhat.weather.service;

import io.quarkus.test.junit.QuarkusTest;
import jakarta.inject.Inject;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.*;

@QuarkusTest
class LightningServiceTest {

    @Inject
    LightningService lightningService;

    @Test
    void testGetRecentStrikesReturnsNonNull() {
        assertNotNull(lightningService.getRecentStrikes());
    }

    @Test
    void testGetRecentCountReturnsNonNegative() {
        assertTrue(lightningService.getRecentCount() >= 0);
    }

    @Test
    void testFetchAndStoreStrikesHandlesApiErrors() {
        assertDoesNotThrow(() -> lightningService.fetchAndStoreStrikes());
    }

    @Test
    void testDeactivateOldStrikesDoesNotThrow() {
        assertDoesNotThrow(() -> lightningService.deactivateOldStrikes(LocalDateTime.now().minusDays(1)));
    }
}
