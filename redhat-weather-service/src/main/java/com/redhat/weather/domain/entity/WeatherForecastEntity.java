package com.redhat.weather.domain.entity;

import io.quarkus.hibernate.orm.panache.PanacheEntityBase;
import jakarta.persistence.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "weather_forecasts", indexes = {
    @Index(name = "idx_forecast_location", columnList = "location_id"),
    @Index(name = "idx_forecast_source", columnList = "source"),
    @Index(name = "idx_forecast_valid_from", columnList = "valid_from"),
    @Index(name = "idx_forecast_active", columnList = "is_active")
})
public class WeatherForecastEntity extends PanacheEntityBase {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    public Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "location_id", nullable = false)
    public LocationEntity location;

    @Column(name = "source", nullable = false, length = 50)
    public String source;

    // Time information
    @Column(name = "forecast_time", nullable = false)
    public LocalDateTime forecastTime;

    @Column(name = "valid_from", nullable = false)
    public LocalDateTime validFrom;

    @Column(name = "valid_to", nullable = false)
    public LocalDateTime validTo;

    @Column(name = "fetched_at", nullable = false)
    public LocalDateTime fetchedAt;

    // Coordinates (denormalized)
    @Column(name = "latitude", nullable = false, precision = 10, scale = 7)
    public BigDecimal latitude;

    @Column(name = "longitude", nullable = false, precision = 10, scale = 7)
    public BigDecimal longitude;

    // Weather data (JSONB)
    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "forecast_data", columnDefinition = "jsonb", nullable = false)
    public String forecastData;

    // Extracted searchable fields
    @Column(name = "temperature_fahrenheit", precision = 5, scale = 2)
    public BigDecimal temperatureFahrenheit;

    @Column(name = "temperature_celsius", precision = 5, scale = 2)
    public BigDecimal temperatureCelsius;

    @Column(name = "precipitation_probability")
    public Integer precipitationProbability;

    @Column(name = "wind_speed_mph", precision = 5, scale = 2)
    public BigDecimal windSpeedMph;

    @Column(name = "wind_direction")
    public Integer windDirection;

    @Column(name = "humidity")
    public Integer humidity;

    @Column(name = "weather_description", columnDefinition = "TEXT")
    public String weatherDescription;

    @Column(name = "weather_short_description")
    public String weatherShortDescription;

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
