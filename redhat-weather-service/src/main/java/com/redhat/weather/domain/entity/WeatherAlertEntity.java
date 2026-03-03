package com.redhat.weather.domain.entity;

import io.quarkus.hibernate.orm.panache.PanacheEntityBase;
import jakarta.persistence.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.LocalDateTime;

@Entity
@Table(name = "weather_alerts", indexes = {
    @Index(name = "idx_alert_alert_id", columnList = "alert_id"),
    @Index(name = "idx_alert_severity", columnList = "severity"),
    @Index(name = "idx_alert_active", columnList = "is_active"),
    @Index(name = "idx_alert_expires", columnList = "expires")
})
public class WeatherAlertEntity extends PanacheEntityBase {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    public Long id;

    // Alert identification
    @Column(name = "alert_id", nullable = false, unique = true, length = 255)
    public String alertId;

    @Column(name = "event", nullable = false, length = 200)
    public String event;

    // Alert details
    @Column(name = "headline", columnDefinition = "TEXT")
    public String headline;

    @Column(name = "description", columnDefinition = "TEXT")
    public String description;

    @Column(name = "severity", length = 20)
    public String severity;

    @Column(name = "certainty", length = 20)
    public String certainty;

    @Column(name = "urgency", length = 20)
    public String urgency;

    // Affected area
    @Column(name = "area_desc", columnDefinition = "TEXT")
    public String areaDesc;

    // Time information
    @Column(name = "effective")
    public LocalDateTime effective;

    @Column(name = "expires")
    public LocalDateTime expires;

    // Source
    @Column(name = "sender_name", length = 200)
    public String senderName;

    // Full alert data
    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "alert_data", columnDefinition = "jsonb", nullable = false)
    public String alertData;

    // Metadata
    @Column(name = "is_active")
    public Boolean isActive = true;

    @Column(name = "fetched_at", nullable = false)
    public LocalDateTime fetchedAt;

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
