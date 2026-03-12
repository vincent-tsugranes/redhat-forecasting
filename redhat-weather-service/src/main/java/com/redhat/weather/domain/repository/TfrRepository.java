package com.redhat.weather.domain.repository;

import com.redhat.weather.domain.entity.TfrEntity;
import io.quarkus.hibernate.orm.panache.PanacheRepositoryBase;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.transaction.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@ApplicationScoped
public class TfrRepository implements PanacheRepositoryBase<TfrEntity, Long> {

    public List<TfrEntity> findActive() {
        return list("isActive = true ORDER BY fetchedAt DESC");
    }

    public List<TfrEntity> findByType(String tfrType) {
        return list("isActive = true AND tfrType = ?1 ORDER BY fetchedAt DESC", tfrType);
    }

    public List<TfrEntity> findByState(String state) {
        return list("isActive = true AND state = ?1 ORDER BY fetchedAt DESC", state);
    }

    public List<TfrEntity> findByFacility(String facility) {
        return list("isActive = true AND facility = ?1 ORDER BY fetchedAt DESC", facility);
    }

    public boolean existsByNotamId(String notamId) {
        return count("notamId = ?1", notamId) > 0;
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
