package com.redhat.weather.dto;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * DTO for space weather data from NOAA SWPC.
 * Not a persisted entity — aggregated from multiple SWPC API endpoints.
 */
public class SpaceWeatherDTO {

    public double kpIndex;
    public String kpLevel;
    public Double solarWindSpeed;
    public String geomagneticStormLevel;
    public String auroraChance;
    public List<SpaceWeatherAlert> alerts = new ArrayList<>();
    public String fetchedAt;

    public SpaceWeatherDTO() {
        this.fetchedAt = LocalDateTime.now().toString();
    }

    public void setKpIndex(double kp) {
        this.kpIndex = kp;
        this.kpLevel = determineKpLevel(kp);
        this.geomagneticStormLevel = determineStormLevel(kp);
        this.auroraChance = determineAuroraChance(kp);
    }

    private String determineKpLevel(double kp) {
        if (kp >= 8) return "extreme";
        if (kp >= 6) return "strong storm";
        if (kp >= 5) return "storm";
        if (kp >= 4) return "unsettled";
        return "quiet";
    }

    private String determineStormLevel(double kp) {
        if (kp >= 9) return "G5";
        if (kp >= 8) return "G4";
        if (kp >= 7) return "G3";
        if (kp >= 6) return "G2";
        if (kp >= 5) return "G1";
        return "G0";
    }

    private String determineAuroraChance(double kp) {
        if (kp >= 7) return "high";
        if (kp >= 5) return "moderate";
        if (kp >= 3) return "low";
        return "very low";
    }

    public static class SpaceWeatherAlert {
        public String issueTime;
        public String message;

        public SpaceWeatherAlert() {
        }

        public SpaceWeatherAlert(String issueTime, String message) {
            this.issueTime = issueTime;
            this.message = message;
        }
    }
}
