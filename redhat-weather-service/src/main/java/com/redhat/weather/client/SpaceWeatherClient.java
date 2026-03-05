package com.redhat.weather.client;

import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.WebApplicationException;
import jakarta.ws.rs.core.MediaType;
import org.eclipse.microprofile.faulttolerance.CircuitBreaker;
import org.eclipse.microprofile.faulttolerance.Retry;
import org.eclipse.microprofile.faulttolerance.Timeout;
import org.eclipse.microprofile.rest.client.inject.RegisterRestClient;

import java.io.IOException;

/**
 * REST client for NOAA Space Weather Prediction Center API
 * API Documentation: https://www.swpc.noaa.gov/products-and-data
 */
@RegisterRestClient(configKey = "swpc-api")
@Path("/products")
@Timeout(10000)
public interface SpaceWeatherClient {

    @GET
    @Path("/noaa-planetary-k-index.json")
    @Produces(MediaType.APPLICATION_JSON)
    @Retry(maxRetries = 2, delay = 2000, jitter = 500,
           retryOn = {WebApplicationException.class, IOException.class})
    @CircuitBreaker(requestVolumeThreshold = 10, failureRatio = 0.5,
                    delay = 120000, successThreshold = 3)
    String getKpIndex();

    @GET
    @Path("/solar-wind/mag-2-hour.json")
    @Produces(MediaType.APPLICATION_JSON)
    @Retry(maxRetries = 2, delay = 2000, jitter = 500,
           retryOn = {WebApplicationException.class, IOException.class})
    @CircuitBreaker(requestVolumeThreshold = 10, failureRatio = 0.5,
                    delay = 120000, successThreshold = 3)
    String getSolarWindData();

    @GET
    @Path("/alerts.json")
    @Produces(MediaType.APPLICATION_JSON)
    @Retry(maxRetries = 2, delay = 2000, jitter = 500,
           retryOn = {WebApplicationException.class, IOException.class})
    @CircuitBreaker(requestVolumeThreshold = 10, failureRatio = 0.5,
                    delay = 120000, successThreshold = 3)
    String getAlerts();
}
