package com.redhat.weather.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.redhat.weather.client.OpenWeatherMapClient;
import com.redhat.weather.domain.entity.LocationEntity;
import com.redhat.weather.domain.repository.LocationRepository;
import com.redhat.weather.dto.SolarDataDTO;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.eclipse.microprofile.rest.client.inject.RestClient;
import org.jboss.logging.Logger;

import java.util.Optional;

@ApplicationScoped
public class SolarDataService {

    private static final Logger LOG = Logger.getLogger(SolarDataService.class);
    private static final ObjectMapper objectMapper = new ObjectMapper();

    @Inject
    LocationRepository locationRepository;

    @Inject
    @RestClient
    OpenWeatherMapClient openWeatherClient;

    @ConfigProperty(name = "openweather.api.key")
    Optional<String> openWeatherApiKey;

    public Optional<SolarDataDTO> getSolarData(Long locationId) {
        if (openWeatherApiKey.isEmpty()) {
            LOG.debug("OpenWeatherMap API key not configured, skipping solar data");
            return Optional.empty();
        }

        try {
            Optional<LocationEntity> locationOpt = locationRepository.findByIdOptional(locationId);
            if (locationOpt.isEmpty()) {
                LOG.warn("Location not found: " + locationId);
                return Optional.empty();
            }

            LocationEntity location = locationOpt.get();
            String response = openWeatherClient.getCurrentWeather(
                location.latitude.doubleValue(),
                location.longitude.doubleValue(),
                openWeatherApiKey.get(),
                "imperial"
            );

            JsonNode data = objectMapper.readTree(response);
            JsonNode sys = data.path("sys");

            long sunrise = sys.path("sunrise").asLong();
            long sunset = sys.path("sunset").asLong();
            int timezone = data.path("timezone").asInt(0);

            if (sunrise == 0 || sunset == 0) {
                LOG.warn("No sunrise/sunset data in response for location: " + location.name);
                return Optional.empty();
            }

            SolarDataDTO dto = new SolarDataDTO(
                location.id, location.name, sunrise, sunset, timezone
            );
            return Optional.of(dto);

        } catch (Exception e) {
            LOG.error("Error fetching solar data for location " + locationId, e);
            return Optional.empty();
        }
    }
}
