package com.redhat.weather.service;

import com.redhat.weather.domain.entity.LocationEntity;
import io.quarkus.test.junit.QuarkusTest;
import jakarta.inject.Inject;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

@QuarkusTest
class LocationServiceTest {

    @Inject
    LocationService locationService;

    @Test
    void testGetAllLocationsReturnsNonNull() {
        List<LocationEntity> locations = locationService.getAllLocations();
        assertNotNull(locations);
    }

    @Test
    void testGetLocationByIdNotFound() {
        Optional<LocationEntity> location = locationService.getLocationById(999999L);
        assertTrue(location.isEmpty());
    }

    @Test
    void testGetAirportLocationsReturnsNonNull() {
        List<LocationEntity> airports = locationService.getAirportLocations();
        assertNotNull(airports);
    }

    @Test
    void testSearchLocationsByNameReturnsNonNull() {
        List<LocationEntity> results = locationService.searchLocationsByName("nonexistent-xyz");
        assertNotNull(results);
        assertTrue(results.isEmpty());
    }

    @Test
    void testPaginatedLocations() {
        List<LocationEntity> page = locationService.getAllLocations(0, 5);
        assertNotNull(page);
        assertTrue(page.size() <= 5);
    }

    @Test
    void testCountAllLocations() {
        long count = locationService.countAllLocations();
        assertTrue(count >= 0);
    }

    @Test
    void testGetLocationsByType() {
        List<LocationEntity> cities = locationService.getLocationsByType("city");
        assertNotNull(cities);
    }

    @Test
    void testCreateUpdateDeleteLocation() {
        LocationEntity loc = new LocationEntity();
        loc.name = "Service Test City";
        loc.state = "NY";
        loc.country = "US";
        loc.latitude = BigDecimal.valueOf(40.7128);
        loc.longitude = BigDecimal.valueOf(-74.0060);
        loc.locationType = "city";

        // Create
        LocationEntity created = locationService.createLocation(loc);
        assertNotNull(created.id);
        assertEquals("Service Test City", created.name);

        // Update
        LocationEntity update = new LocationEntity();
        update.name = "Updated Service City";
        update.state = "NY";
        update.country = "US";
        update.latitude = BigDecimal.valueOf(40.7128);
        update.longitude = BigDecimal.valueOf(-74.0060);
        update.locationType = "city";

        LocationEntity updated = locationService.updateLocation(created.id, update);
        assertEquals("Updated Service City", updated.name);

        // Delete
        boolean deleted = locationService.deleteLocation(created.id);
        assertTrue(deleted);

        // Verify deleted
        assertTrue(locationService.getLocationById(created.id).isEmpty());
    }
}
