package com.redhat.weather.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.redhat.weather.client.UsgsEarthquakeClient;
import com.redhat.weather.domain.entity.EarthquakeEntity;
import com.redhat.weather.domain.repository.EarthquakeRepository;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;
import org.eclipse.microprofile.rest.client.inject.RestClient;
import org.jboss.logging.Logger;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

@ApplicationScoped
public class EarthquakeService {

    private static final Logger LOG = Logger.getLogger(EarthquakeService.class);
    private static final ObjectMapper objectMapper = new ObjectMapper();

    @Inject
    EarthquakeRepository earthquakeRepository;

    @Inject
    @RestClient
    UsgsEarthquakeClient usgsClient;

    public List<EarthquakeEntity> getRecentEarthquakes() {
        return earthquakeRepository.findRecent();
    }

    public List<EarthquakeEntity> getSignificantEarthquakes() {
        return earthquakeRepository.findSignificant();
    }

    @Transactional
    public void fetchAndStoreEarthquakes() {
        try {
            LOG.info("Fetching recent earthquakes from USGS");

            String startTime = LocalDateTime.now(ZoneOffset.UTC)
                .minusHours(24)
                .format(DateTimeFormatter.ISO_DATE_TIME);

            String response = usgsClient.getRecentEarthquakes(
                "geojson", startTime, 2.5, "time"
            );

            JsonNode data = objectMapper.readTree(response);
            JsonNode features = data.path("features");

            if (!features.isArray()) {
                LOG.info("No earthquake features found in USGS response");
                return;
            }

            List<EarthquakeEntity> earthquakes = new ArrayList<>();

            for (JsonNode feature : features) {
                try {
                    String usgsId = feature.path("id").asText();

                    // Skip if already stored
                    if (earthquakeRepository.existsByUsgsId(usgsId)) {
                        continue;
                    }

                    JsonNode props = feature.path("properties");
                    JsonNode geometry = feature.path("geometry");
                    JsonNode coords = geometry.path("coordinates");

                    EarthquakeEntity eq = new EarthquakeEntity();
                    eq.usgsId = usgsId;
                    eq.magnitude = BigDecimal.valueOf(props.path("mag").asDouble());
                    eq.place = props.path("place").asText(null);

                    long timeMs = props.path("time").asLong();
                    eq.eventTime = LocalDateTime.ofInstant(
                        Instant.ofEpochMilli(timeMs), ZoneOffset.UTC
                    );

                    eq.longitude = BigDecimal.valueOf(coords.get(0).asDouble());
                    eq.latitude = BigDecimal.valueOf(coords.get(1).asDouble());
                    eq.depthKm = BigDecimal.valueOf(coords.get(2).asDouble());

                    eq.magnitudeType = props.path("magType").asText(null);
                    eq.status = props.path("status").asText(null);
                    eq.tsunami = props.path("tsunami").asInt(0) == 1;

                    int feltValue = props.path("felt").asInt(-1);
                    eq.felt = feltValue >= 0 ? feltValue : null;

                    double cdiValue = props.path("cdi").asDouble(-1);
                    eq.cdi = cdiValue >= 0 ? BigDecimal.valueOf(cdiValue) : null;

                    String alertValue = props.path("alert").asText(null);
                    eq.alert = (alertValue != null && !alertValue.equals("null")) ? alertValue : null;

                    int sigValue = props.path("sig").asInt(-1);
                    eq.significance = sigValue >= 0 ? sigValue : null;

                    eq.eventData = feature.toString();
                    eq.fetchedAt = LocalDateTime.now();

                    earthquakes.add(eq);

                } catch (Exception e) {
                    LOG.error("Error parsing earthquake feature", e);
                }
            }

            if (!earthquakes.isEmpty()) {
                earthquakeRepository.persist(earthquakes);
                LOG.info("Stored " + earthquakes.size() + " new earthquakes from USGS");
            } else {
                LOG.info("No new earthquakes to store");
            }

        } catch (Exception e) {
            LOG.error("Error fetching earthquakes from USGS", e);
        }
    }

    @Transactional
    public void deactivateOldEvents(LocalDateTime olderThan) {
        long count = earthquakeRepository.deactivateOld(olderThan);
        LOG.info("Deactivated " + count + " old earthquake events");
    }
}
