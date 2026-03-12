package com.redhat.weather.service;

import io.quarkus.test.junit.QuarkusTest;
import jakarta.inject.Inject;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

@QuarkusTest
class PirepServiceTest {

    @Inject
    PirepService pirepService;

    @Test
    void testGetRecentPirepsReturnsNonNull() {
        assertNotNull(pirepService.getRecentPireps());
    }

    @Test
    void testFetchAndStorePirepsHandlesApiErrors() {
        assertDoesNotThrow(() -> pirepService.fetchAndStorePireps());
    }
}
