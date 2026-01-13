package com.redhat.weather.domain.entity;

import io.quarkus.hibernate.orm.panache.PanacheEntityBase;
import jakarta.persistence.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "hurricanes", indexes = {
    @Index(name = "idx_hurricane_storm_id", columnList = "storm_id"),
    @Index(name = "idx_hurricane_advisory_time", columnList = "advisory_time"),
    @Index(name = "idx_hurricane_status", columnList = "status"),
    @Index(name = "idx_hurricane_active", columnList = "is_active")
})
public class HurricaneEntity extends PanacheEntityBase {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    public Long id;

    // Hurricane identification
    @Column(name = "storm_id", nullable = false, length = 50)
    public String stormId;

    @Column(name = "storm_name", length = 100)
    public String stormName;

    @Column(name = "storm_number")
    public Integer stormNumber;

    @Column(name = "basin", length = 10)
    public String basin;

    @Column(name = "year")
    public Integer year;

    // Time information
    @Column(name = "advisory_time", nullable = false)
    public LocalDateTime advisoryTime;

    @Column(name = "forecast_time", nullable = false)
    public LocalDateTime forecastTime;

    @Column(name = "fetched_at", nullable = false)
    public LocalDateTime fetchedAt;

    // Current position
    @Column(name = "latitude", nullable = false, precision = 10, scale = 7)
    public BigDecimal latitude;

    @Column(name = "longitude", nullable = false, precision = 10, scale = 7)
    public BigDecimal longitude;

    // Hurricane data
    @Column(name = "category")
    public Integer category;

    @Column(name = "max_sustained_winds_mph")
    public Integer maxSustainedWindsMph;

    @Column(name = "max_sustained_winds_knots")
    public Integer maxSustainedWindsKnots;

    @Column(name = "min_central_pressure_mb")
    public Integer minCentralPressureMb;

    @Column(name = "movement_direction")
    public Integer movementDirection;

    @Column(name = "movement_speed_mph", precision = 5, scale = 2)
    public BigDecimal movementSpeedMph;

    @Column(name = "movement_speed_knots", precision = 5, scale = 2)
    public BigDecimal movementSpeedKnots;

    // Full forecast data
    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "forecast_data", columnDefinition = "jsonb", nullable = false)
    public String forecastData;

    // Status and classification
    @Column(name = "status", length = 50)
    public String status;

    @Column(name = "classification", length = 50)
    public String classification;

    @Column(name = "intensity", length = 50)
    public String intensity;

    // Metadata
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
