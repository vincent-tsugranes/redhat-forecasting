package com.redhat.weather.service;

import com.redhat.weather.client.AviationWeatherClient;
import com.redhat.weather.domain.entity.AirportWeatherEntity;
import com.redhat.weather.domain.entity.LocationEntity;
import com.redhat.weather.domain.repository.AirportWeatherRepository;
import com.redhat.weather.domain.repository.LocationRepository;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;
import org.eclipse.microprofile.rest.client.inject.RestClient;
import org.jboss.logging.Logger;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Optional;

@ApplicationScoped
public class AirportWeatherService {

    private static final Logger LOG = Logger.getLogger(AirportWeatherService.class);

    @Inject
    AirportWeatherRepository airportWeatherRepository;

    @Inject
    LocationRepository locationRepository;

    @Inject
    @RestClient
    AviationWeatherClient aviationClient;

    public List<AirportWeatherEntity> getAirportWeather(String airportCode) {
        return airportWeatherRepository.findByAirportCode(airportCode);
    }

    public Optional<AirportWeatherEntity> getLatestMetar(String airportCode) {
        return airportWeatherRepository.findLatestMetar(airportCode);
    }

    public Optional<AirportWeatherEntity> getLatestTaf(String airportCode) {
        return airportWeatherRepository.findLatestTaf(airportCode);
    }

    @Transactional
    public void fetchAndStoreMETAR(String airportCode) {
        try {
            Optional<LocationEntity> locationOpt = locationRepository.findByAirportCode(airportCode);
            if (locationOpt.isEmpty()) {
                LOG.warn("Location not found for airport: " + airportCode);
                return;
            }

            LocationEntity location = locationOpt.get();
            LOG.info("Fetching METAR for airport: " + airportCode);

            List<AviationWeatherClient.MetarResponse> metarResponses = aviationClient.getMETAR(
                airportCode,
                "json"
            );

            if (metarResponses != null && !metarResponses.isEmpty()) {
                for (AviationWeatherClient.MetarResponse metar : metarResponses) {
                    storeMetarData(metar, location);
                }
                LOG.info("Stored " + metarResponses.size() + " METAR reports for " + airportCode);
            } else {
                LOG.warn("No METAR data available for airport: " + airportCode);
            }

        } catch (Exception e) {
            LOG.error("Error fetching METAR for airport " + airportCode, e);
        }
    }

    @Transactional
    public void fetchAndStoreTAF(String airportCode) {
        try {
            Optional<LocationEntity> locationOpt = locationRepository.findByAirportCode(airportCode);
            if (locationOpt.isEmpty()) {
                LOG.warn("Location not found for airport: " + airportCode);
                return;
            }

            LocationEntity location = locationOpt.get();
            LOG.info("Fetching TAF for airport: " + airportCode);

            List<AviationWeatherClient.TafResponse> tafResponses = aviationClient.getTAF(
                airportCode,
                "json"
            );

            if (tafResponses != null && !tafResponses.isEmpty()) {
                for (AviationWeatherClient.TafResponse taf : tafResponses) {
                    storeTafData(taf, location);
                }
                LOG.info("Stored " + tafResponses.size() + " TAF reports for " + airportCode);
            } else {
                LOG.warn("No TAF data available for airport: " + airportCode);
            }

        } catch (Exception e) {
            LOG.error("Error fetching TAF for airport " + airportCode, e);
        }
    }

    private void storeMetarData(AviationWeatherClient.MetarResponse metar, LocationEntity location) {
        AirportWeatherEntity weather = new AirportWeatherEntity();
        weather.location = location;
        weather.airportCode = metar.icaoId != null ? metar.icaoId : location.airportCode;
        weather.latitude = metar.lat != null ? BigDecimal.valueOf(metar.lat) : location.latitude;
        weather.longitude = metar.lon != null ? BigDecimal.valueOf(metar.lon) : location.longitude;
        weather.reportType = "METAR";
        weather.fetchedAt = LocalDateTime.now();
        weather.rawText = metar.rawOb;

        // Parse observation time
        if (metar.reportTime != null) {
            weather.observationTime = parseIso8601(metar.reportTime);
        }

        // Set weather data
        weather.temperatureCelsius = metar.temp != null ? BigDecimal.valueOf(metar.temp) : null;
        weather.dewpointCelsius = metar.dewp != null ? BigDecimal.valueOf(metar.dewp) : null;
        weather.windSpeedKnots = metar.wspd;
        weather.windDirection = metar.wdir;
        weather.windGustKnots = metar.wgst;
        weather.visibilityMiles = metar.visib != null ? BigDecimal.valueOf(metar.visib) : null;
        weather.altimeterInches = metar.altim != null ? BigDecimal.valueOf(metar.altim) : null;
        weather.flightCategory = metar.flightCategory;
        weather.ceilingFeet = metar.ceil;
        weather.skyCondition = metar.cover;

        airportWeatherRepository.persist(weather);
    }

    private void storeTafData(AviationWeatherClient.TafResponse taf, LocationEntity location) {
        AirportWeatherEntity weather = new AirportWeatherEntity();
        weather.location = location;
        weather.airportCode = taf.icaoId != null ? taf.icaoId : location.airportCode;
        weather.latitude = taf.lat != null ? BigDecimal.valueOf(taf.lat) : location.latitude;
        weather.longitude = taf.lon != null ? BigDecimal.valueOf(taf.lon) : location.longitude;
        weather.reportType = "TAF";
        weather.fetchedAt = LocalDateTime.now();
        weather.rawText = taf.rawTAF;

        // Parse issue time
        if (taf.issueTime != null) {
            weather.observationTime = parseIso8601(taf.issueTime);
        }

        airportWeatherRepository.persist(weather);
    }

    @Transactional
    public void deactivateOldReports(LocalDateTime olderThan) {
        long count = airportWeatherRepository.deactivateOldReports(olderThan);
        LOG.info("Deactivated " + count + " old airport weather reports");
    }

    private LocalDateTime parseIso8601(String iso8601) {
        try {
            return LocalDateTime.parse(iso8601, DateTimeFormatter.ISO_DATE_TIME);
        } catch (Exception e) {
            return LocalDateTime.now();
        }
    }
}
