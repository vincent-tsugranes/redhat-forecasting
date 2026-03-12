package com.redhat.weather.domain.repository;

import com.redhat.weather.domain.entity.LocationEntity;
import io.quarkus.hibernate.orm.panache.PanacheRepositoryBase;
import io.quarkus.panache.common.Page;
import jakarta.enterprise.context.ApplicationScoped;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

@ApplicationScoped
public class LocationRepository implements PanacheRepositoryBase<LocationEntity, Long> {

    public List<LocationEntity> getAllLocations() {
        return listAll();
    }

    public Optional<LocationEntity> findByIdOptional(Long id) {
        return find("id", id).firstResultOptional();
    }

    public List<LocationEntity> findAirportLocations() {
        return listAll();
    }

    public Optional<LocationEntity> findByAirportCode(String airportCode) {
        return find("airportCode", airportCode).firstResultOptional();
    }

    public List<LocationEntity> findByState(String state) {
        return list("state", state);
    }

    public List<LocationEntity> findByCountry(String country) {
        return list("country", country);
    }

    public List<LocationEntity> findByCoordinates(BigDecimal latitude, BigDecimal longitude) {
        return list("latitude = ?1 AND longitude = ?2", latitude, longitude);
    }

    public List<LocationEntity> findNearby(BigDecimal latitude, BigDecimal longitude, double radiusDegrees) {
        // Simple bounding box query
        return list(
            "latitude BETWEEN ?1 AND ?2 AND longitude BETWEEN ?3 AND ?4",
            latitude.subtract(BigDecimal.valueOf(radiusDegrees)),
            latitude.add(BigDecimal.valueOf(radiusDegrees)),
            longitude.subtract(BigDecimal.valueOf(radiusDegrees)),
            longitude.add(BigDecimal.valueOf(radiusDegrees))
        );
    }

    public List<LocationEntity> searchByName(String name) {
        return list("LOWER(name) LIKE LOWER(?1)", "%" + name + "%");
    }

    // Paginated query methods

    public List<LocationEntity> getAllLocationsPaginated(int page, int size) {
        return findAll().page(Page.of(page, size)).list();
    }

    public long countAllLocations() {
        return count();
    }

    public List<LocationEntity> findAirportLocationsPaginated(int page, int size) {
        return findAll().page(Page.of(page, size)).list();
    }

    public long countAirportLocations() {
        return count();
    }

    public List<LocationEntity> searchByNamePaginated(String name, int page, int size) {
        return find("LOWER(name) LIKE LOWER(?1)", "%" + name + "%")
            .page(Page.of(page, size)).list();
    }

    public long countByName(String name) {
        return count("LOWER(name) LIKE LOWER(?1)", "%" + name + "%");
    }
}
