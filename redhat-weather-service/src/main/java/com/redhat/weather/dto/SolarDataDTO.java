package com.redhat.weather.dto;

import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.ZoneOffset;

/**
 * DTO for sunrise/sunset solar data.
 * Not a persisted entity — computed from OpenWeatherMap current weather response.
 */
public class SolarDataDTO {

    public Long locationId;
    public String locationName;
    public String sunrise;
    public String sunset;
    public long dayLengthSeconds;
    public String dayLengthFormatted;
    public String fetchedAt;

    public SolarDataDTO() {
    }

    public SolarDataDTO(Long locationId, String locationName,
                        long sunriseEpoch, long sunsetEpoch, int timezoneOffset) {
        this.locationId = locationId;
        this.locationName = locationName;

        ZoneOffset offset = ZoneOffset.ofTotalSeconds(timezoneOffset);
        LocalDateTime sunriseTime = LocalDateTime.ofEpochSecond(sunriseEpoch, 0, offset);
        LocalDateTime sunsetTime = LocalDateTime.ofEpochSecond(sunsetEpoch, 0, offset);

        this.sunrise = sunriseTime.toLocalTime().toString();
        this.sunset = sunsetTime.toLocalTime().toString();
        this.dayLengthSeconds = sunsetEpoch - sunriseEpoch;

        long hours = dayLengthSeconds / 3600;
        long minutes = (dayLengthSeconds % 3600) / 60;
        this.dayLengthFormatted = hours + "h " + minutes + "m";
        this.fetchedAt = LocalDateTime.now().toString();
    }
}
