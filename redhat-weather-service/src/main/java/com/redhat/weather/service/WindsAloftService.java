package com.redhat.weather.service;

import com.redhat.weather.client.AviationWeatherClient;
import com.redhat.weather.domain.entity.WindsAloftEntity;
import com.redhat.weather.domain.repository.WindsAloftRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;
import org.eclipse.microprofile.rest.client.inject.RestClient;
import org.jboss.logging.Logger;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@ApplicationScoped
public class WindsAloftService {

    private static final Logger LOG = Logger.getLogger(WindsAloftService.class);
    private static final ObjectMapper objectMapper = new ObjectMapper();

    private static final int[] STANDARD_ALTITUDES = {3000, 6000, 9000, 12000, 18000, 24000, 30000, 34000, 39000};

    @Inject
    WindsAloftRepository windsAloftRepository;

    @Inject
    @RestClient
    AviationWeatherClient aviationClient;

    public List<WindsAloftEntity> getLatestWinds() {
        return windsAloftRepository.findLatest();
    }

    public List<WindsAloftEntity> getWindsByStation(String stationId) {
        return windsAloftRepository.findByStation(stationId.toUpperCase());
    }

    public List<WindsAloftEntity> getWindsByAltitude(int altitudeFt) {
        return windsAloftRepository.findByAltitude(altitudeFt);
    }

    @Transactional
    public void fetchAndStoreWinds() {
        try {
            List<AviationWeatherClient.WindTempResponse> responses = aviationClient.getWindsAloft("json");

            if (responses == null || responses.isEmpty()) {
                LOG.info("No winds aloft data returned from AWC");
                return;
            }

            List<WindsAloftEntity> winds = new ArrayList<>();

            for (AviationWeatherClient.WindTempResponse resp : responses) {
                try {
                    if (resp.stationId == null) continue;

                    LocalDateTime validTime = parseTime(resp.validTime);

                    // Each response has wind/temp data at multiple altitudes
                    if (resp.fcsts != null) {
                        for (Map.Entry<String, AviationWeatherClient.WindTempForecast> entry : resp.fcsts.entrySet()) {
                            try {
                                int altitude = Integer.parseInt(entry.getKey());
                                AviationWeatherClient.WindTempForecast fcst = entry.getValue();

                                String forecastId = generateForecastId(resp.stationId, altitude, resp.validTime);
                                if (windsAloftRepository.existsByForecastId(forecastId)) continue;

                                WindsAloftEntity entity = new WindsAloftEntity();
                                entity.forecastId = forecastId;
                                entity.stationId = resp.stationId;
                                entity.latitude = resp.lat != null ? BigDecimal.valueOf(resp.lat) : null;
                                entity.longitude = resp.lon != null ? BigDecimal.valueOf(resp.lon) : null;
                                entity.elevationFt = resp.elev;
                                entity.validTime = validTime;
                                entity.forecastHour = resp.fcstHr;
                                entity.altitudeFt = altitude;
                                entity.windDirection = fcst.wdir;
                                entity.windSpeedKnots = fcst.wspd;
                                entity.temperatureCelsius = fcst.temp != null ? BigDecimal.valueOf(fcst.temp) : null;
                                entity.rawData = objectMapper.writeValueAsString(resp);
                                entity.fetchedAt = LocalDateTime.now();

                                winds.add(entity);
                            } catch (NumberFormatException e) {
                                // Skip non-numeric altitude keys
                            }
                        }
                    }
                } catch (Exception e) {
                    LOG.warn("Error parsing winds aloft for station " + resp.stationId + ": " + e.getMessage());
                }
            }

            if (!winds.isEmpty()) {
                windsAloftRepository.persist(winds);
                LOG.info("Stored " + winds.size() + " new winds aloft entries");
            } else {
                LOG.info("No new winds aloft data to store");
            }

        } catch (Exception e) {
            LOG.error("Error fetching winds aloft from AWC", e);
        }
    }

    @Transactional
    public void deactivateOldForecasts(LocalDateTime olderThan) {
        long count = windsAloftRepository.deactivateOld(olderThan);
        LOG.info("Deactivated " + count + " old winds aloft entries");
    }

    private String generateForecastId(String stationId, int altitude, String validTime) {
        String vt = validTime != null ? validTime : "";
        return String.valueOf((stationId + altitude + vt).hashCode());
    }

    private LocalDateTime parseTime(String timeStr) {
        if (timeStr == null) return LocalDateTime.now();
        try {
            return ZonedDateTime.parse(timeStr, DateTimeFormatter.ISO_DATE_TIME).toLocalDateTime();
        } catch (Exception e) {
            return LocalDateTime.now();
        }
    }
}
