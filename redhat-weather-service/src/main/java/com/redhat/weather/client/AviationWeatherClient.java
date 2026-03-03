package com.redhat.weather.client;

import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.QueryParam;
import jakarta.ws.rs.WebApplicationException;
import jakarta.ws.rs.core.MediaType;
import org.eclipse.microprofile.faulttolerance.CircuitBreaker;
import org.eclipse.microprofile.faulttolerance.Retry;
import org.eclipse.microprofile.faulttolerance.Timeout;
import org.eclipse.microprofile.rest.client.inject.RegisterRestClient;

import java.io.IOException;
import java.util.List;

/**
 * REST client for Aviation Weather Center API (New 2025 API)
 * API Documentation: https://aviationweather.gov/data/api/
 */
@RegisterRestClient(configKey = "aviation-weather-api")
@Path("/api/data")
@Timeout(5000)
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
}
