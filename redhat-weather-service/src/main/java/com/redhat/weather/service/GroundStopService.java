package com.redhat.weather.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.redhat.weather.client.FaaDelayClient;
import com.redhat.weather.domain.entity.GroundStopEntity;
import com.redhat.weather.domain.repository.GroundStopRepository;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;
import org.eclipse.microprofile.rest.client.inject.RestClient;
import org.jboss.logging.Logger;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

@ApplicationScoped
public class GroundStopService {

    private static final Logger LOG = Logger.getLogger(GroundStopService.class);
    private static final ObjectMapper objectMapper = new ObjectMapper();
    private static final Set<String> GROUND_STOP_TYPES = Set.of(
        "Ground Stop", "Ground Delay", "Ground Delay Program", "GDP", "GS"
    );

    @Inject
    GroundStopRepository groundStopRepository;

    @Inject
    @RestClient
    FaaDelayClient faaDelayClient;

    public List<GroundStopEntity> getActiveGroundStops() {
        return groundStopRepository.findActive();
    }

    public List<GroundStopEntity> getByAirport(String airportCode) {
        return groundStopRepository.findByAirport(airportCode.toUpperCase());
    }

    @Transactional
    public void fetchAndStoreGroundStops() {
        try {
            String response = faaDelayClient.getAirportStatusList();
            JsonNode data = objectMapper.readTree(response);

            groundStopRepository.deactivateAll();

            List<GroundStopEntity> stops = new ArrayList<>();

            JsonNode airports = data;
            if (data.has("data")) {
                airports = data.get("data");
            }

            if (!airports.isArray()) {
                LOG.info("No airport data found in FAA response for ground stops");
                return;
            }

            for (JsonNode airport : airports) {
                try {
                    String airportCode = airport.path("IATA").asText(airport.path("ARPT").asText(null));
                    if (airportCode == null) continue;

                    boolean hasDelay = airport.path("Delay").asBoolean(false);
                    if (!hasDelay) continue;

                    JsonNode delayStatus = airport.path("Status");
                    if (delayStatus.isMissingNode() || delayStatus.isNull()) continue;

                    if (delayStatus.isArray()) {
                        for (JsonNode status : delayStatus) {
                            GroundStopEntity entity = parseIfGroundStop(airportCode, airport, status);
                            if (entity != null) stops.add(entity);
                        }
                    } else {
                        GroundStopEntity entity = parseIfGroundStop(airportCode, airport, delayStatus);
                        if (entity != null) stops.add(entity);
                    }

                } catch (Exception e) {
                    LOG.warn("Error parsing ground stop entry: " + e.getMessage());
                }
            }

            if (!stops.isEmpty()) {
                groundStopRepository.persist(stops);
                LOG.info("Stored " + stops.size() + " active ground stops/GDPs");
            } else {
                LOG.info("No active ground stops found");
            }

        } catch (Exception e) {
            LOG.error("Error fetching ground stops from FAA", e);
        }
    }

    @Transactional
    public void deactivateOldEntries(LocalDateTime olderThan) {
        long count = groundStopRepository.deactivateOld(olderThan);
        LOG.info("Deactivated " + count + " old ground stop entries");
    }

    private GroundStopEntity parseIfGroundStop(String airportCode, JsonNode airport, JsonNode status) {
        String type = status.path("Type").asText(status.path("Reason").asText(""));
        boolean isGroundStop = GROUND_STOP_TYPES.stream()
            .anyMatch(gs -> type.toLowerCase().contains(gs.toLowerCase()));

        if (!isGroundStop) return null;

        try {
            GroundStopEntity entity = new GroundStopEntity();
            entity.groundStopId = airportCode + "-" + type.replaceAll("\\s+", "-") + "-" + LocalDateTime.now().hashCode();
            entity.airportCode = airportCode;
            entity.airportName = airport.path("Name").asText(null);
            entity.programType = type;
            entity.reason = status.path("Reason").asText(null);
            entity.rawData = status.toString();
            entity.fetchedAt = LocalDateTime.now();

            String avgDelay = status.path("AvgDelay").asText(null);
            if (avgDelay != null) entity.avgDelayMinutes = parseMinutes(avgDelay);
            String maxDelay = status.path("MaxDelay").asText(null);
            if (maxDelay != null) entity.maxDelayMinutes = parseMinutes(maxDelay);

            return entity;
        } catch (Exception e) {
            LOG.warn("Error creating ground stop entity: " + e.getMessage());
            return null;
        }
    }

    private Integer parseMinutes(String delayStr) {
        if (delayStr == null) return null;
        try {
            String cleaned = delayStr.toLowerCase().replaceAll("[^0-9]", " ").trim();
            String[] parts = cleaned.split("\\s+");
            if (parts.length > 0 && !parts[0].isEmpty()) {
                return Integer.parseInt(parts[0]);
            }
        } catch (Exception e) { /* ignore */ }
        return null;
    }
}
