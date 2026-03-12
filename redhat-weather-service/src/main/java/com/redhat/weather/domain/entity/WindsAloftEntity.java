package com.redhat.weather.domain.entity;

import io.quarkus.hibernate.orm.panache.PanacheEntityBase;
import jakarta.persistence.*;
import jakarta.validation.constraints.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "winds_aloft", indexes = {
    @Index(name = "idx_winds_station", columnList = "station_id"),
    @Index(name = "idx_winds_valid_time", columnList = "valid_time"),
    @Index(name = "idx_winds_altitude", columnList = "altitude_ft")
})
public class WindsAloftEntity extends PanacheEntityBase {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    public Long id;

    @NotBlank
    @Size(max = 100)
    @Column(name = "forecast_id", nullable = false, length = 100, unique = true)
    public String forecastId;

    @NotBlank
    @Size(max = 10)
    @Column(name = "station_id", nullable = false, length = 10)
    public String stationId;

    @Column(name = "latitude", precision = 9, scale = 6)
    public BigDecimal latitude;

    @Column(name = "longitude", precision = 9, scale = 6)
    public BigDecimal longitude;

    @Column(name = "elevation_ft")
    public Integer elevationFt;

    @NotNull
    @Column(name = "valid_time", nullable = false)
    public LocalDateTime validTime;

    @Column(name = "forecast_hour")
    public Integer forecastHour;

    @NotNull
    @Column(name = "altitude_ft", nullable = false)
    public Integer altitudeFt;

    @Column(name = "wind_direction")
    public Integer windDirection;

    @Column(name = "wind_speed_knots")
    public Integer windSpeedKnots;

    @Column(name = "temperature_celsius", precision = 5, scale = 1)
    public BigDecimal temperatureCelsius;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "raw_data", columnDefinition = "jsonb", nullable = false)
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
