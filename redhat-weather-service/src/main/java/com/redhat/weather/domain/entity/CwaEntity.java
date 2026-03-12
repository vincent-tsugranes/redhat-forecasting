package com.redhat.weather.domain.entity;

import io.quarkus.hibernate.orm.panache.PanacheEntityBase;
import jakarta.persistence.*;
import jakarta.validation.constraints.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.LocalDateTime;

@Entity
@Table(name = "cwas", indexes = {
    @Index(name = "idx_cwa_id", columnList = "cwa_id"),
    @Index(name = "idx_cwa_artcc", columnList = "artcc"),
    @Index(name = "idx_cwa_hazard", columnList = "hazard"),
    @Index(name = "idx_cwa_valid_to", columnList = "valid_time_to")
})
public class CwaEntity extends PanacheEntityBase {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    public Long id;

    @NotBlank
    @Size(max = 100)
    @Column(name = "cwa_id", nullable = false, length = 100, unique = true)
    public String cwaId;

    @NotBlank
    @Size(max = 10)
    @Column(name = "artcc", nullable = false, length = 10)
    public String artcc;

    @Size(max = 50)
    @Column(name = "hazard", length = 50)
    public String hazard;

    @Size(max = 20)
    @Column(name = "severity", length = 20)
    public String severity;

    @NotNull
    @Column(name = "valid_time_from", nullable = false)
    public LocalDateTime validTimeFrom;

    @NotNull
    @Column(name = "valid_time_to", nullable = false)
    public LocalDateTime validTimeTo;

    @Column(name = "altitude_low_ft")
    public Integer altitudeLowFt;

    @Column(name = "altitude_high_ft")
    public Integer altitudeHighFt;

    @Column(name = "raw_text", columnDefinition = "TEXT")
    public String rawText;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "geojson", columnDefinition = "jsonb")
    public String geojson;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "cwa_data", columnDefinition = "jsonb", nullable = false)
    public String cwaData;

    @Column(name = "fetched_at", nullable = false)
    public LocalDateTime fetchedAt;

    @Column(name = "is_active")
    public Boolean isActive = true;

    @Column(name = "created_at", nullable = false, updatable = false)
    public LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        if (fetchedAt == null) {
            fetchedAt = LocalDateTime.now();
        }
    }
}
