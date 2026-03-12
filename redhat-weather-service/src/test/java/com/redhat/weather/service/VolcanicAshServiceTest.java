package com.redhat.weather.service;

import io.quarkus.test.junit.QuarkusTest;
import jakarta.inject.Inject;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

@QuarkusTest
class VolcanicAshServiceTest {

    @Inject
    VolcanicAshService volcanicAshService;

    @Test
    void testGetActiveAdvisoriesReturnsNonNull() {
        assertNotNull(volcanicAshService.getActiveAdvisories());
    }

    @Test
    void testDeactivateExpiredDoesNotThrow() {
        assertDoesNotThrow(() -> volcanicAshService.deactivateExpired());
    }

    @Test
    void testFetchAndStoreAdvisoriesHandlesApiErrors() {
        assertDoesNotThrow(() -> volcanicAshService.fetchAndStoreAdvisories());
    }
}
