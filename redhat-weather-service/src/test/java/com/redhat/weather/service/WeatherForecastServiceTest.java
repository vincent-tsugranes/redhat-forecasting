package com.redhat.weather.service;

import com.redhat.weather.domain.entity.WeatherForecastEntity;
import io.quarkus.test.junit.QuarkusTest;
import jakarta.inject.Inject;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@QuarkusTest
class WeatherForecastServiceTest {

    @Inject
    WeatherForecastService weatherForecastService;

    @Test
    void testGetForecastsByLocationReturnsNonNull() {
        List<WeatherForecastEntity> forecasts = weatherForecastService.getForecastsByLocation(1L);
        assertNotNull(forecasts);
    }

    @Test
    void testGetForecastsByLocationPaginated() {
        List<WeatherForecastEntity> forecasts = weatherForecastService.getForecastsByLocation(1L, 0, 10);
        assertNotNull(forecasts);
        assertTrue(forecasts.size() <= 10);
    }

    @Test
    void testGetForecastsByCoordinatesReturnsNonNull() {
        LocalDateTime from = LocalDateTime.now().minusDays(1);
        LocalDateTime to = LocalDateTime.now().plusDays(1);
        List<WeatherForecastEntity> forecasts = weatherForecastService.getForecastsByCoordinates(
            BigDecimal.valueOf(40.7128), BigDecimal.valueOf(-74.0060), from, to
        );
        assertNotNull(forecasts);
    }

    @Test
    void testGetForecastsByCoordinatesPaginated() {
        LocalDateTime from = LocalDateTime.now().minusDays(1);
        LocalDateTime to = LocalDateTime.now().plusDays(1);
        List<WeatherForecastEntity> forecasts = weatherForecastService.getForecastsByCoordinates(
            BigDecimal.valueOf(40.7128), BigDecimal.valueOf(-74.0060), from, to, 0, 5
        );
        assertNotNull(forecasts);
        assertTrue(forecasts.size() <= 5);
    }

    @Test
    void testPageSizeClampedToMax() {
        // Request size > 200 should be clamped to 200
        List<WeatherForecastEntity> forecasts = weatherForecastService.getForecastsByLocation(1L, 0, 500);
        assertNotNull(forecasts);
        assertTrue(forecasts.size() <= 200);
    }

    @Test
    void testGetHistoricalForecastsReturnsNonNull() {
        List<WeatherForecastEntity> forecasts = weatherForecastService.getHistoricalForecasts(1L, 7);
        assertNotNull(forecasts);
    }

    @Test
    void testGetCurrentForecastReturnsNonNull() {
        List<WeatherForecastEntity> forecasts = weatherForecastService.getCurrentForecast(
            BigDecimal.valueOf(40.7128), BigDecimal.valueOf(-74.0060)
        );
        assertNotNull(forecasts);
    }

    @Test
    void testDeactivateOldForecastsDoesNotThrow() {
        assertDoesNotThrow(() ->
            weatherForecastService.deactivateOldForecasts(LocalDateTime.now().minusDays(30))
        );
    }
}
