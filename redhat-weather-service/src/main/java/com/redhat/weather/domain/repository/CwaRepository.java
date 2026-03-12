package com.redhat.weather.domain.repository;

import com.redhat.weather.domain.entity.CwaEntity;
import io.quarkus.hibernate.orm.panache.PanacheRepositoryBase;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.transaction.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@ApplicationScoped
public class CwaRepository implements PanacheRepositoryBase<CwaEntity, Long> {

    public List<CwaEntity> findActive() {
        LocalDateTime now = LocalDateTime.now();
        return list("isActive = true AND validTimeTo > ?1 ORDER BY validTimeFrom DESC", now);
    }

    public List<CwaEntity> findByArtcc(String artcc) {
        LocalDateTime now = LocalDateTime.now();
        return list("isActive = true AND artcc = ?1 AND validTimeTo > ?2 ORDER BY validTimeFrom DESC",
                     artcc, now);
    }

    public List<CwaEntity> findByHazard(String hazard) {
        LocalDateTime now = LocalDateTime.now();
        return list("isActive = true AND hazard = ?1 AND validTimeTo > ?2 ORDER BY validTimeFrom DESC",
                     hazard, now);
    }

    public boolean existsByCwaId(String cwaId) {
        return count("cwaId = ?1", cwaId) > 0;
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
