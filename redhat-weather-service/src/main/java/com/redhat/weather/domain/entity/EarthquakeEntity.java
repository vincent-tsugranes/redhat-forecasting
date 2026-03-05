package com.redhat.weather.domain.entity;

import io.quarkus.hibernate.orm.panache.PanacheEntityBase;
import jakarta.persistence.*;
import jakarta.validation.constraints.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "earthquakes", indexes = {
    @Index(name = "idx_earthquake_usgs_id", columnList = "usgs_id"),
    @Index(name = "idx_earthquake_event_time", columnList = "event_time"),
    @Index(name = "idx_earthquake_magnitude", columnList = "magnitude"),
    @Index(name = "idx_earthquake_active", columnList = "is_active")
})
public class EarthquakeEntity extends PanacheEntityBase {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    public Long id;

    @NotBlank
    @Size(max = 50)
    @Column(name = "usgs_id", nullable = false, length = 50)
    public String usgsId;

    @Column(name = "magnitude", precision = 4, scale = 2)
    public BigDecimal magnitude;

    @Column(name = "place", length = 255)
    public String place;

    @NotNull
    @Column(name = "event_time", nullable = false)
    public LocalDateTime eventTime;

    @NotNull
    @Column(name = "latitude", nullable = false, precision = 10, scale = 7)
    public BigDecimal latitude;

    @NotNull
    @Column(name = "longitude", nullable = false, precision = 10, scale = 7)
    public BigDecimal longitude;

    @Column(name = "depth_km", precision = 7, scale = 2)
    public BigDecimal depthKm;

    @Column(name = "magnitude_type", length = 10)
    public String magnitudeType;

    @Column(name = "status", length = 20)
    public String status;

    @Column(name = "tsunami")
    public Boolean tsunami;

    @Column(name = "felt")
    public Integer felt;

    @Column(name = "cdi", precision = 4, scale = 2)
    public BigDecimal cdi;

    @Column(name = "alert", length = 10)
    public String alert;

    @Column(name = "significance")
    public Integer significance;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "event_data", columnDefinition = "jsonb", nullable = false)
    public String eventData;

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
