package com.redhat.weather.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * DTO for climate normals computed from historical forecast data.
 * Not a persisted entity — computed on demand from existing weather_forecasts table.
 */
public class ClimateNormalsDTO {

    public Long locationId;
    public String locationName;
    public int month;
    public BigDecimal avgHighF;
    public BigDecimal avgLowF;
    public BigDecimal avgHighC;
    public BigDecimal avgLowC;
    public BigDecimal avgPrecipProbability;
    public BigDecimal avgHumidity;
    public BigDecimal avgWindSpeedMph;
    public long sampleCount;
    public String fetchedAt;

    public ClimateNormalsDTO() {
        this.fetchedAt = LocalDateTime.now().toString();
    }

    public ClimateNormalsDTO(Long locationId, String locationName, int month) {
        this();
        this.locationId = locationId;
        this.locationName = locationName;
        this.month = month;
    }
}
