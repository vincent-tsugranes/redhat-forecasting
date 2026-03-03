package com.redhat.weather.domain.repository;

import com.redhat.weather.domain.entity.WeatherAlertEntity;
import io.quarkus.hibernate.orm.panache.PanacheRepositoryBase;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.transaction.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@ApplicationScoped
public class WeatherAlertRepository implements PanacheRepositoryBase<WeatherAlertEntity, Long> {

    public List<WeatherAlertEntity> findActiveAlerts() {
        return list(
            "isActive = true AND (expires IS NULL OR expires > ?1) ORDER BY effective DESC",
            LocalDateTime.now()
        );
    }

    public List<WeatherAlertEntity> findBySeverity(String severity) {
        return list(
            "severity = ?1 AND isActive = true AND (expires IS NULL OR expires > ?2) ORDER BY effective DESC",
            severity, LocalDateTime.now()
        );
    }

    public Optional<WeatherAlertEntity> findByAlertId(String alertId) {
        return find("alertId = ?1", alertId).firstResultOptional();
    }

    @Transactional
    public long deactivateExpired() {
        return update(
            "isActive = false WHERE isActive = true AND expires IS NOT NULL AND expires < ?1",
            LocalDateTime.now()
        );
    }

    @Transactional
    public long deleteOldAlerts(LocalDateTime olderThan) {
        return delete("fetchedAt < ?1", olderThan);
    }
}
