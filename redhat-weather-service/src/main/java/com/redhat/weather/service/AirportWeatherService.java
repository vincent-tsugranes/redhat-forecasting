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
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import java.io.ByteArrayInputStream;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Optional;

@ApplicationScoped
public class AirportWeatherService {

    private static final Logger LOG = Logger.getLogger(AirportWeatherService.class);
    private static final DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();

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

            String metarResponse = aviationClient.getMETAR(
                "metars",
                "retrieve",
                "xml",
                airportCode,
                2
            );

            parseAndStoreAirportWeather(metarResponse, location, "METAR");

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

            String tafResponse = aviationClient.getTAF(
                "tafs",
                "retrieve",
                "xml",
                airportCode,
                6
            );

            parseAndStoreAirportWeather(tafResponse, location, "TAF");

        } catch (Exception e) {
            LOG.error("Error fetching TAF for airport " + airportCode, e);
        }
    }

    private void parseAndStoreAirportWeather(String xmlResponse, LocationEntity location, String reportType) {
        try {
            DocumentBuilder builder = factory.newDocumentBuilder();
            Document doc = builder.parse(new ByteArrayInputStream(xmlResponse.getBytes()));

            String dataTag = reportType.equals("METAR") ? "METAR" : "TAF";
            NodeList dataNodes = doc.getElementsByTagName(dataTag);

            for (int i = 0; i < dataNodes.getLength(); i++) {
                Element dataElement = (Element) dataNodes.item(i);

                AirportWeatherEntity weather = new AirportWeatherEntity();
                weather.location = location;
                weather.airportCode = location.airportCode;
                weather.latitude = location.latitude;
                weather.longitude = location.longitude;
                weather.reportType = reportType;
                weather.fetchedAt = LocalDateTime.now();

                // Get raw text
                weather.rawText = getElementText(dataElement, "raw_text");

                // Get observation time
                String obsTime = getElementText(dataElement, "observation_time");
                weather.observationTime = parseIso8601(obsTime);

                // Store full XML as JSON (simplified for now)
                if (reportType.equals("METAR")) {
                    weather.metarData = xmlResponse;
                } else {
                    weather.tafData = xmlResponse;
                }

                // Extract fields
                weather.temperatureCelsius = getElementDecimal(dataElement, "temp_c");
                weather.dewpointCelsius = getElementDecimal(dataElement, "dewpoint_c");
                weather.windSpeedKnots = getElementInteger(dataElement, "wind_speed_kt");
                weather.windDirection = getElementInteger(dataElement, "wind_dir_degrees");
                weather.windGustKnots = getElementInteger(dataElement, "wind_gust_kt");
                weather.visibilityMiles = getElementDecimal(dataElement, "visibility_statute_mi");
                weather.altimeterInches = getElementDecimal(dataElement, "altim_in_hg");
                weather.flightCategory = getElementText(dataElement, "flight_category");
                weather.skyCondition = getElementText(dataElement, "sky_condition");
                weather.weatherConditions = getElementText(dataElement, "wx_string");

                airportWeatherRepository.persist(weather);
            }

            LOG.info("Stored " + dataNodes.getLength() + " " + reportType + " reports for " + location.airportCode);

        } catch (Exception e) {
            LOG.error("Error parsing " + reportType + " XML", e);
        }
    }

    @Transactional
    public void deactivateOldReports(LocalDateTime olderThan) {
        long count = airportWeatherRepository.deactivateOldReports(olderThan);
        LOG.info("Deactivated " + count + " old airport weather reports");
    }

    private String getElementText(Element parent, String tagName) {
        NodeList nodeList = parent.getElementsByTagName(tagName);
        if (nodeList.getLength() > 0) {
            return nodeList.item(0).getTextContent();
        }
        return null;
    }

    private Integer getElementInteger(Element parent, String tagName) {
        String text = getElementText(parent, tagName);
        if (text != null && !text.isEmpty()) {
            try {
                return Integer.parseInt(text);
            } catch (NumberFormatException e) {
                return null;
            }
        }
        return null;
    }

    private BigDecimal getElementDecimal(Element parent, String tagName) {
        String text = getElementText(parent, tagName);
        if (text != null && !text.isEmpty()) {
            try {
                return new BigDecimal(text);
            } catch (NumberFormatException e) {
                return null;
            }
        }
        return null;
    }

    private LocalDateTime parseIso8601(String iso8601) {
        try {
            return LocalDateTime.parse(iso8601, DateTimeFormatter.ISO_DATE_TIME);
        } catch (Exception e) {
            return LocalDateTime.now();
        }
    }
}
