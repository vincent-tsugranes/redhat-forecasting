package com.redhat.weather.client;

import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.QueryParam;
import jakarta.ws.rs.core.MediaType;
import org.eclipse.microprofile.rest.client.inject.RegisterRestClient;

/**
 * REST client for OpenWeatherMap API
 * API Documentation: https://openweathermap.org/api
 */
@RegisterRestClient(configKey = "openweather-api")
@Path("/data/2.5")
public interface OpenWeatherMapClient {

    /**
     * Get 5-day weather forecast
     * @param lat Latitude
     * @param lon Longitude
     * @param appid API key
     * @param units Units of measurement (imperial, metric, standard)
     * @return Forecast data in JSON format
     */
    @GET
    @Path("/forecast")
    @Produces(MediaType.APPLICATION_JSON)
    String getForecast(@QueryParam("lat") double lat,
                      @QueryParam("lon") double lon,
                      @QueryParam("appid") String appid,
                      @QueryParam("units") String units);

    /**
     * Get current weather data
     * @param lat Latitude
     * @param lon Longitude
     * @param appid API key
     * @param units Units of measurement (imperial, metric, standard)
     * @return Current weather data in JSON format
     */
    @GET
    @Path("/weather")
    @Produces(MediaType.APPLICATION_JSON)
    String getCurrentWeather(@QueryParam("lat") double lat,
                            @QueryParam("lon") double lon,
                            @QueryParam("appid") String appid,
                            @QueryParam("units") String units);
}
