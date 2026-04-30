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
 * REST client for US Naval Observatory API
 * API Documentation: https://aa.usno.navy.mil/data/api
 */
@RegisterRestClient(configKey = "usno-api")
public interface UsnoClient {

    @GET
    @Path("/api/moon/phases/date")
    @Produces(MediaType.APPLICATION_JSON)
    @Retry(maxRetries = 2, delay = 1000, jitter = 500,
           retryOn = {WebApplicationException.class, IOException.class})
    @CircuitBreaker(requestVolumeThreshold = 10, failureRatio = 0.5,
                    delay = 120000, successThreshold = 3)
    String getMoonPhases(@QueryParam("date") String date,
                         @QueryParam("nump") int numPhases);
}
