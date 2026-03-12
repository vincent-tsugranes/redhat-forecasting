package com.redhat.weather.service;

import io.quarkus.test.junit.QuarkusTest;
import jakarta.inject.Inject;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

@QuarkusTest
class AirportDelayServiceTest {

    @Inject
    AirportDelayService airportDelayService;

    @Test
    void testGetActiveDelaysReturnsNonNull() {
        assertNotNull(airportDelayService.getActiveDelays());
    }

    @Test
    void testFetchAndStoreDelaysHandlesApiErrors() {
        assertDoesNotThrow(() -> airportDelayService.fetchAndStoreDelays());
    }
}
