package com.redhat.weather.domain.entity;

import io.quarkus.hibernate.orm.panache.PanacheEntityBase;
import jakarta.persistence.*;
import jakarta.validation.constraints.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "pireps", indexes = {
    @Index(name = "idx_pirep_id", columnList = "pirep_id"),
    @Index(name = "idx_pirep_observation_time", columnList = "observation_time"),
    @Index(name = "idx_pirep_turbulence", columnList = "turbulence_intensity"),
    @Index(name = "idx_pirep_icing", columnList = "icing_intensity")
})
public class PirepEntity extends PanacheEntityBase {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    public Long id;

    @NotBlank
    @Size(max = 100)
    @Column(name = "pirep_id", nullable = false, length = 100, unique = true)
    public String pirepId;

    @NotBlank
    @Size(max = 10)
    @Column(name = "report_type", nullable = false, length = 10)
    public String reportType;

    @NotBlank
    @Column(name = "raw_text", columnDefinition = "TEXT", nullable = false)
    public String rawText;

    @NotNull
    @Column(name = "observation_time", nullable = false)
    public LocalDateTime observationTime;

    @NotNull
    @Column(name = "latitude", nullable = false, precision = 10, scale = 7)
    public BigDecimal latitude;

    @NotNull
    @Column(name = "longitude", nullable = false, precision = 10, scale = 7)
    public BigDecimal longitude;

    @Column(name = "altitude_ft")
    public Integer altitudeFt;

    @Size(max = 20)
    @Column(name = "aircraft_type", length = 20)
    public String aircraftType;

    @Column(name = "sky_condition")
    public String skyCondition;

    @Size(max = 20)
    @Column(name = "turbulence_intensity", length = 20)
    public String turbulenceIntensity;

    @Size(max = 20)
    @Column(name = "turbulence_type", length = 20)
    public String turbulenceType;

    @Size(max = 20)
    @Column(name = "icing_intensity", length = 20)
    public String icingIntensity;

    @Size(max = 20)
    @Column(name = "icing_type", length = 20)
    public String icingType;

    @Column(name = "weather_conditions")
    public String weatherConditions;

    @Column(name = "temperature_celsius", precision = 5, scale = 2)
    public BigDecimal temperatureCelsius;

    @Column(name = "wind_speed_knots")
    public Integer windSpeedKnots;

    @Column(name = "wind_direction")
    public Integer windDirection;

    @Column(name = "visibility_miles", precision = 5, scale = 2)
    public BigDecimal visibilityMiles;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "pirep_data", columnDefinition = "jsonb", nullable = false)
    public String pirepData;

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
