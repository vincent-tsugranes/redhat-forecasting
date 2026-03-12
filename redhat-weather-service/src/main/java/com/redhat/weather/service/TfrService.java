package com.redhat.weather.service;

import com.redhat.weather.client.FaaTfrClient;
import com.redhat.weather.client.FaaTfrGeoClient;
import com.redhat.weather.domain.entity.TfrEntity;
import com.redhat.weather.domain.repository.TfrRepository;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;
import org.eclipse.microprofile.rest.client.inject.RestClient;
import org.jboss.logging.Logger;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@ApplicationScoped
public class TfrService {

    private static final Logger LOG = Logger.getLogger(TfrService.class);
    private static final ObjectMapper objectMapper = new ObjectMapper();

    // Pattern to extract dates like "Friday, March 13, 2026" or with "through" for ranges
    private static final Pattern DATE_PATTERN = Pattern.compile(
        "(?:Monday|Tuesday|Wednesday|Thursday|Friday|Saturday|Sunday),\\s+" +
        "(\\w+ \\d{1,2}, \\d{4})");
    private static final DateTimeFormatter DESCRIPTION_DATE_FORMAT =
        DateTimeFormatter.ofPattern("MMMM d, yyyy", Locale.ENGLISH);

    @Inject
    TfrRepository tfrRepository;

    @Inject
    @RestClient
    FaaTfrClient tfrClient;

    @Inject
    @RestClient
    FaaTfrGeoClient tfrGeoClient;

    public List<TfrEntity> getActiveTfrs() {
        return tfrRepository.findActive();
    }

    public List<TfrEntity> getTfrsByType(String type) {
        return tfrRepository.findByType(type.toUpperCase());
    }

    public List<TfrEntity> getTfrsByState(String state) {
        return tfrRepository.findByState(state.toUpperCase());
    }

    public List<TfrEntity> getTfrsByFacility(String facility) {
        return tfrRepository.findByFacility(facility.toUpperCase());
    }

    @Transactional
    public void fetchAndStoreTfrs() {
        try {
            // Step 1: Fetch TFR list from FAA API
            List<FaaTfrClient.TfrListEntry> tfrList = tfrClient.getTfrList();

            if (tfrList == null || tfrList.isEmpty()) {
                LOG.info("No TFRs returned from FAA");
                return;
            }

            // Step 2: Fetch GeoJSON boundaries
            Map<String, JsonNode> geoMap = fetchGeoData();

            // Step 3: Deactivate all existing TFRs (FAA list is the source of truth)
            tfrRepository.deactivateAll();

            List<TfrEntity> tfrs = new ArrayList<>();

            for (FaaTfrClient.TfrListEntry entry : tfrList) {
                try {
                    if (entry.notam_id == null) continue;

                    TfrEntity entity = new TfrEntity();
                    entity.notamId = entry.notam_id;
                    entity.facility = entry.facility != null ? entry.facility : "UNK";
                    entity.state = entry.state;
                    entity.tfrType = entry.type != null ? entry.type : "UNKNOWN";
                    entity.description = entry.description;
                    entity.isNew = "Y".equals(entry.is_new);
                    entity.isActive = true;

                    // Parse dates from description
                    parseDatesFromDescription(entity, entry.description);

                    // Match GeoJSON boundary from WFS data
                    String geoKey = entry.notam_id;
                    JsonNode geoFeature = geoMap.get(geoKey);
                    if (geoFeature != null) {
                        JsonNode geometry = geoFeature.get("geometry");
                        if (geometry != null) {
                            entity.geojson = objectMapper.writeValueAsString(geometry);
                            // Extract centroid from first coordinate
                            extractCentroid(entity, geometry);
                        }
                    }

                    entity.tfrData = objectMapper.writeValueAsString(entry);
                    entity.fetchedAt = LocalDateTime.now();

                    tfrs.add(entity);
                } catch (Exception e) {
                    LOG.warn("Error parsing TFR " + entry.notam_id + ": " + e.getMessage());
                }
            }

            if (!tfrs.isEmpty()) {
                tfrRepository.persist(tfrs);
                LOG.info("Stored " + tfrs.size() + " TFRs");
            }

        } catch (Exception e) {
            LOG.error("Error fetching TFRs from FAA", e);
        }
    }

    @Transactional
    public void deactivateOldTfrs(LocalDateTime olderThan) {
        long count = tfrRepository.deactivateOld(olderThan);
        LOG.info("Deactivated " + count + " old TFRs");
    }

    private Map<String, JsonNode> fetchGeoData() {
        Map<String, JsonNode> geoMap = new HashMap<>();
        try {
            String geoJson = tfrGeoClient.getTfrFeatures(
                "WFS", "1.1.0", "GetFeature",
                "TFR:V_TFR_LOC", 300,
                "application/json", "EPSG:4326"
            );

            JsonNode root = objectMapper.readTree(geoJson);
            JsonNode features = root.get("features");
            if (features != null && features.isArray()) {
                for (JsonNode feature : features) {
                    String featureId = feature.has("id") ? feature.get("id").asText() : "";
                    // Feature ID format: "V_TFR_LOC.6/3691" -> extract "6/3691"
                    String notamId = featureId.contains(".") ?
                        featureId.substring(featureId.indexOf('.') + 1) : featureId;
                    geoMap.put(notamId, feature);
                }
            }
            LOG.info("Fetched " + geoMap.size() + " TFR geo features from WFS");
        } catch (Exception e) {
            LOG.warn("Failed to fetch TFR geo data (TFRs will lack boundaries): " + e.getMessage());
        }
        return geoMap;
    }

    private void parseDatesFromDescription(TfrEntity entity, String description) {
        if (description == null) return;
        try {
            List<String> dates = new ArrayList<>();
            Matcher matcher = DATE_PATTERN.matcher(description);
            while (matcher.find()) {
                dates.add(matcher.group(1));
            }
            if (!dates.isEmpty()) {
                entity.effectiveDate = java.time.LocalDate.parse(dates.get(0), DESCRIPTION_DATE_FORMAT)
                    .atStartOfDay();
            }
            if (dates.size() > 1) {
                entity.expireDate = java.time.LocalDate.parse(dates.get(dates.size() - 1), DESCRIPTION_DATE_FORMAT)
                    .atTime(23, 59, 59);
            }
        } catch (Exception e) {
            // Date parsing is best-effort
        }
    }

    private void extractCentroid(TfrEntity entity, JsonNode geometry) {
        try {
            JsonNode coords = geometry.get("coordinates");
            if (coords == null || !coords.isArray() || coords.isEmpty()) return;

            // For Polygon: coordinates[0] is the ring
            JsonNode ring = coords.get(0);
            if (ring == null || !ring.isArray() || ring.isEmpty()) return;

            double sumLat = 0, sumLon = 0;
            int count = 0;
            for (JsonNode point : ring) {
                if (point.isArray() && point.size() >= 2) {
                    sumLon += point.get(0).asDouble();
                    sumLat += point.get(1).asDouble();
                    count++;
                }
            }
            if (count > 0) {
                entity.latitude = BigDecimal.valueOf(sumLat / count);
                entity.longitude = BigDecimal.valueOf(sumLon / count);
            }
        } catch (Exception e) {
            // Centroid extraction is best-effort
        }
    }
}
