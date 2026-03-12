package com.redhat.weather.domain.repository;

import com.redhat.weather.domain.entity.LightningStrikeEntity;
import io.quarkus.hibernate.orm.panache.PanacheRepositoryBase;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.transaction.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@ApplicationScoped
public class LightningRepository implements PanacheRepositoryBase<LightningStrikeEntity, Long> {

    public List<LightningStrikeEntity> findRecent() {
        LocalDateTime oneHourAgo = LocalDateTime.now().minusHours(1);
        return list("isActive = true AND strikeTime > ?1 ORDER BY strikeTime DESC", oneHourAgo);
    }

    public long countRecent() {
        LocalDateTime oneHourAgo = LocalDateTime.now().minusHours(1);
        return count("isActive = true AND strikeTime > ?1", oneHourAgo);
    }

    public boolean existsByStrikeId(String strikeId) {
        return count("strikeId = ?1", strikeId) > 0;
    }

    @Transactional
    public long deactivateOld(LocalDateTime olderThan) {
        return update("isActive = false WHERE strikeTime < ?1 AND isActive = true", olderThan);
    }
}
