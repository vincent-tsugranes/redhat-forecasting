package com.redhat.weather.domain.entity;

import io.quarkus.hibernate.orm.panache.PanacheEntityBase;
import jakarta.persistence.*;
import jakarta.validation.constraints.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "tfrs", indexes = {
    @Index(name = "idx_tfr_notam_id", columnList = "notam_id"),
    @Index(name = "idx_tfr_facility", columnList = "facility"),
    @Index(name = "idx_tfr_type", columnList = "tfr_type"),
    @Index(name = "idx_tfr_state", columnList = "state")
})
public class TfrEntity extends PanacheEntityBase {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    public Long id;

    @NotBlank
    @Size(max = 20)
    @Column(name = "notam_id", nullable = false, length = 20, unique = true)
    public String notamId;

    @Size(max = 50)
    @Column(name = "notam_key", length = 50)
    public String notamKey;

    @NotBlank
    @Size(max = 10)
    @Column(name = "facility", nullable = false, length = 10)
    public String facility;

    @Size(max = 10)
    @Column(name = "state", length = 10)
    public String state;

    @NotBlank
    @Size(max = 50)
    @Column(name = "tfr_type", nullable = false, length = 50)
    public String tfrType;

    @Column(name = "description", columnDefinition = "TEXT")
    public String description;

    @Column(name = "effective_date")
    public LocalDateTime effectiveDate;

    @Column(name = "expire_date")
    public LocalDateTime expireDate;

    @Column(name = "latitude", precision = 9, scale = 6)
    public BigDecimal latitude;

    @Column(name = "longitude", precision = 9, scale = 6)
    public BigDecimal longitude;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "geojson", columnDefinition = "jsonb")
    public String geojson;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "tfr_data", columnDefinition = "jsonb", nullable = false)
    public String tfrData;

    @Column(name = "is_new")
    public Boolean isNew = false;

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
