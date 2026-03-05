package com.redhat.weather.domain.repository;

import com.redhat.weather.domain.entity.EarthquakeEntity;
import io.quarkus.hibernate.orm.panache.PanacheRepositoryBase;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.transaction.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@ApplicationScoped
public class EarthquakeRepository implements PanacheRepositoryBase<EarthquakeEntity, Long> {

    public List<EarthquakeEntity> findRecent() {
        LocalDateTime since = LocalDateTime.now().minusHours(24);
        return list("isActive = true AND eventTime >= ?1 ORDER BY eventTime DESC", since);
    }

    public List<EarthquakeEntity> findByMinMagnitude(double minMagnitude) {
        LocalDateTime since = LocalDateTime.now().minusHours(24);
        return list(
            "isActive = true AND eventTime >= ?1 AND magnitude >= ?2 ORDER BY eventTime DESC",
            since, minMagnitude
        );
    }

    public List<EarthquakeEntity> findByTimeRange(LocalDateTime from, LocalDateTime to) {
        return list(
            "isActive = true AND eventTime >= ?1 AND eventTime <= ?2 ORDER BY eventTime DESC",
            from, to
        );
    }

    public List<EarthquakeEntity> findSignificant() {
        LocalDateTime since = LocalDateTime.now().minusHours(48);
        return list(
            "isActive = true AND eventTime >= ?1 AND (significance >= 500 OR magnitude >= 5.0) ORDER BY eventTime DESC",
            since
        );
    }

    public boolean existsByUsgsId(String usgsId) {
        return count("usgsId = ?1", usgsId) > 0;
    }

    public Optional<LocalDateTime> findLatestEventTime() {
        return find("isActive = true ORDER BY eventTime DESC")
            .firstResultOptional()
            .map(eq -> eq.eventTime);
    }

    @Transactional
    public long deactivateOld(LocalDateTime olderThan) {
        return update("isActive = false WHERE fetchedAt < ?1 AND isActive = true", olderThan);
    }
}
