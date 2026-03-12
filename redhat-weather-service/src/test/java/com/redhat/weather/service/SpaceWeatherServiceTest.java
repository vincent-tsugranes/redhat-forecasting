package com.redhat.weather.service;

import com.redhat.weather.dto.SpaceWeatherDTO;
import io.quarkus.test.junit.QuarkusTest;
import jakarta.inject.Inject;
import org.junit.jupiter.api.Test;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

@QuarkusTest
class SpaceWeatherServiceTest {

    @Inject
    SpaceWeatherService spaceWeatherService;

    @Test
    void testGetSpaceWeatherReturnsResult() {
        // May return data or empty depending on API availability, but should not throw
        Optional<SpaceWeatherDTO> result = spaceWeatherService.getSpaceWeather();
        assertNotNull(result);
    }

    @Test
    void testGetSpaceWeatherHandlesApiErrors() {
        // Should gracefully handle any API failures
        assertDoesNotThrow(() -> spaceWeatherService.getSpaceWeather());
    }

    @Test
    void testSpaceWeatherDTODefaults() {
        // Verify DTO has sensible defaults when created
        SpaceWeatherDTO dto = new SpaceWeatherDTO();
        assertNotNull(dto.alerts);
        assertTrue(dto.alerts.isEmpty());
    }
}
