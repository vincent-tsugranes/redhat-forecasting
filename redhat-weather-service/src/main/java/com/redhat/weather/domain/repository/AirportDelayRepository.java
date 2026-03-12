package com.redhat.weather.domain.repository;

import com.redhat.weather.domain.entity.AirportDelayEntity;
import io.quarkus.hibernate.orm.panache.PanacheRepositoryBase;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.transaction.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@ApplicationScoped
public class AirportDelayRepository implements PanacheRepositoryBase<AirportDelayEntity, Long> {

    public List<AirportDelayEntity> findActiveDelays() {
        return list("isActive = true AND isDelayed = true ORDER BY avgDelayMinutes DESC");
    }

    public List<AirportDelayEntity> findByAirportCode(String code) {
        return list("isActive = true AND airportCode = ?1 ORDER BY fetchedAt DESC", code);
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
