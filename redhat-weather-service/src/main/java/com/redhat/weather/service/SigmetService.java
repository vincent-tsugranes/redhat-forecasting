package com.redhat.weather.service;

import com.redhat.weather.client.AviationWeatherClient;
import com.redhat.weather.domain.entity.SigmetEntity;
import com.redhat.weather.domain.repository.SigmetRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;
import org.eclipse.microprofile.rest.client.inject.RestClient;
import org.jboss.logging.Logger;

import java.time.LocalDateTime;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

@ApplicationScoped
public class SigmetService {

    private static final Logger LOG = Logger.getLogger(SigmetService.class);
    private static final ObjectMapper objectMapper = new ObjectMapper();

    @Inject
    SigmetRepository sigmetRepository;

    @Inject
    @RestClient
    AviationWeatherClient aviationClient;

    public List<SigmetEntity> getActiveSigmets() {
        return sigmetRepository.findActive();
    }

    public List<SigmetEntity> getSigmetsByType(String type) {
        return sigmetRepository.findByType(type.toUpperCase());
    }

    public List<SigmetEntity> getSigmetsByHazard(String hazard) {
        return sigmetRepository.findByHazard(hazard.toUpperCase());
    }

    @Transactional
    public void fetchAndStoreSigmets() {
        try {
            List<AviationWeatherClient.AirSigmetResponse> responses = aviationClient.getAirSigmets("json");

            if (responses == null || responses.isEmpty()) {
                LOG.info("No SIGMETs/AIRMETs returned from AWC");
                return;
            }

            List<SigmetEntity> sigmets = new ArrayList<>();

            for (AviationWeatherClient.AirSigmetResponse resp : responses) {
                try {
                    String sigmetId = generateSigmetId(resp);
                    if (sigmetRepository.existsBySigmetId(sigmetId)) continue;

                    SigmetEntity entity = new SigmetEntity();
                    entity.sigmetId = sigmetId;
                    entity.sigmetType = resp.airSigmetType != null ? resp.airSigmetType : "UNKNOWN";
                    entity.hazard = resp.hazard;
                    entity.severity = resp.severity;
                    entity.validTimeFrom = parseTime(resp.validTimeFrom);
                    entity.validTimeTo = parseTime(resp.validTimeTo);
                    entity.altitudeLowFt = resp.altitudeLow != null ? resp.altitudeLow * 100 : null;
                    entity.altitudeHighFt = resp.altitudeHi != null ? resp.altitudeHi * 100 : null;
                    entity.rawText = resp.rawAirSigmet;
                    entity.geojson = buildGeoJson(resp.coords);
                    entity.sigmetData = objectMapper.writeValueAsString(resp);
                    entity.fetchedAt = LocalDateTime.now();

                    sigmets.add(entity);
                } catch (Exception e) {
                    LOG.warn("Error parsing SIGMET: " + e.getMessage());
                }
            }

            if (!sigmets.isEmpty()) {
                sigmetRepository.persist(sigmets);
                LOG.info("Stored " + sigmets.size() + " new SIGMETs/AIRMETs");
            } else {
                LOG.info("No new SIGMETs/AIRMETs to store");
            }

        } catch (Exception e) {
            LOG.error("Error fetching SIGMETs from AWC", e);
        }
    }

    @Transactional
    public void deactivateExpired() {
        long count = sigmetRepository.deactivateExpired();
        if (count > 0) {
            LOG.info("Deactivated " + count + " expired SIGMETs/AIRMETs");
        }
    }

    @Transactional
    public void deactivateOldEntries(LocalDateTime olderThan) {
        long count = sigmetRepository.deactivateOld(olderThan);
        LOG.info("Deactivated " + count + " old SIGMETs/AIRMETs");
    }

    private String generateSigmetId(AviationWeatherClient.AirSigmetResponse resp) {
        String type = resp.airSigmetType != null ? resp.airSigmetType : "";
        String hazard = resp.hazard != null ? resp.hazard : "";
        String from = resp.validTimeFrom != null ? resp.validTimeFrom : "";
        return String.valueOf((type + hazard + from).hashCode());
    }

    private LocalDateTime parseTime(String timeStr) {
        if (timeStr == null) return LocalDateTime.now();
        try {
            return ZonedDateTime.parse(timeStr, DateTimeFormatter.ISO_DATE_TIME).toLocalDateTime();
        } catch (Exception e) {
            return LocalDateTime.now();
        }
    }

    private String buildGeoJson(List<AviationWeatherClient.CoordPair> coords) {
        if (coords == null || coords.size() < 3) return null;
        try {
            StringBuilder sb = new StringBuilder();
            sb.append("{\"type\":\"Polygon\",\"coordinates\":[[");
            for (int i = 0; i < coords.size(); i++) {
                if (i > 0) sb.append(",");
                sb.append("[").append(coords.get(i).lon).append(",").append(coords.get(i).lat).append("]");
            }
            // Close the polygon
            sb.append(",[").append(coords.get(0).lon).append(",").append(coords.get(0).lat).append("]");
            sb.append("]]}");
            return sb.toString();
        } catch (Exception e) {
            return null;
        }
    }
}
