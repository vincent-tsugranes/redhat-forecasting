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
 * REST client for Joint Typhoon Warning Center (JTWC)
 * Provides tropical cyclone data for Western Pacific, Indian Ocean, and Southern Hemisphere basins.
 * Data source: METOC (Naval Meteorology and Oceanography Command)
 */
@RegisterRestClient(configKey = "jtwc-api")
public interface JtwcClient {

    @GET
    @Path("/jtwc/rss/jtwc.rss")
    @Produces(MediaType.APPLICATION_XML)
    @Retry(maxRetries = 2, delay = 5000, jitter = 2000,
           retryOn = {WebApplicationException.class, IOException.class})
    @CircuitBreaker(requestVolumeThreshold = 4, failureRatio = 0.75,
                    delay = 600000, successThreshold = 2)
    String getActiveWarnings();
}
