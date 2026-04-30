package com.redhat.weather.domain.repository;

import com.redhat.weather.domain.entity.AirQualityEntity;
import io.quarkus.hibernate.orm.panache.PanacheRepositoryBase;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.transaction.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@ApplicationScoped
public class AirQualityRepository implements PanacheRepositoryBase<AirQualityEntity, Long> {

    public List<AirQualityEntity> findByLocation(Long locationId) {
        return find("location.id = ?1 and isActive = true order by validAt desc", locationId).list();
    }

    public Optional<AirQualityEntity> findLatestByLocation(Long locationId) {
        return find("location.id = ?1 and isActive = true order by validAt desc", locationId).firstResultOptional();
    }

    public List<AirQualityEntity> findByCoordinates(BigDecimal lat, BigDecimal lon) {
        return find("latitude = ?1 and longitude = ?2 and isActive = true order by validAt desc", lat, lon).list();
    }

    @Transactional
    public long deactivateOld(LocalDateTime olderThan) {
        return update("isActive = false where isActive = true and validAt < ?1", olderThan);
    }
}
