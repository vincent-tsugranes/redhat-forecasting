package com.redhat.weather.domain.entity;

import io.quarkus.hibernate.orm.panache.PanacheEntityBase;
import jakarta.persistence.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "airport_weather", indexes = {
    @Index(name = "idx_airport_location", columnList = "location_id"),
    @Index(name = "idx_airport_code", columnList = "airport_code"),
    @Index(name = "idx_airport_observation_time", columnList = "observation_time"),
    @Index(name = "idx_airport_report_type", columnList = "report_type")
})
public class AirportWeatherEntity extends PanacheEntityBase {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    public Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "location_id", nullable = false)
    public LocationEntity location;

    @Column(name = "airport_code", nullable = false, length = 10)
    public String airportCode;

    // Time information
    @Column(name = "observation_time", nullable = false)
    public LocalDateTime observationTime;

    @Column(name = "fetched_at", nullable = false)
    public LocalDateTime fetchedAt;

    // Coordinates
    @Column(name = "latitude", nullable = false, precision = 10, scale = 7)
    public BigDecimal latitude;

    @Column(name = "longitude", nullable = false, precision = 10, scale = 7)
    public BigDecimal longitude;

    // METAR/TAF data
    @Column(name = "report_type", nullable = false, length = 10)
    public String reportType;

    @Column(name = "raw_text", columnDefinition = "TEXT", nullable = false)
    public String rawText;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "metar_data", columnDefinition = "jsonb")
    public String metarData;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "taf_data", columnDefinition = "jsonb")
    public String tafData;

    // Extracted fields
    @Column(name = "visibility_miles", precision = 5, scale = 2)
    public BigDecimal visibilityMiles;

    @Column(name = "ceiling_feet")
    public Integer ceilingFeet;

    @Column(name = "wind_speed_knots")
    public Integer windSpeedKnots;

    @Column(name = "wind_direction")
    public Integer windDirection;

    @Column(name = "wind_gust_knots")
    public Integer windGustKnots;

    @Column(name = "temperature_celsius", precision = 5, scale = 2)
    public BigDecimal temperatureCelsius;

    @Column(name = "dewpoint_celsius", precision = 5, scale = 2)
    public BigDecimal dewpointCelsius;

    @Column(name = "altimeter_inches", precision = 5, scale = 2)
    public BigDecimal altimeterInches;

    @Column(name = "flight_category", length = 10)
    public String flightCategory;

    @Column(name = "sky_condition")
    public String skyCondition;

    @Column(name = "weather_conditions")
    public String weatherConditions;

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
