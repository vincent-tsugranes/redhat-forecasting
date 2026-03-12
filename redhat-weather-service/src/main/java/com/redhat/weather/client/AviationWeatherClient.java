package com.redhat.weather.client;

import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.QueryParam;
import jakarta.ws.rs.WebApplicationException;
import jakarta.ws.rs.core.MediaType;
import org.eclipse.microprofile.faulttolerance.CircuitBreaker;
import org.eclipse.microprofile.faulttolerance.Retry;
import org.eclipse.microprofile.rest.client.inject.RegisterRestClient;

import java.io.IOException;
import java.util.List;

/**
 * REST client for Aviation Weather Center API (New 2025 API)
 * API Documentation: https://aviationweather.gov/data/api/
 */
@RegisterRestClient(configKey = "aviation-weather-api")
@Path("/api/data")
public interface AviationWeatherClient {

    @GET
    @Path("/metar")
    @Produces(MediaType.APPLICATION_JSON)
    @Retry(maxRetries = 2, delay = 2000, jitter = 500,
           retryOn = {WebApplicationException.class, IOException.class})
    @CircuitBreaker(requestVolumeThreshold = 15, failureRatio = 0.5,
                    delay = 60000, successThreshold = 3)
    List<MetarResponse> getMETAR(@QueryParam("ids") String ids,
                                  @QueryParam("format") String format);

    @GET
    @Path("/taf")
    @Produces(MediaType.APPLICATION_JSON)
    @Retry(maxRetries = 2, delay = 2000, jitter = 500,
           retryOn = {WebApplicationException.class, IOException.class})
    @CircuitBreaker(requestVolumeThreshold = 15, failureRatio = 0.5,
                    delay = 60000, successThreshold = 3)
    List<TafResponse> getTAF(@QueryParam("ids") String ids,
                              @QueryParam("format") String format);

    /**
     * METAR response DTO from Aviation Weather API
     */
    class MetarResponse {
        public String metar;
        public String icaoId;
        public String reportTime;
        public Double lat;
        public Double lon;
        public Double temp;
        public Double dewp;
        public Integer wdir;
        public Integer wspd;
        public Integer wgst;
        public Double visib;
        public Double altim;
        public String flightCategory;
        public String cover;
        public Integer ceil;
        public String rawOb;
        public String wxString;
    }

    /**
     * TAF response DTO from Aviation Weather API
     */
    class TafResponse {
        public String taf;
        public String icaoId;
        public String issueTime;
        public String validTime;
        public Double lat;
        public Double lon;
        public String rawTAF;
    }

    @GET
    @Path("/pirep")
    @Produces(MediaType.APPLICATION_JSON)
    @Retry(maxRetries = 2, delay = 2000, jitter = 500,
           retryOn = {WebApplicationException.class, IOException.class})
    @CircuitBreaker(requestVolumeThreshold = 15, failureRatio = 0.5,
                    delay = 60000, successThreshold = 3)
    List<PirepResponse> getPIREPs(@QueryParam("age") int ageHours,
                                   @QueryParam("format") String format);

    @GET
    @Path("/airsigmet")
    @Produces(MediaType.APPLICATION_JSON)
    @Retry(maxRetries = 2, delay = 2000, jitter = 500,
           retryOn = {WebApplicationException.class, IOException.class})
    @CircuitBreaker(requestVolumeThreshold = 15, failureRatio = 0.5,
                    delay = 60000, successThreshold = 3)
    List<AirSigmetResponse> getAirSigmets(@QueryParam("format") String format);

    class PirepResponse {
        public String receipt;
        public String obsTime;
        public Double lat;
        public Double lon;
        public Integer fltlvl;
        public String acType;
        public String icgInten;
        public String icgType;
        public String turbInten;
        public String turbType;
        public String wxString;
        public Double temp;
        public Integer wdir;
        public Integer wspd;
        public Double visib;
        public String clouds;
        public String rawOb;
        public String pirepType;
    }

    class AirSigmetResponse {
        public String airSigmetType;
        public String hazard;
        public String severity;
        public String validTimeFrom;
        public String validTimeTo;
        public Integer altitudeLow;
        public Integer altitudeHi;
        public String rawAirSigmet;
        public java.util.List<CoordPair> coords;
    }

    class CoordPair {
        public Double lat;
        public Double lon;
    }

    @GET
    @Path("/cwa")
    @Produces(MediaType.APPLICATION_JSON)
    @Retry(maxRetries = 2, delay = 2000, jitter = 500,
           retryOn = {WebApplicationException.class, IOException.class})
    @CircuitBreaker(requestVolumeThreshold = 15, failureRatio = 0.5,
                    delay = 60000, successThreshold = 3)
    List<CwaResponse> getCWAs(@QueryParam("format") String format);

    @GET
    @Path("/isigmet")
    @Produces(MediaType.APPLICATION_JSON)
    @Retry(maxRetries = 2, delay = 2000, jitter = 500,
           retryOn = {WebApplicationException.class, IOException.class})
    @CircuitBreaker(requestVolumeThreshold = 15, failureRatio = 0.5,
                    delay = 60000, successThreshold = 3)
    List<IntlSigmetResponse> getInternationalSigmets(@QueryParam("format") String format);

    @GET
    @Path("/windtemp")
    @Produces(MediaType.APPLICATION_JSON)
    @Retry(maxRetries = 2, delay = 2000, jitter = 500,
           retryOn = {WebApplicationException.class, IOException.class})
    @CircuitBreaker(requestVolumeThreshold = 15, failureRatio = 0.5,
                    delay = 60000, successThreshold = 3)
    List<WindTempResponse> getWindsAloft(@QueryParam("format") String format);

    class CwaResponse {
        public String cwsu;
        public String hazard;
        public String qualifier;
        public String validTimeFrom;
        public String validTimeTo;
        public Integer base;
        public Integer top;
        public String cwaText;
        public java.util.List<CoordPair> coords;
    }

    class IntlSigmetResponse {
        public String isigmetId;
        public String firId;
        public String firName;
        public String hazard;
        public String qualifier;
        public String validTimeFrom;
        public String validTimeTo;
        public Integer altitudeLow;
        public Integer altitudeHi;
        public String rawSigmet;
        public java.util.List<CoordPair> coords;
    }

    class WindTempResponse {
        public String stationId;
        public Double lat;
        public Double lon;
        public Integer elev;
        public String validTime;
        public Integer fcstHr;
        public java.util.Map<String, WindTempForecast> fcsts;
    }

    class WindTempForecast {
        public Integer wdir;
        public Integer wspd;
        public Double temp;
    }
}
