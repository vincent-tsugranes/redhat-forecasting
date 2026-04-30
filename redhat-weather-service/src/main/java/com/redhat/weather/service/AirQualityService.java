package com.redhat.weather.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.redhat.weather.client.OpenWeatherMapClient;
import com.redhat.weather.domain.entity.AirQualityEntity;
import com.redhat.weather.domain.entity.LocationEntity;
import com.redhat.weather.domain.repository.AirQualityRepository;
import com.redhat.weather.domain.repository.LocationRepository;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;
import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.eclipse.microprofile.rest.client.inject.RestClient;
import org.jboss.logging.Logger;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;

@ApplicationScoped
public class AirQualityService {

    private static final Logger LOG = Logger.getLogger(AirQualityService.class);
    private static final ObjectMapper objectMapper = new ObjectMapper();

    @Inject
    AirQualityRepository airQualityRepository;

    @Inject
    LocationRepository locationRepository;

    @Inject
    @RestClient
    OpenWeatherMapClient openWeatherClient;

    @ConfigProperty(name = "openweather.api.key")
    Optional<String> openWeatherApiKey;

    @Transactional
    public void fetchAndStoreAirQuality(Long locationId) {
        if (openWeatherApiKey.isEmpty()) {
            LOG.debug("OpenWeatherMap API key not configured, skipping air quality fetch");
            return;
        }

        try {
            Optional<LocationEntity> locationOpt = locationRepository.findByIdOptional(locationId);
            if (locationOpt.isEmpty()) {
                LOG.warn("Location not found: " + locationId);
                return;
            }

            LocationEntity location = locationOpt.get();
            LOG.info("Fetching air quality data for location: " + location.name);

            String response = openWeatherClient.getAirPollution(
                location.latitude.doubleValue(),
                location.longitude.doubleValue(),
                openWeatherApiKey.get()
            );

            JsonNode root = objectMapper.readTree(response);
            JsonNode list = root.path("list");

            if (list.isMissingNode() || !list.isArray() || list.isEmpty()) {
                LOG.warn("No air quality data in response for location: " + location.name);
                return;
            }

            for (JsonNode item : list) {
                AirQualityEntity entity = new AirQualityEntity();
                entity.location = location;
                entity.latitude = location.latitude;
                entity.longitude = location.longitude;

                // Parse AQI from main object
                JsonNode main = item.path("main");
                entity.aqi = main.path("aqi").asInt();

                // Parse pollutant components
                JsonNode components = item.path("components");
                entity.co = decimalOrNull(components, "co");
                entity.no = decimalOrNull(components, "no");
                entity.no2 = decimalOrNull(components, "no2");
                entity.o3 = decimalOrNull(components, "o3");
                entity.so2 = decimalOrNull(components, "so2");
                entity.pm2_5 = decimalOrNull(components, "pm2_5");
                entity.pm10 = decimalOrNull(components, "pm10");
                entity.nh3 = decimalOrNull(components, "nh3");

                // Parse timestamp
                long timestamp = item.path("dt").asLong();
                entity.validAt = LocalDateTime.ofEpochSecond(timestamp, 0, ZoneOffset.UTC);
                entity.fetchedAt = LocalDateTime.now();
                entity.source = "openweathermap";
                entity.isActive = true;

                airQualityRepository.persist(entity);
            }

            LOG.info("Stored air quality data for location: " + location.name);

        } catch (Exception e) {
            LOG.error("Error fetching air quality data for location " + locationId, e);
        }
    }

    public Optional<AirQualityEntity> getLatestByLocation(Long locationId) {
        return airQualityRepository.findLatestByLocation(locationId);
    }

    public List<AirQualityEntity> getByCoordinates(BigDecimal lat, BigDecimal lon) {
        return airQualityRepository.findByCoordinates(lat, lon);
    }

    @Transactional
    public long deactivateOld(LocalDateTime olderThan) {
        long count = airQualityRepository.deactivateOld(olderThan);
        LOG.info("Deactivated " + count + " old air quality records");
        return count;
    }

    private BigDecimal decimalOrNull(JsonNode node, String field) {
        JsonNode value = node.path(field);
        if (value.isMissingNode() || value.isNull()) {
            return null;
        }
        return BigDecimal.valueOf(value.asDouble());
    }
}
