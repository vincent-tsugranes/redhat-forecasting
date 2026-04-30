package com.redhat.weather.dto;

import java.util.List;

public class AstronomicalDTO {
    public String sunrise;
    public String sunset;
    public String solarNoon;
    public String dayLength;
    public String civilTwilightBegin;
    public String civilTwilightEnd;
    public String nauticalTwilightBegin;
    public String nauticalTwilightEnd;
    public String moonPhaseName;
    public Double moonIllumination;
    public List<MoonPhaseDTO> nextMoonPhases;
    public Double uvIndex;

    public static class MoonPhaseDTO {
        public String phase;
        public String date;
        public String time;

        public MoonPhaseDTO() {}
        public MoonPhaseDTO(String phase, String date, String time) {
            this.phase = phase;
            this.date = date;
            this.time = time;
        }
    }
}
