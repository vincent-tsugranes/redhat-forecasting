package com.redhat.weather.domain.entity;

import io.quarkus.hibernate.orm.panache.PanacheEntityBase;
import jakarta.persistence.*;
import jakarta.validation.constraints.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "lightning_strikes", indexes = {
    @Index(name = "idx_lightning_strike_id", columnList = "strike_id"),
    @Index(name = "idx_lightning_time", columnList = "strike_time"),
    @Index(name = "idx_lightning_active", columnList = "is_active"),
    @Index(name = "idx_lightning_coords", columnList = "latitude, longitude")
})
public class LightningStrikeEntity extends PanacheEntityBase {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    public Long id;

    @NotBlank
    @Size(max = 100)
    @Column(name = "strike_id", nullable = false, length = 100, unique = true)
    public String strikeId;

    @NotNull
    @DecimalMin("-90.0")
    @DecimalMax("90.0")
    @Column(name = "latitude", nullable = false, precision = 10, scale = 7)
    public BigDecimal latitude;

    @NotNull
    @DecimalMin("-180.0")
    @DecimalMax("180.0")
    @Column(name = "longitude", nullable = false, precision = 10, scale = 7)
    public BigDecimal longitude;

    @NotNull
    @Column(name = "strike_time", nullable = false)
    public LocalDateTime strikeTime;

    @Column(name = "amplitude_ka")
    public Double amplitudeKa;

    @Size(max = 20)
    @Column(name = "strike_type", length = 20)
    public String strikeType;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "raw_data", columnDefinition = "jsonb")
    public String rawData;

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
