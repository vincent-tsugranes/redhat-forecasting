package com.redhat.weather.service;

import com.redhat.weather.client.AviationWeatherClient;
import com.redhat.weather.domain.entity.PirepEntity;
import com.redhat.weather.domain.repository.PirepRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;
import org.eclipse.microprofile.rest.client.inject.RestClient;
import org.jboss.logging.Logger;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

@ApplicationScoped
public class PirepService {

    private static final Logger LOG = Logger.getLogger(PirepService.class);
    private static final ObjectMapper objectMapper = new ObjectMapper();

    @Inject
    PirepRepository pirepRepository;

    @Inject
    @RestClient
    AviationWeatherClient aviationClient;

    public List<PirepEntity> getRecentPireps() {
        return pirepRepository.findRecent();
    }

    public List<PirepEntity> getPirepsByTurbulence(String intensity) {
        return pirepRepository.findByTurbulenceIntensity(intensity.toUpperCase());
    }

    public List<PirepEntity> getPirepsByIcing(String intensity) {
        return pirepRepository.findByIcingIntensity(intensity.toUpperCase());
    }

    @Transactional
    public void fetchAndStorePireps() {
        try {
            List<AviationWeatherClient.PirepResponse> responses = aviationClient.getPIREPs(3, "json");

            if (responses == null || responses.isEmpty()) {
                LOG.info("No PIREPs returned from AWC");
                return;
            }

            List<PirepEntity> pireps = new ArrayList<>();

            for (AviationWeatherClient.PirepResponse resp : responses) {
                try {
                    if (resp.lat == null || resp.lon == null || resp.rawOb == null) continue;

                    String pirepId = generatePirepId(resp);
                    if (pirepRepository.existsByPirepId(pirepId)) continue;

                    PirepEntity entity = new PirepEntity();
                    entity.pirepId = pirepId;
                    entity.reportType = resp.pirepType != null ? resp.pirepType : "UA";
                    entity.rawText = resp.rawOb;
                    entity.observationTime = parseTime(resp.obsTime);
                    entity.latitude = BigDecimal.valueOf(resp.lat);
                    entity.longitude = BigDecimal.valueOf(resp.lon);
                    entity.altitudeFt = resp.fltlvl != null ? resp.fltlvl * 100 : null;
                    entity.aircraftType = resp.acType;
                    entity.skyCondition = resp.clouds;
                    entity.turbulenceIntensity = resp.turbInten;
                    entity.turbulenceType = resp.turbType;
                    entity.icingIntensity = resp.icgInten;
                    entity.icingType = resp.icgType;
                    entity.weatherConditions = resp.wxString;
                    entity.temperatureCelsius = resp.temp != null ? BigDecimal.valueOf(resp.temp) : null;
                    entity.windSpeedKnots = resp.wspd;
                    entity.windDirection = resp.wdir;
                    entity.visibilityMiles = resp.visib != null ? BigDecimal.valueOf(resp.visib) : null;
                    entity.pirepData = objectMapper.writeValueAsString(resp);
                    entity.fetchedAt = LocalDateTime.now();

                    pireps.add(entity);
                } catch (Exception e) {
                    LOG.warn("Error parsing PIREP: " + e.getMessage());
                }
            }

            if (!pireps.isEmpty()) {
                pirepRepository.persist(pireps);
                LOG.info("Stored " + pireps.size() + " new PIREPs");
            } else {
                LOG.info("No new PIREPs to store");
            }

        } catch (Exception e) {
            LOG.error("Error fetching PIREPs from AWC", e);
        }
    }

    @Transactional
    public void deactivateOldReports(LocalDateTime olderThan) {
        long count = pirepRepository.deactivateOld(olderThan);
        LOG.info("Deactivated " + count + " old PIREPs");
    }

    private String generatePirepId(AviationWeatherClient.PirepResponse resp) {
        String receipt = resp.receipt != null ? resp.receipt : "";
        String lat = resp.lat != null ? String.format("%.3f", resp.lat) : "";
        String lon = resp.lon != null ? String.format("%.3f", resp.lon) : "";
        String alt = resp.fltlvl != null ? String.valueOf(resp.fltlvl) : "";
        return String.valueOf((receipt + lat + lon + alt).hashCode());
    }

    private LocalDateTime parseTime(String timeStr) {
        if (timeStr == null) return LocalDateTime.now();
        try {
            return ZonedDateTime.parse(timeStr, DateTimeFormatter.ISO_DATE_TIME).toLocalDateTime();
        } catch (Exception e) {
            return LocalDateTime.now();
        }
    }
}
