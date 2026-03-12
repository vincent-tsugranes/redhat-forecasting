package com.redhat.weather.domain.repository;

import com.redhat.weather.domain.entity.SigmetEntity;
import io.quarkus.hibernate.orm.panache.PanacheRepositoryBase;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.transaction.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@ApplicationScoped
public class SigmetRepository implements PanacheRepositoryBase<SigmetEntity, Long> {

    public List<SigmetEntity> findActive() {
        LocalDateTime now = LocalDateTime.now();
        return list("isActive = true AND validTimeTo > ?1 ORDER BY validTimeFrom DESC", now);
    }

    public List<SigmetEntity> findByType(String sigmetType) {
        LocalDateTime now = LocalDateTime.now();
        return list("isActive = true AND sigmetType = ?1 AND validTimeTo > ?2 ORDER BY validTimeFrom DESC",
                     sigmetType, now);
    }

    public List<SigmetEntity> findByHazard(String hazard) {
        LocalDateTime now = LocalDateTime.now();
        return list("isActive = true AND hazard = ?1 AND validTimeTo > ?2 ORDER BY validTimeFrom DESC",
                     hazard, now);
    }

    public boolean existsBySigmetId(String sigmetId) {
        return count("sigmetId = ?1", sigmetId) > 0;
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
