package com.redhat.weather.domain.repository;

import com.redhat.weather.domain.entity.GroundStopEntity;
import io.quarkus.hibernate.orm.panache.PanacheRepositoryBase;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.transaction.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@ApplicationScoped
public class GroundStopRepository implements PanacheRepositoryBase<GroundStopEntity, Long> {

    public List<GroundStopEntity> findActive() {
        return list("isActive = true ORDER BY fetchedAt DESC");
    }

    public List<GroundStopEntity> findByAirport(String airportCode) {
        return list("isActive = true AND airportCode = ?1 ORDER BY fetchedAt DESC", airportCode);
    }

    public boolean existsByGroundStopId(String groundStopId) {
        return count("groundStopId = ?1", groundStopId) > 0;
    }

    @Transactional
    public long deactivateAll() {
        return update("isActive = false WHERE isActive = true");
    }

    @Transactional
    public long deactivateOld(LocalDateTime olderThan) {
        return update("isActive = false WHERE fetchedAt < ?1 AND isActive = true", olderThan);
    }
}
