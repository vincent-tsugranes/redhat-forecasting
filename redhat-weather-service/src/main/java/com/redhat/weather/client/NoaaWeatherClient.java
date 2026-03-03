package com.redhat.weather.client;

import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.WebApplicationException;
import jakarta.ws.rs.core.MediaType;
import org.eclipse.microprofile.faulttolerance.CircuitBreaker;
import org.eclipse.microprofile.faulttolerance.Retry;
import org.eclipse.microprofile.faulttolerance.Timeout;
import org.eclipse.microprofile.rest.client.inject.RegisterRestClient;

import java.io.IOException;

/**
 * REST client for NOAA Weather API
 * API Documentation: https://www.weather.gov/documentation/services-web-api
 */
@RegisterRestClient(configKey = "noaa-api")
@Timeout(5000)
public interface NoaaWeatherClient {

    @GET
    @Path("/points/{latitude},{longitude}")
    @Produces(MediaType.APPLICATION_JSON)
    @Retry(maxRetries = 3, delay = 2000, jitter = 500,
           retryOn = {WebApplicationException.class, IOException.class})
    @CircuitBreaker(requestVolumeThreshold = 10, failureRatio = 0.5,
                    delay = 60000, successThreshold = 3)
    String getPointData(@PathParam("latitude") double latitude,
                        @PathParam("longitude") double longitude);

    @GET
    @Produces(MediaType.APPLICATION_JSON)
    @Retry(maxRetries = 2, delay = 3000, jitter = 1000,
           retryOn = {WebApplicationException.class, IOException.class})
    @CircuitBreaker(requestVolumeThreshold = 10, failureRatio = 0.5,
                    delay = 60000, successThreshold = 3)
    String getForecast(@jakarta.ws.rs.QueryParam("url") String forecastUrl);

    @GET
    @Path("/alerts/active")
    @Produces(MediaType.APPLICATION_JSON)
    @Retry(maxRetries = 2, delay = 2000, jitter = 500,
           retryOn = {WebApplicationException.class, IOException.class})
    @CircuitBreaker(requestVolumeThreshold = 8, failureRatio = 0.5,
                    delay = 120000, successThreshold = 2)
    String getActiveAlerts();
}
