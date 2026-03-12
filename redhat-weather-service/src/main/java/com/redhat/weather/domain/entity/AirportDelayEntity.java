package com.redhat.weather.domain.entity;

import io.quarkus.hibernate.orm.panache.PanacheEntityBase;
import jakarta.persistence.*;
import jakarta.validation.constraints.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.LocalDateTime;

@Entity
@Table(name = "airport_delays", indexes = {
    @Index(name = "idx_delay_id", columnList = "delay_id"),
    @Index(name = "idx_delay_airport_code", columnList = "airport_code"),
    @Index(name = "idx_delay_type", columnList = "delay_type")
})
public class AirportDelayEntity extends PanacheEntityBase {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    public Long id;

    @NotBlank
    @Size(max = 100)
    @Column(name = "delay_id", nullable = false, length = 100, unique = true)
    public String delayId;

    @NotBlank
    @Size(max = 10)
    @Column(name = "airport_code", nullable = false, length = 10)
    public String airportCode;

    @Size(max = 200)
    @Column(name = "airport_name", length = 200)
    public String airportName;

    @NotBlank
    @Size(max = 50)
    @Column(name = "delay_type", nullable = false, length = 50)
    public String delayType;

    @Size(max = 500)
    @Column(name = "reason", length = 500)
    public String reason;

    @Column(name = "avg_delay_minutes")
    public Integer avgDelayMinutes;

    @Column(name = "min_delay_minutes")
    public Integer minDelayMinutes;

    @Column(name = "max_delay_minutes")
    public Integer maxDelayMinutes;

    @Size(max = 20)
    @Column(name = "trend", length = 20)
    public String trend;

    @Column(name = "is_delayed")
    public Boolean isDelayed = false;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "delay_data", columnDefinition = "jsonb", nullable = false)
    public String delayData;

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
