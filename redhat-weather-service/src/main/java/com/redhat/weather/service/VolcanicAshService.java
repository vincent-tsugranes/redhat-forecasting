package com.redhat.weather.service;

import com.redhat.weather.client.AviationWeatherClient;
import com.redhat.weather.domain.entity.VolcanicAshAdvisoryEntity;
import com.redhat.weather.domain.repository.VolcanicAshRepository;
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
public class VolcanicAshService {

    private static final Logger LOG = Logger.getLogger(VolcanicAshService.class);
    private static final ObjectMapper objectMapper = new ObjectMapper();

    @Inject
    VolcanicAshRepository volcanicAshRepository;

    @Inject
    @RestClient
    AviationWeatherClient aviationClient;

    public List<VolcanicAshAdvisoryEntity> getActiveAdvisories() {
        return volcanicAshRepository.findActive();
    }

    @Transactional
    public void fetchAndStoreAdvisories() {
        try {
            List<AviationWeatherClient.IntlSigmetResponse> responses = aviationClient.getInternationalSigmets("json");

            if (responses == null || responses.isEmpty()) {
                LOG.info("No international SIGMETs returned from AWC");
                return;
            }

            List<VolcanicAshAdvisoryEntity> advisories = new ArrayList<>();

            for (AviationWeatherClient.IntlSigmetResponse resp : responses) {
                try {
                    if (!isVolcanicAsh(resp.hazard)) continue;

                    String advisoryId = generateAdvisoryId(resp);
                    if (volcanicAshRepository.existsByAdvisoryId(advisoryId)) continue;

                    VolcanicAshAdvisoryEntity entity = new VolcanicAshAdvisoryEntity();
                    entity.advisoryId = advisoryId;
                    entity.firId = resp.firId;
                    entity.firName = resp.firName;
                    entity.volcanoName = extractVolcanoName(resp.rawSigmet);
                    entity.hazard = resp.hazard;
                    entity.severity = resp.qualifier;
                    entity.validTimeFrom = parseTime(resp.validTimeFrom);
                    entity.validTimeTo = parseTime(resp.validTimeTo);
                    entity.altitudeLowFt = resp.altitudeLow != null ? resp.altitudeLow * 100 : null;
                    entity.altitudeHighFt = resp.altitudeHi != null ? resp.altitudeHi * 100 : null;
                    entity.rawText = resp.rawSigmet;
                    entity.geojson = buildGeoJson(resp.coords);
                    entity.advisoryData = objectMapper.writeValueAsString(resp);
                    entity.fetchedAt = LocalDateTime.now();

                    advisories.add(entity);
                } catch (Exception e) {
                    LOG.warn("Error parsing volcanic ash advisory: " + e.getMessage());
                }
            }

            if (!advisories.isEmpty()) {
                volcanicAshRepository.persist(advisories);
                LOG.info("Stored " + advisories.size() + " new volcanic ash advisories");
            } else {
                LOG.info("No new volcanic ash advisories to store");
            }

        } catch (Exception e) {
            LOG.error("Error fetching volcanic ash advisories from AWC", e);
        }
    }

    @Transactional
    public void deactivateExpired() {
        long count = volcanicAshRepository.deactivateExpired();
        if (count > 0) {
            LOG.info("Deactivated " + count + " expired volcanic ash advisories");
        }
    }

    @Transactional
    public void deactivateOldEntries(LocalDateTime olderThan) {
        long count = volcanicAshRepository.deactivateOld(olderThan);
        LOG.info("Deactivated " + count + " old volcanic ash advisories");
    }

    private boolean isVolcanicAsh(String hazard) {
        if (hazard == null) return false;
        String h = hazard.toUpperCase();
        return h.contains("VA") || h.contains("VOLCANIC") || h.contains("ASH");
    }

    private String extractVolcanoName(String rawText) {
        if (rawText == null) return null;
        // Try to extract volcano name from raw SIGMET text
        // Common pattern: "MT VOLCANONAME" or "VOLCANO: NAME"
        String upper = rawText.toUpperCase();
        int mtIdx = upper.indexOf("MT ");
        if (mtIdx >= 0) {
            int end = rawText.indexOf(' ', mtIdx + 3);
            if (end < 0) end = Math.min(rawText.length(), mtIdx + 30);
            return rawText.substring(mtIdx, end).trim();
        }
        return null;
    }

    private String generateAdvisoryId(AviationWeatherClient.IntlSigmetResponse resp) {
        String id = resp.isigmetId != null ? resp.isigmetId : "";
        String fir = resp.firId != null ? resp.firId : "";
        String from = resp.validTimeFrom != null ? resp.validTimeFrom : "";
        return "va-" + (id + fir + from).hashCode();
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
            sb.append(",[").append(coords.get(0).lon).append(",").append(coords.get(0).lat).append("]");
            sb.append("]]}");
            return sb.toString();
        } catch (Exception e) {
            return null;
        }
    }
}
