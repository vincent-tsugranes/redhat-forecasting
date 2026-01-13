package com.redhat.weather.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.redhat.weather.client.NationalHurricaneClient;
import com.redhat.weather.domain.entity.HurricaneEntity;
import com.redhat.weather.domain.repository.HurricaneRepository;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;
import org.eclipse.microprofile.rest.client.inject.RestClient;
import org.jboss.logging.Logger;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

@ApplicationScoped
public class HurricaneService {

    private static final Logger LOG = Logger.getLogger(HurricaneService.class);
    private static final ObjectMapper objectMapper = new ObjectMapper();

    @Inject
    HurricaneRepository hurricaneRepository;

    @Inject
    @RestClient
    NationalHurricaneClient nhcClient;

    public List<HurricaneEntity> getActiveStorms() {
        return hurricaneRepository.findActiveStorms();
    }

    public List<HurricaneEntity> getStormById(String stormId) {
        return hurricaneRepository.findByStormId(stormId);
    }

    public List<HurricaneEntity> getStormTrack(String stormId, LocalDateTime from, LocalDateTime to) {
        return hurricaneRepository.findStormTrack(stormId, from, to);
    }

    @Transactional
    public void fetchAndStoreActiveStorms() {
        try {
            LOG.info("Fetching active storms from NHC");

            String response = nhcClient.getCurrentStorms();
            JsonNode data = objectMapper.readTree(response);

            JsonNode activeStorms = data.path("activeStorms");
            if (!activeStorms.isArray()) {
                LOG.info("No active storms found");
                return;
            }

            List<HurricaneEntity> hurricanes = new ArrayList<>();

            for (JsonNode storm : activeStorms) {
                try {
                    HurricaneEntity hurricane = new HurricaneEntity();

                    // Storm identification
                    hurricane.stormId = storm.path("id").asText();
                    hurricane.stormName = storm.path("name").asText();
                    hurricane.basin = storm.path("binNumber").asText().substring(0, 2);
                    hurricane.stormNumber = Integer.parseInt(
                        storm.path("binNumber").asText().substring(2, 4)
                    );
                    hurricane.year = LocalDateTime.now().getYear();

                    // Current position
                    JsonNode latestPosition = storm.path("latestPosition");
                    if (latestPosition != null && !latestPosition.isMissingNode()) {
                        hurricane.latitude = BigDecimal.valueOf(latestPosition.path("lat").asDouble());
                        hurricane.longitude = BigDecimal.valueOf(latestPosition.path("lon").asDouble());
                    }

                    // Storm data
                    hurricane.maxSustainedWindsKnots = storm.path("intensity").path("kts").asInt();
                    hurricane.maxSustainedWindsMph = knotsToMph(hurricane.maxSustainedWindsKnots);
                    hurricane.minCentralPressureMb = storm.path("pressure").path("mb").asInt();
                    hurricane.classification = storm.path("classification").asText();
                    hurricane.status = "active";

                    // Determine category
                    hurricane.category = determineCategory(hurricane.maxSustainedWindsMph);

                    // Movement
                    JsonNode movement = storm.path("movement");
                    if (movement != null && !movement.isMissingNode()) {
                        hurricane.movementDirection = movement.path("degrees").asInt();
                        hurricane.movementSpeedKnots = BigDecimal.valueOf(movement.path("kts").asDouble());
                        hurricane.movementSpeedMph = BigDecimal.valueOf(movement.path("mph").asDouble());
                    }

                    // Time
                    String lastUpdate = storm.path("lastUpdate").asText();
                    hurricane.advisoryTime = parseIso8601(lastUpdate);
                    hurricane.forecastTime = LocalDateTime.now();
                    hurricane.fetchedAt = LocalDateTime.now();

                    // Store full storm data
                    hurricane.forecastData = storm.toString();

                    hurricanes.add(hurricane);

                } catch (Exception e) {
                    LOG.error("Error parsing storm data", e);
                }
            }

            if (!hurricanes.isEmpty()) {
                hurricaneRepository.persist(hurricanes);
                LOG.info("Stored " + hurricanes.size() + " hurricane advisories");
            }

        } catch (Exception e) {
            LOG.error("Error fetching active storms from NHC", e);
        }
    }

    @Transactional
    public void deactivateOldAdvisories(LocalDateTime olderThan) {
        long count = hurricaneRepository.deactivateOldAdvisories(olderThan);
        LOG.info("Deactivated " + count + " old hurricane advisories");
    }

    private Integer knotsToMph(Integer knots) {
        if (knots == null) return null;
        return (int) Math.round(knots * 1.15078);
    }

    private Integer determineCategory(Integer windSpeedMph) {
        if (windSpeedMph == null) return 0;
        if (windSpeedMph >= 157) return 5;
        if (windSpeedMph >= 130) return 4;
        if (windSpeedMph >= 111) return 3;
        if (windSpeedMph >= 96) return 2;
        if (windSpeedMph >= 74) return 1;
        return 0; // Tropical storm
    }

    private LocalDateTime parseIso8601(String iso8601) {
        try {
            return LocalDateTime.parse(iso8601, DateTimeFormatter.ISO_DATE_TIME);
        } catch (Exception e) {
            return LocalDateTime.now();
        }
    }
}
