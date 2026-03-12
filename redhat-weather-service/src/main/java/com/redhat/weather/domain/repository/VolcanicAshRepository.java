package com.redhat.weather.domain.repository;

import com.redhat.weather.domain.entity.VolcanicAshAdvisoryEntity;
import io.quarkus.hibernate.orm.panache.PanacheRepositoryBase;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.transaction.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@ApplicationScoped
public class VolcanicAshRepository implements PanacheRepositoryBase<VolcanicAshAdvisoryEntity, Long> {

    public List<VolcanicAshAdvisoryEntity> findActive() {
        LocalDateTime now = LocalDateTime.now();
        return list("isActive = true AND validTimeTo > ?1 ORDER BY validTimeFrom DESC", now);
    }

    public boolean existsByAdvisoryId(String advisoryId) {
        return count("advisoryId = ?1", advisoryId) > 0;
    }

    @Transactional
    public long deactivateExpired() {
        LocalDateTime now = LocalDateTime.now();
        return update("isActive = false WHERE validTimeTo < ?1 AND isActive = true", now);
    }

    @Transactional
    public long deactivateOld(LocalDateTime olderThan) {
        return update("isActive = false WHERE fetchedAt < ?1 AND isActive = true", olderThan);
    }
}
