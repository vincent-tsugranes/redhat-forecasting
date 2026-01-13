package com.redhat.weather.client;

import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import org.eclipse.microprofile.rest.client.inject.RegisterRestClient;

/**
 * REST client for NOAA Weather API
 * API Documentation: https://www.weather.gov/documentation/services-web-api
 */
@RegisterRestClient(configKey = "noaa-api")
public interface NoaaWeatherClient {

    /**
     * Get point metadata for a location
     * @param latitude Latitude in decimal degrees
     * @param longitude Longitude in decimal degrees
     * @return Point metadata including forecast URLs
     */
    @GET
    @Path("/points/{latitude},{longitude}")
    @Produces(MediaType.APPLICATION_JSON)
    String getPointData(@PathParam("latitude") double latitude,
                        @PathParam("longitude") double longitude);

    /**
     * Get forecast for a grid point
     * Note: The forecastUrl should be extracted from the point data response
     * Example URL: https://api.weather.gov/gridpoints/TOP/31,80/forecast
     */
    @GET
    @Produces(MediaType.APPLICATION_JSON)
    String getForecast(@jakarta.ws.rs.QueryParam("url") String forecastUrl);
}
