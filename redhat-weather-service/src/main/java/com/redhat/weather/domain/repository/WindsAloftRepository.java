package com.redhat.weather.domain.repository;

import com.redhat.weather.domain.entity.WindsAloftEntity;
import io.quarkus.hibernate.orm.panache.PanacheRepositoryBase;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.transaction.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@ApplicationScoped
public class WindsAloftRepository implements PanacheRepositoryBase<WindsAloftEntity, Long> {

    public List<WindsAloftEntity> findLatest() {
        LocalDateTime cutoff = LocalDateTime.now().minusHours(12);
        return list("isActive = true AND validTime > ?1 ORDER BY stationId, altitudeFt", cutoff);
    }

    public List<WindsAloftEntity> findByStation(String stationId) {
        LocalDateTime cutoff = LocalDateTime.now().minusHours(12);
        return list("isActive = true AND stationId = ?1 AND validTime > ?2 ORDER BY altitudeFt",
                     stationId, cutoff);
    }

    public List<WindsAloftEntity> findByAltitude(int altitudeFt) {
        LocalDateTime cutoff = LocalDateTime.now().minusHours(12);
        return list("isActive = true AND altitudeFt = ?1 AND validTime > ?2 ORDER BY stationId",
                     altitudeFt, cutoff);
    }

    public boolean existsByForecastId(String forecastId) {
        return count("forecastId = ?1", forecastId) > 0;
    }

    @Transactional
    public long deactivateOld(LocalDateTime olderThan) {
        return update("isActive = false WHERE fetchedAt < ?1 AND isActive = true", olderThan);
    }
}
