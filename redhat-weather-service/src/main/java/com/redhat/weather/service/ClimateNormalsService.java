package com.redhat.weather.service;

import com.redhat.weather.domain.entity.LocationEntity;
import com.redhat.weather.domain.entity.WeatherForecastEntity;
import com.redhat.weather.domain.repository.LocationRepository;
import com.redhat.weather.domain.repository.WeatherForecastRepository;
import com.redhat.weather.dto.ClimateNormalsDTO;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.jboss.logging.Logger;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@ApplicationScoped
public class ClimateNormalsService {

    private static final Logger LOG = Logger.getLogger(ClimateNormalsService.class);

    @Inject
    LocationRepository locationRepository;

    @Inject
    WeatherForecastRepository forecastRepository;

    public Optional<ClimateNormalsDTO> getClimateNormals(Long locationId) {
        try {
            Optional<LocationEntity> locationOpt = locationRepository.findByIdOptional(locationId);
            if (locationOpt.isEmpty()) {
                LOG.warn("Location not found: " + locationId);
                return Optional.empty();
            }

            LocationEntity location = locationOpt.get();
            int currentMonth = LocalDateTime.now().getMonthValue();

            // Get historical data for this location (last 90 days)
            LocalDateTime since = LocalDateTime.now().minusDays(90);
            List<WeatherForecastEntity> historicalData =
                forecastRepository.findHistoricalByLocation(locationId, since);

            if (historicalData.size() < 10) {
                LOG.debug("Insufficient historical data for location " + location.name
                    + ": " + historicalData.size() + " samples");
                return Optional.empty();
            }

            ClimateNormalsDTO dto = new ClimateNormalsDTO(locationId, location.name, currentMonth);

            // Compute averages
            BigDecimal sumHighF = BigDecimal.ZERO;
            BigDecimal sumLowF = BigDecimal.ZERO;
            BigDecimal sumPrecip = BigDecimal.ZERO;
            BigDecimal sumHumidity = BigDecimal.ZERO;
            BigDecimal sumWind = BigDecimal.ZERO;
            long tempCount = 0;
            long precipCount = 0;
            long humidityCount = 0;
            long windCount = 0;

            BigDecimal maxTemp = null;
            BigDecimal minTemp = null;

            for (WeatherForecastEntity forecast : historicalData) {
                if (forecast.temperatureFahrenheit != null) {
                    sumHighF = sumHighF.add(forecast.temperatureFahrenheit);
                    if (maxTemp == null || forecast.temperatureFahrenheit.compareTo(maxTemp) > 0) {
                        maxTemp = forecast.temperatureFahrenheit;
                    }
                    if (minTemp == null || forecast.temperatureFahrenheit.compareTo(minTemp) < 0) {
                        minTemp = forecast.temperatureFahrenheit;
                    }
                    tempCount++;
                }
                if (forecast.precipitationProbability != null) {
                    sumPrecip = sumPrecip.add(BigDecimal.valueOf(forecast.precipitationProbability));
                    precipCount++;
                }
                if (forecast.humidity != null) {
                    sumHumidity = sumHumidity.add(BigDecimal.valueOf(forecast.humidity));
                    humidityCount++;
                }
                if (forecast.windSpeedMph != null) {
                    sumWind = sumWind.add(forecast.windSpeedMph);
                    windCount++;
                }
            }

            if (tempCount > 0) {
                dto.avgHighF = sumHighF.divide(BigDecimal.valueOf(tempCount), 1, RoundingMode.HALF_UP);
                dto.avgLowF = minTemp;
                dto.avgHighC = fahrenheitToCelsius(dto.avgHighF);
                dto.avgLowC = fahrenheitToCelsius(dto.avgLowF);
            }
            if (precipCount > 0) {
                dto.avgPrecipProbability = sumPrecip.divide(BigDecimal.valueOf(precipCount), 1, RoundingMode.HALF_UP);
            }
            if (humidityCount > 0) {
                dto.avgHumidity = sumHumidity.divide(BigDecimal.valueOf(humidityCount), 1, RoundingMode.HALF_UP);
            }
            if (windCount > 0) {
                dto.avgWindSpeedMph = sumWind.divide(BigDecimal.valueOf(windCount), 1, RoundingMode.HALF_UP);
            }

            dto.sampleCount = historicalData.size();
            return Optional.of(dto);

        } catch (Exception e) {
            LOG.error("Error computing climate normals for location " + locationId, e);
            return Optional.empty();
        }
    }

    private BigDecimal fahrenheitToCelsius(BigDecimal fahrenheit) {
        if (fahrenheit == null) return null;
        return fahrenheit.subtract(BigDecimal.valueOf(32))
            .multiply(BigDecimal.valueOf(5))
            .divide(BigDecimal.valueOf(9), 1, RoundingMode.HALF_UP);
    }
}
