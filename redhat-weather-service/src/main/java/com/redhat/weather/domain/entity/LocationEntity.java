package com.redhat.weather.domain.entity;

import io.quarkus.hibernate.orm.panache.PanacheEntityBase;
import jakarta.persistence.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "locations", indexes = {
    @Index(name = "idx_location_coordinates", columnList = "latitude,longitude"),
    @Index(name = "idx_location_type", columnList = "location_type"),
    @Index(name = "idx_location_name", columnList = "name")
})
public class LocationEntity extends PanacheEntityBase {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    public Long id;

    @Column(name = "name", nullable = false)
    public String name;

    @Column(name = "latitude", nullable = false, precision = 10, scale = 7)
    public BigDecimal latitude;

    @Column(name = "longitude", nullable = false, precision = 10, scale = 7)
    public BigDecimal longitude;

    @Column(name = "location_type", nullable = false, length = 50)
    public String locationType;

    @Column(name = "airport_code", length = 10)
    public String airportCode;

    @Column(name = "state", length = 100)
    public String state;

    @Column(name = "country", length = 100)
    public String country;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "metadata", columnDefinition = "jsonb")
    public String metadata;

    @Column(name = "created_at", nullable = false, updatable = false)
    public LocalDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    public LocalDateTime updatedAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }
}
