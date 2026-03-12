package com.redhat.weather.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.redhat.weather.client.BlitzortungClient;
import com.redhat.weather.domain.entity.LightningStrikeEntity;
import com.redhat.weather.domain.repository.LightningRepository;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;
import org.eclipse.microprofile.rest.client.inject.RestClient;
import org.jboss.logging.Logger;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;

@ApplicationScoped
public class LightningService {

    private static final Logger LOG = Logger.getLogger(LightningService.class);
    private static final ObjectMapper objectMapper = new ObjectMapper();

    @Inject
    LightningRepository lightningRepository;

    @Inject
    @RestClient
    BlitzortungClient blitzortungClient;

    public List<LightningStrikeEntity> getRecentStrikes() {
        return lightningRepository.findRecent();
    }

    public long getRecentCount() {
        return lightningRepository.countRecent();
    }

    @Transactional
    public void fetchAndStoreStrikes() {
        try {
            String response = blitzortungClient.getRecentStrikes(15);
            JsonNode data = objectMapper.readTree(response);

            if (!data.isArray() || data.isEmpty()) {
                LOG.info("No lightning strikes returned from API");
                return;
            }

            List<LightningStrikeEntity> strikes = new ArrayList<>();
            int skipped = 0;

            for (JsonNode strike : data) {
                try {
                    String strikeId = generateStrikeId(strike);
                    if (lightningRepository.existsByStrikeId(strikeId)) {
                        skipped++;
                        continue;
                    }

                    LightningStrikeEntity entity = new LightningStrikeEntity();
                    entity.strikeId = strikeId;
                    entity.latitude = BigDecimal.valueOf(strike.path("lat").asDouble());
                    entity.longitude = BigDecimal.valueOf(strike.path("lon").asDouble());
                    entity.strikeTime = parseStrikeTime(strike);
                    entity.amplitudeKa = strike.has("sig") ? strike.path("sig").asDouble() / 1000.0 : null;
                    entity.strikeType = strike.path("type").asText(null);
                    entity.rawData = strike.toString();
                    entity.fetchedAt = LocalDateTime.now();

                    strikes.add(entity);
                } catch (Exception e) {
                    LOG.warn("Error parsing lightning strike: " + e.getMessage());
                }
            }

            if (!strikes.isEmpty()) {
                lightningRepository.persist(strikes);
                LOG.info("Stored " + strikes.size() + " new lightning strikes (skipped " + skipped + " duplicates)");
            } else {
                LOG.info("No new lightning strikes to store (" + skipped + " duplicates skipped)");
            }

        } catch (Exception e) {
            LOG.error("Error fetching lightning data", e);
        }
    }

    @Transactional
    public void deactivateOldStrikes(LocalDateTime olderThan) {
        long count = lightningRepository.deactivateOld(olderThan);
        if (count > 0) {
            LOG.info("Deactivated " + count + " old lightning strikes");
        }
    }

    private String generateStrikeId(JsonNode strike) {
        long time = strike.path("time").asLong(strike.path("t").asLong(0));
        double lat = strike.path("lat").asDouble();
        double lon = strike.path("lon").asDouble();
        return String.valueOf((time + "-" + lat + "-" + lon).hashCode());
    }

    private LocalDateTime parseStrikeTime(JsonNode strike) {
        long time = strike.path("time").asLong(strike.path("t").asLong(0));
        if (time > 1_000_000_000_000L) {
            // Milliseconds
            return LocalDateTime.ofInstant(Instant.ofEpochMilli(time), ZoneOffset.UTC);
        } else if (time > 0) {
            // Seconds
            return LocalDateTime.ofInstant(Instant.ofEpochSecond(time), ZoneOffset.UTC);
        }
        return LocalDateTime.now();
    }
}
