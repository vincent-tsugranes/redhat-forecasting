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

/**
 * REST client for Sunrise-Sunset API
 * API Documentation: https://sunrise-sunset.org/api
 */
@RegisterRestClient(configKey = "sunrise-sunset-api")
public interface SunriseSunsetClient {

    @GET
    @Path("/json")
    @Produces(MediaType.APPLICATION_JSON)
    @Retry(maxRetries = 2, delay = 1000, jitter = 500,
           retryOn = {WebApplicationException.class, IOException.class})
    @CircuitBreaker(requestVolumeThreshold = 10, failureRatio = 0.5,
                    delay = 120000, successThreshold = 3)
    String getSunData(@QueryParam("lat") double lat,
                      @QueryParam("lng") double lon,
                      @QueryParam("date") String date,
                      @QueryParam("formatted") int formatted);
}
