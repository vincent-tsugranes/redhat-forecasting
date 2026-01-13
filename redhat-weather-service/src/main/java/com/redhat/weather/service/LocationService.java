package com.redhat.weather.service;

import com.redhat.weather.domain.entity.LocationEntity;
import com.redhat.weather.domain.repository.LocationRepository;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;

import java.util.List;
import java.util.Optional;

@ApplicationScoped
public class LocationService {

    @Inject
    LocationRepository locationRepository;

    public List<LocationEntity> getAllLocations() {
        return locationRepository.getAllLocations();
    }

    public Optional<LocationEntity> getLocationById(Long id) {
        return locationRepository.findByIdOptional(id);
    }

    public List<LocationEntity> getLocationsByType(String locationType) {
        return locationRepository.findByType(locationType);
    }

    public List<LocationEntity> getAirportLocations() {
        return locationRepository.findAirportLocations();
    }

    public Optional<LocationEntity> getLocationByAirportCode(String airportCode) {
        return locationRepository.findByAirportCode(airportCode);
    }

    public List<LocationEntity> searchLocationsByName(String name) {
        return locationRepository.searchByName(name);
    }

    @Transactional
    public LocationEntity createLocation(LocationEntity location) {
        locationRepository.persist(location);
        return location;
    }

    @Transactional
    public LocationEntity updateLocation(Long id, LocationEntity updatedLocation) {
        LocationEntity existingLocation = locationRepository.findById(id);
        if (existingLocation != null) {
            existingLocation.name = updatedLocation.name;
            existingLocation.latitude = updatedLocation.latitude;
            existingLocation.longitude = updatedLocation.longitude;
            existingLocation.locationType = updatedLocation.locationType;
            existingLocation.airportCode = updatedLocation.airportCode;
            existingLocation.state = updatedLocation.state;
            existingLocation.country = updatedLocation.country;
            existingLocation.metadata = updatedLocation.metadata;
            locationRepository.persist(existingLocation);
        }
        return existingLocation;
    }

    @Transactional
    public boolean deleteLocation(Long id) {
        return locationRepository.deleteById(id);
    }
}
