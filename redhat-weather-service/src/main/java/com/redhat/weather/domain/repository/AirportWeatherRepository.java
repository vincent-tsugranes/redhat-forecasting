package com.redhat.weather.domain.repository;

import com.redhat.weather.domain.entity.AirportWeatherEntity;
import io.quarkus.hibernate.orm.panache.PanacheRepositoryBase;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.transaction.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@ApplicationScoped
public class AirportWeatherRepository implements PanacheRepositoryBase<AirportWeatherEntity, Long> {

    public List<AirportWeatherEntity> findByAirportCode(String airportCode) {
        return list("airportCode = ?1 AND isActive = true ORDER BY observationTime DESC", airportCode);
    }

    public Optional<AirportWeatherEntity> findLatestByAirportCode(String airportCode) {
        return find("airportCode = ?1 AND isActive = true ORDER BY observationTime DESC", airportCode)
            .firstResultOptional();
    }

    public Optional<AirportWeatherEntity> findLatestMetar(String airportCode) {
        return find("airportCode = ?1 AND reportType = 'METAR' AND isActive = true ORDER BY observationTime DESC", airportCode)
            .firstResultOptional();
    }

    public Optional<AirportWeatherEntity> findLatestTaf(String airportCode) {
        return find("airportCode = ?1 AND reportType = 'TAF' AND isActive = true ORDER BY observationTime DESC", airportCode)
            .firstResultOptional();
    }

    public List<AirportWeatherEntity> findByReportType(String reportType) {
        return list("reportType = ?1 AND isActive = true ORDER BY observationTime DESC", reportType);
    }

    public List<AirportWeatherEntity> findByFlightCategory(String flightCategory) {
        return list("flightCategory = ?1 AND isActive = true ORDER BY observationTime DESC", flightCategory);
    }

    public List<AirportWeatherEntity> findByLocation(Long locationId) {
        return list("location.id = ?1 AND isActive = true ORDER BY observationTime DESC", locationId);
    }

    public List<AirportWeatherEntity> findByTimeRange(LocalDateTime from, LocalDateTime to) {
        return list(
            "observationTime >= ?1 AND observationTime <= ?2 AND isActive = true ORDER BY observationTime DESC",
            from, to
        );
    }

    public List<AirportWeatherEntity> findByAirportCodeAndTimeRange(
            String airportCode, LocalDateTime from, LocalDateTime to) {
        return list(
            "airportCode = ?1 AND observationTime >= ?2 AND observationTime <= ?3 AND isActive = true ORDER BY observationTime DESC",
            airportCode, from, to
        );
    }

    @Transactional
    public long deactivateOldReports(LocalDateTime olderThan) {
        return update("isActive = false WHERE fetchedAt < ?1 AND isActive = true", olderThan);
    }

    @Transactional
    public long deleteOldReports(LocalDateTime olderThan) {
        return delete("fetchedAt < ?1", olderThan);
    }
}
