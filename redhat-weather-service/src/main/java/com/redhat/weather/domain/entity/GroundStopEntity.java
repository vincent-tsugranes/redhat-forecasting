package com.redhat.weather.domain.entity;

import io.quarkus.hibernate.orm.panache.PanacheEntityBase;
import jakarta.persistence.*;
import jakarta.validation.constraints.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.LocalDateTime;

@Entity
@Table(name = "ground_stops", indexes = {
    @Index(name = "idx_ground_stop_id", columnList = "ground_stop_id"),
    @Index(name = "idx_ground_stop_airport", columnList = "airport_code"),
    @Index(name = "idx_ground_stop_active", columnList = "is_active"),
    @Index(name = "idx_ground_stop_type", columnList = "program_type")
})
public class GroundStopEntity extends PanacheEntityBase {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    public Long id;

    @NotBlank
    @Size(max = 100)
    @Column(name = "ground_stop_id", nullable = false, length = 100, unique = true)
    public String groundStopId;

    @NotBlank
    @Size(max = 10)
    @Column(name = "airport_code", nullable = false, length = 10)
    public String airportCode;

    @Size(max = 200)
    @Column(name = "airport_name", length = 200)
    public String airportName;

    @NotBlank
    @Size(max = 50)
    @Column(name = "program_type", nullable = false, length = 50)
    public String programType;

    @Size(max = 500)
    @Column(name = "reason", length = 500)
    public String reason;

    @Column(name = "end_time")
    public LocalDateTime endTime;

    @Column(name = "avg_delay_minutes")
    public Integer avgDelayMinutes;

    @Column(name = "max_delay_minutes")
    public Integer maxDelayMinutes;

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
