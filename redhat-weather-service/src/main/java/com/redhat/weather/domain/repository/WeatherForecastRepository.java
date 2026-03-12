package com.redhat.weather.domain.repository;

import com.redhat.weather.domain.entity.WeatherForecastEntity;
import io.quarkus.hibernate.orm.panache.PanacheRepositoryBase;
import io.quarkus.panache.common.Page;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.transaction.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@ApplicationScoped
public class WeatherForecastRepository implements PanacheRepositoryBase<WeatherForecastEntity, Long> {

    public List<WeatherForecastEntity> findByLocation(Long locationId) {
        return list("location.id = ?1 AND isActive = true ORDER BY validFrom", locationId);
    }

    public List<WeatherForecastEntity> findByLocationAndTimeRange(Long locationId, LocalDateTime from, LocalDateTime to) {
        return list(
            "location.id = ?1 AND validFrom <= ?2 AND validTo >= ?3 AND isActive = true ORDER BY validFrom",
            locationId, to, from
        );
    }

    public List<WeatherForecastEntity> findByCoordinates(BigDecimal lat, BigDecimal lon) {
        return list("latitude = ?1 AND longitude = ?2 AND isActive = true ORDER BY validFrom", lat, lon);
    }

    public List<WeatherForecastEntity> findByCoordinatesAndTimeRange(
            BigDecimal lat, BigDecimal lon, LocalDateTime from, LocalDateTime to) {
        return list(
            "latitude = ?1 AND longitude = ?2 AND validFrom <= ?3 AND validTo >= ?4 AND isActive = true ORDER BY validFrom",
            lat, lon, to, from
        );
    }

    public List<WeatherForecastEntity> findBySource(String source) {
        return list("source = ?1 AND isActive = true ORDER BY validFrom", source);
    }

    public List<WeatherForecastEntity> findByTimeRange(LocalDateTime from, LocalDateTime to) {
        return list(
            "validFrom <= ?1 AND validTo >= ?2 AND isActive = true ORDER BY validFrom",
            to, from
        );
    }

    public List<WeatherForecastEntity> findCurrent(Long locationId) {
        LocalDateTime now = LocalDateTime.now();
        return list(
            "location.id = ?1 AND validFrom <= ?2 AND validTo >= ?2 AND isActive = true ORDER BY validFrom",
            locationId, now
        );
    }

    public List<WeatherForecastEntity> findCurrentByCoordinates(BigDecimal lat, BigDecimal lon) {
        LocalDateTime now = LocalDateTime.now();
        return list(
            "latitude = ?1 AND longitude = ?2 AND validFrom <= ?3 AND validTo >= ?3 AND isActive = true ORDER BY validFrom",
            lat, lon, now
        );
    }

    public List<WeatherForecastEntity> findByLocationPaginated(Long locationId, int page, int size) {
        return find("location.id = ?1 AND isActive = true ORDER BY validFrom", locationId)
            .page(Page.of(page, size))
            .list();
    }

    public List<WeatherForecastEntity> findByCoordinatesAndTimeRangePaginated(
            BigDecimal lat, BigDecimal lon, LocalDateTime from, LocalDateTime to, int page, int size) {
        return find(
            "latitude = ?1 AND longitude = ?2 AND validFrom <= ?3 AND validTo >= ?4 AND isActive = true ORDER BY validFrom",
            lat, lon, to, from
        ).page(Page.of(page, size)).list();
    }

    public long countByLocation(Long locationId) {
        return count("location.id = ?1 AND isActive = true", locationId);
    }

    public long countByCoordinatesAndTimeRange(BigDecimal lat, BigDecimal lon, LocalDateTime from, LocalDateTime to) {
        return count(
            "latitude = ?1 AND longitude = ?2 AND validFrom <= ?3 AND validTo >= ?4 AND isActive = true",
            lat, lon, to, from
        );
    }

    public List<WeatherForecastEntity> findHistoricalByLocation(Long locationId, LocalDateTime since) {
        return list(
            "location.id = ?1 AND isActive = false AND forecastTime >= ?2 ORDER BY forecastTime DESC",
            locationId, since
        );
    }

    public List<WeatherForecastEntity> findHistoricalByLocationPaginated(Long locationId, LocalDateTime since, int page, int size) {
        return find(
            "location.id = ?1 AND isActive = false AND forecastTime >= ?2 ORDER BY forecastTime DESC",
            locationId, since
        ).page(Page.of(page, size)).list();
    }

    public long countHistoricalByLocation(Long locationId, LocalDateTime since) {
        return count("location.id = ?1 AND isActive = false AND forecastTime >= ?2", locationId, since);
    }

    @Transactional
    public long deactivateOldForecasts(LocalDateTime olderThan) {
        return update("isActive = false WHERE fetchedAt < ?1 AND isActive = true", olderThan);
    }

    @Transactional
    public long deleteOldForecasts(LocalDateTime olderThan) {
        return delete("fetchedAt < ?1", olderThan);
    }
}
