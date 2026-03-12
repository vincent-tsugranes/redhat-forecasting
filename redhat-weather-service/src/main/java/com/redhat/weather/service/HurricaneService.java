package com.redhat.weather.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.redhat.weather.client.JtwcClient;
import com.redhat.weather.client.NationalHurricaneClient;
import com.redhat.weather.domain.entity.HurricaneEntity;
import com.redhat.weather.domain.repository.HurricaneRepository;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;
import org.eclipse.microprofile.rest.client.inject.RestClient;
import org.jboss.logging.Logger;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;
import java.io.ByteArrayInputStream;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@ApplicationScoped
public class HurricaneService {

    private static final Logger LOG = Logger.getLogger(HurricaneService.class);
    private static final ObjectMapper objectMapper = new ObjectMapper();

    // Patterns for parsing JTWC warning text
    private static final Pattern LAT_LON_PATTERN = Pattern.compile(
        "(\\d+\\.?\\d*)([NS])\\s+(\\d+\\.?\\d*)([EW])"
    );
    private static final Pattern WIND_PATTERN = Pattern.compile(
        "MAX\\s+SUSTAINED\\s+WINDS\\s*[-:]?\\s*(\\d+)\\s*(?:KTS|KNOTS|KT)", Pattern.CASE_INSENSITIVE
    );
    private static final Pattern STORM_NAME_PATTERN = Pattern.compile(
        "(?:TYPHOON|TROPICAL STORM|TROPICAL DEPRESSION|SUPER TYPHOON|SEVERE TROPICAL STORM|TROPICAL CYCLONE)\\s+(?:\\(([^)]+)\\)|([A-Z][A-Z0-9-]+))",
        Pattern.CASE_INSENSITIVE
    );

    @Inject
    HurricaneRepository hurricaneRepository;

    @Inject
    @RestClient
    NationalHurricaneClient nhcClient;

    @Inject
    @RestClient
    JtwcClient jtwcClient;

    public List<HurricaneEntity> getActiveStorms() {
        return hurricaneRepository.findActiveStorms();
    }

    public List<HurricaneEntity> getStormById(String stormId) {
        return hurricaneRepository.findByStormId(stormId);
    }

    public List<HurricaneEntity> getStormTrack(String stormId, LocalDateTime from, LocalDateTime to) {
        return hurricaneRepository.findStormTrack(stormId, from, to);
    }

    /**
     * Fetches storms from both NHC (Atlantic/Eastern Pacific) and JTWC (Western Pacific/Indian Ocean/Southern Hemisphere).
     */
    @Transactional
    public void fetchAndStoreActiveStorms() {
        fetchNhcStorms();
        fetchJtwcStorms();
    }

    private void fetchNhcStorms() {
        try {
            LOG.info("Fetching active storms from NHC");

            String response = nhcClient.getCurrentStorms();
            JsonNode data = objectMapper.readTree(response);

            JsonNode activeStorms = data.path("activeStorms");
            if (!activeStorms.isArray()) {
                LOG.info("No active storms from NHC");
                return;
            }

            List<HurricaneEntity> hurricanes = new ArrayList<>();

            for (JsonNode storm : activeStorms) {
                try {
                    HurricaneEntity hurricane = new HurricaneEntity();

                    // Storm identification
                    hurricane.stormId = storm.path("id").asText();
                    hurricane.stormName = storm.path("name").asText();
                    hurricane.basin = storm.path("binNumber").asText().substring(0, 2);
                    hurricane.stormNumber = Integer.parseInt(
                        storm.path("binNumber").asText().substring(2, 4)
                    );
                    hurricane.year = LocalDateTime.now().getYear();

                    // Current position
                    JsonNode latestPosition = storm.path("latestPosition");
                    if (latestPosition != null && !latestPosition.isMissingNode()) {
                        hurricane.latitude = BigDecimal.valueOf(latestPosition.path("lat").asDouble());
                        hurricane.longitude = BigDecimal.valueOf(latestPosition.path("lon").asDouble());
                    }

                    // Storm data
                    hurricane.maxSustainedWindsKnots = storm.path("intensity").path("kts").asInt();
                    hurricane.maxSustainedWindsMph = knotsToMph(hurricane.maxSustainedWindsKnots);
                    hurricane.minCentralPressureMb = storm.path("pressure").path("mb").asInt();
                    hurricane.classification = storm.path("classification").asText();
                    hurricane.status = "active";

                    // Determine category
                    hurricane.category = determineCategory(hurricane.maxSustainedWindsMph);

                    // Movement
                    JsonNode movement = storm.path("movement");
                    if (movement != null && !movement.isMissingNode()) {
                        hurricane.movementDirection = movement.path("degrees").asInt();
                        hurricane.movementSpeedKnots = BigDecimal.valueOf(movement.path("kts").asDouble());
                        hurricane.movementSpeedMph = BigDecimal.valueOf(movement.path("mph").asDouble());
                    }

                    // Time
                    String lastUpdate = storm.path("lastUpdate").asText();
                    hurricane.advisoryTime = parseIso8601(lastUpdate);
                    hurricane.forecastTime = LocalDateTime.now();
                    hurricane.fetchedAt = LocalDateTime.now();

                    // Store full storm data
                    hurricane.forecastData = storm.toString();

                    hurricanes.add(hurricane);

                } catch (Exception e) {
                    LOG.error("Error parsing NHC storm data", e);
                }
            }

            if (!hurricanes.isEmpty()) {
                hurricaneRepository.persist(hurricanes);
                LOG.info("Stored " + hurricanes.size() + " NHC storm advisories");
            }

        } catch (Exception e) {
            LOG.error("Error fetching active storms from NHC", e);
        }
    }

    /**
     * Fetches active tropical cyclones from JTWC RSS feed.
     * Covers Western Pacific (typhoons), Indian Ocean (cyclones), and Southern Hemisphere.
     */
    private void fetchJtwcStorms() {
        try {
            LOG.info("Fetching active storms from JTWC");

            String rssXml = jtwcClient.getActiveWarnings();
            List<HurricaneEntity> storms = parseJtwcRss(rssXml);

            if (!storms.isEmpty()) {
                hurricaneRepository.persist(storms);
                LOG.info("Stored " + storms.size() + " JTWC storm advisories");
            } else {
                LOG.info("No active storms from JTWC");
            }

        } catch (Exception e) {
            LOG.warn("Error fetching storms from JTWC (non-critical): " + e.getMessage());
        }
    }

    List<HurricaneEntity> parseJtwcRss(String rssXml) {
        List<HurricaneEntity> storms = new ArrayList<>();

        try {
            DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
            // Disable external entities for security
            factory.setFeature("http://apache.org/xml/features/disallow-doctype-decl", true);
            factory.setFeature("http://xml.org/sax/features/external-general-entities", false);
            factory.setFeature("http://xml.org/sax/features/external-parameter-entities", false);
            DocumentBuilder builder = factory.newDocumentBuilder();
            Document doc = builder.parse(new ByteArrayInputStream(rssXml.getBytes(StandardCharsets.UTF_8)));

            NodeList items = doc.getElementsByTagName("item");
            for (int i = 0; i < items.getLength(); i++) {
                try {
                    Element item = (Element) items.item(i);
                    String title = getElementText(item, "title");
                    String description = getElementText(item, "description");

                    // Only process tropical cyclone warnings (skip outlook, prognostic, etc.)
                    if (title == null || description == null) continue;
                    String titleUpper = title.toUpperCase();
                    if (!titleUpper.contains("TROPICAL") && !titleUpper.contains("TYPHOON")
                            && !titleUpper.contains("CYCLONE")) continue;
                    // Skip prognostic reasoning and other non-warning products
                    if (titleUpper.contains("PROGNOSTIC") || titleUpper.contains("OUTLOOK")) continue;

                    HurricaneEntity storm = parseJtwcWarning(title, description);
                    if (storm != null) {
                        storms.add(storm);
                    }
                } catch (Exception e) {
                    LOG.debug("Error parsing JTWC RSS item: " + e.getMessage());
                }
            }
        } catch (Exception e) {
            LOG.error("Error parsing JTWC RSS feed", e);
        }

        return storms;
    }

    private HurricaneEntity parseJtwcWarning(String title, String description) {
        HurricaneEntity storm = new HurricaneEntity();

        // Determine basin from title
        String titleUpper = title.toUpperCase();
        if (titleUpper.contains("WPAC") || titleUpper.contains("WESTERN PACIFIC")) {
            storm.basin = "WP";
        } else if (titleUpper.contains("IO") || titleUpper.contains("INDIAN OCEAN")
                || titleUpper.contains("NORTH INDIAN") || titleUpper.contains("ARABIAN")
                || titleUpper.contains("BAY OF BENGAL")) {
            storm.basin = "IO";
        } else if (titleUpper.contains("SHEM") || titleUpper.contains("SOUTHERN")
                || titleUpper.contains("SOUTH PACIFIC") || titleUpper.contains("SOUTH INDIAN")) {
            storm.basin = "SH";
        } else {
            // Default to WP for JTWC data
            storm.basin = "WP";
        }

        // Extract storm name
        Matcher nameMatcher = STORM_NAME_PATTERN.matcher(title + " " + description);
        if (nameMatcher.find()) {
            storm.stormName = nameMatcher.group(1) != null ? nameMatcher.group(1) : nameMatcher.group(2);
        }

        // Generate storm ID from title
        storm.stormId = "JTWC-" + title.replaceAll("[^A-Za-z0-9]", "").substring(0, Math.min(20, title.length()));

        // Extract position
        String combined = title + " " + description;
        Matcher latLonMatcher = LAT_LON_PATTERN.matcher(combined);
        if (latLonMatcher.find()) {
            double lat = Double.parseDouble(latLonMatcher.group(1));
            if ("S".equals(latLonMatcher.group(2))) lat = -lat;
            double lon = Double.parseDouble(latLonMatcher.group(3));
            if ("W".equals(latLonMatcher.group(4))) lon = -lon;
            storm.latitude = BigDecimal.valueOf(lat);
            storm.longitude = BigDecimal.valueOf(lon);
        } else {
            // Can't place on map without position
            return null;
        }

        // Extract wind speed
        Matcher windMatcher = WIND_PATTERN.matcher(combined);
        if (windMatcher.find()) {
            storm.maxSustainedWindsKnots = Integer.parseInt(windMatcher.group(1));
            storm.maxSustainedWindsMph = knotsToMph(storm.maxSustainedWindsKnots);
        }

        // Determine category
        storm.category = determineCategory(storm.maxSustainedWindsMph);
        storm.status = "active";
        storm.year = LocalDateTime.now().getYear();
        storm.advisoryTime = LocalDateTime.now();
        storm.forecastTime = LocalDateTime.now();
        storm.fetchedAt = LocalDateTime.now();
        storm.forecastData = "{\"source\":\"JTWC\",\"title\":\"" + title.replace("\"", "'") + "\"}";

        return storm;
    }

    private String getElementText(Element parent, String tagName) {
        NodeList nodes = parent.getElementsByTagName(tagName);
        if (nodes.getLength() > 0) {
            return nodes.item(0).getTextContent();
        }
        return null;
    }

    @Transactional
    public void deactivateOldAdvisories(LocalDateTime olderThan) {
        long count = hurricaneRepository.deactivateOldAdvisories(olderThan);
        LOG.info("Deactivated " + count + " old storm advisories");
    }

    private Integer knotsToMph(Integer knots) {
        if (knots == null) return null;
        return (int) Math.round(knots * 1.15078);
    }

    private Integer determineCategory(Integer windSpeedMph) {
        if (windSpeedMph == null) return 0;
        if (windSpeedMph >= 157) return 5;
        if (windSpeedMph >= 130) return 4;
        if (windSpeedMph >= 111) return 3;
        if (windSpeedMph >= 96) return 2;
        if (windSpeedMph >= 74) return 1;
        return 0; // Tropical storm
    }

    private LocalDateTime parseIso8601(String iso8601) {
        try {
            return LocalDateTime.parse(iso8601, DateTimeFormatter.ISO_DATE_TIME);
        } catch (Exception e) {
            return LocalDateTime.now();
        }
    }
}
