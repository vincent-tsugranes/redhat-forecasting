package com.redhat.weather.domain.repository;

import com.redhat.weather.domain.entity.PirepEntity;
import io.quarkus.hibernate.orm.panache.PanacheRepositoryBase;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.transaction.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@ApplicationScoped
public class PirepRepository implements PanacheRepositoryBase<PirepEntity, Long> {

    public List<PirepEntity> findRecent() {
        LocalDateTime threeHoursAgo = LocalDateTime.now().minusHours(3);
        return list("isActive = true AND observationTime >= ?1 ORDER BY observationTime DESC", threeHoursAgo);
    }

    public List<PirepEntity> findByTurbulenceIntensity(String intensity) {
        LocalDateTime threeHoursAgo = LocalDateTime.now().minusHours(3);
        return list("isActive = true AND turbulenceIntensity = ?1 AND observationTime >= ?2 ORDER BY observationTime DESC",
                     intensity, threeHoursAgo);
    }

    public List<PirepEntity> findByIcingIntensity(String intensity) {
        LocalDateTime threeHoursAgo = LocalDateTime.now().minusHours(3);
        return list("isActive = true AND icingIntensity = ?1 AND observationTime >= ?2 ORDER BY observationTime DESC",
                     intensity, threeHoursAgo);
    }

    public boolean existsByPirepId(String pirepId) {
        return count("pirepId = ?1", pirepId) > 0;
    }

    @Transactional
    public long deactivateOld(LocalDateTime olderThan) {
        return update("isActive = false WHERE fetchedAt < ?1 AND isActive = true", olderThan);
    }
}
