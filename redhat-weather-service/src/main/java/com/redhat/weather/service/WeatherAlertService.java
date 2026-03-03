package com.redhat.weather.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.redhat.weather.client.NoaaWeatherClient;
import com.redhat.weather.domain.entity.WeatherAlertEntity;
import com.redhat.weather.domain.repository.WeatherAlertRepository;
import io.quarkus.cache.CacheInvalidateAll;
import io.quarkus.cache.CacheResult;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;
import org.eclipse.microprofile.rest.client.inject.RestClient;
import org.jboss.logging.Logger;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Optional;

@ApplicationScoped
public class WeatherAlertService {

    private static final Logger LOG = Logger.getLogger(WeatherAlertService.class);
    private static final ObjectMapper objectMapper = new ObjectMapper();

    @Inject
    WeatherAlertRepository alertRepository;

    @Inject
    @RestClient
    NoaaWeatherClient noaaClient;

    @CacheResult(cacheName = "alerts-active")
    public List<WeatherAlertEntity> getActiveAlerts() {
        return alertRepository.findActiveAlerts();
    }

    @CacheResult(cacheName = "alerts-by-severity")
    public List<WeatherAlertEntity> getAlertsBySeverity(String severity) {
        return alertRepository.findBySeverity(severity);
    }

    @Transactional
    @CacheInvalidateAll(cacheName = "alerts-active")
    @CacheInvalidateAll(cacheName = "alerts-by-severity")
    public void fetchAndStoreAlerts() {
        try {
            LOG.info("Fetching active weather alerts from NOAA");

            String response = noaaClient.getActiveAlerts();
            JsonNode data = objectMapper.readTree(response);

            JsonNode features = data.path("features");
            if (!features.isArray()) {
                LOG.info("No alert features found in response");
                return;
            }

            int newCount = 0;
            int updatedCount = 0;

            for (JsonNode feature : features) {
                try {
                    JsonNode properties = feature.path("properties");
                    String alertId = properties.path("id").asText();

                    if (alertId == null || alertId.isEmpty()) continue;

                    Optional<WeatherAlertEntity> existing = alertRepository.findByAlertId(alertId);

                    WeatherAlertEntity alert;
                    if (existing.isPresent()) {
                        alert = existing.get();
                        updatedCount++;
                    } else {
                        alert = new WeatherAlertEntity();
                        alert.alertId = alertId;
                        newCount++;
                    }

                    alert.event = properties.path("event").asText("Unknown");
                    alert.headline = nullIfEmpty(properties.path("headline").asText(null));
                    alert.description = nullIfEmpty(properties.path("description").asText(null));
                    alert.severity = nullIfEmpty(properties.path("severity").asText(null));
                    alert.certainty = nullIfEmpty(properties.path("certainty").asText(null));
                    alert.urgency = nullIfEmpty(properties.path("urgency").asText(null));
                    alert.areaDesc = nullIfEmpty(properties.path("areaDesc").asText(null));
                    alert.senderName = nullIfEmpty(properties.path("senderName").asText(null));

                    String effective = properties.path("effective").asText(null);
                    if (effective != null) {
                        alert.effective = parseIso8601(effective);
                    }

                    String expires = properties.path("expires").asText(null);
                    if (expires != null) {
                        alert.expires = parseIso8601(expires);
                    }

                    alert.alertData = feature.toString();
                    alert.isActive = true;
                    alert.fetchedAt = LocalDateTime.now();

                    alertRepository.persist(alert);

                } catch (Exception e) {
                    LOG.error("Error parsing alert data", e);
                }
            }

            LOG.info("Weather alerts fetch completed. New: " + newCount + ", Updated: " + updatedCount);

        } catch (Exception e) {
            LOG.error("Error fetching weather alerts from NOAA", e);
        }
    }

    @Transactional
    @CacheInvalidateAll(cacheName = "alerts-active")
    @CacheInvalidateAll(cacheName = "alerts-by-severity")
    public void deactivateExpired() {
        long count = alertRepository.deactivateExpired();
        if (count > 0) {
            LOG.info("Deactivated " + count + " expired weather alerts");
        }
    }

    private String nullIfEmpty(String value) {
        return (value != null && !value.isEmpty()) ? value : null;
    }

    private LocalDateTime parseIso8601(String iso8601) {
        try {
            return LocalDateTime.parse(iso8601, DateTimeFormatter.ISO_OFFSET_DATE_TIME);
        } catch (Exception e) {
            try {
                return LocalDateTime.parse(iso8601, DateTimeFormatter.ISO_DATE_TIME);
            } catch (Exception e2) {
                return LocalDateTime.now();
            }
        }
    }
}
