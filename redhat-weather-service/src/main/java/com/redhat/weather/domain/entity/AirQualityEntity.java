package com.redhat.weather.domain.entity;

import io.quarkus.hibernate.orm.panache.PanacheEntityBase;
import jakarta.persistence.*;
import jakarta.validation.constraints.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "air_quality", indexes = {
    @Index(name = "idx_aq_location", columnList = "location_id"),
    @Index(name = "idx_aq_valid_at", columnList = "valid_at"),
    @Index(name = "idx_aq_active", columnList = "is_active")
})
public class AirQualityEntity extends PanacheEntityBase {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    public Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "location_id")
    public LocationEntity location;

    @NotNull
    @Column(name = "latitude", nullable = false, precision = 10, scale = 7)
    public BigDecimal latitude;

    @NotNull
    @Column(name = "longitude", nullable = false, precision = 10, scale = 7)
    public BigDecimal longitude;

    @NotNull
    @Column(name = "aqi", nullable = false)
    public Integer aqi; // 1-5 OWM scale

    @Column(name = "co", precision = 10, scale = 2)
    public BigDecimal co;    // Carbon monoxide μg/m³

    @Column(name = "no", precision = 10, scale = 2)
    public BigDecimal no;    // Nitrogen monoxide μg/m³

    @Column(name = "no2", precision = 10, scale = 2)
    public BigDecimal no2;   // Nitrogen dioxide μg/m³

    @Column(name = "o3", precision = 10, scale = 2)
    public BigDecimal o3;    // Ozone μg/m³

    @Column(name = "so2", precision = 10, scale = 2)
    public BigDecimal so2;   // Sulphur dioxide μg/m³

    @Column(name = "pm2_5", precision = 10, scale = 2)
    public BigDecimal pm2_5; // PM2.5 μg/m³

    @Column(name = "pm10", precision = 10, scale = 2)
    public BigDecimal pm10;  // PM10 μg/m³

    @Column(name = "nh3", precision = 10, scale = 2)
    public BigDecimal nh3;   // Ammonia μg/m³

    @NotNull
    @Column(name = "valid_at", nullable = false)
    public LocalDateTime validAt;

    @NotNull
    @Column(name = "fetched_at", nullable = false)
    public LocalDateTime fetchedAt;

    @NotNull
    @Column(name = "source", nullable = false, length = 50)
    public String source = "openweathermap";

    @Column(name = "is_active")
    public boolean isActive = true;

    @Column(name = "created_at", nullable = false, updatable = false)
    public LocalDateTime createdAt;

    @Column(name = "updated_at")
    public LocalDateTime updatedAt;

    @PrePersist
    protected void onPrePersist() {
        createdAt = updatedAt = LocalDateTime.now();
    }

    @PreUpdate
    protected void onPreUpdate() {
        updatedAt = LocalDateTime.now();
    }
}
