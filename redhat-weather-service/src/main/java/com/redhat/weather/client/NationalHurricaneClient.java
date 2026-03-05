package com.redhat.weather.client;

import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.WebApplicationException;
import jakarta.ws.rs.core.MediaType;
import org.eclipse.microprofile.faulttolerance.CircuitBreaker;
import org.eclipse.microprofile.faulttolerance.Retry;
import org.eclipse.microprofile.rest.client.inject.RegisterRestClient;

import java.io.IOException;

/**
 * REST client for National Hurricane Center API
 * API Documentation: https://www.nhc.noaa.gov/
 */
@RegisterRestClient(configKey = "nhc-api")
public interface NationalHurricaneClient {

    @GET
    @Path("/CurrentStorms.json")
    @Produces(MediaType.APPLICATION_JSON)
    @Retry(maxRetries = 2, delay = 5000, jitter = 2000,
           retryOn = {WebApplicationException.class, IOException.class})
    @CircuitBreaker(requestVolumeThreshold = 4, failureRatio = 0.5,
                    delay = 300000, successThreshold = 2)
    String getCurrentStorms();

    @GET
    @Path("/gis-at.xml")
    @Produces(MediaType.APPLICATION_XML)
    @Retry(maxRetries = 2, delay = 5000, jitter = 2000,
           retryOn = {WebApplicationException.class, IOException.class})
    @CircuitBreaker(requestVolumeThreshold = 4, failureRatio = 0.5,
                    delay = 300000, successThreshold = 2)
    String getAtlanticStorms();
}
