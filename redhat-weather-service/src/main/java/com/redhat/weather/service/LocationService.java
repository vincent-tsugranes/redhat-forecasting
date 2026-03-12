package com.redhat.weather.service;

import com.redhat.weather.domain.entity.LocationEntity;
import com.redhat.weather.domain.repository.LocationRepository;
import io.quarkus.cache.CacheInvalidateAll;
import io.quarkus.cache.CacheResult;
import io.quarkus.runtime.StartupEvent;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.event.Observes;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;
import org.jboss.logging.Logger;

import java.util.List;
import java.util.Optional;

@ApplicationScoped
public class LocationService {

    private static final Logger LOG = Logger.getLogger(LocationService.class);

    @Inject
    LocationRepository locationRepository;

    void onStartup(@Observes StartupEvent ev) {
        LOG.info("Warming location caches...");
        try {
            List<LocationEntity> all = getAllLocations();
            LOG.info("Location caches warmed: " + all.size() + " locations (all airports)");
        } catch (Exception e) {
            LOG.warn("Failed to warm location caches: " + e.getMessage());
        }
    }

    @CacheResult(cacheName = "locations-all")
    public List<LocationEntity> getAllLocations() {
        return locationRepository.getAllLocations();
    }

    public Optional<LocationEntity> getLocationById(Long id) {
        return locationRepository.findByIdOptional(id);
    }

    @CacheResult(cacheName = "locations-airports")
    public List<LocationEntity> getAirportLocations() {
        return locationRepository.findAirportLocations();
    }

    public Optional<LocationEntity> getLocationByAirportCode(String airportCode) {
        return locationRepository.findByAirportCode(airportCode);
    }

    @Transactional
    @CacheInvalidateAll(cacheName = "locations-all")
    @CacheInvalidateAll(cacheName = "locations-airports")
    public LocationEntity createLocation(LocationEntity location) {
        locationRepository.persist(location);
        return location;
    }

    @Transactional
    @CacheInvalidateAll(cacheName = "locations-all")
    @CacheInvalidateAll(cacheName = "locations-airports")
    public LocationEntity updateLocation(Long id, LocationEntity updatedLocation) {
        LocationEntity existingLocation = locationRepository.findById(id);
        if (existingLocation != null) {
            existingLocation.name = updatedLocation.name;
            existingLocation.latitude = updatedLocation.latitude;
            existingLocation.longitude = updatedLocation.longitude;
            existingLocation.airportCode = updatedLocation.airportCode;
            existingLocation.state = updatedLocation.state;
            existingLocation.country = updatedLocation.country;
            existingLocation.metadata = updatedLocation.metadata;
            locationRepository.persist(existingLocation);
        }
        return existingLocation;
    }

    @Transactional
    @CacheInvalidateAll(cacheName = "locations-all")
    @CacheInvalidateAll(cacheName = "locations-airports")
    public boolean deleteLocation(Long id) {
        return locationRepository.deleteById(id);
    }

    // Paginated query methods

    public List<LocationEntity> getAllLocations(int page, int size) {
        return locationRepository.getAllLocationsPaginated(page, size);
    }

    public long countAllLocations() {
        return locationRepository.countAllLocations();
    }

    public List<LocationEntity> getAirportLocations(int page, int size) {
        return locationRepository.findAirportLocationsPaginated(page, size);
    }

    public long countAirportLocations() {
        return locationRepository.countAirportLocations();
    }

    public List<LocationEntity> searchLocationsByName(String name, int page, int size) {
        return locationRepository.searchByNamePaginated(name, page, size);
    }

    public long countLocationsByName(String name) {
        return locationRepository.countByName(name);
    }
}
