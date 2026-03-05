package com.redhat.weather.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.redhat.weather.client.SpaceWeatherClient;
import com.redhat.weather.dto.SpaceWeatherDTO;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.eclipse.microprofile.rest.client.inject.RestClient;
import org.jboss.logging.Logger;

import java.util.Optional;

@ApplicationScoped
public class SpaceWeatherService {

    private static final Logger LOG = Logger.getLogger(SpaceWeatherService.class);
    private static final ObjectMapper objectMapper = new ObjectMapper();

    @Inject
    @RestClient
    SpaceWeatherClient swpcClient;

    public Optional<SpaceWeatherDTO> getSpaceWeather() {
        try {
            SpaceWeatherDTO dto = new SpaceWeatherDTO();

            // Fetch Kp Index
            try {
                String kpResponse = swpcClient.getKpIndex();
                JsonNode kpData = objectMapper.readTree(kpResponse);
                if (kpData.isArray() && kpData.size() > 1) {
                    // First row is header, last row is most recent
                    JsonNode latest = kpData.get(kpData.size() - 1);
                    double kp = latest.get(1).asDouble(0);
                    dto.setKpIndex(kp);
                }
            } catch (Exception e) {
                LOG.warn("Failed to fetch Kp index: " + e.getMessage());
                dto.setKpIndex(0);
            }

            // Fetch Solar Wind
            try {
                String windResponse = swpcClient.getSolarWindData();
                JsonNode windData = objectMapper.readTree(windResponse);
                if (windData.isArray() && windData.size() > 1) {
                    // Find the most recent non-null speed value
                    for (int i = windData.size() - 1; i > 0; i--) {
                        JsonNode row = windData.get(i);
                        if (row.isArray() && row.size() > 6) {
                            String btStr = row.get(6).asText("");
                            if (!btStr.isEmpty() && !btStr.equals("null")) {
                                try {
                                    dto.solarWindSpeed = Double.parseDouble(btStr);
                                    break;
                                } catch (NumberFormatException ignored) {
                                }
                            }
                        }
                    }
                }
            } catch (Exception e) {
                LOG.warn("Failed to fetch solar wind data: " + e.getMessage());
            }

            // Fetch Alerts
            try {
                String alertsResponse = swpcClient.getAlerts();
                JsonNode alertsData = objectMapper.readTree(alertsResponse);
                if (alertsData.isArray()) {
                    int count = Math.min(alertsData.size(), 5);
                    for (int i = 0; i < count; i++) {
                        JsonNode alert = alertsData.get(i);
                        String issueTime = alert.path("issue_datetime").asText("");
                        String message = alert.path("message").asText("");
                        if (!message.isEmpty()) {
                            // Truncate long messages
                            if (message.length() > 200) {
                                message = message.substring(0, 200) + "...";
                            }
                            dto.alerts.add(new SpaceWeatherDTO.SpaceWeatherAlert(issueTime, message));
                        }
                    }
                }
            } catch (Exception e) {
                LOG.warn("Failed to fetch space weather alerts: " + e.getMessage());
            }

            return Optional.of(dto);

        } catch (Exception e) {
            LOG.error("Error aggregating space weather data", e);
            return Optional.empty();
        }
    }
}
