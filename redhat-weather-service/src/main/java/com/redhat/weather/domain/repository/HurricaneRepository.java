package com.redhat.weather.domain.repository;

import com.redhat.weather.domain.entity.HurricaneEntity;
import io.quarkus.hibernate.orm.panache.PanacheRepositoryBase;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.transaction.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@ApplicationScoped
public class HurricaneRepository implements PanacheRepositoryBase<HurricaneEntity, Long> {

    public List<HurricaneEntity> findActiveStorms() {
        return list(
            "isActive = true AND status IN ('active', 'Active') ORDER BY advisoryTime DESC"
        );
    }

    public List<HurricaneEntity> findByStormId(String stormId) {
        return list("stormId = ?1 ORDER BY advisoryTime DESC", stormId);
    }

    public Optional<HurricaneEntity> findLatestByStormId(String stormId) {
        return find("stormId = ?1 ORDER BY advisoryTime DESC", stormId)
            .firstResultOptional();
    }

    public List<HurricaneEntity> findByStormName(String stormName) {
        return list("stormName = ?1 ORDER BY advisoryTime DESC", stormName);
    }

    public List<HurricaneEntity> findByBasin(String basin) {
        return list("basin = ?1 AND isActive = true ORDER BY advisoryTime DESC", basin);
    }

    public List<HurricaneEntity> findByYear(Integer year) {
        return list("year = ?1 ORDER BY advisoryTime DESC", year);
    }

    public List<HurricaneEntity> findByBasinAndYear(String basin, Integer year) {
        return list("basin = ?1 AND year = ?2 ORDER BY stormNumber, advisoryTime DESC", basin, year);
    }

    public List<HurricaneEntity> findByCategory(Integer category) {
        return list("category = ?1 AND isActive = true ORDER BY advisoryTime DESC", category);
    }

    public List<HurricaneEntity> findByMinimumCategory(Integer minimumCategory) {
        return list("category >= ?1 AND isActive = true ORDER BY category DESC, advisoryTime DESC", minimumCategory);
    }

    public List<HurricaneEntity> findByStatus(String status) {
        return list("status = ?1 ORDER BY advisoryTime DESC", status);
    }

    public List<HurricaneEntity> findByTimeRange(LocalDateTime from, LocalDateTime to) {
        return list(
            "advisoryTime >= ?1 AND advisoryTime <= ?2 AND isActive = true ORDER BY advisoryTime DESC",
            from, to
        );
    }

    public List<HurricaneEntity> findStormTrack(String stormId, LocalDateTime from, LocalDateTime to) {
        return list(
            "stormId = ?1 AND advisoryTime >= ?2 AND advisoryTime <= ?3 ORDER BY advisoryTime ASC",
            stormId, from, to
        );
    }

    @Transactional
    public long deactivateOldAdvisories(LocalDateTime olderThan) {
        return update("isActive = false WHERE fetchedAt < ?1 AND isActive = true", olderThan);
    }

    @Transactional
    public long deleteOldAdvisories(LocalDateTime olderThan) {
        return delete("fetchedAt < ?1", olderThan);
    }
}
