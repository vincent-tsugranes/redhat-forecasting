package com.redhat.weather.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.redhat.weather.client.FaaDelayClient;
import com.redhat.weather.domain.entity.AirportDelayEntity;
import com.redhat.weather.domain.repository.AirportDelayRepository;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;
import org.eclipse.microprofile.rest.client.inject.RestClient;
import org.jboss.logging.Logger;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@ApplicationScoped
public class AirportDelayService {

    private static final Logger LOG = Logger.getLogger(AirportDelayService.class);
    private static final ObjectMapper objectMapper = new ObjectMapper();

    @Inject
    AirportDelayRepository airportDelayRepository;

    @Inject
    @RestClient
    FaaDelayClient faaDelayClient;

    public List<AirportDelayEntity> getActiveDelays() {
        return airportDelayRepository.findActiveDelays();
    }

    public List<AirportDelayEntity> getDelaysByAirport(String code) {
        return airportDelayRepository.findByAirportCode(code.toUpperCase());
    }

    @Transactional
    public void fetchAndStoreDelays() {
        try {
            String response = faaDelayClient.getAirportStatusList();
            JsonNode data = objectMapper.readTree(response);

            // Deactivate all previous entries before storing fresh data
            airportDelayRepository.deactivateAll();

            List<AirportDelayEntity> delays = new ArrayList<>();

            // FAA API returns an array of airport delay objects
            JsonNode airports = data;
            if (data.has("data")) {
                airports = data.get("data");
            }

            if (!airports.isArray()) {
                LOG.info("No airport delay data found in FAA response");
                return;
            }

            for (JsonNode airport : airports) {
                try {
                    String airportCode = airport.path("IATA").asText(airport.path("ARPT").asText(null));
                    if (airportCode == null) continue;

                    String name = airport.path("Name").asText(null);
                    boolean hasDelay = airport.path("Delay").asBoolean(false);

                    // Check for various delay types
                    JsonNode delayStatus = airport.path("Status");
                    if (delayStatus.isMissingNode() || delayStatus.isNull()) {
                        // Even if no specific delay info, record status
                        AirportDelayEntity entity = new AirportDelayEntity();
                        entity.delayId = airportCode + "-status-" + LocalDateTime.now().hashCode();
                        entity.airportCode = airportCode;
                        entity.airportName = name;
                        entity.delayType = "Status";
                        entity.isDelayed = hasDelay;
                        entity.delayData = airport.toString();
                        entity.fetchedAt = LocalDateTime.now();
                        delays.add(entity);
                        continue;
                    }

                    // Parse delay details
                    if (delayStatus.isArray()) {
                        for (JsonNode status : delayStatus) {
                            delays.add(parseDelayEntry(airportCode, name, status, hasDelay));
                        }
                    } else {
                        delays.add(parseDelayEntry(airportCode, name, delayStatus, hasDelay));
                    }

                } catch (Exception e) {
                    LOG.warn("Error parsing airport delay entry: " + e.getMessage());
                }
            }

            if (!delays.isEmpty()) {
                airportDelayRepository.persist(delays);
                long delayedCount = delays.stream().filter(d -> Boolean.TRUE.equals(d.isDelayed)).count();
                LOG.info("Stored " + delays.size() + " airport status entries (" + delayedCount + " delayed)");
            }

        } catch (Exception e) {
            LOG.error("Error fetching airport delays from FAA", e);
        }
    }

    @Transactional
    public void deactivateOldDelays(LocalDateTime olderThan) {
        long count = airportDelayRepository.deactivateOld(olderThan);
        LOG.info("Deactivated " + count + " old airport delay entries");
    }

    private AirportDelayEntity parseDelayEntry(String airportCode, String name, JsonNode status, boolean hasDelay) {
        AirportDelayEntity entity = new AirportDelayEntity();
        entity.delayId = airportCode + "-" + status.path("Type").asText("delay") + "-" + LocalDateTime.now().hashCode();
        entity.airportCode = airportCode;
        entity.airportName = name;
        entity.delayType = status.path("Type").asText(status.path("Reason").asText("Unknown"));
        entity.reason = status.path("Reason").asText(null);
        entity.isDelayed = hasDelay;
        entity.delayData = status.toString();
        entity.fetchedAt = LocalDateTime.now();

        // Parse delay duration if available
        String avgDelay = status.path("AvgDelay").asText(null);
        if (avgDelay != null) {
            entity.avgDelayMinutes = parseMinutes(avgDelay);
        }
        String minDelay = status.path("MinDelay").asText(null);
        if (minDelay != null) {
            entity.minDelayMinutes = parseMinutes(minDelay);
        }
        String maxDelay = status.path("MaxDelay").asText(null);
        if (maxDelay != null) {
            entity.maxDelayMinutes = parseMinutes(maxDelay);
        }

        entity.trend = status.path("Trend").asText(null);

        return entity;
    }

    private Integer parseMinutes(String delayStr) {
        if (delayStr == null) return null;
        try {
            // Handle formats like "45 minutes", "1 hour", etc.
            String cleaned = delayStr.toLowerCase().replaceAll("[^0-9]", " ").trim();
            String[] parts = cleaned.split("\\s+");
            if (parts.length > 0 && !parts[0].isEmpty()) {
                return Integer.parseInt(parts[0]);
            }
        } catch (Exception e) {
            // ignore
        }
        return null;
    }
}
