package com.redhat.weather.service;

import io.quarkus.test.junit.QuarkusTest;
import jakarta.inject.Inject;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

@QuarkusTest
class CwaServiceTest {

    @Inject
    CwaService cwaService;

    @Test
    void testGetActiveCwasReturnsNonNull() {
        assertNotNull(cwaService.getActiveCwas());
    }

    @Test
    void testFetchAndStoreCwasHandlesApiErrors() {
        assertDoesNotThrow(() -> cwaService.fetchAndStoreCwas());
    }
}
