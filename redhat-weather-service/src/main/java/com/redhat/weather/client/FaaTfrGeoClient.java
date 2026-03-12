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
 * REST client for FAA TFR GeoServer WFS endpoint
 * Returns GeoJSON FeatureCollection with TFR boundaries
 */
@RegisterRestClient(configKey = "faa-tfr-geo-api")
@Path("/geoserver/TFR/ows")
public interface FaaTfrGeoClient {

    @GET
    @Produces(MediaType.APPLICATION_JSON)
    @Retry(maxRetries = 2, delay = 3000, jitter = 1000,
           retryOn = {WebApplicationException.class, IOException.class})
    @CircuitBreaker(requestVolumeThreshold = 10, failureRatio = 0.5,
                    delay = 120000, successThreshold = 3)
    String getTfrFeatures(
        @QueryParam("service") String service,
        @QueryParam("version") String version,
        @QueryParam("request") String request,
        @QueryParam("typeName") String typeName,
        @QueryParam("maxFeatures") int maxFeatures,
        @QueryParam("outputFormat") String outputFormat,
        @QueryParam("srsname") String srsName
    );
}
