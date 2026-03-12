package com.redhat.weather.domain.entity;

import io.quarkus.hibernate.orm.panache.PanacheEntityBase;
import jakarta.persistence.*;
import jakarta.validation.constraints.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.LocalDateTime;

@Entity
@Table(name = "sigmets", indexes = {
    @Index(name = "idx_sigmet_id", columnList = "sigmet_id"),
    @Index(name = "idx_sigmet_type", columnList = "sigmet_type"),
    @Index(name = "idx_sigmet_hazard", columnList = "hazard"),
    @Index(name = "idx_sigmet_valid_to", columnList = "valid_time_to")
})
public class SigmetEntity extends PanacheEntityBase {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    public Long id;

    @NotBlank
    @Size(max = 100)
    @Column(name = "sigmet_id", nullable = false, length = 100, unique = true)
    public String sigmetId;

    @NotBlank
    @Size(max = 20)
    @Column(name = "sigmet_type", nullable = false, length = 20)
    public String sigmetType;

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
    @Column(name = "sigmet_data", columnDefinition = "jsonb", nullable = false)
    public String sigmetData;

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
