package com.redhat.weather.client;

import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.QueryParam;
import jakarta.ws.rs.core.MediaType;
import org.eclipse.microprofile.rest.client.inject.RegisterRestClient;

/**
 * REST client for Aviation Weather Center API
 * API Documentation: https://aviationweather.gov/data/api/
 */
@RegisterRestClient(configKey = "aviation-weather-api")
@Path("/cgi-bin/data/dataserver.php")
public interface AviationWeatherClient {

    /**
     * Get METAR data for an airport
     * @param dataSource Data source (metars)
     * @param requestType Request type (retrieve)
     * @param format Output format (xml)
     * @param stationString ICAO airport code (e.g., KJFK)
     * @param hoursBeforeNow Hours before now to retrieve
     * @return METAR data in XML format
     */
    @GET
    @Produces(MediaType.APPLICATION_XML)
    String getMETAR(@QueryParam("dataSource") String dataSource,
                    @QueryParam("requestType") String requestType,
                    @QueryParam("format") String format,
                    @QueryParam("stationString") String stationString,
                    @QueryParam("hoursBeforeNow") int hoursBeforeNow);

    /**
     * Get TAF data for an airport
     * @param dataSource Data source (tafs)
     * @param requestType Request type (retrieve)
     * @param format Output format (xml)
     * @param stationString ICAO airport code (e.g., KJFK)
     * @param hoursBeforeNow Hours before now to retrieve
     * @return TAF data in XML format
     */
    @GET
    @Produces(MediaType.APPLICATION_XML)
    String getTAF(@QueryParam("dataSource") String dataSource,
                  @QueryParam("requestType") String requestType,
                  @QueryParam("format") String format,
                  @QueryParam("stationString") String stationString,
                  @QueryParam("hoursBeforeNow") int hoursBeforeNow);
}
