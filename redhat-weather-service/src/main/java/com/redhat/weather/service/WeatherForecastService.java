package com.redhat.weather.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.redhat.weather.client.NoaaWeatherClient;
import com.redhat.weather.client.OpenWeatherMapClient;
import com.redhat.weather.domain.entity.LocationEntity;
import com.redhat.weather.domain.entity.WeatherForecastEntity;
import com.redhat.weather.domain.repository.LocationRepository;
import com.redhat.weather.domain.repository.WeatherForecastRepository;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;
import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.eclipse.microprofile.rest.client.inject.RestClient;
import org.jboss.logging.Logger;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@ApplicationScoped
public class WeatherForecastService {

    private static final Logger LOG = Logger.getLogger(WeatherForecastService.class);
    private static final ObjectMapper objectMapper = new ObjectMapper();

    @Inject
    WeatherForecastRepository weatherForecastRepository;

    @Inject
    LocationRepository locationRepository;

    @Inject
    @RestClient
    NoaaWeatherClient noaaClient;

    @Inject
    @RestClient
    OpenWeatherMapClient openWeatherClient;

    @ConfigProperty(name = "openweather.api.key")
    Optional<String> openWeatherApiKey;

    public List<WeatherForecastEntity> getForecastsByLocation(Long locationId) {
        return weatherForecastRepository.findByLocation(locationId);
    }

    public List<WeatherForecastEntity> getForecastsByCoordinates(
            BigDecimal lat, BigDecimal lon, LocalDateTime from, LocalDateTime to) {
        return weatherForecastRepository.findByCoordinatesAndTimeRange(lat, lon, from, to);
    }

    public List<WeatherForecastEntity> getCurrentForecast(BigDecimal lat, BigDecimal lon) {
        return weatherForecastRepository.findCurrentByCoordinates(lat, lon);
    }

    @Transactional
    public void fetchAndStoreNoaaForecast(Long locationId) {
        try {
            Optional<LocationEntity> locationOpt = locationRepository.findByIdOptional(locationId);
            if (locationOpt.isEmpty()) {
                LOG.warn("Location not found: " + locationId);
                return;
            }

            LocationEntity location = locationOpt.get();
            LOG.info("Fetching NOAA forecast for location: " + location.name);

            // Get point metadata
            String pointResponse = noaaClient.getPointData(
                location.latitude.doubleValue(),
                location.longitude.doubleValue()
            );

            JsonNode pointData = objectMapper.readTree(pointResponse);
            String forecastUrl = pointData.path("properties").path("forecast").asText();

            if (forecastUrl == null || forecastUrl.isEmpty()) {
                LOG.warn("No forecast URL in NOAA point data for location: " + location.name);
                return;
            }

            // Get forecast data
            String forecastResponse = noaaClient.getForecast(forecastUrl);
            JsonNode forecastData = objectMapper.readTree(forecastResponse);

            // Parse and store forecast periods
            JsonNode periods = forecastData.path("properties").path("periods");
            List<WeatherForecastEntity> forecasts = new ArrayList<>();

            for (JsonNode period : periods) {
                WeatherForecastEntity forecast = new WeatherForecastEntity();
                forecast.location = location;
                forecast.source = "noaa";
                forecast.latitude = location.latitude;
                forecast.longitude = location.longitude;
                forecast.forecastData = period.toString();
                forecast.fetchedAt = LocalDateTime.now();

                // Extract searchable fields
                forecast.temperatureFahrenheit = BigDecimal.valueOf(period.path("temperature").asInt());
                forecast.temperatureCelsius = fahrenheitToCelsius(forecast.temperatureFahrenheit);
                forecast.windSpeedMph = parseWindSpeed(period.path("windSpeed").asText());
                forecast.windDirection = parseWindDirection(period.path("windDirection").asText());
                forecast.weatherShortDescription = period.path("shortForecast").asText();
                forecast.weatherDescription = period.path("detailedForecast").asText();

                // Parse time periods
                String startTime = period.path("startTime").asText();
                String endTime = period.path("endTime").asText();
                forecast.validFrom = parseIso8601(startTime);
                forecast.validTo = parseIso8601(endTime);
                forecast.forecastTime = LocalDateTime.now();

                forecasts.add(forecast);
            }

            weatherForecastRepository.persist(forecasts);
            LOG.info("Stored " + forecasts.size() + " NOAA forecasts for location: " + location.name);

        } catch (Exception e) {
            LOG.error("Error fetching NOAA forecast for location " + locationId, e);
        }
    }

    @Transactional
    public void fetchAndStoreOpenWeatherForecast(Long locationId) {
        if (openWeatherApiKey.isEmpty()) {
            LOG.debug("OpenWeatherMap API key not configured, skipping");
            return;
        }

        try {
            Optional<LocationEntity> locationOpt = locationRepository.findByIdOptional(locationId);
            if (locationOpt.isEmpty()) {
                LOG.warn("Location not found: " + locationId);
                return;
            }

            LocationEntity location = locationOpt.get();
            LOG.info("Fetching OpenWeatherMap forecast for location: " + location.name);

            String forecastResponse = openWeatherClient.getForecast(
                location.latitude.doubleValue(),
                location.longitude.doubleValue(),
                openWeatherApiKey.get(),
                "imperial"
            );

            JsonNode forecastData = objectMapper.readTree(forecastResponse);
            JsonNode list = forecastData.path("list");
            List<WeatherForecastEntity> forecasts = new ArrayList<>();

            for (JsonNode item : list) {
                WeatherForecastEntity forecast = new WeatherForecastEntity();
                forecast.location = location;
                forecast.source = "openweathermap";
                forecast.latitude = location.latitude;
                forecast.longitude = location.longitude;
                forecast.forecastData = item.toString();
                forecast.fetchedAt = LocalDateTime.now();

                // Extract searchable fields
                JsonNode main = item.path("main");
                forecast.temperatureFahrenheit = BigDecimal.valueOf(main.path("temp").asDouble());
                forecast.temperatureCelsius = fahrenheitToCelsius(forecast.temperatureFahrenheit);
                forecast.humidity = main.path("humidity").asInt();

                JsonNode wind = item.path("wind");
                forecast.windSpeedMph = BigDecimal.valueOf(wind.path("speed").asDouble());
                forecast.windDirection = wind.path("deg").asInt();

                JsonNode weather = item.path("weather").get(0);
                if (weather != null) {
                    forecast.weatherShortDescription = weather.path("main").asText();
                    forecast.weatherDescription = weather.path("description").asText();
                }

                // Parse timestamp
                long timestamp = item.path("dt").asLong();
                LocalDateTime validTime = LocalDateTime.ofEpochSecond(timestamp, 0,
                    java.time.ZoneOffset.UTC);
                forecast.validFrom = validTime;
                forecast.validTo = validTime.plusHours(3); // OpenWeather forecasts are 3-hour intervals
                forecast.forecastTime = LocalDateTime.now();

                forecasts.add(forecast);
            }

            weatherForecastRepository.persist(forecasts);
            LOG.info("Stored " + forecasts.size() + " OpenWeatherMap forecasts for location: " + location.name);

        } catch (Exception e) {
            LOG.error("Error fetching OpenWeatherMap forecast for location " + locationId, e);
        }
    }

    @Transactional
    public void deactivateOldForecasts(LocalDateTime olderThan) {
        long count = weatherForecastRepository.deactivateOldForecasts(olderThan);
        LOG.info("Deactivated " + count + " old forecasts");
    }

    private BigDecimal fahrenheitToCelsius(BigDecimal fahrenheit) {
        if (fahrenheit == null) return null;
        return fahrenheit.subtract(BigDecimal.valueOf(32))
            .multiply(BigDecimal.valueOf(5))
            .divide(BigDecimal.valueOf(9), 2, java.math.RoundingMode.HALF_UP);
    }

    private BigDecimal parseWindSpeed(String windSpeed) {
        try {
            // Parse "10 mph" or "10 to 20 mph"
            String[] parts = windSpeed.split(" ");
            return new BigDecimal(parts[0]);
        } catch (Exception e) {
            return null;
        }
    }

    private Integer parseWindDirection(String direction) {
        // Convert compass direction to degrees
        return switch (direction.toUpperCase()) {
            case "N" -> 0;
            case "NNE" -> 22;
            case "NE" -> 45;
            case "ENE" -> 67;
            case "E" -> 90;
            case "ESE" -> 112;
            case "SE" -> 135;
            case "SSE" -> 157;
            case "S" -> 180;
            case "SSW" -> 202;
            case "SW" -> 225;
            case "WSW" -> 247;
            case "W" -> 270;
            case "WNW" -> 292;
            case "NW" -> 315;
            case "NNW" -> 337;
            default -> null;
        };
    }

    private LocalDateTime parseIso8601(String iso8601) {
        try {
            return LocalDateTime.parse(iso8601, DateTimeFormatter.ISO_DATE_TIME);
        } catch (Exception e) {
            return LocalDateTime.now();
        }
    }
}
