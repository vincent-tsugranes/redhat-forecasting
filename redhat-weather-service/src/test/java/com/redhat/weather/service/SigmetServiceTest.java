package com.redhat.weather.service;

import io.quarkus.test.junit.QuarkusTest;
import jakarta.inject.Inject;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.*;

@QuarkusTest
class SigmetServiceTest {

    @Inject
    SigmetService sigmetService;

    @Test
    void testGetActiveSigmetsReturnsNonNull() {
        assertNotNull(sigmetService.getActiveSigmets());
    }

    @Test
    void testDeactivateExpiredDoesNotThrow() {
        assertDoesNotThrow(() -> sigmetService.deactivateExpired());
    }

    @Test
    void testFetchAndStoreSigmetsHandlesApiErrors() {
        assertDoesNotThrow(() -> sigmetService.fetchAndStoreSigmets());
    }
}
