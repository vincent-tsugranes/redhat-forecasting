package com.redhat.weather.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.redhat.weather.client.OpenWeatherMapClient;
import com.redhat.weather.client.SunriseSunsetClient;
import com.redhat.weather.client.UsnoClient;
import com.redhat.weather.dto.AstronomicalDTO;
import io.quarkus.cache.CacheResult;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.eclipse.microprofile.rest.client.inject.RestClient;
import org.jboss.logging.Logger;

import java.util.ArrayList;
import java.util.Optional;

@ApplicationScoped
public class AstronomicalService {

    private static final Logger LOG = Logger.getLogger(AstronomicalService.class);
    private static final ObjectMapper objectMapper = new ObjectMapper();

    @Inject @RestClient SunriseSunsetClient sunriseSunsetClient;
    @Inject @RestClient UsnoClient usnoClient;
    @Inject @RestClient OpenWeatherMapClient openWeatherClient;

    @ConfigProperty(name = "openweather.api.key") Optional<String> openWeatherApiKey;

    @CacheResult(cacheName = "astronomical-cache")
    public AstronomicalDTO getAstronomicalData(double lat, double lon, String date) {
        AstronomicalDTO dto = new AstronomicalDTO();

        // Fetch sun data
        try {
            String sunResponse = sunriseSunsetClient.getSunData(lat, lon, date, 0);
            JsonNode sunData = objectMapper.readTree(sunResponse);
            JsonNode results = sunData.path("results");
            dto.sunrise = results.path("sunrise").asText(null);
            dto.sunset = results.path("sunset").asText(null);
            dto.solarNoon = results.path("solar_noon").asText(null);
            dto.dayLength = results.path("day_length").asText(null);
            dto.civilTwilightBegin = results.path("civil_twilight_begin").asText(null);
            dto.civilTwilightEnd = results.path("civil_twilight_end").asText(null);
            dto.nauticalTwilightBegin = results.path("nautical_twilight_begin").asText(null);
            dto.nauticalTwilightEnd = results.path("nautical_twilight_end").asText(null);
        } catch (Exception e) {
            LOG.warn("Failed to fetch sun data", e);
        }

        // Fetch moon phases
        try {
            String moonResponse = usnoClient.getMoonPhases(date, 4);
            JsonNode moonData = objectMapper.readTree(moonResponse);
            JsonNode phases = moonData.path("phasedata");
            dto.nextMoonPhases = new ArrayList<>();
            if (phases.isArray()) {
                for (JsonNode phase : phases) {
                    String phaseName = phase.path("phase").asText();
                    String phaseDate = phase.path("year").asText() + "-" +
                        String.format("%02d", phase.path("month").asInt()) + "-" +
                        String.format("%02d", phase.path("day").asInt());
                    String phaseTime = phase.path("time").asText("");
                    dto.nextMoonPhases.add(new AstronomicalDTO.MoonPhaseDTO(phaseName, phaseDate, phaseTime));
                }
                // Determine current moon phase from the closest phase
                if (!dto.nextMoonPhases.isEmpty()) {
                    dto.moonPhaseName = dto.nextMoonPhases.get(0).phase;
                }
            }
        } catch (Exception e) {
            LOG.warn("Failed to fetch moon phase data", e);
        }

        // Fetch UV index
        if (openWeatherApiKey.isPresent()) {
            try {
                String uvResponse = openWeatherClient.getUvIndex(lat, lon, openWeatherApiKey.get());
                JsonNode uvData = objectMapper.readTree(uvResponse);
                dto.uvIndex = uvData.path("value").asDouble();
            } catch (Exception e) {
                LOG.warn("Failed to fetch UV index", e);
            }
        }

        return dto;
    }
}
