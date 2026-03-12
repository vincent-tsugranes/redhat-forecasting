package com.redhat.weather.service;

import com.redhat.weather.client.AviationWeatherClient;
import com.redhat.weather.domain.entity.CwaEntity;
import com.redhat.weather.domain.repository.CwaRepository;
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
public class CwaService {

    private static final Logger LOG = Logger.getLogger(CwaService.class);
    private static final ObjectMapper objectMapper = new ObjectMapper();

    @Inject
    CwaRepository cwaRepository;

    @Inject
    @RestClient
    AviationWeatherClient aviationClient;

    public List<CwaEntity> getActiveCwas() {
        return cwaRepository.findActive();
    }

    public List<CwaEntity> getCwasByArtcc(String artcc) {
        return cwaRepository.findByArtcc(artcc.toUpperCase());
    }

    public List<CwaEntity> getCwasByHazard(String hazard) {
        return cwaRepository.findByHazard(hazard.toUpperCase());
    }

    @Transactional
    public void fetchAndStoreCwas() {
        try {
            List<AviationWeatherClient.CwaResponse> responses = aviationClient.getCWAs("json");

            if (responses == null || responses.isEmpty()) {
                LOG.info("No CWAs returned from AWC");
                return;
            }

            List<CwaEntity> cwas = new ArrayList<>();

            for (AviationWeatherClient.CwaResponse resp : responses) {
                try {
                    String cwaId = generateCwaId(resp);
                    if (cwaRepository.existsByCwaId(cwaId)) continue;

                    CwaEntity entity = new CwaEntity();
                    entity.cwaId = cwaId;
                    entity.artcc = resp.cwsu != null ? resp.cwsu : "UNK";
                    entity.hazard = resp.hazard;
                    entity.severity = resp.qualifier;
                    entity.validTimeFrom = parseTime(resp.validTimeFrom);
                    entity.validTimeTo = parseTime(resp.validTimeTo);
                    entity.altitudeLowFt = resp.base != null ? resp.base * 100 : null;
                    entity.altitudeHighFt = resp.top != null ? resp.top * 100 : null;
                    entity.rawText = resp.cwaText;
                    entity.geojson = buildGeoJson(resp.coords);
                    entity.cwaData = objectMapper.writeValueAsString(resp);
                    entity.fetchedAt = LocalDateTime.now();

                    cwas.add(entity);
                } catch (Exception e) {
                    LOG.warn("Error parsing CWA: " + e.getMessage());
                }
            }

            if (!cwas.isEmpty()) {
                cwaRepository.persist(cwas);
                LOG.info("Stored " + cwas.size() + " new CWAs");
            } else {
                LOG.info("No new CWAs to store");
            }

        } catch (Exception e) {
            LOG.error("Error fetching CWAs from AWC", e);
        }
    }

    @Transactional
    public void deactivateExpired() {
        long count = cwaRepository.deactivateExpired();
        if (count > 0) {
            LOG.info("Deactivated " + count + " expired CWAs");
        }
    }

    @Transactional
    public void deactivateOldEntries(LocalDateTime olderThan) {
        long count = cwaRepository.deactivateOld(olderThan);
        LOG.info("Deactivated " + count + " old CWAs");
    }

    private String generateCwaId(AviationWeatherClient.CwaResponse resp) {
        String cwsu = resp.cwsu != null ? resp.cwsu : "";
        String hazard = resp.hazard != null ? resp.hazard : "";
        String from = resp.validTimeFrom != null ? resp.validTimeFrom : "";
        return String.valueOf((cwsu + hazard + from).hashCode());
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
