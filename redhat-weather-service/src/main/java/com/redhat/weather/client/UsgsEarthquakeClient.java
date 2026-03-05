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

/**
 * REST client for USGS Earthquake Hazards Program API
 * API Documentation: https://earthquake.usgs.gov/fdsnws/event/1/
 */
@RegisterRestClient(configKey = "usgs-earthquake-api")
@Path("/fdsnws/event/1")
@Timeout(10000)
public interface UsgsEarthquakeClient {

    @GET
    @Path("/query")
    @Produces(MediaType.APPLICATION_JSON)
    @Retry(maxRetries = 2, delay = 2000, jitter = 500,
           retryOn = {WebApplicationException.class, IOException.class})
    @CircuitBreaker(requestVolumeThreshold = 10, failureRatio = 0.5,
                    delay = 120000, successThreshold = 3)
    String getRecentEarthquakes(@QueryParam("format") String format,
                                @QueryParam("starttime") String starttime,
                                @QueryParam("minmagnitude") double minmagnitude,
                                @QueryParam("orderby") String orderby);
}
