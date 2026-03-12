package com.redhat.weather.service;

import io.quarkus.test.junit.QuarkusTest;
import jakarta.inject.Inject;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

@QuarkusTest
class TfrServiceTest {

    @Inject
    TfrService tfrService;

    @Test
    void testGetActiveTfrsReturnsNonNull() {
        assertNotNull(tfrService.getActiveTfrs());
    }

    @Test
    void testFetchAndStoreTfrsHandlesApiErrors() {
        assertDoesNotThrow(() -> tfrService.fetchAndStoreTfrs());
    }
}
