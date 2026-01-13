package com.redhat.weather.client;

import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import org.eclipse.microprofile.rest.client.inject.RegisterRestClient;

/**
 * REST client for National Hurricane Center API
 * API Documentation: https://www.nhc.noaa.gov/
 */
@RegisterRestClient(configKey = "nhc-api")
public interface NationalHurricaneClient {

    /**
     * Get current active storms
     * @return JSON with active tropical systems
     */
    @GET
    @Path("/CurrentStorms.json")
    @Produces(MediaType.APPLICATION_JSON)
    String getCurrentStorms();

    /**
     * Get GeoJSON data for active storms
     * @return GeoJSON with storm positions and forecasts
     */
    @GET
    @Path("/gis-at.xml")
    @Produces(MediaType.APPLICATION_XML)
    String getAtlanticStorms();
}
