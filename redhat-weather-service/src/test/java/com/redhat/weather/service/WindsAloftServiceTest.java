package com.redhat.weather.service;

import io.quarkus.test.junit.QuarkusTest;
import jakarta.inject.Inject;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

@QuarkusTest
class WindsAloftServiceTest {

    @Inject
    WindsAloftService windsAloftService;

    @Test
    void testGetLatestWindsAloftReturnsNonNull() {
        assertNotNull(windsAloftService.getLatestWinds());
    }

    @Test
    void testFetchAndStoreWindsAloftHandlesApiErrors() {
        assertDoesNotThrow(() -> windsAloftService.fetchAndStoreWinds());
    }
}
